package com.puxtech.reuters.rfa.Consumer;
import com.puxtech.reuters.rfa.Common.Configuration;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.ElementIterator;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.EventQueue;
import com.bloomberglp.blpapi.Identity;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.Subscription;
import com.bloomberglp.blpapi.SubscriptionList;
import com.bloomberglp.blpapi.Event.EventType;
import com.puxtech.reuters.rfa.Common.ConfigNode;
import com.puxtech.reuters.rfa.Common.QuoteSource;
import com.puxtech.reuters.rfa.Common.SpreadConfig;
import com.puxtech.reuters.rfa.Common.SpreadConsumer;
import com.puxtech.reuters.rfa.Common.SubNode;

public class BloombergSpreadConsumer implements SpreadConsumer, Runnable {
	private static final Log monitorLog = LogFactory.getLog("moniter");
	private static final Log bloombergLog = LogFactory.getLog("Bloomberg");
	private QuoteSource quoteSource = null;
	private SpreadConfig spreadConfig = null;

	private String host = "123.150.11.59";
	private int port = 8194;
	private String host2 = "123.150.11.59";
	private int port2 = 8194;
	private static final String AUTH_SVC = "//blp/apiauth";
	private static final String MKTDATA_SVC = "//blp/mktdata";

	private static final Name AUTHORIZATION_SUCCESS = Name
			.getName("AuthorizationSuccess");
	private static final Name TOKEN_SUCCESS = Name
			.getName("TokenGenerationSuccess");

	private BloombergAuthType d_authOption = BloombergAuthType.APPLICATION;
	private String d_name = "SZQHP:PROD";
	private Identity d_identity;
	private Session d_session;
	private int initCount = 1;
	private int createSessionCount = 1;
	private int authCount = 1;
	private int subscribeCount = 1;

	private void initSpreadConfig(){
		monitorLog.info("init Count = " + initCount++);
		if(this.quoteSource != null){
			d_authOption = BloombergAuthType.valueOf(quoteSource.getProperty("AuthOption"));
			d_name = quoteSource.getProperty("AuthName");
			host = this.quoteSource.getProperty("Addr");
			port = this.quoteSource.getPropertyInt("Port");
			host2 = this.quoteSource.getProperty("Addr2");
			port2 = this.quoteSource.getPropertyInt("Port2");
		}
	}
	
	public void setSpreadConfig(SpreadConfig spreadConfig) {
		this.spreadConfig = spreadConfig;
	}

	public Map<String, BigDecimal> getClosePriceMap() {
		return closePriceMap;
	}

	public Map<String, BigDecimal> getLastPriceMap() {
		return lastPriceMap;
	}

	private boolean createSession() throws IOException, InterruptedException {
		monitorLog.info("createSession Count = " + createSessionCount++);
		String authOptions = null;
		switch(d_authOption){
		case APPLICATION:
			// Set Application Authentication Option
			authOptions = "AuthenticationMode=APPLICATION_ONLY;";
			authOptions += "ApplicationAuthenticationType=APPNAME_AND_KEY;";
			// ApplicationName is the entry in EMRS.
			authOptions += "ApplicationName=" + d_name;
			break;
		case USER_APP:
			// Set User and Application Authentication Option
			authOptions = "AuthenticationMode=USER_AND_APPLICATION;";
			authOptions += "AuthenticationType=OS_LOGON;";
			authOptions += "ApplicationAuthenticationType=APPNAME_AND_KEY;";
			// ApplicationName is the entry in EMRS.
			authOptions += "ApplicationName=" + d_name;
			break;
		case DIRSVC:
			// Authenticate user using active directory service
			// property
			authOptions = "AuthenticationType=DIRECTORY_SERVICE;";
			authOptions += "DirSvcPropertyName=" + d_name;
			break;
		case NONE:
			break;
		default:
			authOptions = "AuthenticationType=OS_LOGON";
		}

		bloombergLog.info("authOptions = " + authOptions);
		SessionOptions sessionOptions = new SessionOptions();
		if (d_authOption != null) {
			sessionOptions.setAuthenticationOptions(authOptions);
		}

		sessionOptions.setServerHost(host);
		sessionOptions.setServerPort(port);
		sessionOptions.setAutoRestartOnDisconnection(true);

		d_session = new Session(sessionOptions);

		return d_session.start();
	}

	private boolean authorize() throws IOException, InterruptedException {
		monitorLog.info("auth Count = " + authCount++);
		Event event;
		MessageIterator msgIter;
		EventQueue tokenEventQueue = new EventQueue();
		CorrelationID corrlationId = new CorrelationID(99);
		d_session.generateToken(corrlationId, tokenEventQueue);
		String token = null;
		int timeoutMilliSeonds = 10000;
		event = tokenEventQueue.nextEvent(timeoutMilliSeonds);
		if (event.eventType() == EventType.TOKEN_STATUS) {
			MessageIterator iter = event.messageIterator();
			while (iter.hasNext()) {
				Message msg = iter.next();
				bloombergLog.info(msg.toString());
				if (msg.messageType() == TOKEN_SUCCESS) {
					token = msg.getElementAsString("token");
				}
			}
		}
		if (token == null) {
			bloombergLog.error("Failed to get token");
			return false;
		}

		if (d_session.openService(AUTH_SVC)) {
			Service authService = d_session.getService(AUTH_SVC);
			Request authRequest = authService.createAuthorizationRequest();
			authRequest.set("token", token);

			EventQueue authEventQueue = new EventQueue();

			d_session.sendAuthorizationRequest(authRequest, d_identity, authEventQueue, new CorrelationID(d_identity));

			while (true) {
				event = authEventQueue.nextEvent();
				if (event.eventType() == EventType.RESPONSE
						|| event.eventType() == EventType.PARTIAL_RESPONSE
						|| event.eventType() == EventType.REQUEST_STATUS) {
					msgIter = event.messageIterator();
					while (msgIter.hasNext()) {
						Message msg = msgIter.next();
						bloombergLog.info(msg);
						if (msg.messageType() == AUTHORIZATION_SUCCESS) {
							return true;
						} else {
							bloombergLog.info("Not authorized");
							return false;
						}
					}
				}
			}
		}
		return false;
	}

	private void subscribe() throws Exception {
//		int correlationId = 2;
		monitorLog.info("subscribe Count = " + subscribeCount++);
		if (this.spreadConfig != null) {
			SubscriptionList subscriptions = new SubscriptionList();
			String subCode = "PREV_CLOSE_VALUE_REALTIME,LAST_PRICE,MKTDATA_EVENT_TYPE,MKTDATA_EVENT_SUBTYPE";
			for (ConfigNode configNode : this.spreadConfig.getConfigList()) {
				for(SubNode node : configNode.getNodeList()){
					String bloombergCode = node.getContractCode();
					CorrelationID subscriptionID = new CorrelationID(d_session.createIdentity());
					subscriptions.add(new Subscription(bloombergCode, subCode,"",subscriptionID));
				}
			}
			d_session.subscribe(subscriptions, d_identity);
			int updateCount = 0;
			while (true) {
				if(isRestart){
					monitorLog.info("BloombergSpreadConsumer restart now!");
					break;
				}
				Event event = d_session.nextEvent();
				switch (event.eventType().intValue()) {
				case Event.EventType.Constants.SUBSCRIPTION_DATA:
					handleDataEvent(event, updateCount++);
					break;
				default:
					handleOtherEvent(event);
					break;
				}
			}
		}
	}
	
	private Map<String, BigDecimal> closePriceMap = new HashMap<String, BigDecimal>();
	private Map<String, BigDecimal> lastPriceMap = new HashMap<String, BigDecimal>();

	private void handleDataEvent(Event event, int updateCount)  {
		MessageIterator iter = event.messageIterator();
		while (iter.hasNext()) {
			Message message = iter.next();
			bloombergLog.info(message);
			String eventType = message.getElementAsString("MKTDATA_EVENT_TYPE");
			String eventSubType = message.getElementAsString("MKTDATA_EVENT_SUBTYPE");
			String bloombergCode = message.topicName();
			if(message.hasElement("PREV_CLOSE_VALUE_REALTIME")){
				bloombergLog.info("bloombergCode="+bloombergCode);
				bloombergLog.info("PREV_CLOSE_VALUE_REALTIME="+message.getElementAsString("PREV_CLOSE_VALUE_REALTIME"));
				bloombergLog.info("MKTDATA_EVENT_TYPE="+message.getElementAsString("MKTDATA_EVENT_TYPE"));
				bloombergLog.info("MKTDATA_EVENT_SUBTYPE="+message.getElementAsString("MKTDATA_EVENT_SUBTYPE"));
				closePriceMap.put(bloombergCode, new BigDecimal(message.getElementAsString("PREV_CLOSE_VALUE_REALTIME")));
				bloombergLog.info("更新收盘价：" + bloombergCode +":" + new BigDecimal(message.getElementAsString("PREV_CLOSE_VALUE_REALTIME")));
			}
			if(message.hasElement("LAST_PRICE") && "TRADE".equalsIgnoreCase(eventType) && "NEW".equalsIgnoreCase(eventSubType)){
				bloombergLog.info("bloombergCode="+bloombergCode);
				bloombergLog.info("LAST_PRICE="+message.getElementAsString("LAST_PRICE"));
				lastPriceMap.put(bloombergCode, new BigDecimal(message.getElementAsFloat64("LAST_PRICE")));
				bloombergLog.info("更新最新价：" + bloombergCode +":" +new BigDecimal(message.getElementAsFloat64("LAST_PRICE")));
			}
		}
	}

	private void handleOtherEvent(Event event) {
		MessageIterator iter = event.messageIterator();
		while (iter.hasNext()) {
			Message message = iter.next();
			bloombergLog.info("get a Other Event! \r\n");
			bloombergLog.info("correlationID=" + message.correlationID() + ","
					+ "messageType=" + message.messageType());
			if("SessionTerminated" == message.messageType().toString()){
				bloombergLog.info("Terminating: " + message.messageType());
				this.setRestart(true);					
			}else if("SubscriptionTerminated" == message.messageType().toString()){
				bloombergLog.info("Terminating: " + message.messageType());
				this.setRestart(true);
			}
		}
	}

	public void setRestart(boolean isRestart) {
		this.isRestart = isRestart;
	}

	private boolean isRestart = false;
	@Override
	public void run() {
		//重启线程测试代码
		/*		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(20000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				setRestart(true);
			}
		}).start();*/
		try {
			while(true){
				this.initSpreadConfig();
				if(this.spreadConfig != null){
					if(!createSession()){
						monitorLog.error("Failed to start session.");
						Thread.sleep(2000);
						continue;
					}
					d_identity = d_session.createIdentity();
					if (!authorize()) {
						Thread.sleep(2000);
						continue;
					}
					monitorLog.info("Connected successfully.");

					if (!d_session.openService(MKTDATA_SVC)) {
						monitorLog.error("Failed to open " + MKTDATA_SVC);
						d_session.stop();
						Thread.sleep(2000);
						continue;
					}else{
						try {
							this.subscribe();
						} catch (Exception e) {
							monitorLog.error("subscribe失败", e);
						}
						d_session.stop();
						isRestart = false;
					}
				}else{
					monitorLog.info("BloombergConsumer的Contract列表为空，Consumer线程即将结束...");
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	public void setQuoteSource(QuoteSource quoteSource) {
		this.quoteSource = quoteSource;
	}
	
	public void shutdown() {
		
	}
	
	public void restart() {
		this.isRestart = true;
	}
	
	public static void main(String[] agrs){
		
	}
}
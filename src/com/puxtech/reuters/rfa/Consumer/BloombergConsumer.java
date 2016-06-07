package com.puxtech.reuters.rfa.Consumer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bloomberglp.blpapi.AbstractSession.StopOption;
import com.bloomberglp.blpapi.CorrelationID;
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
import com.bloomberglp.blpapi.SessionOptions.ServerAddress;
import com.bloomberglp.blpapi.Subscription;
import com.bloomberglp.blpapi.SubscriptionList;
import com.bloomberglp.blpapi.Event.EventType;
import com.lmax.disruptor.RingBuffer;
import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.Contract;
import com.puxtech.reuters.rfa.Common.Quote;
import com.puxtech.reuters.rfa.Common.QuoteSource;
import com.puxtech.reuters.rfa.RelayServer.QuoteEvent;
import com.puxtech.reuters.rfa.RelayServer.RelayServer;

public class BloombergConsumer implements ConsumerConfig, Runnable {
	private static final Log monitorLog = LogFactory.getLog("moniter");
	private static final Log bloombergLog = LogFactory.getLog("Bloomberg");
	private QuoteSource quoteSource = null;
	private List<Contract> contractList = new ArrayList<Contract>();
	private Map<String, Contract> contractMap = new HashMap<String, Contract>();

	@Override
	public void setQuoteSource(QuoteSource quoteSource) {
		this.quoteSource = quoteSource;
	}

	@Override
	public void shutdown() {
		this.stop = true;
		if(this.d_session != null){
			try {
				this.d_session.stop();
			} catch (InterruptedException e) {
				monitorLog.error("bloombergConsumer stop Interrupted!");
			}
		}
	}

	@Override
	public void restart() {
		this.isRestart = true;
	}

	private String host = "123.150.11.59";
	private int port = 8194;
	private String host2 = null;
	private int port2 = -1;
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

	private void initBloombergConfig(){
		monitorLog.info("init Count = " + initCount++);
		if(this.quoteSource != null){
			d_authOption = BloombergAuthType.valueOf(quoteSource.getProperty("AuthOption"));
			d_name = quoteSource.getProperty("AuthName");
			host = this.quoteSource.getProperty("Addr");
			port = this.quoteSource.getPropertyInt("Port");
			if(this.quoteSource.containsProperty("Addr2") && this.quoteSource.containsProperty("Port2")){				
				host2 = this.quoteSource.getProperty("Addr2");
				port2 = this.quoteSource.getPropertyInt("Port2");
			}
			List<Contract> contracts = Configuration.getInstance().getQuoteSourceContractMap().get(this.quoteSource.getName());
			this.contractList.clear();
			this.contractMap.clear();
			for (Contract contract : contracts) {
				if(contract.getSourceName().equals(this.quoteSource.getName()) && !contract.isVirtual()){
					this.contractList.add(contract);
					this.contractMap.put(contract.getSourceCfg().get("BloombergCode"), contract);
					RelayServer.getConsumerMap().put(contract.getExchangeCode(), this);
				}
			}
		}
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
		ServerAddress[] addresses;
		if(this.host2 != null){			
			addresses = new ServerAddress[]{new ServerAddress(host, port), new ServerAddress(host2, port2)};
		}else{
			addresses = new ServerAddress[]{new ServerAddress(host, port)};
		}
		sessionOptions.setServerAddresses(addresses);
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
		if (this.contractList != null) {
			SubscriptionList subscriptions = new SubscriptionList();
			String subCode = "LAST_PRICE,MKTDATA_EVENT_TYPE,MKTDATA_EVENT_SUBTYPE";
			for (Contract contract : this.contractList) {
				if(contract.getPriceAlgorithm() == 1){
					subCode = "LAST_PRICE,MKTDATA_EVENT_TYPE,MKTDATA_EVENT_SUBTYPE";
				}else if(contract.getPriceAlgorithm() == 2){
					subCode = "BID,ASK,MKTDATA_EVENT_TYPE,MKTDATA_EVENT_SUBTYPE";
				}
				Map<String, String> sourceCfg = contract.getSourceCfg();
				String bloombergCode = sourceCfg.get("BloombergCode");
				CorrelationID subscriptionID = new CorrelationID(d_session.createIdentity());
				subscriptions.add(new Subscription(bloombergCode, subCode,"",subscriptionID));
			}
			d_session.subscribe(subscriptions, d_identity);
			int updateCount = 0;
			while (!stop) {
				if(isRestart){
					monitorLog.info("BloombergConsumer restart now!");
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
	
	private Map<String, BigDecimal> bidMap = new HashMap<String, BigDecimal>();
	private Map<String, BigDecimal> askMap = new HashMap<String, BigDecimal>();

	private void handleDataEvent(Event event, int updateCount)  {
		MessageIterator iter = event.messageIterator();
		while (iter.hasNext()) {
			Message message = iter.next();
			bloombergLog.info(message);
			String eventType = message.getElementAsString("MKTDATA_EVENT_TYPE");
			String eventSubType = message.getElementAsString("MKTDATA_EVENT_SUBTYPE");
			String bloombergCode = message.topicName();
/*			if(message.hasElement("PREV_CLOSE_VALUE_REALTIME")){
				bloombergLog.info("bloombergCode="+bloombergCode);
				bloombergLog.info("PREV_CLOSE_VALUE_REALTIME="+message.getElementAsString("PREV_CLOSE_VALUE_REALTIME"));
				bloombergLog.info("MKTDATA_EVENT_TYPE="+message.getElementAsString("MKTDATA_EVENT_TYPE"));
				bloombergLog.info("MKTDATA_EVENT_SUBTYPE="+message.getElementAsString("MKTDATA_EVENT_SUBTYPE"));
				closePriceMap.put(bloombergCode, new BigDecimal(message.getElementAsString("PREV_CLOSE_VALUE_REALTIME")));
			}*/
			Contract contract = this.contractMap.get(bloombergCode);
			if(contract != null){
				Quote quote = null;
				if(contract.getPriceAlgorithm() == 1){
					if("TRADE".equalsIgnoreCase(eventType) && "NEW".equalsIgnoreCase(eventSubType) && message.hasElement("LAST_PRICE")){						
						quote = new Quote();
						quote.priceTime = new Date();
						quote.exchangeCode = contract.getExchangeCode();
						quote.reutersCode = bloombergCode;
						quote.newPrice = new BigDecimal(message.getElementAsFloat64("LAST_PRICE"));
					}
				}else if(contract.getPriceAlgorithm() == 2){
					if("QUOTE".equalsIgnoreCase(eventType) && "BID".equalsIgnoreCase(eventSubType)){
						//only bid price
						if(message.hasElement("BID")){
							BigDecimal bidPrice = new BigDecimal(message.getElementAsFloat64("BID"));
							bidMap.put(contract.getExchangeCode(), bidPrice);
							BigDecimal askPrice = askMap.get(contract.getExchangeCode());
							if(askPrice !=  null){
								quote = new Quote();
								quote.priceTime = new Date();
								quote.exchangeCode = contract.getExchangeCode();
								quote.reutersCode = bloombergCode;
								quote.bidPrice = bidPrice;
								quote.askPrice = askPrice;
								int scale = contract.getScale();
								quote.newPrice = quote.askPrice.add(quote.bidPrice).divide(new BigDecimal(2), scale, RoundingMode.HALF_UP);
							}
						}
					}else if("QUOTE".equalsIgnoreCase(eventType) && "ASK".equalsIgnoreCase(eventSubType)){
						//only ask price
						if(message.hasElement("ASK")){
							BigDecimal askPrice = new BigDecimal(message.getElementAsFloat64("ASK"));
							askMap.put(contract.getExchangeCode(), askPrice);
							BigDecimal bidPrice = bidMap.get(contract.getExchangeCode());
							if(bidPrice !=  null){
								quote = new Quote();
								quote.priceTime = new Date();
								quote.exchangeCode = contract.getExchangeCode();
								quote.reutersCode = bloombergCode;
								quote.bidPrice = bidPrice;
								quote.askPrice = askPrice;
								int scale = contract.getScale();
								quote.newPrice = quote.askPrice.add(quote.bidPrice).divide(new BigDecimal(2), scale, RoundingMode.HALF_UP);
							}
						}
					}else if("QUOTE".equalsIgnoreCase(eventType) && "PAIRED".equalsIgnoreCase(eventSubType)){
						//both bid and ask
						BigDecimal askPrice = null;
						BigDecimal bidPrice = null;
						if(message.hasElement("ASK")){
							askPrice = new BigDecimal(message.getElementAsFloat64("ASK")); 
							askMap.put(contract.getExchangeCode(), askPrice);
							if(message.hasElement("BID")){
								bidPrice = new BigDecimal(message.getElementAsFloat64("BID"));
								bidMap.put(contract.getExchangeCode(), bidPrice);
								quote = new Quote();
								quote.priceTime = new Date();
								quote.exchangeCode = contract.getExchangeCode();
								quote.reutersCode = bloombergCode;
								quote.bidPrice = bidPrice;
								quote.askPrice = askPrice;
								int scale = contract.getScale();
								quote.newPrice = quote.askPrice.add(quote.bidPrice).divide(new BigDecimal(2), scale, RoundingMode.HALF_UP);
							}else{
								monitorLog.info("SubType=PAIRED时，没有bid价");
							}
						}else{
							monitorLog.info("SubType=PAIRED时，没有ask价" + (message.hasElement("BID") ? "" : "和bid价"));
						}
					}
				}
				if(quote != null ){
//					lastPriceMap.put(quote.reutersCode, quote.newPrice);
					dispatchQuote(quote);
				}
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
			if (Event.EventType.Constants.SESSION_STATUS == event.eventType().intValue()) {
				if("SessionTerminated" == message.messageType().toString()){
					bloombergLog.info("Terminating: " + message.messageType());
					this.setRestart(true);					
				}else if("SubscriptionTerminated" == message.messageType().toString()){
					bloombergLog.info("Terminating: " + message.messageType());
					this.setRestart(true);
				}
			}
			
		}
	}

	private static final void dispatchQuote(Quote quote) {
		try {
			RingBuffer<QuoteEvent> ringBuffer = RelayServer.getRingBuffer();
			if (ringBuffer != null && quote != null) {
				//monitorLog.info("开始申请下一个ringbuffer可用位置");
				long sequence = ringBuffer.next();
				//monitorLog.info("申请下一个ringbuffer可用位置成功");
				QuoteEvent event = ringBuffer.get(sequence);
				event.setValue(quote);
				ringBuffer.publish(sequence);
			}
		} catch (Exception e) {
			monitorLog.error("dispatchQuote过程中出现异常", e);
		}
	}

	public void setRestart(boolean isRestart) {
		this.isRestart = isRestart;
	}

	private boolean isRestart = false;
	private boolean stop = false;
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
			while(!stop){
				this.initBloombergConfig();
				if(this.contractList != null && this.contractList.size() > 0){
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
			//重置stop开关，以便于重启线程
			stop = false;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
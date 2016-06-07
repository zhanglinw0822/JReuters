package com.puxtech.reuters.rfa.Consumer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.puxtech.reuters.rfa.Common.ConfigNode;
import com.puxtech.reuters.rfa.Common.QuoteSource;
import com.puxtech.reuters.rfa.Common.SpreadConfig;
import com.puxtech.reuters.rfa.Common.SpreadConsumer;
import com.puxtech.reuters.rfa.Common.SubNode;
import com.puxtech.reuters.rfa.Common.TextCodecFactory;

public class ODSSpreadConsumer implements Runnable, SpreadConsumer {
	private static final Log monitorLog = LogFactory.getLog("moniter");
	private static final Log odsLog = LogFactory.getLog("ODS");
	private String addr = "112.124.211.146";
	private int port = 9101;
	private String userName = "chn-t2-2014w";
	private String password = "shoppingworks";
	private QuoteSource quoteSource = null;
    private IoConnector connector = new NioSocketConnector();;
    private IoSession session;
	private Map<Integer, Integer> tokenStatusMap = new HashMap<Integer, Integer>();
	private static final int ReadQuoIdleTime =10;
	private SpreadConfig spreadConfig = null;
	private Map<String, BigDecimal> closePriceMap = new HashMap<String, BigDecimal>();
	private Map<String, BigDecimal> lastPriceMap = new HashMap<String, BigDecimal>();
	private boolean flag = true;
	private boolean isRestart = false;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
	}

	@Override
	public void setQuoteSource(QuoteSource quoteSource) {
		this.quoteSource = quoteSource;
	}

	private void initSpreadConfig(){
		if(this.quoteSource != null){
			this.addr = this.quoteSource.getProperty("Addr");
			this.port = this.quoteSource.getPropertyInt("Port");
			this.userName = this.quoteSource.getProperty("UserName");
			this.password = this.quoteSource.getProperty("Pwd");
		}
	}
	
	private boolean connect(){
		if(session != null && session.isConnected()){
			return true;
		}
		int count = this.reConnectCount++;
		monitorLog.info("第" + count + "次连接开始..."+addr+":"+port);
		if(connector.getHandler()==null){
			connector.setHandler(new ODSHandler());			
			connector.getSessionConfig().setReaderIdleTime(10);
			connector.setConnectTimeoutMillis(15*1000);
		}
        ConnectFuture connFuture = connector.connect(new InetSocketAddress(addr, port));
        connFuture.awaitUninterruptibly();
        session = connFuture.getSession();
        if(session != null && session.isConnected()){
        	session.getFilterChain().addLast("logger", new LoggingFilter());
        	session.getFilterChain().addLast( "codec", new ProtocolCodecFilter(new TextCodecFactory(Charset.forName("utf-8"))));
        	monitorLog.info("第" + count + "次连接成功...");
        	this.reConnectCount = 1;
        	return true;
        }else{
        	monitorLog.info("第" + count + "连接失败...");
        	return false;
        }
	}
	private static boolean isLogon = false;
	private void logon(){
		if(connect()){
			ODSPackage logonPkg = new ODSPackage();
			logonPkg.appendParam(0, "Login");
			logonPkg.appendParam(1, String.valueOf(ODSConstants.LOGON));
			logonPkg.appendParam(1000, userName);
			logonPkg.appendParam(1001, password);
			send(logonPkg.toString());
		}
	}
	
	private static boolean isSubscribed = false;
	private void subscribe(){
		if(this.spreadConfig.getConfigList() != null){	
			for (ConfigNode configNode : this.spreadConfig.getConfigList()) {
				for(SubNode node : configNode.getNodeList()){
					String odsCode = node.getContractCode();
					ODSPackage subscribePkg = new ODSPackage();
					subscribePkg.appendParam(0, "Subscribe");
					subscribePkg.appendParam(1, String.valueOf(ODSConstants.SUBSCRIBE));
					subscribePkg.appendParam(10, odsCode);
					this.send(subscribePkg.toString());
				}
			}
			//订阅心跳
			ODSPackage subscribePkg = new ODSPackage();
			subscribePkg.appendParam(0, "Subscribe");
			subscribePkg.appendParam(1, String.valueOf(ODSConstants.SUBSCRIBE));
			subscribePkg.appendParam(10, "$TIME");
			this.send(subscribePkg.toString());
		}
	}
	private int reConnectCount = 1;
	private static boolean stop = false;
	@Override
	public void run() {
		while (true) {
			try {
				initSpreadConfig();
				if(this.spreadConfig != null){
					// 登录
					if(session == null || !session.isConnected()){
						this.logon();
					}
				}
			} catch (Exception e) {
				monitorLog.error("odscunsermer error",e);
			}
		}
	}
	
	private void send(String content){
		if(this.session != null && this.session.isConnected()){
			IoBuffer buff = IoBuffer.allocate(content.getBytes().length);
			buff.put(content.getBytes());
			buff.flip();
            session.write(buff);
            odsLog.info("Send:" + content + "\r\n");
		}
	}
	
	private void received(Object message){
		ODSPackage pkg = new ODSPackage(message.toString());
    	if(pkg != null){
    		String tag = pkg.getParam(1);
    		if(tag != null){
    			switch(Integer.valueOf(tag)){
        		case ODSConstants.LOGON:
        			//登录返回
        			if(pkg.getParamInt(5) == 0 || pkg.getParamInt(5) == 51002){
        				//登录成功
        				isLogon = true;
        				this.subscribe();
        			}else if(pkg.getParamInt(5) == 51005){
        				odsLog.info("登录失败,用户名密码错误!\r\n");
        				isLogon = false;
        			}else if(pkg.getParamInt(5) == 51010){
        				odsLog.info("登录失败,用户名已在其他链接中登录!\r\n");
        				isLogon = false;
        			}else	{
        				//登录失败
        				odsLog.info("登录失败,未知异常,返回码=" + pkg.getParamInt(5) + "\r\n");
        			}
        			if(!isLogon){
        				try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							monitorLog.info("登录重试间隔被中断！");
						}
        				this.logon();
        			}
        			break;
        		case ODSConstants.SUBSCRIBE:
        			//订阅返回处理，行情接收
        			if(pkg.getParamInt(5) == 0){
        				//订阅成功
        				odsLog.info("订阅成功！\r\n");
        				isSubscribed = true;
        			}else if(pkg.getParamInt(5) == 50101){
        				odsLog.info("订阅失败，无效商品代码！\r\n");
        				isSubscribed = false;
        			}else{
        				//订阅失败
        				odsLog.info("订阅失败,返回码=" + pkg.getParamInt(5) + "\r\n");
        				isSubscribed = false;
        			}
        			if(!isSubscribed){
        				try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							monitorLog.info("订阅失败重试间隔被意外中断！");
						}
        				if(this.session != null && this.session.isConnected()){        					
        					this.subscribe();
        				}else{
        					this.logon();
        				}
        			}
        			break;
        		case ODSConstants.UNSUBSCRIBE:
        			//取消订阅返回处理
        			break;
        		default:
        			odsLog.info("未知标签" + pkg.getParam(1) + "\r\n");
        		}
    		}else if("1".equals(pkg.getParam(5))){
    			//没有标签且5=1则认为是行情数据
    			String odsCode = pkg.getParam(10); //ods合约代码
				BigDecimal newPrice = new BigDecimal(pkg.getParam(24));	
				lastPriceMap.put(odsCode, newPrice);
				try{
					closePriceMap.put(odsCode, new BigDecimal(pkg.getParam(42)));
				} catch (java.lang.NullPointerException e) {
					odsLog.info("close price 为空\r\n");
				}
    		}
    		
    	}
	}
	
	
	class ODSHandler extends IoHandlerAdapter{
		   /**
	     * {@inheritDoc}
	     */
	    @Override
	    public void exceptionCaught(IoSession session1, Throwable cause) throws Exception {
	    	monitorLog.error("ODS行情接收异常，连接异常", cause);
	        if(cause instanceof IOException){
	    		if(session1 != null){
	    			session1.close(true);
	    			session = null;
	    			monitorLog.info("ODS连接关闭！");
	    		}
	        }
	    }

	    /**
	     * {@inheritDoc}
	     */
	    @Override
	    public void messageReceived(IoSession session, Object message) throws Exception {
	    	odsLog.info("Received:" + message + "\r\n");
	    	received(message);
	    }

	    /**
	     * {@inheritDoc}
	     */
	    @Override
	    public void messageSent(IoSession session, Object message) throws Exception {
	    }

	    /**
	     * {@inheritDoc}
	     */
	    @Override
	    public void sessionClosed(IoSession session) throws Exception {
	    	monitorLog.info("连接断开"+session.getRemoteAddress());
	    	if(stop){
	    		monitorLog.info("stop flsg is true,close session"+session.getRemoteAddress());
	    	}
	    }

	    /**
	     * {@inheritDoc}
	     */
	    @Override
	    public void sessionCreated(IoSession session) throws Exception {
	    }

	    /**
	     * {@inheritDoc}
	     */
	    @Override
	    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
	    	monitorLog.info("ODS读取超时，连接关闭！");
	    	session.close(false);
			session = null;
	    }
	    
	    /**
	     * {@inheritDoc}
	     */
	    @Override
	    public void sessionOpened(IoSession session) throws Exception {
	    }
	}
	
	public void shutdown() {
		this.flag = false;
	}

	public void restart() {
		this.isRestart = true;
	}

	@Override
	public Map<String, BigDecimal> getClosePriceMap() {
		return closePriceMap;
	}

	@Override
	public Map<String, BigDecimal> getLastPriceMap() {
		return lastPriceMap;
	}

	@Override
	public void setSpreadConfig(SpreadConfig spreadConfig) {
		this.spreadConfig = spreadConfig;
	}
	
	public SpreadConfig getSpreadConfig(){
		return this.spreadConfig;
	}

}
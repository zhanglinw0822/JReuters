package com.puxtech.reuters.rfa.Consumer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.lmax.disruptor.RingBuffer;
import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.Contract;
import com.puxtech.reuters.rfa.Common.Quote;
import com.puxtech.reuters.rfa.Common.QuoteSource;
import com.puxtech.reuters.rfa.Common.TextCodecFactory;
import com.puxtech.reuters.rfa.Common.TextEncoder;
import com.puxtech.reuters.rfa.RelayServer.QuoteEvent;
import com.puxtech.reuters.rfa.RelayServer.RelayServer;

public class ODSConsumer implements Runnable, ConsumerConfig {
	private static final Log monitorLog = LogFactory.getLog("moniter");
	private static final Log odsLog = LogFactory.getLog("ODS");
	private String addr = "112.124.211.146";
	private int port = 9101;
	private String userName = "chn-t2-2014w";
	private String password = "shoppingworks";
	private QuoteSource quoteSource = null;
	private List<Contract> contractList = new ArrayList<Contract>();
    private IoConnector connector = new NioSocketConnector();;
    private IoSession session;
	private Map<Integer, Integer> tokenStatusMap = new HashMap<Integer, Integer>();
	private static final int ReadQuoIdleTime =10;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
	}

	@Override
	public void setQuoteSource(QuoteSource quoteSource) {
		this.quoteSource = quoteSource;
	}

	private void initODSConfig(){
		if(this.quoteSource != null){
			this.addr = this.quoteSource.getProperty("Addr");
			this.port = this.quoteSource.getPropertyInt("Port");
			this.userName = this.quoteSource.getProperty("UserName");
			this.password = this.quoteSource.getProperty("Pwd");
			this.contractList.clear();
			List<Contract> contracts = Configuration.getInstance().getQuoteSourceContractMap().get(this.quoteSource.getName());
			for(Contract contract : contracts){
				if(contract.isVirtual()){
					continue;
				}else{
					this.contractList.add(contract);
					RelayServer.getConsumerMap().put(contract.getExchangeCode(), this);
				}
			}
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
		if(this.contractList != null){			
			for(Contract contract : this.contractList){
				Map<String, String> sourceCfg = contract.getSourceCfg();
				String odsCode = sourceCfg.get("OdsCode");
				ODSPackage subscribePkg = new ODSPackage();
				subscribePkg.appendParam(0, "Subscribe");
				subscribePkg.appendParam(1, String.valueOf(ODSConstants.SUBSCRIBE));
				subscribePkg.appendParam(10, odsCode);
				this.send(subscribePkg.toString());
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
	@Override
	public void run() {
		monitorLog.info("ods thread begin threadid"+Thread.currentThread().getId());
		while (!Thread.interrupted()) {
			try {
				if(reConnectCount>1)
					Thread.sleep(1000);
				if (this.session == null || !this.session.isConnected()) {
					initODSConfig();
					// 登录
					this.logon();
				}
			} catch (Exception e) {
				monitorLog.error("odscunsermer error",e);
			}
		}
		monitorLog.info("ods thread finished. threadid"+Thread.currentThread().getId());
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
    			for(Contract contract : this.contractList){
    				if(odsCode.equals(contract.getSourceCfg().get("OdsCode"))){
    					int algorithm = contract.getPriceAlgorithm();
    					Quote quote = null;
    					if(algorithm == 1){
    						quote = new Quote();
    						quote.priceTime = new Date();
    						quote.exchangeCode = contract.getExchangeCode();
    						quote.reutersCode = odsCode;
    						quote.newPrice = new BigDecimal(pkg.getParam(24));
    						try {
								quote.bidPrice = new BigDecimal(pkg.getParam(22));
								quote.askPrice = new BigDecimal(pkg.getParam(20));
							} catch (java.lang.NullPointerException e) {
								odsLog.info("bidprice askprice 为零");
							}
    					}else if(algorithm == 2){
    						quote = new Quote();
    						quote.priceTime = new Date();
    						quote.exchangeCode = contract.getExchangeCode();
    						quote.reutersCode = odsCode;
    						quote.bidPrice = new BigDecimal(pkg.getParam(22));
    						quote.askPrice = new BigDecimal(pkg.getParam(20));
    						int scale = contract.getScale();
    						quote.newPrice = quote.askPrice.add(quote.bidPrice).divide(new BigDecimal(2), scale, RoundingMode.HALF_UP);
    					}
    					if(quote != null){
    						dispatchQuote(quote);
    					}
    				}
    			}
    		}
    		
    	}
	}
	
	private static final void dispatchQuote(Quote quote){
		try {
			RingBuffer<QuoteEvent> ringBuffer = RelayServer.getRingBuffer();
			if(ringBuffer != null && quote != null){
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

	@Override
	public void shutdown() {
		Thread.interrupted();
		if(this.session != null && this.session.isConnected()){
			this.session.close(true);
			this.session = null;
		}
	}

	@Override
	public void restart() {
		if(this.session != null && this.session.isConnected()){
			this.session.close(true);
			this.session = null;
		}
	}

}
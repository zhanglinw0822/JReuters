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
		monitorLog.info("��" + count + "�����ӿ�ʼ..."+addr+":"+port);
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
        	monitorLog.info("��" + count + "�����ӳɹ�...");
        	this.reConnectCount = 1;
        	return true;
        }else{
        	monitorLog.info("��" + count + "����ʧ��...");
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
			//��������
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
					// ��¼
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
        			//��¼����
        			if(pkg.getParamInt(5) == 0 || pkg.getParamInt(5) == 51002){
        				//��¼�ɹ�
        				isLogon = true;
        				this.subscribe();
        			}else if(pkg.getParamInt(5) == 51005){
        				odsLog.info("��¼ʧ��,�û����������!\r\n");
        				isLogon = false;
        			}else if(pkg.getParamInt(5) == 51010){
        				odsLog.info("��¼ʧ��,�û����������������е�¼!\r\n");
        				isLogon = false;
        			}else	{
        				//��¼ʧ��
        				odsLog.info("��¼ʧ��,δ֪�쳣,������=" + pkg.getParamInt(5) + "\r\n");
        			}
        			if(!isLogon){
        				try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							monitorLog.info("��¼���Լ�����жϣ�");
						}
        				this.logon();
        			}
        			break;
        		case ODSConstants.SUBSCRIBE:
        			//���ķ��ش����������
        			if(pkg.getParamInt(5) == 0){
        				//���ĳɹ�
        				odsLog.info("���ĳɹ���\r\n");
        				isSubscribed = true;
        			}else if(pkg.getParamInt(5) == 50101){
        				odsLog.info("����ʧ�ܣ���Ч��Ʒ���룡\r\n");
        				isSubscribed = false;
        			}else{
        				//����ʧ��
        				odsLog.info("����ʧ��,������=" + pkg.getParamInt(5) + "\r\n");
        				isSubscribed = false;
        			}
        			if(!isSubscribed){
        				try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							monitorLog.info("����ʧ�����Լ���������жϣ�");
						}
        				if(this.session != null && this.session.isConnected()){        					
        					this.subscribe();
        				}else{
        					this.logon();
        				}
        			}
        			break;
        		case ODSConstants.UNSUBSCRIBE:
        			//ȡ�����ķ��ش���
        			break;
        		default:
        			odsLog.info("δ֪��ǩ" + pkg.getParam(1) + "\r\n");
        		}
    		}else if("1".equals(pkg.getParam(5))){
    			//û�б�ǩ��5=1����Ϊ����������
    			String odsCode = pkg.getParam(10); //ods��Լ����
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
								odsLog.info("bidprice askprice Ϊ��");
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
				//monitorLog.info("��ʼ������һ��ringbuffer����λ��");
				long sequence = ringBuffer.next();
				//monitorLog.info("������һ��ringbuffer����λ�óɹ�");
				QuoteEvent event = ringBuffer.get(sequence);
				event.setValue(quote);
				ringBuffer.publish(sequence);
			}
		} catch (Exception e) {
			monitorLog.error("dispatchQuote�����г����쳣", e);
		}
	}
	
	class ODSHandler extends IoHandlerAdapter{
		   /**
	     * {@inheritDoc}
	     */
	    @Override
	    public void exceptionCaught(IoSession session1, Throwable cause) throws Exception {
	    	monitorLog.error("ODS��������쳣�������쳣", cause);
	        if(cause instanceof IOException){
	    		if(session1 != null){
	    			session1.close(true);
	    			session = null;
	    			monitorLog.info("ODS���ӹرգ�");
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
	    	monitorLog.info("���ӶϿ�"+session.getRemoteAddress());
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
	    	monitorLog.info("ODS��ȡ��ʱ�����ӹرգ�");
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
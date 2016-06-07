package com.puxtech.reuters.rfa.test;

import java.net.InetSocketAddress;
import java.net.ConnectException;
import java.nio.charset.Charset;
import java.util.Date;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.keepalive.KeepAliveFilter;
import org.apache.mina.filter.keepalive.KeepAliveMessageFactory;
import org.apache.mina.filter.keepalive.KeepAliveRequestTimeoutHandler;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.puxtech.reuters.rfa.Common.TextCodecFactory;

public class MinaClient {
	public SocketConnector socketConnector;  
	  
    /** 
     * 缺省连接超时时间 
     */  
    public static final int DEFAULT_CONNECT_TIMEOUT = 5;  
  
    public static final String HOST = "10.150.16.118";  
  
    public static final int PORT = 9931;  
  
    public MinaClient() {  
        init();  
    }  
  
    public void init() {  
        socketConnector = new NioSocketConnector(Runtime.getRuntime().availableProcessors());  
  
        // 长连接  
        // socketConnector.getSessionConfig().setKeepAlive(true);  
  
//        socketConnector.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);  
        
  
//        socketConnector.setReaderIdleTime(DEFAULT_CONNECT_TIMEOUT);  
//        socketConnector.setWriterIdleTime(DEFAULT_CONNECT_TIMEOUT);  
//        socketConnector.setBothIdleTime(DEFAULT_CONNECT_TIMEOUT);  
  
//        socketConnector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory()));  
//        socketConnector.getFilterChain().addLast("logger", new LoggingFilter());
//        socketConnector.getFilterChain().addLast( "codec", new ProtocolCodecFilter(new TextCodecFactory(Charset.forName("GBK"))));
        
//        KeepAliveMessageFactory heartBeatFactory = new KeepAliveMessageFactoryImpl();
////		KeepAliveRequestTimeoutHandler heartBeatHandler = new KeepAliveRequestTimeoutHandlerImpl();
//		KeepAliveFilter heartBeat = new KeepAliveFilter(heartBeatFactory,
//				IdleStatus.BOTH_IDLE, KeepAliveRequestTimeoutHandler.DEAF_SPEAKER);
//		/** 是否回发 */
//		heartBeat.setForwardEvent(true);
//		/** 发送频率 */
//		heartBeat.setRequestInterval(10);
//		socketConnector.getFilterChain().addLast("heartbeat", heartBeat);
        
        ClientIoHandler ioHandler = new ClientIoHandler();  
        socketConnector.setHandler(ioHandler);  
        socketConnector.getSessionConfig().setReaderIdleTime(20);
    }  
  
    boolean run = true;
    class NioExecuter implements Runnable{

		@Override
		public void run() {
	        InetSocketAddress addr = new InetSocketAddress(HOST, PORT);  
	        ConnectFuture cf = socketConnector.connect(addr);  
	        try {  
	        	
	            cf.awaitUninterruptibly();
//	            cf.getSession().write(msg);  
//	            System.out.println("send message " + msg);  
//	            while(run){
//	            	System.out.println("running......");
//	            }
	        } catch (RuntimeIoException e) {  
	            if (e.getCause() instanceof ConnectException) {  
	                try {  
	                    if (cf.isConnected()) {  
	                        cf.getSession().close();  
	                    }  
	                } catch (RuntimeIoException e1) {  
	                }  
	            }  
	        }  
		}
    	
    }
    
    public void sendMessage(final String msg) {  
        InetSocketAddress addr = new InetSocketAddress(HOST, PORT);  
        ConnectFuture cf = socketConnector.connect(addr);  
        try {  
        	
            cf.isConnected();
            cf.getSession().getCloseFuture().awaitUninterruptibly();
            socketConnector.dispose();
//            cf.getSession().write(msg);  
//            System.out.println("send message " + msg);  
//            while(run){
//            	System.out.println("running......");
//            }
        } catch (RuntimeIoException e) {  
            if (e.getCause() instanceof ConnectException) {  
                try {  
                    if (cf.isConnected()) {  
                        cf.getSession().close();  
                    }  
                } catch (RuntimeIoException e1) {  
                }  
            }  
        }  
    }  
  
    public static void main(String[] args) throws InterruptedException {  
        MinaClient clent = new MinaClient();  
        new Thread(clent.new NioExecuter()).start();
       
//        for (int i = 0; i < 1; i++) {  
//            System.err.println(i);  
//            clent.sendMessage("Hello World " + i);  
//        }  
//        clent.getSocketConnector().dispose();  
        //System.exit(0);  
    }  
  
    public SocketConnector getSocketConnector() {  
        return socketConnector;  
    }  
  
    public void setSocketConnector(SocketConnector socketConnector) {  
        this.socketConnector = socketConnector;  
    }  

    private static class KeepAliveMessageFactoryImpl implements KeepAliveMessageFactory {
		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.apache.mina.filter.keepalive.KeepAliveMessageFactory#getRequest
		 * (org.apache.mina.core.session.IoSession)
		 */
		private static long initDate = new Date().getTime();
		@Override
		public Object getRequest(IoSession session) {
			System.out.println("返回预设语句");
			/** 返回预设语句 */
//			if(new Date().getTime() - initDate >= 1*60*1000){
//				return null;
//			}
			return "";
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.apache.mina.filter.keepalive.KeepAliveMessageFactory#getResponse
		 * (org.apache.mina.core.session.IoSession, java.lang.Object)
		 */
		@Override
		public Object getResponse(IoSession session, Object request) {
			System.out.println("返回null");
			/** 返回预设语句 */
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.apache.mina.filter.keepalive.KeepAliveMessageFactory#isRequest
		 * (org.apache.mina.core.session.IoSession, java.lang.Object)
		 */
		@Override
		public boolean isRequest(IoSession session, Object message) {
			System.out.println("是否是心跳包Request: " + message);
//			if(message.equals(HEARTBEATREQUEST))
				return true;
//			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.apache.mina.filter.keepalive.KeepAliveMessageFactory#isResponse
		 * (org.apache.mina.core.session.IoSession, java.lang.Object)
		 */
		@Override
		public boolean isResponse(IoSession session, Object message) {
			System.out.println("是否是心跳包Response: " + message);
//			if(message.equals(HEARTBEATRESPONSE))
				return true;
//			return false;
		}

	}
      
}  


  
class ClientIoHandler implements IoHandler {  
  
    private void releaseSession(IoSession session) throws Exception {  
        System.out.println("releaseSession");  
        if (session.isConnected()) {  
            session.close(true);
        }  
    }  
  
    @Override  
    public void sessionOpened(IoSession session) throws Exception {  
        System.out.println("sessionOpened");  
    }  
  
    @Override  
    public void sessionClosed(IoSession session) throws Exception {  
        System.out.println("sessionClosed");  
    }  
  
    @Override  
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {  
        System.out.println("sessionIdle");  
//        try {  
//            releaseSession(session);  
//        } catch (RuntimeIoException e) {  
//        }  
    }  
  
    @Override  
    public void messageReceived(IoSession session, Object message) throws Exception {  
        System.out.println("Receive Server message " + message.getClass().getName());  
        org.apache.mina.core.buffer.SimpleBufferAllocator buff;
//        super.messageReceived(session, message);  
  
//        releaseSession(session);  
    }  
  
    @Override  
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {  
        System.out.println("exceptionCaught");  
        cause.printStackTrace();  
        releaseSession(session);  
    }  
  
    @Override  
    public void messageSent(IoSession session, Object message) throws Exception {  
        System.out.println("messageSent");  
//        super.messageSent(session, message);  
    }

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		// TODO Auto-generated method stub
		
	}  
	
}



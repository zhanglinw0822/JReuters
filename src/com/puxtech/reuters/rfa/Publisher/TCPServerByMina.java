package com.puxtech.reuters.rfa.Publisher;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.service.IoServiceStatistics;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.keepalive.KeepAliveFilter;
import org.apache.mina.filter.keepalive.KeepAliveMessageFactory;
import org.apache.mina.filter.keepalive.KeepAliveRequestTimeoutHandler;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.TextCodecFactory;
import com.puxtech.reuters.rfa.utility.GenericOMMParser;

public class TCPServerByMina {

	private static int PORT = 9901; 
    private static final Logger LOGGER = LoggerFactory.getLogger(TCPServerByMina.class);
    private static final Log log = LogFactory.getLog(TCPServerByMina.class);
    /** 30秒后超时 */
	private static final int IDELTIMEOUT = 10;
	/** 15秒发送一次心跳包 */
	private static final int HEARTBEATRATE = 10;
	/** 心跳包内容 */
	private static String HEARTBEATREQUEST = "0|7000|||||";
	private static final String HEARTBEATRESPONSE = "HEARTBEATRESPONSE";
	private static NioSocketAcceptor acceptor = null;
	
	static{
		byte[] heartBeatBytes = HEARTBEATREQUEST.getBytes();
		byte[] heartBeatBytes2 = new byte[heartBeatBytes.length + 1];
		System.arraycopy(heartBeatBytes, 0, heartBeatBytes2, 0, heartBeatBytes.length);
		heartBeatBytes2[heartBeatBytes.length] = 0x00;
		HEARTBEATREQUEST = new String(heartBeatBytes2);
		PORT = Configuration.getInstance().getLocalPort();
	}

    public void init() throws IOException {
    	NioSocketAcceptor acceptor = new NioSocketAcceptor(Runtime.getRuntime().availableProcessors());
        //acceptor.getFilterChain().addLast("logger", new LoggingFilter());
        acceptor.getFilterChain().addLast( "codec", new ProtocolCodecFilter(new TextCodecFactory(Charset.forName("GBK")))); 
        KeepAliveMessageFactory heartBeatFactory = new KeepAliveMessageFactoryImpl();
//		KeepAliveRequestTimeoutHandler heartBeatHandler = new KeepAliveRequestTimeoutHandlerImpl();
		KeepAliveFilter heartBeat = new KeepAliveFilter(heartBeatFactory,
				IdleStatus.BOTH_IDLE, KeepAliveRequestTimeoutHandler.DEAF_SPEAKER);
		/** 是否回发 */
		heartBeat.setForwardEvent(true);
		/** 发送频率 */
		heartBeat.setRequestInterval(HEARTBEATRATE);
		acceptor.getFilterChain().addLast("heartbeat", heartBeat);
		QuoteSendHandler handler = new QuoteSendHandler();
        acceptor.setHandler(handler);
        acceptor.getSessionConfig().setWriteTimeout(2);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, IDELTIMEOUT);
        acceptor.bind(new InetSocketAddress(PORT));
        TCPServerByMina.acceptor = acceptor;
        log.info("TCP服务已启动，端口是" + PORT); 
    } 

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	/***
	 * @ClassName: KeepAliveMessageFactoryImpl
	 * @Description: 内部类，实现心跳工厂
	 * @author Minsc Wang ys2b7_hotmail_com
	 * @date 2011-3-7 下午04:09:02
	 * 
	 */
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
			System.out.println("返回预设语句" + HEARTBEATREQUEST);
			/** 返回预设语句 */
//			if(new Date().getTime() - initDate >= 1*60*1000){
//				return null;
//			}
			return HEARTBEATREQUEST;
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
			if(message.equals(HEARTBEATREQUEST))
				return true;
			return false;
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
			if(message.equals(HEARTBEATRESPONSE))
				return true;
			return false;
		}

	}

	/***
	 * @ClassName: KeepAliveRequestTimeoutHandlerImpl
	 * @Description: 当心跳超时时的处理，也可以用默认处理 这里like
	 *               KeepAliveRequestTimeoutHandler.LOG的处理
	 * @author Minsc Wang ys2b7_hotmail_com
	 * @date 2011-3-7 下午04:15:39
	 * 
	 */
	private static class KeepAliveRequestTimeoutHandlerImpl implements
			KeepAliveRequestTimeoutHandler {

		/*
		 * (non-Javadoc)
		 * 
		 * @seeorg.apache.mina.filter.keepalive.KeepAliveRequestTimeoutHandler#
		 * keepAliveRequestTimedOut
		 * (org.apache.mina.filter.keepalive.KeepAliveFilter,
		 * org.apache.mina.core.session.IoSession)
		 */
		@Override
		public void keepAliveRequestTimedOut(KeepAliveFilter filter,
				IoSession session) throws Exception {
			System.out.println("心跳超时！");
		}
	}
	
	public static void broadcast(Object message){
		TCPServerByMina.acceptor.broadcast(message);
	}
	
	public static IoServiceStatistics getStatistics(){
		return TCPServerByMina.acceptor.getStatistics();
	}
}
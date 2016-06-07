package com.puxtech.reuters.rfa.Publisher;

import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.puxtech.reuters.rfa.Common.ServerMonitor;
import com.puxtech.reuters.rfa.RelayServer.QuoteEvent;
import com.puxtech.reuters.rfa.RelayServer.QuoteEventHandler;
import com.puxtech.reuters.rfa.RelayServer.RelayServer;

public class QuoteSendHandler extends IoHandlerAdapter{
	private static final Logger LOGGER = LoggerFactory.getLogger(QuoteSendHandler.class);
	private static final Logger minaLog = LoggerFactory.getLogger("mina");
	private Executor executor = Executors.newCachedThreadPool();

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception { 
		LOGGER.warn(cause.getMessage(), cause);
		if(session != null && !session.isConnected()){
			Object attachment = session.getAttribute("processor");
			if(attachment != null && attachment instanceof BatchEventProcessor){
				BatchEventProcessor curProcessor = (BatchEventProcessor)attachment;
				resetRingBuffer(curProcessor);
			}
			session.close(true);
			session = null;
		}
	}
	private void resetRingBuffer(BatchEventProcessor curProcessor){
		BatchEventProcessor lastProcessor;
		synchronized (RelayServer.processorList) {			
			RelayServer.processorList.remove(curProcessor);
			lastProcessor = RelayServer.processorList.get(RelayServer.processorList.size() - 1);
		}
		LOGGER.info("开始重新设置gateSequences");
		RelayServer.getRingBuffer().setGatingSequences(lastProcessor.getSequence());
		LOGGER.info("sessionClosed, GatingSequences修改，最新Processor=" + lastProcessor);
		LOGGER.info("sessionClosed, GatingSequences修改，最新GatingSequences=" + lastProcessor.getSequence());
		LOGGER.info("开始终止processor");
		curProcessor.halt();
		LOGGER.info("session关闭，processor终止");
	}
	@Override
	public void sessionIdle(IoSession session, IdleStatus status)
			throws Exception {
		//minaLog.info("session正空闲" + status.toString());
	}
	@Override
	public void sessionClosed(IoSession session){
		minaLog.info("session被关闭");
		if(session != null){
			Object attachment = session.getAttribute("processor");
			if(attachment != null && attachment instanceof BatchEventProcessor){
				BatchEventProcessor curProcessor = (BatchEventProcessor)attachment;
				resetRingBuffer(curProcessor);
			}
//			session.close(true);
//			session = null;
		}
	}
	private static int sessionCount = 0;
	
	@Override
	public void sessionOpened(IoSession session){
		Date acceptTime = new Date();
		RingBuffer<QuoteEvent> ringBuffer = RelayServer.getRingBuffer();
		EventHandlerGroup<QuoteEvent> handlerGroup = RelayServer.getFilterHandlerGroup();
		if(ringBuffer != null){
			QuoteEventHandler handler = new QuoteEventHandler();
			handler.setSession(session);
			handler.setAcceptTime(acceptTime);
			SequenceBarrier barrier = handlerGroup.asSequenceBarrier();
			BatchEventProcessor<QuoteEvent> customProcessor = new BatchEventProcessor<QuoteEvent>(ringBuffer, barrier, handler);
			session.setAttribute("processor", customProcessor);
			handler.setProcessor(customProcessor);
			ringBuffer.setGatingSequences(customProcessor.getSequence());
			LOGGER.info("sessionOpened, GatingSequences修改，最新Processor=" + customProcessor);
			LOGGER.info("sessionOpened, GatingSequences修改，最新GatingSequences=" + customProcessor.getSequence());
			synchronized (RelayServer.processorList) {			
				RelayServer.processorList.add(customProcessor);
			}
			executor.execute(customProcessor);
		}
		ServerMonitor.appendSession(session);
	}
	
	private void appendProcessorList(BatchEventProcessor processor){
		
	}
	
//	@Override
//	public void messageReceived(IoSession session, Object message) throws Exception { 
//		String expression = message.toString(); 
//		if ("quit".equalsIgnoreCase(expression.trim())) { 
//			session.close(true); 
//			return; 
//		} 
//		try { 
//			Object result = jsEngine.eval(expression); 
//			session.write(result.toString()); 
//		} catch (ScriptException e) { 
//			LOGGER.warn(e.getMessage(), e); 
//			session.write("Wrong expression, try again."); 
//		} 
//	} 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
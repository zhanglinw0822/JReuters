package com.puxtech.reuters.rfa.Publisher;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.puxtech.reuters.rfa.RelayServer.QuoteEvent;
import com.puxtech.reuters.rfa.RelayServer.QuoteEventHandler;
import com.puxtech.reuters.rfa.RelayServer.RelayServer;



public class TCPIoProcessorImpl {
	private static final Log log = LogFactory.getLog(TCPIoProcessorImpl.class);
	
//	private static final List<BatchEventProcessor<QuoteEvent>> processorList = new ArrayList<BatchEventProcessor<QuoteEvent>>();
	
	Disruptor<QuoteEvent> disruptor;
	private Executor executor = Executors.newCachedThreadPool();

	public void setDisruptor(Disruptor<QuoteEvent> disruptor) {
		this.disruptor = disruptor;
	}

	public TCPIoProcessorImpl() {
	}

	public void handleAccept(SelectionKey key) throws IOException {
		final SocketChannel clientChannel = ((ServerSocketChannel)key.channel()).accept();
		// 设置为非阻塞套接字
		clientChannel.configureBlocking(false);
		clientChannel.socket().setTcpNoDelay(true);
		clientChannel.socket().setSoLinger(true, 0);
		clientChannel.socket().setKeepAlive(true);
		Date acceptTime = new Date();
		// 加入到selector中
//		clientChannel.register(key.selector(), SelectionKey.OP_READ, new TCPDataParser());
				
		RingBuffer<QuoteEvent> ringBuffer = RelayServer.getRingBuffer();
		EventHandlerGroup<QuoteEvent> handlerGroup = RelayServer.getFilterHandlerGroup();
		if(ringBuffer != null){
			SequenceBarrier barrier = handlerGroup.asSequenceBarrier();
			QuoteEventHandler handler = new QuoteEventHandler();
			BatchEventProcessor<QuoteEvent> customProcessor = new BatchEventProcessor<QuoteEvent>(ringBuffer, barrier, handler);
			handler.setClientChannel(clientChannel);
			handler.setProcessor(customProcessor);
			handler.setAcceptTime(acceptTime);
			ringBuffer.setGatingSequences(customProcessor.getSequence());
			executor.execute(customProcessor);
		}
		//			disruptor.handleEventsWith(customProcessor);
		
	}
	
	public int handleWrite(SelectionKey key) throws IOException {
		return 0;
	}
}

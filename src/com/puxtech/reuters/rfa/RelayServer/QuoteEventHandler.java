package com.puxtech.reuters.rfa.RelayServer;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventProcessor;
import com.puxtech.reuters.rfa.Common.Quote;
import com.puxtech.reuters.rfa.Common.QuoteEncoder;
import com.puxtech.reuters.rfa.Publisher.TCPIoProcessorImpl;

public class QuoteEventHandler implements EventHandler<QuoteEvent> {
	private static final Log moniterLog = LogFactory.getLog("quotationSend");
	private static final Log minaErrLog = LogFactory.getLog("mina");
	private static final Log log = LogFactory.getLog(QuoteEventHandler.class);
	private EventProcessor processor;
	private SocketChannel clientChannel;
	private Date acceptTime;
	private IoSession session;
	private long startup = new Date().getTime();

	public void setSession(IoSession session) {
		this.session = session;
	}

	public void setProcessor(EventProcessor processor) {
		this.processor = processor;
	}

	public void setClientChannel(SocketChannel clientChannel) {
		this.clientChannel = clientChannel;
	}

	public void setAcceptTime(Date acceptTime) {
		this.acceptTime = acceptTime;
	}

	private String SUCESS = "成功";
	private String FAIL = "失败";

	private IoFutureListener writeFutureListener = new IoFutureListener<WriteFuture>() {

		@Override
		public void operationComplete(WriteFuture future) {
			if(!future.isWritten()){
				Throwable exception = future.getException();
				if(exception != null){
					StackTraceElement[] elements = exception.getStackTrace();
					StringBuffer sb = new StringBuffer();
					sb.append("Cause by " + exception.getClass().getName() + " :" + exception.getMessage() + "\r\n");
					for(StackTraceElement element : elements){
						sb.append(element.getClassName() + element.getMethodName() + "(" + element.getFileName() + ":" + element.getLineNumber() + ")\r\n");
					}
					moniterLog.info(sb.toString());
				}
			}
		}
	}; 

	private void transferQuoteByMina(Quote quote){
		if(!quote.canSend()){
			moniterLog.info("发送行情异常，该行情未被完整处理!quote="+ quote+",handledflag="+quote.getHandledFlag());
			return;
		}
		if(this.session == null){
			return;
		}
		if(!this.session.isConnected()){
			return;
		}
		IoBuffer buf = null;
		try {
			if(quote.priceTime.after(acceptTime) && this.session.isConnected()){
				byte[] quoteBytes = new QuoteEncoder().encode(quote);
				buf = IoBuffer.allocate(quoteBytes.length).setAutoExpand(true);
				buf.put(quoteBytes);
				buf.flip();
				//moniterLog.info("开始发送一条行情,size=" + quoteBytes.length);
				long begin = System.currentTimeMillis();
				WriteFuture future = this.session.write(buf).addListener(writeFutureListener);
				future.await(500);
				String result = future.isWritten() ? SUCESS : FAIL;
				if(!future.isWritten())
					moniterLog.info("发送一条行情" + result + "，耗时"+ (System.currentTimeMillis()-begin) + "毫秒..."+",size="+ quoteBytes.length);
			}
		} catch (Exception e) {
			moniterLog.info("发送行情过程中出现异常!");
			moniterLog.info(this, e);
		} 
//		finally{
//			if(buf != null){
//				System.out.println("mark="+buf.markValue());
//				System.out.println("pos="+buf.position());
//				buf.clear();
//			}
//		}
	}

	@Override
	public void onEvent(QuoteEvent event, long sequence, boolean endOfBatch) throws Exception {
		try {
			Quote quote = event.getValue();
			if(quote.isFilter)
				return;
			if(quote.priceTime.after(acceptTime)){
				transferQuoteByMina(quote);
			}
		} catch (Exception e) {
			log.error(this, e);
		}
	}
}
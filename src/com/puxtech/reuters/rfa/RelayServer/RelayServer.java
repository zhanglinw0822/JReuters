package com.puxtech.reuters.rfa.RelayServer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.MultiThreadedClaimStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.puxtech.reuters.db.ReuterDao;
import com.puxtech.reuters.offset.Singleton;
import com.puxtech.reuters.rfa.Common.ConfigNode;
import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.Contract;
import com.puxtech.reuters.rfa.Common.JobManager;
import com.puxtech.reuters.rfa.Common.QuoteSource;
import com.puxtech.reuters.rfa.Common.ServerMonitor;
import com.puxtech.reuters.rfa.Common.SpreadConfig;
import com.puxtech.reuters.rfa.Common.SpreadConsumer;
import com.puxtech.reuters.rfa.Consumer.ConsumerConfig;
import com.puxtech.reuters.rfa.Filter.FilterGenerator;
import com.puxtech.reuters.rfa.Publisher.TCPServerByMina;
import com.puxtech.reuters.rfa.utility.CommonAdjust;
@SuppressWarnings("unchecked")
public class RelayServer {
	private static final Log moniterLog = LogFactory.getLog("moniter");
	private static final Log log = LogFactory.getLog(RelayServer.class);
	private static int RING_SIZE = 4096;
	private static Disruptor<QuoteEvent> disruptor;
	private static RingBuffer<QuoteEvent> ringBuffer;
	private static EventHandlerGroup<QuoteEvent> filterHandlerGroup;
	private static QuoteEventFilterHandler quoteEventFilterHandler = new QuoteEventFilterHandler();
	//	public static BatchEventProcessor lastProcessor = null;
	public static List<BatchEventProcessor> processorList = Collections.synchronizedList(new ArrayList<BatchEventProcessor>());
	private static List consumerList = new ArrayList();
	private static Map<String, ConsumerConfig> consumerMap = new HashMap<String, ConsumerConfig>();
	private static Map<String, SpreadConsumer> spreadConsumerMap = new HashMap<String, SpreadConsumer>();

	public static Map<String, SpreadConsumer> getSpreadConsumerMap() {
		return spreadConsumerMap;
	}
	public static Map<String, ConsumerConfig> getConsumerMap() {
		return consumerMap;
	}
	public static RingBuffer<QuoteEvent> getRingBuffer(){
		return ringBuffer;
	}
	public static Disruptor<QuoteEvent> getDisruptor(){
		return disruptor;
	}
	public static EventHandlerGroup<QuoteEvent> getFilterHandlerGroup() {
		return filterHandlerGroup;
	}
	public static void resetFilterBenchMark(String exchangeCode){
		quoteEventFilterHandler.resetFilterBenchMark(exchangeCode);
	}
	private static void initAndStart() throws Exception{
		Executor executor = Executors.newCachedThreadPool();
		disruptor = new Disruptor<QuoteEvent>(QuoteEvent.EVENT_FACTORY, executor, new MultiThreadedClaimStrategy(RING_SIZE), new SleepingWaitStrategy());
		FilterGenerator filterGenerator = new FilterGenerator(Configuration.getInstance());
		for(Contract contract : Configuration.getInstance().getContractList()){
			log.info("filter Name = " + contract.getFilterName());
			quoteEventFilterHandler.putFilterList(contract.getExchangeCode(), filterGenerator.buildFilterList(contract.getFilterName()));
		}
		QuoteEventIDHandler idHandler = new QuoteEventIDHandler(Configuration.getInstance().getContractList());
		//先进行滤价处理，再进行价差处理
		filterHandlerGroup = disruptor.handleEventsWith(quoteEventFilterHandler).then(new QuoteEventOffSetHandler()).then(idHandler);	
		if(ringBuffer == null){
			ringBuffer = disruptor.start();
			SequenceBarrier barrier = filterHandlerGroup.asSequenceBarrier();
			QuoteEventLogHandler logEventHandler = new QuoteEventLogHandler(); 
			BatchEventProcessor<QuoteEvent> logProcessor = new BatchEventProcessor<QuoteEvent>(ringBuffer, barrier, logEventHandler);
			ringBuffer.setGatingSequences(logProcessor.getSequence());
			System.out.println("GatingSequences修改，最新Processor=" + logProcessor);
			System.out.println("GatingSequences修改，最新GatingSequences=" + logProcessor.getSequence());
			processorList.add(logProcessor);
			executor.execute(logProcessor);
		}
	}

	private static void startConsumers() throws InstantiationException, IllegalAccessException, ClassNotFoundException{
		Collection<QuoteSource> sourceList = Configuration.getInstance().getQuoteSourceMap().values();
		synchronized (consumerList) {
			for(QuoteSource source : sourceList){
				String consumerClassName = source.getProperty("Consumer");
				Object consObject = Class.forName(consumerClassName).newInstance();
				consumerList.add(consObject);
				if(consObject instanceof ConsumerConfig){
					((ConsumerConfig)consObject).setQuoteSource(source);
				}
				if(consObject instanceof Runnable){
					new Thread((Runnable)consObject).start();
				}
			}
		}
	}
	
	private static void startSpreadConsumer() {
		SpreadConfig spreadConfig = Configuration.getInstance().getSpreadConfig();
		if(spreadConfig != null && spreadConfig.getConfigList().size() > 0){	
			for(ConfigNode cfgNode : spreadConfig.getConfigList()){
				QuoteSource source = Configuration.getInstance().getQuoteSourceMap().get(cfgNode.getQuoteSourceName());
				if(source != null){
					cfgNode.getSpreadConsumer().setQuoteSource(source);
					cfgNode.getSpreadConsumer().setSpreadConfig(spreadConfig);
				}
				if(cfgNode.getSpreadConsumer() != null && cfgNode.getSpreadConsumer() instanceof Runnable){
					new Thread((Runnable)cfgNode.getSpreadConsumer()).start();
					spreadConsumerMap.put(cfgNode.getExchangeCode(), cfgNode.getSpreadConsumer());
				}
			}
		}
	}

	public static void restartConsumers(){
		moniterLog.info("重启Consumer！");
		if(consumerList != null){
			synchronized (consumerList) {
				for(Object obj : consumerList){
					if(obj instanceof ConsumerConfig){
						((ConsumerConfig)obj).restart();
					}
				}
			}
		}
		moniterLog.info("重启Consumer成功！");
	}

	public static void main(String[] args) {
		if (CommonAdjust.INSTANCE.adjustPriceFlag()) {
			Singleton.INSTANCE.loadDiffPeriod();
		}
		try {
			initAndStart();
			JobManager.addRefreshConfigJob();
		} catch (Exception e1) {
			log.error("初始化失败！", e1);
			return;
		}
		try {
			TCPServerByMina server = new TCPServerByMina();
			server.init();
			startConsumers();
			for(Contract contract : Configuration.getInstance().getContractList()){
				if(contract.isVirtual()){
					VirtualProducer producer = new VirtualProducer();
					producer.setContractName(contract.getExchangeCode());
					producer.init();
					new Thread(producer).start();
				}
			}
			new Thread(new ServerMonitor()).start();

			Singleton.INSTANCE.refreshT_contractOffset();
			startSpreadConsumer();
			JobManager.addSpreadJob();
//			Thread.sleep(8000);
//			for(Object obj : consumerList){
//				ConsumerConfig consumer = (ConsumerConfig) obj;
//				consumer.subscribeSpread();
//			}
/*			new Thread(new Runnable() {
				@Override
				public void run() {
					List<IoSession> sessionList = ServerMonitor.getSessionList();
					while(true){
						for(IoSession session : sessionList){
							if(session.isConnected()){
								byte[] quoteBytes = "0|6001|WTI|CLK5 COMB Comdty|CLM5 COMB Comdty|1.80|".getBytes();
								quoteBytes[quoteBytes.length-1] = 0;
								IoBuffer buf = IoBuffer.allocate(quoteBytes.length).setAutoExpand(true);
								buf.put(quoteBytes);
								buf.flip();
								session.write(buf);
								
								quoteBytes = "0|6001|BRUENT|COK5 Comdty|COM5 Comdty|1.60|".getBytes();
								quoteBytes[quoteBytes.length-1] = 0;
								buf = IoBuffer.allocate(quoteBytes.length).setAutoExpand(true);
								buf.put(quoteBytes);
								buf.flip();
								session.write(buf);
							}
						}
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							moniterLog.info("sleep been interrupted");
						}
					}
				}
			}).start();*/
		} catch (Exception e) {
			e.printStackTrace();
			log.error(null, e);
		}
	}
}
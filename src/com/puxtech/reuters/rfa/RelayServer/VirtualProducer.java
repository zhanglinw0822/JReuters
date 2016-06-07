package com.puxtech.reuters.rfa.RelayServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lmax.disruptor.RingBuffer;
import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.Contract;
import com.puxtech.reuters.rfa.Common.Quote;
import com.puxtech.reuters.rfa.Common.QuoteSignal;
import com.puxtech.reuters.rfa.Common.QuoteSource;
import com.puxtech.reuters.rfa.Common.VirtualRange;

public class VirtualProducer implements Runnable {
	private static final Log moniterLog = LogFactory.getLog("moniter");
	private int publishCount = 0;
	private boolean runFlag = true;
	private RingBuffer<QuoteEvent> ringBuffer = RelayServer.getRingBuffer();
	private QuoteSource quoteSource;
	private List<Quote> quoteList = new ArrayList<Quote>();
//	private List<Long> timeList = new ArrayList<Long>();
	private String contractName = "";
	private String dataFolderName = 
//			"E:\\reutersLog\\原始行情.txt";
//			System.getProperty("user.dir") + "\\test.txt";
			System.getProperty("user.dir") + "/QuoteReplays";

	public boolean init(){
		boolean flag = false;
		try {
			File dataFolder = new File(dataFolderName);
			if(dataFolder.exists()){
					File quoteFile = new File(dataFolderName + "/" + contractName + ".log");
					if(quoteFile.exists()){							
						BufferedReader reader = new BufferedReader(new FileReader(quoteFile));
						if(reader != null){
							try {
								String line = "";
								while((line = reader.readLine()) != null){
									Quote quote = Quote.generateQuote(line);
									if(quote != null){										
										quoteList.add(quote);
									}
								}
							} catch (IOException e) {
								e.printStackTrace();
							} finally {
								try {
									reader.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		flag = quoteList.size() > 0;
		return flag;
	}
	
	public String getContractName() {
		return contractName;
	}

	public void setContractName(String contractName) {
		this.contractName = contractName;
	}

	public QuoteSource getQuoteSource() {
		return quoteSource;
	}

	public void setQuoteSource(QuoteSource quoteSource) {
		this.quoteSource = quoteSource;
	}
	
	public void replay(){
		Quote quote = new Quote();
		
		
	}

	public void produce(){
		if(this.ringBuffer != null){
//			int ram = Math.abs(new Random().nextInt() % 4);
			Quote quote;
//			System.out.println("ram="+ram);
			List<Contract> contracts = Configuration.getInstance().getContractList();
			for(Contract contract : contracts){
				if(contract.isVirtual()){
					quote = new Quote();
					BigDecimal benchMark = (BigDecimal) contract.getvPriceGenCfg().get("Benchmark");
					BigDecimal offset = (BigDecimal) contract.getvPriceGenCfg().get("OffSet");
					VirtualRange range = (VirtualRange) contract.getvPriceGenCfg().get("range");
					MathContext mc = new MathContext(2, RoundingMode.HALF_DOWN);
					switch(range){
					case up:
						quote.askPrice = benchMark.add(offset.multiply(new BigDecimal(Math.random())));
						quote.bidPrice  = quote.askPrice;
						quote.newPrice = quote.askPrice;
						quote.exchangeCode = contract.getExchangeCode();
						quote.reutersCode = contract.getSourceCfg().get("Code");
						quote.highPrice= benchMark.add(offset);
						quote.lowPrice = benchMark;
						quote.yClosePrice = benchMark.add(offset.divide(new BigDecimal("2"),mc));
						quote.ySettlePrice = quote.yClosePrice;
						quote.openPrice = benchMark.add(offset.divide(new BigDecimal("3"),mc));
						quote.amp = new BigDecimal("0.0023");
						quote.hold = 10000L;
						quote.holdDif = 1030L;
						quote.range = new BigDecimal("0.0031");
						quote.spread = new BigDecimal("8");
						quote.tradeQty = 5L;
						break;
					case down:
						quote.askPrice = benchMark.subtract(offset.multiply(new BigDecimal(Math.random())));
						quote.bidPrice  = quote.askPrice;
						quote.newPrice = quote.askPrice;
						quote.exchangeCode = contract.getExchangeCode();
						quote.reutersCode = contract.getSourceCfg().get("Code");
						quote.highPrice= benchMark;
						quote.lowPrice = benchMark.subtract(offset);
						quote.yClosePrice = benchMark.subtract(offset.divide(new BigDecimal("2"),mc));
						quote.ySettlePrice = quote.yClosePrice;
						quote.openPrice = benchMark.subtract(offset.divide(new BigDecimal("3"),mc));
						quote.amp = new BigDecimal("0.0023");
						quote.hold = 10000L;
						quote.holdDif = 1030L;
						quote.range = new BigDecimal("0.0031");
						quote.spread = new BigDecimal("8");
						quote.tradeQty = 5L;
						break;
					case arround:
						quote.askPrice = Math.abs(new Random().nextInt() % 2) == 1 ? benchMark.add(offset.multiply(new BigDecimal(Math.random()))) : benchMark.subtract(offset.multiply(new BigDecimal(Math.random())));;
						quote.bidPrice  = quote.askPrice;
						quote.newPrice = quote.askPrice;
						quote.exchangeCode = contract.getExchangeCode();
						quote.reutersCode = contract.getSourceCfg().get("Code");
						quote.highPrice= benchMark.add(offset);
						quote.lowPrice = benchMark.subtract(offset);
						quote.yClosePrice = benchMark.add(offset.divide(new BigDecimal("2"),mc));
						quote.ySettlePrice = quote.yClosePrice;
						quote.openPrice = benchMark.add(offset.divide(new BigDecimal("3"),mc));
						quote.amp = new BigDecimal("0.0023");
						quote.hold = 10000L;
						quote.holdDif = 1030L;
						quote.range = new BigDecimal("0.0031");
						quote.spread = new BigDecimal("8");
						quote.tradeQty = 5L;
						break;
					}
					//moniterLog.info("开始申请下一个可用的ringbuffer位置");
					//long begin = new Date().getTime();
					long sequence = ringBuffer.next();
					//long time = new Date().getTime() - begin;
					//moniterLog.info("申请到下一个可用的ringbuffer位置，耗时" + time + "毫秒...");
					System.out.println(sequence);
					QuoteEvent event = ringBuffer.get(sequence);
					quote.priceTime = new Date();
					event.setValue(quote);
					ringBuffer.publish(sequence);
					publishCount += 1;		
				}
			}
		}
	}
	public void produce(BigDecimal virtualPrice){
		long sequence = ringBuffer.next();
		System.out.println(sequence);
		QuoteEvent event = ringBuffer.get(sequence);
		Quote quote = new Quote();
		quote.askPrice = virtualPrice;
    	quote.newPrice = virtualPrice;
    	quote.exchangeCode = "DAGP";
    	quote.reutersCode = "XAG=";
    	quote.priceTime = new Date();
		event.setValue(quote);
		ringBuffer.publish(sequence);
		publishCount += 1;
//		
//		sequence = ringBuffer.next();
//		System.out.println(sequence);
//		event = ringBuffer.get(sequence);
//		quote = new Quote();
//		quote.askPrice = virtualPrice;
//    	quote.newPrice = virtualPrice;
//    	quote.exchangeCode = "Cu";
//    	quote.reutersCode = "MCU3=LX";
//    	quote.priceTime = new Date();
//		event.setValue(quote);
//		ringBuffer.publish(sequence);
//		publishCount += 1;
	}
	
	public void produce(Quote quote){
		long sequence = ringBuffer.next();
		System.out.println(sequence);
		QuoteEvent event = ringBuffer.get(sequence);
		event.setValue(quote);
		quote.priceTime = new Date();
		ringBuffer.publish(sequence);
		publishCount += 1;
	}

	@Override
	public void run() {
		try{
				long sleepTime = 0L;
				if(quoteList != null){
					int i = 0;
					int size = quoteList.size();
					System.out.println("虚拟行情数量:" + size);
					if(size <= 0){
						System.out.println("没有虚拟行情，虚拟行情生成结束！");
						return;
					}
					while(runFlag){
						Quote quote = quoteList.get(i);
						Quote nextQuote;
						if(i == (size - 1)){
							sleepTime = 2000;
						}else{
							nextQuote = quoteList.get(i + 1);
							sleepTime = nextQuote.priceTime.getTime() - quote.priceTime.getTime();
						}
						produce(quote);
						try {
							Thread.sleep(1000);//先写死1秒
						} catch (InterruptedException e) {
							moniterLog.info("sleep been Interrupted");
						}
						if(i == (size - 1)){
							i = 0;
						}else{							
							i++;
						}
					}
				}
		}catch(Exception e){
			moniterLog.info("虚拟行情生产进程发生未知异常！", e);
		}finally{
			moniterLog.info("虚拟行情终止! publishCount=" + publishCount);
		}
	}
	
	public void shutDown(){
		this.runFlag = false;
	}
}

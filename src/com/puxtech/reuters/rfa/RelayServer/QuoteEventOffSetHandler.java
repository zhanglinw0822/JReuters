package com.puxtech.reuters.rfa.RelayServer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lmax.disruptor.EventHandler;
import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.Contract;
import com.puxtech.reuters.rfa.Common.Quote;
import com.puxtech.reuters.rfa.utility.CommonAdjust;
import com.puxtech.reuters.offset.Singleton;
public class QuoteEventOffSetHandler implements EventHandler<QuoteEvent> {
	private static final Log offsetLog = LogFactory.getLog("offset");
	private static Map<String, BigDecimal> offsetMap = null;
	private static ConcurrentHashMap<String, Double> tempOffsetMap;
	private static final Map<String, Log> loggerMap = new HashMap<String, Log>();
	static{
		for(Contract contract : Configuration.getInstance().getContractList()){
			//获取原始数据日志的log
			loggerMap.put(contract.getExchangeCode(), LogFactory.getLog(contract.getExchangeCode()+"_RAW"));
		}
	}
	public static Map<String, BigDecimal> getOffsetMap() {
		return offsetMap;
	}

	public static void refreshOffset(Map<String, BigDecimal> map){
		if(map != null){
			if(offsetMap == null){
				offsetMap = new HashMap<String, BigDecimal>();
			}
			synchronized (offsetMap) {
				offsetMap.clear();
				offsetMap.putAll(map);
			}
		}else{
			offsetMap = null;
		}
	}
	
	@Override
	public void onEvent(QuoteEvent event, long sequence, boolean endOfBatch)
			throws Exception {
		Quote quote = event.getValue();
		if(CommonAdjust.INSTANCE.adjustPriceFlag()){
			tempOffsetMap = Singleton.INSTANCE.calDiff();
			if (tempOffsetMap != null) {
				if (tempOffsetMap.keySet().contains(quote.exchangeCode)) {
					offsetLog.info("价差偏移量调整前," + quote.exchangeCode + ":" + quote.newPrice.doubleValue() + "," + tempOffsetMap.get(quote.exchangeCode));
					log(quote);
					quote.newPrice = quote.newPrice.subtract(new BigDecimal(tempOffsetMap.get(quote.exchangeCode)));
					offsetLog.info("价差偏移量调整后：" + quote.exchangeCode + ":" + quote.newPrice.doubleValue());
				} else {
					offsetLog.info("无调整：" + quote.exchangeCode + ":" + quote.newPrice.doubleValue());
				}
			}
		}else{
			offsetLog.info("不参与自动调价：" + quote.exchangeCode + ":" + quote.newPrice.doubleValue());
		}
		quote.setOffsetHandled();
	}
	
	private void log(Quote quote)
			throws Exception {
		Log logger = loggerMap.get(quote.exchangeCode);
		if(logger != null){
			logger.info(quote + "\r\n");
		}
	}
}
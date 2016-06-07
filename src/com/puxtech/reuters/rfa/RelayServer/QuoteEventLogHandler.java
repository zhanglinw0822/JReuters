package com.puxtech.reuters.rfa.RelayServer;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lmax.disruptor.EventHandler;
import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.Contract;
import com.puxtech.reuters.rfa.Common.QuoteSignal;

public class QuoteEventLogHandler implements EventHandler<QuoteEvent> {
	private static final Log log = LogFactory.getLog(QuoteEventLogHandler.class);
	private static final Map<String, Log> loggerMap = new HashMap<String, Log>();
	static{
		for(Contract contract : Configuration.getInstance().getContractList()){
			loggerMap.put(contract.getExchangeCode(), LogFactory.getLog(contract.getExchangeCode()));
		}
//		PropertyConfigurator.configure(new Properties());
//		wtiLog.info("��Ʒ����,·͸����,ʱ��,ASK,BID,�۸�,�Ƿ񱻹���" + "\r\n");
//		bruentLog.info("��Ʒ����,·͸����,ʱ��,ASK,BID,�۸�,�Ƿ񱻹���" + "\r\n");
//		dagLog.info("��Ʒ����,·͸����,ʱ��,ASK,BID,�۸�,�Ƿ񱻹���" + "\r\n");
	}
	@Override
	public void onEvent(QuoteEvent event, long sequence, boolean endOfBatch)
			throws Exception {
		Log logger = loggerMap.get(event.getValue().exchangeCode);
		if(logger != null){
			logger.info(event.getValue() + "\r\n");
		}else{
			log.error("����һ��δ֪���������, exchangeCode=" + event.getValue() + "\r\n");
		}
		event.getValue().setLogHandled();
	}
}

package com.puxtech.reuters.rfa.RelayServer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.lmax.disruptor.EventHandler;
import com.puxtech.reuters.rfa.Common.Contract;
import com.puxtech.reuters.rfa.Common.Quote;

public class QuoteEventIDHandler implements EventHandler<QuoteEvent> {
	private static Map<String, Integer> idMap = new HashMap<String, Integer>();
	private static Map<String, Date> timeStampMap = new HashMap<String, Date>();
	private static Map<String, Object> lockMap = new HashMap<String, Object>();
	
	public QuoteEventIDHandler(List<Contract> contractList){
		for(Contract contract : contractList){
			idMap.put(contract.getExchangeCode(), 1);
			timeStampMap.put(contract.getExchangeCode(), new Date());
			lockMap.put(contract.getExchangeCode(), new Object());
		}
	}
	@Override
	public void onEvent(QuoteEvent event, long sequence, boolean endOfBatch) throws Exception {
		if(event != null && event.getValue() != null){
			Quote quote = event.getValue();
			Object quoteLock = lockMap.get(quote.exchangeCode);
			synchronized (quoteLock) {
				Date stamp = timeStampMap.get(quote.exchangeCode);
				if(new Date().getTime() - stamp.getTime() >= 1000){
					idMap.put(quote.exchangeCode, 1);
					timeStampMap.put(quote.exchangeCode, new Date());
					quote.id=1;
				}else{
					int id = idMap.get(quote.exchangeCode) + 1;
					idMap.put(quote.exchangeCode, id);
					quote.id=id;
				}
			}
			quote.setIDHandled();
		}
	}
}
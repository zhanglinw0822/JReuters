package com.puxtech.reuters.rfa.RelayServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lmax.disruptor.EventHandler;
import com.puxtech.reuters.rfa.Filter.Filter;

public class QuoteEventFilterHandler implements EventHandler<QuoteEvent> {
	private static final Log offsetLog = LogFactory.getLog("offsetLog");
	private Map<String, List<Filter>> filterMaps = new HashMap<String, List<Filter>>();
	
	public void resetFilterBenchMark(String exchangeCode){
		List<Filter> filterList = this.filterMaps.get(exchangeCode);
		if(filterList != null){
			for(Filter filter : filterList){
				filter.resetBenchMark();
			}
		}
	}
	
	public void putFilterList(String exchangeCode, List<Filter> filterList){
		this.filterMaps.put(exchangeCode, filterList);
	}
	
	public Map<String, List<Filter>> getFilterMaps() {
		return filterMaps;
	}

	@Override
	public void onEvent(QuoteEvent event, long sequence, boolean endOfBatch) throws Exception {
		List<Filter> filterList = this.filterMaps.get(event.getValue().exchangeCode);
		if(filterList != null){
//			moniterLog.info("filterList size=" + filterList.size());
			for(Filter filter : filterList){
				filter.filte(event.getValue());
			}
		}
		if(event != null && event.getValue() != null){
			event.getValue().setFilterHandled();
		}
	}
}

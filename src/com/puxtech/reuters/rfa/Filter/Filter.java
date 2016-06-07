package com.puxtech.reuters.rfa.Filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.puxtech.reuters.rfa.Common.Quote;

public class Filter {
	private List<FilterStrategy> strategyList = new ArrayList<FilterStrategy>();
	private BigDecimal prevPrice = new BigDecimal("0.0");
	public Filter() {
		super();
	}
	
	public void addStrategy(FilterStrategy strategy){
		this.strategyList.add(strategy);
	}
	
	public void filte(Quote quote){
		if(quote == null)
			return;
//		moniterLog.info("strategyList size=" + strategyList.size());
		for(FilterStrategy filterStrategy : strategyList){
			filterStrategy.setPrevPrice(prevPrice);
			if(filterStrategy.isFilter(quote)){
				quote.isFilter = true;
				prevPrice = quote.newPrice;
				return;
			}
		}
		quote.isFilter = false;
		prevPrice = quote.newPrice;
	}
	
	public int getFilteStrategyCount(){
		return this.strategyList != null ? this.strategyList.size() : 0;
	}

	public List<FilterStrategy> getStrategyList() {
		return strategyList;
	}
	
	public void resetBenchMark(){
		for(FilterStrategy filterStrategy : strategyList){
			filterStrategy.resetBenchMark();
		}
	}
	
}
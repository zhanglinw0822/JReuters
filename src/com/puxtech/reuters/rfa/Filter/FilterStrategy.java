package com.puxtech.reuters.rfa.Filter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.puxtech.reuters.rfa.Common.Quote;

public abstract class FilterStrategy {
//	protected static Map<String, BigDecimal> benchMarkMap = new HashMap<String, BigDecimal>();
	public abstract boolean isFilter(Quote quote);
	public abstract void appendRule(Rule rule, QuotePool quotePool);
//	protected static BigDecimal prevPrice;
//	protected static Map<String, Integer> filteCountMaps = new HashMap<String, Integer>();
	public abstract void setUnlimitRule(Rule unlimitRule);
	protected BigDecimal benchMark;
	protected BigDecimal prePrice;
	public void setPrevPrice(BigDecimal lastPrice){
		this.prePrice=lastPrice;
	}
	public abstract void resetBenchMark();
	private static Map<String, DisparityQuoteFilterStrategy> disparityQuoteFilterStrategyMap;
//	protected int filteCount = 0;
	
}

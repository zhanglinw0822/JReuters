package com.puxtech.reuters.rfa.Filter;

import java.math.BigDecimal;

import com.puxtech.reuters.rfa.Common.Quote;

public class ZeroQuoteFilterStrategy extends FilterStrategy {

	@Override
	public boolean isFilter(Quote quote) {
		return quote != null && quote.newPrice.compareTo(new BigDecimal(0.0)) == 0;
	}

	@Override
	public void appendRule(Rule rule, QuotePool quotePool) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUnlimitRule(Rule unlimitRule) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resetBenchMark() {
		// TODO Auto-generated method stub
		
	}

}

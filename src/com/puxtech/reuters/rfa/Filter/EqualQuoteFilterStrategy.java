package com.puxtech.reuters.rfa.Filter;

import com.puxtech.reuters.rfa.Common.Quote;

public class EqualQuoteFilterStrategy extends FilterStrategy {

	@Override
	public boolean isFilter(Quote quote) {
		if(quote == null){
			return false;
		}
		return quote.newPrice.compareTo(prePrice) == 0;
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

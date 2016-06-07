package com.puxtech.reuters.rfa.Common;

import java.math.BigDecimal;
import java.util.Map;

public interface SpreadConsumer{
	Map<String, BigDecimal> getClosePriceMap();
	Map<String, BigDecimal> getLastPriceMap();
	void setSpreadConfig(SpreadConfig spreadConfig);
	void setQuoteSource(QuoteSource quoteSource);
}

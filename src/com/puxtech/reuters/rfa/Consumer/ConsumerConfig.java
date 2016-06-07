package com.puxtech.reuters.rfa.Consumer;

import com.puxtech.reuters.rfa.Common.QuoteSource;

public interface ConsumerConfig {
	public void setQuoteSource(QuoteSource quoteSource);
	public void shutdown();
	public void restart();
}

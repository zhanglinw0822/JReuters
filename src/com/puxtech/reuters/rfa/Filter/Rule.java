package com.puxtech.reuters.rfa.Filter;

import java.math.BigDecimal;

import com.puxtech.reuters.rfa.Common.Quote;

public abstract class Rule{
	public abstract boolean match(Quote quote, BigDecimal benchMark);
	public abstract int getFilteCount();
	public abstract int getFilteredCount() ;
	public abstract BigDecimal getPercent();
	public abstract void setFilteredCount(int filteredCount);
	public abstract void addFilteredCount(int count);
	public abstract void clearFilteredCount();
}

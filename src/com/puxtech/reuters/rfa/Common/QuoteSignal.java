package com.puxtech.reuters.rfa.Common;

import java.math.BigDecimal;

public class QuoteSignal {
	private String reutersCode = "";
	private String exchangeCode = "";
	private String dcMarket = "";
	private String dcCode = "";
	private String PriceAlgorithm = "";
	private String filterName = "";
	private BigDecimal offset = null;
	private boolean isVirtual = false;
	private BigDecimal benchMark = new BigDecimal("0.0");
	private BigDecimal virtualOffset = new BigDecimal("0.0");
	private VirtualRange range = VirtualRange.arround;
	
	public String getFilterName() {
		return filterName;
	}
	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}
	private int scale=2;
	public String getReutersCode() {
		return reutersCode;
	}
	public void setReutersCode(String reutersCode) {
		this.reutersCode = reutersCode;
	}
	public String getExchangeCode() {
		return exchangeCode;
	}
	public void setExchangeCode(String exchangeCode) {
		this.exchangeCode = exchangeCode;
	}
	public String getPriceAlgorithm() {
		return PriceAlgorithm;
	}
	public void setPriceAlgorithm(String PriceAlgorithm) {
		this.PriceAlgorithm = PriceAlgorithm;
	}
	public int getScale() {
		return scale;
	}
	public void setScale(int scale) {
		this.scale = scale;
	}
	public String getDcMarket() {
		return dcMarket;
	}
	public void setDcMarket(String dcMarket) {
		this.dcMarket = dcMarket;
	}
	public String getDcCode() {
		return dcCode;
	}
	public void setDcCode(String dcCode) {
		this.dcCode = dcCode;
	}
	public BigDecimal getOffset() {
		return offset;
	}
	public void setOffset(BigDecimal offset) {
		this.offset = offset;
	}
	public boolean isVirtual() {
		return isVirtual;
	}
	public void setVirtual(boolean isVirtual) {
		this.isVirtual = isVirtual;
	}
	public BigDecimal getBenchMark() {
		return benchMark;
	}
	public void setBenchMark(BigDecimal benchMark) {
		this.benchMark = benchMark;
	}
	public BigDecimal getVirtualOffset() {
		return virtualOffset;
	}
	public void setVirtualOffset(BigDecimal virtualOffset) {
		this.virtualOffset = virtualOffset;
	}
	public VirtualRange getRange() {
		return range;
	}
	public void setRange(VirtualRange range) {
		this.range = range;
	}
	
}

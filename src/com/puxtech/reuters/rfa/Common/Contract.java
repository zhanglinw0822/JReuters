package com.puxtech.reuters.rfa.Common;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.puxtech.reuters.rfa.Filter.Filter;

public class Contract {
/*<SourceConfig>
					<ReutersCode>LCOv1</ReutersCode>
				</SourceConfig>
				<ExchangeCode>BRUENT</ExchangeCode>
				<PriceAlgorithm>1</PriceAlgorithm>
				<Filter>DefaultFilter</Filter>*/
	private String sourceName = "";
	private Map<String, String> sourceCfg = new HashMap<String, String>();
	private String exchangeCode = "";
	private int priceAlgorithm = 0;
	private int scale = 0;
	private String filterName = "";
//	private BigDecimal offset = null;
	private boolean isVirtual = false;
	private Map<String, Object> vPriceGenCfg = new HashMap<String, Object>();
	public String getExchangeCode() {
		return exchangeCode;
	}
	public void setExchangeCode(String exchangeCode) {
		this.exchangeCode = exchangeCode;
	}
	public int getPriceAlgorithm() {
		return priceAlgorithm;
	}
	public void setPriceAlgorithm(int priceAlgorithm) {
		this.priceAlgorithm = priceAlgorithm;
	}
	public String getFilterName() {
		return filterName;
	}
	public void setFilterName(String filterName) {
		this.filterName = filterName;
	}
//	public BigDecimal getOffset() {
//		return offset;
//	}
//	public void setOffset(BigDecimal offset) {
//		this.offset = offset;
//	}
	public void appendSourceCfg(String key, String value){
		this.sourceCfg.put(key, value);
	}
	public Map<String, String> getSourceCfg(){
		return this.sourceCfg;
	}
	public String getSourceName() {
		return sourceName;
	}
	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}
	public Map<String, Object> getvPriceGenCfg() {
		return vPriceGenCfg;
	}
	public void appendvPriceGenCfg(String key, Object value){
		this.vPriceGenCfg.put(key, value);
	}
	public boolean isVirtual() {
		return isVirtual;
	}
	public void setVirtual(boolean isVirtual) {
		this.isVirtual = isVirtual;
	}
	public int getScale() {
		return scale;
	}
	public void setScale(int scale) {
		this.scale = scale;
	}
}

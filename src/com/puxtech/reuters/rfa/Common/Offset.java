package com.puxtech.reuters.rfa.Common;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;

public class Offset {
	private String exchangeCode = "";
	private Map<String, String> sourceCfg = new HashMap<String, String>();
	private Element quoteSourceElement = null;
	private BigDecimal offset = null;
	public String getExchangeCode() {
		return exchangeCode;
	}
	public void setExchangeCode(String exchangeCode) {
		this.exchangeCode = exchangeCode;
	}
	public BigDecimal getOffset() {
		return offset;
	}
	public void setOffset(BigDecimal offset) {
		this.offset = offset;
	}
	public Map<String, String> getSourceCfg() {
		return sourceCfg;
	}
	public void appendSourceCfg(String key, String value){
		this.sourceCfg.put(key, value);
	}
	public Element getQuoteSourceElement() {
		return quoteSourceElement;
	}
	public void setQuoteSourceElement(Element quoteSourceElement) {
		this.quoteSourceElement = quoteSourceElement;
	}
	@Override
	public String toString() {
		String sourceCfgStr = "";
		for(String key : sourceCfg.keySet()){
			sourceCfgStr += key + "=" + sourceCfg.get(key) + "|";
		}
		sourceCfgStr = sourceCfgStr.substring(0, sourceCfgStr.length() - 2);
		return "[exchangeCode=" + exchangeCode + "|offset=" + offset.toString() + "|sourceCfg=[" + sourceCfgStr + "]";
	}
}

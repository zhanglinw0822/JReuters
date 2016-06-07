package com.puxtech.reuters.rfa.Common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dom4j.Element;

public class QuoteSource {
	private String name = "";
	private Map<String, String> propertyMap = new HashMap<String, String>();
	private List<QuoteSignal> dcQuoteSignList = new ArrayList<QuoteSignal>();
	private List<QuoteSignal> virtualQuoteSignList = new ArrayList<QuoteSignal>();
	Element contractEle = null;

	public List<QuoteSignal> getDcQuoteSignList() {
		return dcQuoteSignList;
	}
	public void setDcQuoteSignList(List<QuoteSignal> dcQuoteSignList) {
		this.dcQuoteSignList = dcQuoteSignList;
	}
	public Element getContractEle() {
		return contractEle;
	}
	public void setContractEle(Element contractEle) {
		this.contractEle = contractEle;
	}
	public List<QuoteSignal> getVirtualQuoteSignList() {
		return virtualQuoteSignList;
	}
	public void setVirtualQuoteSignList(List<QuoteSignal> virtualQuoteSignList) {
		this.virtualQuoteSignList = virtualQuoteSignList;
	}
	public void setProperty(String key, String value){
		this.propertyMap.put(key, value);
	}
	public String getProperty(String key){
		return this.propertyMap.get(key);
	}
	public boolean containsProperty(String key){
		return this.propertyMap.containsKey(key);
	}
	public Integer getPropertyInt(String key){
		String proStr = this.propertyMap.get(key);
		if(proStr != null && proStr.matches("\\d+")){
			return Integer.valueOf(proStr);
		}else{
			return null;
		}
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}

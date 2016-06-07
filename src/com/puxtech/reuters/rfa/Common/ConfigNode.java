package com.puxtech.reuters.rfa.Common;

import java.util.ArrayList;
import java.util.List;

public class ConfigNode {
	String exchangeCode;
	private String quoteSourceName = null;
	SpreadConsumer spreadConsumer = null;
	List<SubNode> nodeList = new ArrayList<SubNode>();

	public String getExchangeCode() {
		return exchangeCode;
	}

	public void setExchangeCode(String exchangeCode) {
		this.exchangeCode = exchangeCode;
	}

	public List<SubNode> getNodeList() {
		return nodeList;
	}

	public void setNodeList(List<SubNode> nodeList) {
		this.nodeList = nodeList;
	}

	public SpreadConsumer getSpreadConsumer() {
		return spreadConsumer;
	}

	public void setSpreadConsumer(SpreadConsumer spreadConsumer) {
		this.spreadConsumer = spreadConsumer;
	}

	public String getQuoteSourceName() {
		return quoteSourceName;
	}

	public void setQuoteSourceName(String quoteSourceName) {
		this.quoteSourceName = quoteSourceName;
	}
	
	
}

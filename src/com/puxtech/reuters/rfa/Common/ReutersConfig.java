package com.puxtech.reuters.rfa.Common;

import java.util.ArrayList;
import java.util.List;

public class ReutersConfig {
	private String serviceName = "";
	private String sessionName = "";
	private String eventQueueName = "";
	private String consumerName = "";
	private List<String> itemNames = new ArrayList<String>();
	public String getServiceName() {
		return serviceName;
	}
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	public String getSessionName() {
		return sessionName;
	}
	public void setSessionName(String sessionName) {
		this.sessionName = sessionName;
	}
	public String getEventQueueName() {
		return eventQueueName;
	}
	public void setEventQueueName(String eventQueueName) {
		this.eventQueueName = eventQueueName;
	}
	public String getConsumerName() {
		return consumerName;
	}
	public void setConsumerName(String consumerName) {
		this.consumerName = consumerName;
	}
	public List<String> getItemNames() {
		return itemNames;
	}
	public void setItemNames(List<String> itemNames) {
		this.itemNames = itemNames;
	}
}
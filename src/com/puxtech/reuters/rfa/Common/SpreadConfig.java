package com.puxtech.reuters.rfa.Common;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import com.puxtech.reuters.db.*;
import com.puxtech.reuters.rfa.utility.CommonAdjust;
import com.puxtech.reuters.rfa.utility.PropUtil;

public class SpreadConfig {
	private static final Log moniterLog = LogFactory.getLog("moniter");
	private static SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private List<ConfigNode> configList = new ArrayList<ConfigNode>();

	public SpreadConfig(String quoteSourceName) {
		if (CommonAdjust.INSTANCE.adjustPriceFlag()) {
			this.configList = ReuterDao.querySpread(quoteSourceName);
		}
	}
	
	/*
	public SpreadConfig(Element cfgElement){
		try {
			List<Element> nodeElements = cfgElement.elements("ConfigNode");
			for(Element nodeEle : nodeElements){
				ConfigNode configNode = new ConfigNode();
				configNode.setQuoteSourceName(nodeEle.attributeValue("QuoteSource"));
				configNode.setExchangeCode(nodeEle.attributeValue("ExchangeCode"));
				String spreadConsumerClassName = nodeEle.attributeValue("SpreadConsumer");
				Object consObject = null;
				try {
					consObject = Class.forName(spreadConsumerClassName).newInstance();
				} catch (InstantiationException e) {
					moniterLog.error("spreadConsumer¿‡–Õ≈‰÷√¥ÌŒÛ", e);
				} catch (IllegalAccessException e) {
					moniterLog.error("spreadConsumer¿‡–Õ≈‰÷√¥ÌŒÛ", e);
				} catch (ClassNotFoundException e) {
					moniterLog.error("spreadConsumer¿‡–Õ≈‰÷√¥ÌŒÛ", e);
				}
				if(consObject != null && consObject instanceof SpreadConsumer){
					SpreadConsumer spreadConsumer = (SpreadConsumer) consObject;
					configNode.setSpreadConsumer(spreadConsumer);
					spreadConsumer.setSpreadConfig(this);
				}
				
				List<Element> subElements = nodeEle.elements("SubNode");
				List<SubNode> subNodeList = new ArrayList<SubNode>();
				for(Element subEle : subElements){
					SubNode node = new SubNode();
					node.setIndex(Integer.valueOf(subEle.attributeValue("index")));
					node.setProviderCode(subEle.elementText("ProviderCode"));
					node.setSendTime(formater.parse(subEle.elementText("SendTime")));
					node.setChangeTime(formater.parse(subEle.elementText("ChangeTime")));
					subNodeList.add(node);
				}
				configNode.setNodeList(subNodeList);
				this.configList.add(configNode);
			}
		} catch (NumberFormatException e) {
			moniterLog.error("º€≤Ó≈‰÷√“Ï≥£", e);
		} catch (ParseException e) {
			moniterLog.error("º€≤Ó≈‰÷√“Ï≥£", e);
		}
	}
	*/

	public List<ConfigNode> getConfigList() {
		return configList;
	}

	public void setConfigList(List<ConfigNode> configList) {
		this.configList = configList;
	}
	
}

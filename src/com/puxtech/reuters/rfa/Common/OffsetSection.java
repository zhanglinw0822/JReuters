package com.puxtech.reuters.rfa.Common;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

public class OffsetSection {
	private static final Log offsetLog = LogFactory.getLog("offset");
	private List<Offset> offSetList = new ArrayList<Offset>();
	private Date timeStamp = null;

	public OffsetSection(Element sectionElement){
		try {
			String timeStr = sectionElement.elementText("TimeStamp");
			this.timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(timeStr);
			for(Object obj : sectionElement.elements("Node")){
				if(obj instanceof Element){
					Element node = (Element) obj;
					Offset offset = new Offset();
					offset.setExchangeCode(node.elementText("ExchangeCode"));
					List<Element> sourceCfgs = node.element("SourceConfig").elements();
					for(Element sourceCfg : sourceCfgs){
						offset.appendSourceCfg(sourceCfg.getName(), sourceCfg.getTextTrim());
					}
					offset.setOffset(new BigDecimal(node.elementText("OffSet")));
					this.offSetList.add(offset);
				}
			}
		} catch (ParseException e) {
			offsetLog.error("≥ı ºªØOffsetSection ß∞‹", e);
		}

	}
	public List<Offset> getOffSetList() {
		return offSetList;
	}
	public Date getTimeStamp() {
		return timeStamp;
	}
}
package com.puxtech.reuters.rfa.Common;

import java.util.ArrayList;
import java.util.List;
import org.dom4j.Element;

public class OffSetConfig {
	private List<OffsetSection> sectionList = new ArrayList<OffsetSection>();
	
	public OffSetConfig(Element configElement){
		if(configElement != null){
			for(Object section : configElement.elements()){
				if(section instanceof Element){
						Element sectionEle = (Element) section;
						OffsetSection offsetSection = new OffsetSection(sectionEle);
						this.sectionList.add(offsetSection);
				}
			}
		}
	}

	public List<OffsetSection> getSectionList() {
		return sectionList;
	}
	
}

package com.puxtech.reuters.rfa.Filter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.QuoteOperator;

public class FilterGenerator {
	private static final Log log = LogFactory.getLog(FilterGenerator.class);
	private Element filterConfigEle = null;
	
	public FilterGenerator(Configuration config) throws Exception{
		if(config == null){
			throw new Exception("配置对象为null");
		}
		this.filterConfigEle = config.getFilterConfig();
	}
	
	public List<Filter> buildFilterList(String filterName){
		List<Filter> filterList = new ArrayList<Filter>();
		boolean result = false;
		if(this.filterConfigEle != null){
			@SuppressWarnings("unchecked")
			Iterator<Element> iter = this.filterConfigEle.elementIterator("Filter");
			while(iter.hasNext()){
				Element filterEle = iter.next();
				String name = filterEle.attributeValue("name");
				if(name != null && name.equals(filterName)){
					String filterCls =  filterEle.attributeValue("class");
					try {
						Filter filter = (Filter) Class.forName(filterCls).newInstance();
						@SuppressWarnings("unchecked")
						List<Element> strategyList = filterEle.element("Strategys").elements();
						for(Element strategyEle : strategyList){
							String enableStr = strategyEle.attributeValue("Enable");
							if(enableStr != null && Boolean.valueOf(enableStr)){
								//							String strategyId = strategyEle.attributeValue("id");
								String strategyCls = strategyEle.attributeValue("class");
								FilterStrategy strategy = (FilterStrategy) Class.forName(strategyCls).newInstance();
								filter.addStrategy(strategy);
								String unlimitThreshold = strategyEle.attributeValue("unlimitThreshold");
								if(unlimitThreshold != null){
									if(unlimitThreshold.indexOf("%") != -1){
										unlimitThreshold = unlimitThreshold.substring(0, unlimitThreshold.indexOf("%"));
										DisparityQuoteRule unlimitRule = new DisparityQuoteRule();
										unlimitRule.setPercent(new BigDecimal(unlimitThreshold));
										unlimitRule.setOperator(QuoteOperator.GREATEROREQUAL);
										strategy.setUnlimitRule(unlimitRule);
									}
								}
								Element rulesEle = strategyEle.element("Rules");
								if(rulesEle != null){
									@SuppressWarnings("unchecked")
									List<Element> ruleNodes = rulesEle.elements();
									for(Element ruleEle : ruleNodes){
										String ruleClsName = ruleEle.attributeValue("class");
										String thresholdStr = ruleEle.elementText("Threshold");
										String operatorStr = ruleEle.elementText("Operator");
										String countStr = ruleEle.elementText("FilterCount");
										if(thresholdStr.indexOf("%") != -1){
											thresholdStr = thresholdStr.substring(0, thresholdStr.indexOf("%"));
										}
										DisparityQuoteRule rule = (DisparityQuoteRule) Class.forName(ruleClsName).newInstance();
										rule.setPercent(new BigDecimal(thresholdStr));
										rule.setOperator(QuoteOperator.valueOf(operatorStr));
										rule.setFilteCount(Integer.valueOf(countStr).intValue() - 1);
										strategy.appendRule(rule, new QuotePool(Integer.valueOf(countStr)));
									}
								}
								
								/*Element monElement = strategyEle.element("Mon");
								Element tueElement = strategyEle.element("Tue");
								Element wedElement = strategyEle.element("Wed");
								Element thuElement = strategyEle.element("Thu");
								Element friElement = strategyEle.element("Fri");
								Element satElement = strategyEle.element("Sat");
								Element sunElement = strategyEle.element("Sun");
								if(monElement != null){
									
								}
								if(tueElement != null){
									
								}
								if(wedElement != null){
									
								}
								if(thuElement != null){
									
								}
								if(friElement != null){
									
								}
								if(satElement != null){
									
								}
								if(sunElement != null){
									
								}*/
								
							}
						}
						filterList.add(filter);
					} catch (ClassNotFoundException e) {
						log.error("过滤器配置错误！java类型配置错误！", e);
					} catch (InstantiationException e) {
						log.error("过滤器配置错误！类型实例化失败！", e);
					} catch (IllegalAccessException e) {
						log.error("类型实例化错误！无法访问！", e);
					} 
					result = true;
				}else{
					continue;
				}
			}
			if(!result){
				log.info("build过滤器失败，没有找到name=filterName的过滤器配置");
			}
		}
		return filterList;
	}
}
package com.puxtech.reuters.rfa.Common;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.puxtech.reuters.rfa.Publisher.TCPServerByMina;
import com.puxtech.reuters.rfa.RelayServer.QuoteEventOffSetHandler;
import com.puxtech.reuters.rfa.RelayServer.RelayServer;

public class RefreshOffSetJob implements Job {
	private static final Log offsetLog = LogFactory.getLog("offset");
	private static final Logger LOGGER = LoggerFactory.getLogger(RefreshOffSetJob.class);

	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		List<Offset> offsetList = (List<Offset>) context.getMergedJobDataMap().get("OffSetList");
		offsetLog.info("开始刷新偏移量...");
		offsetLog.info("offsetList=" + offsetList);
		offsetLog.info("配置的偏移量数=" + offsetList != null ? offsetList.size() : 0);
		if(offsetList != null){
			Map<String, BigDecimal> offsetMap = new HashMap<String, BigDecimal>();
			for(Offset offset : offsetList){
				String exchagneCode = offset.getExchangeCode();
				BigDecimal o = offset.getOffset();
				offsetMap.put(exchagneCode, o);
				offsetLog.info("偏移量=[" + exchagneCode + "=" + o.toString());
			} 
			QuoteEventOffSetHandler.refreshOffset(offsetMap);
			offsetLog.info("刷新偏移量完成...");
		}
		Document doc = Configuration.getInstance().getDoc();
		boolean isUpdated = false;
		if(doc != null){
			List<Element> contractElementList = doc.getRootElement().element("Config").element("Contracts").elements("Contract");
			if(offsetList != null && contractElementList != null){
				for(Offset offset : offsetList){
					String exchangeCode = offset.getExchangeCode();
					for(Element contractElement : contractElementList){
						if(contractElement.elementTextTrim("ExchangeCode").equals(exchangeCode)){
							Element quoteSourceElement = contractElement.element("SourceConfig");
							Map<String,String> quoteSourceCfg = offset.getSourceCfg();
							if(quoteSourceCfg != null &&  quoteSourceCfg.size() > 0){
								isUpdated = true;
								quoteSourceElement.clearContent();								
								for(String key : quoteSourceCfg.keySet()){
									quoteSourceElement.addElement(key).setText(quoteSourceCfg.get(key));
								}
							}
						}
					}
				}
			}
			
			if(isUpdated){
				LOGGER.info("合约代码更新，重启所有Consumer");
				Configuration.getInstance().updateConfigurationFile();
				Configuration.getInstance().refreshConfiguration();
				RelayServer.restartConsumers();
			}
		}
	}

}
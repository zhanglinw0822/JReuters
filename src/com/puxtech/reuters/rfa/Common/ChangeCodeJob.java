package com.puxtech.reuters.rfa.Common;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.puxtech.reuters.rfa.Consumer.ConsumerConfig;
import com.puxtech.reuters.rfa.RelayServer.RelayServer;

public class ChangeCodeJob implements Job {
	private static final Log spreadLog = LogFactory.getLog("spread");

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		String oldCode = (String) context.getMergedJobDataMap().get("oldCode");
		String newCode = (String) context.getMergedJobDataMap().get("newCode");
		String exchangeCode = (String) context.getMergedJobDataMap().get("exchangeCode");
		spreadLog.info("切合约定时任务开始...新的合约代码是:" + newCode + ";交易所接入代码是:" + exchangeCode);
		//step1: 修改配置文件，避免重启后仍接收旧合约
		boolean isUpdate = false;
		try {
			Document doc = Configuration.getInstance().getDoc();
			if(doc != null){
				List<Element> contractElementList = doc.getRootElement().element("Config").element("Contracts").elements("Contract");
				for(Element contractElement : contractElementList){
					if(contractElement.elementTextTrim("ExchangeCode").equals(exchangeCode)){
						Element quoteSourceElement = contractElement.element("SourceConfig");
						Element providerElement = null;
						if("Bloomberg".equals(contractElement.elementText("SourceName"))){
							providerElement = quoteSourceElement.element("BloombergCode");
						}else if("RFA".equals(contractElement.elementText("SourceName"))){
							providerElement = quoteSourceElement.element("ReutersCode");
						}else if("ODS".equals(contractElement.elementText("SourceName"))){
							providerElement = quoteSourceElement.element("OdsCode");
						}else if((contractElement.elementText("SourceName").contains("Bloomberg"))){
							providerElement = quoteSourceElement.element("BloombergCode");
						}else if(contractElement.elementText("SourceName").contains("Reuter")){
							providerElement = quoteSourceElement.element("ReutersCode");
						}else if(contractElement.elementText("SourceName").contains("ODS")){
							providerElement = quoteSourceElement.element("OdsCode");
						}
						else{
							//未知行情源配置							
						}
						if(providerElement != null){
							providerElement.setText(newCode);
							isUpdate = true;
						}
					}
				}
			}
		} catch (Exception e) {
			spreadLog.info("切合约任务，修改配置文件时发生异常！", e);
		}
		//step2:保存修改
		Configuration.updateConfigurationFile();
		Configuration.refreshConfiguration();
		
		//step3:重置价格过滤器
		RelayServer.resetFilterBenchMark(exchangeCode);

		//step4:重启Consumer
		ConsumerConfig consumer = RelayServer.getConsumerMap().get(exchangeCode);
		if(consumer != null && consumer instanceof Runnable){
			new Thread((Runnable)consumer).start();
			spreadLog.info("切合约定时任务成功!");
		}else{
			spreadLog.info("切合约定时任务失败!");
		}
	}
}

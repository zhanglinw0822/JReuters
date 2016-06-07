package com.puxtech.reuters.rfa.Common;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.session.IoSession;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.puxtech.reuters.db.ReuterDao;
import com.puxtech.reuters.model.ContractOffset;
import com.puxtech.reuters.offset.Singleton;
import com.puxtech.reuters.rfa.Consumer.ConsumerConfig;
import com.puxtech.reuters.rfa.RelayServer.RelayServer;
import com.puxtech.reuters.rfa.utility.*;
public class SendSpreadJob implements Job {
	private static final Log spreadLog = LogFactory.getLog("spread");
	private static final Log moniterLog = LogFactory.getLog("moniter");
	
	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		String oldCode = (String) context.getMergedJobDataMap().get("oldCode");
		String newCode = (String) context.getMergedJobDataMap().get("newCode");
		String exchangeCode = (String) context.getMergedJobDataMap().get("exchangeCode");
		spreadLog.info("发价差定时任务开始，step1：停止发送行情！");
		//step1:停止发送行情
		ConsumerConfig consumer = RelayServer.getConsumerMap().get(exchangeCode);
		consumer.shutdown();
		spreadLog.info("发价差定时任务进行中，step2：计算并发送价差！");
		//step2: 发送价差
		Date sendTime = (Date) context.getMergedJobDataMap().get("sendTime");
		SpreadConsumer spreadConsumer = RelayServer.getSpreadConsumerMap().get(exchangeCode);

		BigDecimal oldCodeClose = spreadConsumer.getClosePriceMap().get(oldCode);
		if(oldCodeClose == null){
			oldCodeClose = spreadConsumer.getLastPriceMap().get(oldCode);
		}
		BigDecimal newCodeClose = spreadConsumer.getClosePriceMap().get(newCode);
		if(newCodeClose == null){
			newCodeClose = spreadConsumer.getLastPriceMap().get(newCode);
		}
		
		
		spreadLog.info("旧合约代码=" + oldCode + ";旧合约收盘价=" + oldCodeClose + ";新合约代码=" + newCode + ";新合约收盘价=" + newCodeClose);
		
		if(oldCodeClose != null && newCodeClose != null){
			BigDecimal spread = newCodeClose.subtract(oldCodeClose);
			spreadLog.info(exchangeCode + "的价差=" + spread);
			spreadLog.info("配置的发送价差时间=" + sendTime);
			//while((new Date().getTime() - sendTime.getTime()) < 1 * 59 * 1000){
			//根据时间来发送价差不可靠，如果程序服务器的时间和DB时间相差太大，就可能造成无法发送价差
			int i=10;
			while (i-- >= 0) {
				//String spreadStr = Configuration.getInstance().getSpreadFormat();
				ContractOffset co = new ContractOffset();
				co.setNewContract(newCode);
				co.setOccurtime(new Date());
				co.setOffset(spread.doubleValue());
				co.setOldContract(oldCode);
				co.setSrc(Configuration.getInstance().getQuoteSource());
				co.setSwitchDate(DateUtils.parseYyyyyMMddDate(new Date(), "yyyyMMdd"));
				co.setWorldCommodityId(exchangeCode);
				if(Singleton.INSTANCE.getT_contractOffset().containsKey(co.getWorldCommodityId())){
					if (Singleton.INSTANCE.getT_contractOffset().get(co.getWorldCommodityId()).contains(co)) {
						spreadLog.info("内存中已包含此价差对象" + co.toString());
					}else{
						ReuterDao.getInstance().insertT_contractoffset(co);
						Singleton.INSTANCE.refreshT_contractOffset();
						spreadLog.info("入库此价差对象" + co.toString());
					}
				}else{
					ReuterDao.getInstance().insertT_contractoffset(co);
					Singleton.INSTANCE.refreshT_contractOffset();
					spreadLog.info("入库此价差对象" + co.toString());
				}
				//spreadLog.info("send:[" + spreadStr + "]");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					spreadLog.info("sleep been interrupted", e);
				}
			}
		}else{
			spreadLog.error("未取到收盘价！！！");
			for(String key : spreadConsumer.getClosePriceMap().keySet()){
				spreadLog.error("ClosePrice key=" + key + ";value=" + spreadConsumer.getClosePriceMap().get(key));
			}
			for(String key : spreadConsumer.getLastPriceMap().keySet()){
				spreadLog.error("LastPrice key=" + key + ";value=" + spreadConsumer.getLastPriceMap().get(key));
			}
		}
		//发价差的job完成后，刷新下价差内存对象
		Singleton.INSTANCE.refreshT_contractOffset();
	}
}
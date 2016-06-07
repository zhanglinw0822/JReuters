package com.puxtech.reuters.offset;
import com.puxtech.reuters.rfa.Common.ConfigNode;
import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.SubNode;
import com.puxtech.reuters.rfa.utility.CommonAdjust;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import com.puxtech.reuters.db.ReuterDao;
import com.puxtech.reuters.model.ContractOffset;
import com.puxtech.reuters.model.OffsetTreeMap;
import com.puxtech.reuters.model.T_offset;

public enum Singleton {
	INSTANCE;
	private  final Log offsetLog = LogFactory.getLog("offset");
	private  ConcurrentHashMap<String, ContractOffset> optimalContractOffset;
	
	private Singleton() {}
	
	//getter setter begin
	private static String quoteSource = Configuration.getInstance().getQuoteSource();
	
	public static String getServerName() {
		return quoteSource;
	}

	//getter setter end
	
	//处理价差对象 begin
	private  ConcurrentHashMap<String, Vector<ContractOffset>> t_contractOffset;

	public   ConcurrentHashMap<String, Vector<ContractOffset>> getT_contractOffset() {
		t_contractOffset = ReuterDao.getInstance().queryT_contractoffset();
		return t_contractOffset;
	}

	public   void setT_contractOffset(ConcurrentHashMap<String, Vector<ContractOffset>> offset) {
		t_contractOffset = offset;
	}
	
	public   void refreshT_contractOffset(){
		t_contractOffset =  ReuterDao.getInstance().queryT_contractoffset();
		offsetLog.info("刷新价差对象成功：" + t_contractOffset.size());
	}
	//处理价差对象 end
	
	private static ConcurrentHashMap<String,Integer> diffMap = new ConcurrentHashMap<String,Integer>();
	
	public static ConcurrentHashMap<String, Integer> getDiffMap() {
		return diffMap;
	}

	public void loadDiffPeriod() {
		offsetLog.info("loadDiffPeriod");
		refreshDiffPeriod();

		List<SubNode> allList = ReuterDao.queryDiffPeriod(quoteSource, 3);
		for (SubNode cn : allList) {
			addClearJob(cn.getAttenEndTime());
			addRefreshJob(cn.getAttenBeginTime());
		}

	}
	
	public void refreshDiffPeriod() {
		offsetLog.info("refreshDiffPeriod");
		List<SubNode> list = ReuterDao.queryDiffPeriod(quoteSource, 1);
		int trycount=3;
		while(trycount-->0 &&list.isEmpty()){
			offsetLog.warn("没有获得点差衰减周期,10秒后重试");
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}
				list = ReuterDao.queryDiffPeriod(quoteSource, 1);
		}
		
		if(list.isEmpty()){
			offsetLog.error("没有获得点差衰减周期,error");
		}

		for (SubNode cn : list) {
			diffMap.put(cn.getExchangeCode(), cn.getDiffPricePeriod());
		}
		for (String key : diffMap.keySet()) {
			offsetLog.info("价差衰减map：" + key + "," + diffMap.get(key));
		}
	}
	
	
	
	private void addClearJob(java.util.Date date) {
		try {
			SchedulerFactory sf = new StdSchedulerFactory();
			JobDetail jobDetail = JobBuilder.newJob(ClearDiffMapJob.class).build();
			Trigger trigger = TriggerBuilder.newTrigger().startAt(date)
					.withSchedule(SimpleScheduleBuilder.simpleSchedule()).build();
			Scheduler sched = sf.getScheduler();
			sched.scheduleJob(jobDetail, trigger);
			sched.start();
		} catch (SchedulerException e) {
			offsetLog.info("addClearJob发生异常" + e.getMessage());
			//e.printStackTrace();
		}
	}
	
	private void addRefreshJob(java.util.Date date) {
		try {
			SchedulerFactory sf = new StdSchedulerFactory();
			JobDetail jobDetail = JobBuilder.newJob(RefreshDiffMapJob.class).build();
			Trigger trigger = TriggerBuilder.newTrigger().startAt(date).withSchedule(SimpleScheduleBuilder.simpleSchedule()).build();
			Scheduler sched = sf.getScheduler();
			sched.scheduleJob(jobDetail, trigger);
			sched.start();
		} catch (SchedulerException e) {
			offsetLog.info("addRefreshJob发生异常" + e.getMessage());
			//e.printStackTrace();
		}
	}

	
	/**
	 * 投票选出理想价差,保证不同行情源投票选出唯一的价差值
	 * 
	 * @param chm
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public synchronized ConcurrentHashMap<String, ContractOffset> voteOptimalContractOffset() {
		if (t_contractOffset == null || t_contractOffset.size() == 0) {
			return null;
		} else {
			ConcurrentHashMap<String, ContractOffset> result = new ConcurrentHashMap<String, ContractOffset>();
			for (String commodityIdKey : t_contractOffset.keySet()) {
				Vector<ContractOffset> cos = t_contractOffset.get(commodityIdKey);
				OffsetTreeMap<ContractOffset, Integer> sortkey = new OffsetTreeMap<ContractOffset, Integer>();
				for (ContractOffset contractOffset : cos) {
					if (sortkey.containsKey(contractOffset)) {
						sortkey.put(contractOffset, sortkey.get(contractOffset) + 1);
					} else {
						sortkey.put(contractOffset, 0);
					}
				}
				List<ContractOffset> arrayList = new ArrayList(sortkey.entrySet());
				Collections.sort(arrayList, new Comparator() {
					public int compare(Object o1, Object o2) {
						Map.Entry obj1 = (Map.Entry) o1;
						Map.Entry obj2 = (Map.Entry) o2;
						return ((Integer) obj2.getValue()).compareTo((Integer) obj1.getValue());
					}
				});
				if (arrayList.size() > 0) {
					result.put(commodityIdKey, (ContractOffset) ((Map.Entry) arrayList.get(0)).getKey());
					offsetLog.info("最优价差：" + result.get(commodityIdKey));
				} else {
					return null;
				}
			}
			return result;
		}
	}
	
	/**
	 * 计算价差（价差 = 过去时间 * 原始价差 / 价差周期）
	 * 
	 * @param contractOffset
	 *            价差对象
	 * @return
	 */
	public synchronized ConcurrentHashMap<String, Double> calDiff() {
		ConcurrentHashMap<String, Double> result = new ConcurrentHashMap<String, Double>();
		optimalContractOffset = voteOptimalContractOffset();
		if (optimalContractOffset == null) {
			offsetLog.info("optimalContractOffset为空");
			return null;
		} else {
			for (String commodityId : optimalContractOffset.keySet()) {
				Long nowMill = Calendar.getInstance().getTimeInMillis();
				Long quotationMill =  optimalContractOffset.get(commodityId).getOccurtime().getTime();
				Long diffSec = (nowMill - quotationMill) / 1000;
				if (diffMap != null && diffMap.keySet().contains(commodityId)) {
					Double tempD = diffSec * 1d / diffMap.get(commodityId);
					if (tempD >= 1 || tempD <= 0) {
						result.put(commodityId, 0d);
						T_offset co = new T_offset();
						co.setCommodityId(commodityId);
						co.setNowTime(new Date(nowMill));
						co.setRecTime(new Date(quotationMill));
						co.setDiffTime((nowMill - quotationMill) / 1000);
						co.setDownset(0d);
						co.setOffset(optimalContractOffset.get(commodityId).getOffset());
						co.setDiffPricePeriod(diffMap.get(commodityId));
						co.setSrc(quoteSource);
						offsetLog.info("更新价差表：" + co.toString());
						ReuterDao.updateT_OFFSET(co);
					} else {
						Double tempResult = optimalContractOffset.get(commodityId).getOffset() * Math.abs(1 - tempD);
						result.put(commodityId, tempResult);
						T_offset co = new T_offset();
						co.setCommodityId(commodityId);
						co.setNowTime(new Date(nowMill));
						co.setRecTime(new Date(quotationMill));
						co.setDiffTime((nowMill - quotationMill) / 1000);
						co.setDownset(tempResult);
						co.setOffset(optimalContractOffset.get(commodityId).getOffset());
						co.setDiffPricePeriod(diffMap.get(commodityId));
						co.setSrc(quoteSource);
						offsetLog.info("更新价差表：" + co.toString());
						ReuterDao.updateT_OFFSET(co);
					}
				}
			}
		}
		return result;
	}
	
	private static void testVoteSys(){
		ConcurrentHashMap<String, Vector<ContractOffset>> offset = new ConcurrentHashMap<String, Vector<ContractOffset>>();
		Vector<ContractOffset> vectors = new Vector<ContractOffset>();
		Date date = Calendar.getInstance().getTime();
		
		ContractOffset co1 = new ContractOffset();
		co1.setNewContract("new");
		co1.setOldContract("old");
		co1.setOccurtime(date);
		co1.setOffset(12.3d);
		co1.setSrc("src1");
		co1.setSwitchDate("20150528");
		co1.setWorldCommodityId("WTI");
		
		
		ContractOffset co2 = new ContractOffset();
		co2.setNewContract("new");
		co2.setOldContract("old");
		co2.setOccurtime(date);
		co2.setOffset(12.4d);
		co2.setSrc("src2");
		co2.setSwitchDate("20150528");
		co2.setWorldCommodityId("WTI");
		
		vectors.add(co1);
		vectors.add(co2);
		
//		ContractOffset co3 = new ContractOffset();
//		co3.setNewContract("new");
//		co3.setOldContract("old");
//		co3.setOccurtime(date);
//		co3.setOffset(12.4d);
//		co3.setSrc("src3");
//		co3.setSwitchDate("20150528");
//		co3.setWorldCommodityId("WTI");
//		vectors.add(co3);
		offset.put("WTI", vectors);
		
		Singleton.INSTANCE.setT_contractOffset(offset);
		while (true) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ConcurrentHashMap<String, ContractOffset> chm = Singleton.INSTANCE.voteOptimalContractOffset();
			for (String key : chm.keySet()) {
				// System.out.println(key + "," + chm.get(key));
				if (chm.get(key).getOffset().equals(12.4)) {
					System.out.println(chm.get(key).getOffset());
				}
			}
		}
	}
	
	public static void main(String[] args){
		Singleton.testVoteSys();
		
	}
}
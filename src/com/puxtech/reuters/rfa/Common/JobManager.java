package com.puxtech.reuters.rfa.Common;

import static org.quartz.JobBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.TriggerBuilder.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import com.puxtech.reuters.rfa.Consumer.SpreadLoginClient;

public class JobManager {
	private static final Log log = LogFactory.getLog("quartz");
	private static final SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
	private static final SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
	private static final SimpleDateFormat dayFormat = new SimpleDateFormat("dd");
	private static final SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
	private static final SimpleDateFormat minFormat = new SimpleDateFormat("mm");
	private static final SimpleDateFormat secondFormat = new SimpleDateFormat("ss");

	private static SchedulerFactory schedulerFactory = new org.quartz.impl.StdSchedulerFactory();
	private static String JOB_GROUP_NAME = "RECONFIG_JOBGROUP_NAME";  
	private static String TRIGGER_GROUP_NAME = "RECONFIG_TRIGGERGROUP_NAME";
	private static List<TriggerKey> triggerKeyList = new ArrayList<TriggerKey>();
	private static Scheduler offSetScheduler = null;
	static{
		if(offSetScheduler == null){
			try {
				offSetScheduler = schedulerFactory.getScheduler();
			} catch (SchedulerException e) {
				log.info("初始化offSetScheduler失败", e);
			}
		}
	}

	private static String getCronExpress(Date date) {
		return secondFormat.format(date) + " " + minFormat.format(date) + " " + hourFormat.format(date) + " " + dayFormat.format(date) + " " + monthFormat.format(date) + " ? "
				+ yearFormat.format(date);
	}

	public static void addSpreadJob() throws SchedulerException{	
		SpreadConfig spreadConfig = Configuration.getInstance().getSpreadConfig();
		if(spreadConfig != null && spreadConfig.getConfigList().size() > 0){			
			List<ConfigNode> configNodeList = spreadConfig.getConfigList();
			for (int j = 0; j < configNodeList.size(); j++){
				ConfigNode cfgNode = configNodeList.get(j);
				List<SubNode> subNodeList = cfgNode.getNodeList();
				Collections.sort(subNodeList);
				String exchangeCode = cfgNode.getExchangeCode();
				for(int k = 0; k < subNodeList.size() - 1; k++){
					SubNode subNodeOld = subNodeList.get(k);
					SubNode subNodeNew = subNodeList.get(k + 1);
					String oldCode = subNodeOld.getContractCode();
					String newCode = subNodeNew.getContractCode();
					String sendCronExpress = getCronExpress(subNodeOld.getAttenBeginTime());
					String changeCronExpress = getCronExpress(subNodeOld.getChangeContractTime());
					//添加发价差定时任务
					Map<String, Object> sendSpreadCfgMap = new HashMap<String, Object>();
					sendSpreadCfgMap.put("oldCode", oldCode);
					sendSpreadCfgMap.put("newCode", newCode);
					sendSpreadCfgMap.put("exchangeCode", exchangeCode);
					sendSpreadCfgMap.put("sendTime", subNodeOld.getAttenBeginTime());
					Scheduler sched = schedulerFactory.getScheduler();
					sched.start();
					// define the job and tie it to our HelloJob class
					JobDetail job = newJob(SendSpreadJob.class)
							.withIdentity("sendSpreadJob" + k, JOB_GROUP_NAME)
							.build();
					// Trigger the job to run now, and then every 40 seconds
					Trigger trigger = newTrigger()
							.withIdentity("sendSpreadTrigger" + k, TRIGGER_GROUP_NAME)
							.withSchedule(cronSchedule(sendCronExpress))
							.forJob(job.getKey())
							.usingJobData(new JobDataMap(sendSpreadCfgMap))
							.build();
					// Tell quartz to schedule the job using our trigger
					try {
						sched.scheduleJob(job, trigger);
						log.info("添加一个发价差定时任务，任务类型=" + job.getJobClass().getName() + ";定时表达式:" + sendCronExpress);
					} catch (SchedulerException e) {
						log.error("添加定时任务错误！", e);
					}
					
					//添加切合约定时任务
					Map<String, Object> changeCodeCfgMap = new HashMap<String, Object>();
					changeCodeCfgMap.put("oldCode", oldCode);
					changeCodeCfgMap.put("newCode", newCode);
					changeCodeCfgMap.put("exchangeCode", exchangeCode);
					sched = schedulerFactory.getScheduler();
					sched.start();
					// define the job and tie it to our HelloJob class
					job = newJob(ChangeCodeJob.class)
							.withIdentity("changeCodeJob" + k, JOB_GROUP_NAME)
							.build();
					// Trigger the job to run now, and then every 40 seconds
					trigger = newTrigger()
							.withIdentity("changeCodeTrigger" + k, TRIGGER_GROUP_NAME)
							.withSchedule(cronSchedule(changeCronExpress))
							.forJob(job.getKey())
							.usingJobData(new JobDataMap(changeCodeCfgMap))
							.build();
					// Tell quartz to schedule the job using our trigger
					sched.scheduleJob(job, trigger);
					log.info("添加一个切合约定时任务，任务类型=" + job.getJobClass().getName() + ";定时表达式:" + changeCronExpress);
				}
			}
		}else{
			log.info("未配置价差!");
		}
	}

	public static void addRefreshConfigJob() throws SchedulerException{
		Scheduler sched = schedulerFactory.getScheduler();
		sched.start();
		// define the job and tie it to our HelloJob class
		JobDetail job = newJob(RefreshConfigJob.class)
				.withIdentity("refreshConfigJob", JOB_GROUP_NAME)
				.build();
		// Trigger the job to run now, and then every 40 seconds
		Trigger trigger = newTrigger()
				.withIdentity("refreshConfigTrigger", TRIGGER_GROUP_NAME)
				.withSchedule(cronSchedule(Configuration.getInstance().getCronExpress()))
				.forJob(job.getKey())
				.build();
		// Tell quartz to schedule the job using our trigger
		sched.scheduleJob(job, trigger);
	}

	public static void unScheduleOffSetJobs(){
		synchronized (triggerKeyList) {
			if(offSetScheduler != null){
				try {
					offSetScheduler.unscheduleJobs(triggerKeyList);
					triggerKeyList.clear();
				} catch (SchedulerException e) {
					log.info("unScheduleOffSetJobs失败", e);
				}
			}
		}
	}

	public static void addRefreshOffSetJob(String jobName, String triggerName, String cronExpress, Map dataMap) throws SchedulerException {
		if(offSetScheduler != null){
			if(offSetScheduler.isInStandbyMode()){	
				offSetScheduler.start();
			}
			// define the job and tie it to our HelloJob class
			JobDetail job = newJob(RefreshOffSetJob.class)
					.withIdentity(jobName, JOB_GROUP_NAME)
					.build();
			// Trigger the job to run now, and then every 40 seconds
			Trigger trigger = newTrigger()
					.withIdentity(triggerName, TRIGGER_GROUP_NAME)
					.withSchedule(cronSchedule(cronExpress))
					.forJob(job.getKey())
					.usingJobData(new JobDataMap(dataMap))
					.build();
			synchronized (triggerKeyList) {	
				triggerKeyList.add(trigger.getKey());
			}
			// Tell quartz to schedule the job using our trigger
			offSetScheduler.scheduleJob(job, trigger);
		}
	}
}
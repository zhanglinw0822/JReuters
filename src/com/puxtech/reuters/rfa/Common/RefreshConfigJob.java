package com.puxtech.reuters.rfa.Common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import com.puxtech.reuters.rfa.RelayServer.RelayServer;
import com.puxtech.reuters.rfa.utility.MD5;

public class RefreshConfigJob implements Job {
	private static final Log offsetLog = LogFactory.getLog("offset");
	private static final SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
	private static final SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
	private static final SimpleDateFormat dayFormat = new SimpleDateFormat("dd");
	private static final SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
	private static final SimpleDateFormat minFormat = new SimpleDateFormat("mm");
	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		//step1: 获取配置
		boolean isConfigUpdate = false;
		File localConfigFile = Configuration.getInstance().getConfigFile();
		File remoteConfigFile = new File(Configuration.getInstance().getSourcePath());
		if(localConfigFile != null && localConfigFile.exists() && remoteConfigFile != null && remoteConfigFile.exists()){
			isConfigUpdate = MD5.getFileMD5(remoteConfigFile).equals(MD5.getFileMD5(localConfigFile));
		}
		if(isConfigUpdate){
			try {
				FileInputStream fis = new FileInputStream(remoteConfigFile);
				FileOutputStream fos = new FileOutputStream(localConfigFile);
				byte[] buff = new byte[1024];
				int readLength = -1;
				while((readLength=fis.read(buff)) != -1){
					fos.write(buff, 0, readLength);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		//step2: 刷新配置
		if(!isConfigUpdate)
			return;
		Configuration.refreshConfiguration();
		//step3: 配置生效
		//偏移量配置生效
	}
}
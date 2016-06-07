package com.puxtech.reuters.offset;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class RefreshDiffMapJob implements Job {
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		Singleton.INSTANCE.refreshDiffPeriod();
	}
}

package com.puxtech.reuters.offset;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class ClearDiffMapJob implements Job {
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		Singleton.INSTANCE.getDiffMap().clear();
	}
}

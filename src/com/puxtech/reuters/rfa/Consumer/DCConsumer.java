package com.puxtech.reuters.rfa.Consumer;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.puxtech.dc.common.CallbackBehaviourImpl;
import com.puxtech.dc.common.DCHelper;
import com.puxtech.reuters.rfa.Common.QuoteSource;

public class DCConsumer implements Runnable, ConsumerConfig{
	private static final Log moniterLog = LogFactory.getLog("moniter");
	private String addr = "183.60.143.126";
	private int port = 30003;
	private String userName = "wgrk";
	private String password = "wgrk88";
	private QuoteSource quoteSource = null;

	private void initDcConfig(){
		if(this.quoteSource != null){
			this.addr = this.quoteSource.getProperty("Addr");
			this.port = this.quoteSource.getPropertyInt("Port");
			this.userName = this.quoteSource.getProperty("UserName");
			this.password = this.quoteSource.getProperty("Pwd");
		}
	}

	public static void main(String[] args) {

	}
	public boolean stop = false;
	public void shutdown(){
		stop = true;
	}
	
	@Override
	public void run() {
		moniterLog.info(this.getClass().getName() + "started！");
		if(this.quoteSource == null){
			System.out.println("初始化行情数据源失败！");
			return;
		}
		boolean success = false;
		DCHelper dc = null;
		initDcConfig();
		dc = new DCHelper();
		dc.DC_SetSerAddr(addr, port, userName, password);
		dc.setCallBack(new CallbackBehaviourImpl(this.quoteSource), LogFactory.getLog("dc"));
		success = dc.DC_Init();
		if(!success){
			System.out.println("初始化失败！");
			return;
		}
		while(!stop){
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if(dc != null)
			dc.DC_Stop();
		
		moniterLog.info(this.getClass().getName() + "shutdown！");
	}

	public QuoteSource getQuoteSource() {
		return quoteSource;
	}

	public void setQuoteSource(QuoteSource quoteSource) {
		this.quoteSource = quoteSource;
	}

	@Override
	public void restart() {
		// TODO Auto-generated method stub
		
	}
}

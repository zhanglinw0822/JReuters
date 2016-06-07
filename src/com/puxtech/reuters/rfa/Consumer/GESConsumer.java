package com.puxtech.reuters.rfa.Consumer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.ifx.apicore.TradeManager;
import com.ifx.exception.FXConnectionException;
import com.ifx.model.FXUserInfo;
import com.ifx.quote.QuotePriceUpdate;
import com.ifx.trade.FXResponse;
import com.ifx.trade.listener.QuoteListener;
import com.lmax.disruptor.RingBuffer;
import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.Contract;
import com.puxtech.reuters.rfa.Common.Quote;
import com.puxtech.reuters.rfa.Common.QuoteSource;
import com.puxtech.reuters.rfa.RelayServer.QuoteEvent;
import com.puxtech.reuters.rfa.RelayServer.RelayServer;

public class GESConsumer implements Runnable, ConsumerConfig {
	private static final Log monitorLog = LogFactory.getLog("moniter");
	private static final Log gesLog = LogFactory.getLog("GES");
	private QuoteSource quoteSource;
	private List<Contract> contractList = new ArrayList<Contract>();
	private TradeManager tradeMgr;
	private FXUserInfo user;
	private GESQuoteListener quoteListener = new GESQuoteListener();
	private String[] ASIP = {};
	private int[] ASFromPort = {};
	private int[] ASToPort = {};
	private boolean[] ASEnableSSL = {};
	private String[] QSIP = {};
	private int[] QSFromPort = {};
	private int[] QSToPort = {};
	private String ClientCode = "";
	private String Password = "";
	private boolean override = true;

	public void init(){
		if(this.quoteSource != null){
			this.ASIP = this.quoteSource.getProperty("ASIP") != null ? this.quoteSource.getProperty("ASIP").split(",",-1) : new String[]{};
			this.ASFromPort = this.quoteSource.getProperty("ASFromPort") != null ? splitToInt(this.quoteSource.getProperty("ASFromPort"),",") : new int[]{};
			this.ASToPort = this.quoteSource.getProperty("ASToPort") != null ? splitToInt(this.quoteSource.getProperty("ASToPort"),",") : new int[]{};
			this.ASEnableSSL = this.quoteSource.getProperty("ASEnableSSL") != null ? splitToBoolean(this.quoteSource.getProperty("ASEnableSSL"), ",") : new boolean[]{};
			this.QSIP = this.quoteSource.getProperty("QSIP") != null ? this.quoteSource.getProperty("QSIP").split(",",-1) : new String[]{};
			this.QSFromPort = this.quoteSource.getProperty("QSFromPort") != null ? splitToInt(this.quoteSource.getProperty("QSFromPort"),",") : new int[]{};
			this.QSToPort = this.quoteSource.getProperty("QSToPort") != null ? splitToInt(this.quoteSource.getProperty("QSToPort"),",") : new int[]{};
			this.ClientCode = this.quoteSource.getProperty("ClientCode");
			this.Password = this.quoteSource.getProperty("Password");
			
			this.contractList.clear();
			List<Contract> contracts = Configuration.getInstance().getQuoteSourceContractMap().get(this.quoteSource.getName());
			for(Contract contract : contracts){
				if(contract.isVirtual()){
					continue;
				}else{
					this.contractList.add(contract);
				}
			}
		}
		tradeMgr = new TradeManager(ASIP, ASFromPort, ASToPort, ASEnableSSL,
				QSIP, QSFromPort, QSToPort);
	}
	
	private int[] splitToInt(String str, String delim) {
		String[] strArr = str.split(delim, -1);
		int[] intArr = new int[strArr.length];
		for (int i=0; i < strArr.length; i++) {
			intArr[i] = Integer.parseInt(strArr[i]);
		}
		return intArr;
	}

	private boolean[] splitToBoolean(String str, String delim) {
		String[] strArr = str.split(delim, -1);
		boolean[] boolArr = new boolean[strArr.length];
		for (int i=0; i < strArr.length; i++) {
			boolArr[i] = Boolean.parseBoolean(strArr[i]);
		}
		return boolArr;   
	}

	private boolean connectAndLogin() throws Exception {
		tradeMgr.setQSAutoReconnect(true);
		tradeMgr.setASAutoReconnect(true);
		try {
			tradeMgr.connectToAppServer();
		} catch (FXConnectionException ce) {
			monitorLog.error("连接过程中发生异常",ce);
			return true;
		}
		FXResponse resLogin = tradeMgr.loginUser(ClientCode, Password, override);
		monitorLog.info(resLogin.getReply() + ":" + resLogin.getMessage());
		if (resLogin.getReply() == 1 || resLogin.getReply() == 2) {
			user = (FXUserInfo) resLogin.getResponseObj();
			if (user.getSecurityStatus() < 0 && user.getSecurityStatus() != -2) {
				//需要修改密码
				monitorLog.warn("GES账号需要修改密码！");
			}
			if (user.getIsAgent()) {
				tradeMgr.authUser();
			}
			tradeMgr.startQuoteUpdateListener();
			tradeMgr.addQuoteListener(quoteListener);
			return true;
		}
		return false;
	}

	@Override
	public void setQuoteSource(QuoteSource quoteSource) {
		this.quoteSource = quoteSource;
	}

	@Override
	public void shutdown() {
		if(tradeMgr != null){
			tradeMgr.removeQuoteListener(quoteListener);
			try {
				tradeMgr.logoutUser();
			} catch (Exception e) {
				monitorLog.info(this.getClass().getName() + "logoutUser异常", e);
			}		
		}
	}

	@Override
	public void restart() {
		this.shutdown();
		this.runConsumer();
	}
	
	private static final void dispatchQuote(Quote quote){
		try {
			RingBuffer<QuoteEvent> ringBuffer = RelayServer.getRingBuffer();
			if(ringBuffer != null && quote != null){
				//monitorLog.info("开始申请下一个ringbuffer可用位置");
				long sequence = ringBuffer.next();
				//monitorLog.info("申请下一个ringbuffer可用位置成功");
				QuoteEvent event = ringBuffer.get(sequence);
				event.setValue(quote);
				ringBuffer.publish(sequence);
			}
		} catch (Exception e) {
			monitorLog.error("dispatchQuote过程中出现异常", e);
		}
	}

	class GESQuoteListener implements QuoteListener{

		@Override
		public void onQuoteChange(Map fullQuoteMap, ArrayList updatedQuoteList, ArrayList newQuoteList) {
			for (int i = 0; i < updatedQuoteList.size(); ++i) {
				QuotePriceUpdate update = (QuotePriceUpdate) updatedQuoteList.get(i);
				gesLog.info(update.getQSDescription() + ":{" + "bid=" + update.getBid() + ",ask=" +update.getAsk() + "\r\n");
				String gesCode = update.getQSDescription();
				for(Contract contract : contractList){
    				if(gesCode.equals(contract.getSourceCfg().get("GESCode"))){
    					int algorithm = contract.getPriceAlgorithm();
    					Quote quote = null;
    					if(algorithm == 1){
    						monitorLog.info("ges接入商不支持算法1，默认按算法2执行，即取ask价和bid价的平均值");
    						quote = new Quote();
    						quote.priceTime = new Date();
    						quote.exchangeCode = contract.getExchangeCode();
    						quote.reutersCode = gesCode;
    						quote.bidPrice = new BigDecimal(update.getBid());
    						quote.askPrice = new BigDecimal(update.getAsk());
    						int scale = contract.getScale();
    						quote.newPrice = quote.askPrice.add(quote.bidPrice).divide(new BigDecimal(2), scale, RoundingMode.HALF_UP);
    					}else if(algorithm == 2){
    						quote = new Quote();
    						quote.priceTime = new Date();
    						quote.exchangeCode = contract.getExchangeCode();
    						quote.reutersCode = gesCode;
    						quote.bidPrice = new BigDecimal(update.getBid());
    						quote.askPrice = new BigDecimal(update.getAsk());
    						int scale = contract.getScale();
    						quote.newPrice = quote.askPrice.add(quote.bidPrice).divide(new BigDecimal(2), scale, RoundingMode.HALF_UP);
    					}
    					if(quote != null){
    						dispatchQuote(quote);
    					}
    				}
    			}
			}			
		}

		@Override
		public void onQuoteServerLost() {
			monitorLog.info("ges连接断开，3秒后重启接收服务！");
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				monitorLog.info("3秒等待过程被异常中断！");
			}
			monitorLog.info("重启GES接收服务！");
			restart();
		}

		@Override
		public void onQuoteServerResumed() {
			monitorLog.info("ges连接恢复！");
		}
	}
	boolean restart = false;
	private void runConsumer(){	
		monitorLog.info("ges行情接收服务初始化开始...");
		this.init();
		monitorLog.info("ges行情接收服务初始化完成...");
		try {
			monitorLog.info("ges行情接收服务连接开始...");
			this.connectAndLogin();
			monitorLog.info("ges行情接收服务连接完成...");
		} catch (Exception e) {
			monitorLog.error("ges行情异常，重新连接", e);
		}
	}

	@Override
	public void run() {
		this.runConsumer();
	}
}
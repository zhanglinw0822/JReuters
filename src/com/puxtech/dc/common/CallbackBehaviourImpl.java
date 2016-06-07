package com.puxtech.dc.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lmax.disruptor.RingBuffer;
import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.Contract;
import com.puxtech.reuters.rfa.Common.Quote;
import com.puxtech.reuters.rfa.Common.QuoteSignal;
import com.puxtech.reuters.rfa.Common.QuoteSource;
import com.puxtech.reuters.rfa.RelayServer.QuoteEvent;
import com.puxtech.reuters.rfa.RelayServer.RelayServer;

public class CallbackBehaviourImpl implements CallbackBehaviour {
	private static final Log logger = LogFactory.getLog("DC");
	private QuoteSource quoteSource;
	List<Contract> contractList = new ArrayList<Contract>();
	public CallbackBehaviourImpl(QuoteSource quoteSource) {
		super();
		this.quoteSource = quoteSource;
		this.init();
	}
	private void init(){
		List<Contract> contracts = Configuration.getInstance().getQuoteSourceContractMap().get(this.quoteSource.getName());
		for(Contract contract : contracts){
			if(contract.isVirtual()){
				continue;
			}else{
				this.contractList.add(contract);
			}
		}
	}

	@Override
	public void OnDCConnect(String pszSerAddr, boolean bConnectSuc) {
		logger.info("��ַ��"+pszSerAddr + "����" +bConnectSuc);
	}

	@Override
	public void OnDCClose(String pszSerAddr) {
		logger.info("��ַ��"+pszSerAddr + "���ӹر�");
	}

	@Override
	public void OnDCAuth(String pszSerAddr, boolean bAuthSuc, String pszErrMsg) {
		if(bAuthSuc){
			logger.info("��ַ��"+pszSerAddr + "��֤�ɹ���");
		}else{
			logger.info("��ַ��"+pszSerAddr + "��֤ʧ�ܣ�" + pszErrMsg);
		}
	}

	@Override
	public void OnDCLog(String pszSerAddr, String pszMsg) {
		logger.info("�յ���־��" + pszMsg);
	}

	@Override
	public void OnDCPrice(String pszSerAddr, String market, String code, int updateFlag, double price, long time) {
		Date priceTime = new Date();
		if((updateFlag & DCHelper.EDCPUFNow) != 0){	
			for(Contract contract : this.contractList){
				if(contract.getSourceCfg().get("DCCode").equals(code)){
					logger.info("���յ��������ݣ�");
					logger.info("����ʱ��="+priceTime);
					logger.info("�г�="+market+";����="+code);
					logger.info("���±�־="+updateFlag+"::"+ (updateFlag & DCHelper.EDCPUFNow));
					logger.info("���¼�="+price);
					Quote quote = new Quote();
					BigDecimal newPrice = new BigDecimal(String.valueOf(price));
					if("OILC".equals(code) && "LDFUT".equals(market)){
						newPrice = newPrice.divide(new BigDecimal(100.0), 3, RoundingMode.HALF_UP);
					}
					quote.exchangeCode = contract.getExchangeCode();
					quote.dcCode = code;
					quote.dcMarket = market;
					quote.newPrice = newPrice;
					quote.askPrice = newPrice;
					quote.priceTime = priceTime;
					dispatchQuote(quote);
				}
			}
		}
	}

	@Override
	public void OnDCFinance(String pszSerAddr, String pstFinance) {

	}
	List<QuoteSignal> quoteSignList = Configuration.getInstance().getDcQuoteSignList();

	public List<QuoteSignal> getQuoteSignList() {
		return quoteSignList;
	}

	public void setQuoteSignList(List<QuoteSignal> quoteSignList) {
		this.quoteSignList = quoteSignList;
	}

	private static final void dispatchQuote(Quote quote){

		RingBuffer<QuoteEvent> ringBuffer = RelayServer.getRingBuffer();
		if(ringBuffer != null && quote != null){
			//log.info("��ʼ������һ��ringbuffer����λ��");
			long sequence = ringBuffer.next();
			//log.info("������һ��ringbuffer����λ�óɹ�");
			QuoteEvent event = ringBuffer.get(sequence);
			event.setValue(quote);
			ringBuffer.publish(sequence);
		}


	}
}
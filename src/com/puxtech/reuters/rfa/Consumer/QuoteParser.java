package com.puxtech.reuters.rfa.Consumer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import com.lmax.disruptor.RingBuffer;
import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.Contract;
import com.puxtech.reuters.rfa.Common.Quote;
import com.puxtech.reuters.rfa.Common.QuoteSignal;
import com.puxtech.reuters.rfa.Common.QuoteSource;
import com.puxtech.reuters.rfa.RelayServer.QuoteEvent;
import com.puxtech.reuters.rfa.RelayServer.RelayServer;
import com.puxtech.reuters.rfa.utility.GenericOMMParser;
import com.reuters.rfa.dictionary.FidDef;
import com.reuters.rfa.dictionary.FieldDictionary;
import com.reuters.rfa.omm.OMMAttribInfo;
import com.reuters.rfa.omm.OMMData;
import com.reuters.rfa.omm.OMMEntry;
import com.reuters.rfa.omm.OMMFieldEntry;
import com.reuters.rfa.omm.OMMFieldList;
import com.reuters.rfa.omm.OMMIterable;
import com.reuters.rfa.omm.OMMMsg;
import com.reuters.rfa.omm.OMMTypes;
import com.reuters.rfa.omm.OMMMsg.MsgType;

public final class QuoteParser {
	private static final Log log = LogFactory.getLog("moniter");
	private Map<String, MarketPrice> marketPriceMap = new HashMap<String, QuoteParser.MarketPrice>();
	
	class MarketPrice{
		private BigDecimal ySettlePrice = new BigDecimal("0.0");
		private BigDecimal yClosePrice = new BigDecimal("0.0");
		private BigDecimal openPrice = new BigDecimal("0.0");
		private BigDecimal highPrice = new BigDecimal("0.0");
		private BigDecimal lowPrice = new BigDecimal("0.0");
		public BigDecimal getySettlePrice() {
			return ySettlePrice;
		}
		public void setySettlePrice(BigDecimal ySettlePrice) {
			this.ySettlePrice = ySettlePrice;
		}
		public BigDecimal getyClosePrice() {
			return yClosePrice;
		}
		public void setyClosePrice(BigDecimal yClosePrice) {
			this.yClosePrice = yClosePrice;
		}
		public BigDecimal getOpenPrice() {
			return openPrice;
		}
		public void setOpenPrice(BigDecimal openPrice) {
			this.openPrice = openPrice;
		}
		public BigDecimal getHighPrice() {
			return highPrice;
		}
		public void setHighPrice(BigDecimal highPrice) {
			this.highPrice = highPrice;
		}
		public BigDecimal getLowPrice() {
			return lowPrice;
		}
		public void setLowPrice(BigDecimal lowPrice) {
			this.lowPrice = lowPrice;
		}
	}
	
	private static final void dispatchQuote(Quote quote, OMMData oData){
		try {
			RingBuffer<QuoteEvent> ringBuffer = RelayServer.getRingBuffer();
			if(ringBuffer != null && quote != null){
				//log.info("开始申请下一个ringbuffer可用位置");
				long sequence = ringBuffer.next();
				//log.info("申请下一个ringbuffer可用位置成功");
				QuoteEvent event = ringBuffer.get(sequence);
				event.setValue(quote);
				ringBuffer.publish(sequence);
			}
		} catch (Exception e) {
			log.error("解析OMMData过程中出现异常", e);
			log.error("datatype=" + oData.getType());
			log.error("data=" + oData);
		}
	}

	QuoteSource quoteSource = null;
	List<String> itemNames = new ArrayList<String>();
	Map<String, Contract> contractMap = new HashMap<String, Contract>();

	public QuoteSource getQuoteSource() {
		return quoteSource;
	}

	public void setQuoteSource(QuoteSource quoteSource) {
		this.quoteSource = quoteSource;
	}

	public void initContracts(){
		if(this.quoteSource != null && Configuration.getInstance().getQuoteSourceContractMap().get(this.quoteSource.getName()) != null){
			List<Contract> contractList = Configuration.getInstance().getQuoteSourceContractMap().get(this.quoteSource.getName());
			for(Contract contractCfg : contractList){
				if(contractCfg.isVirtual()){
					continue;
				}
				String recvReuterCode = contractCfg.getSourceCfg().get("ReutersCode");
				this.itemNames.add(recvReuterCode);
				this.contractMap.put(recvReuterCode, contractCfg);
			}
		}
	}

	public final void parseQuoteFromOMMMsg(OMMMsg msg) throws Exception{
		if(msg != null){
			if(msg.has(OMMMsg.HAS_ATTRIB_INFO) && msg.getMsgType() != MsgType.REFRESH_RESP){
				OMMAttribInfo attribInfo = msg.getAttribInfo();
				if(attribInfo != null && attribInfo.has(OMMAttribInfo.HAS_NAME)){
					String name = attribInfo.getName();
					System.out.println("get a msg, itemName=" + name);
					if(itemNames != null && itemNames.contains(name)){
						if(msg.getDataType() != OMMTypes.NO_DATA){
							OMMData data = msg.getPayload();
							if(OMMTypes.isAggregate(data.getType())){
								short dataType = data.getType();
								if(dataType == OMMTypes.FIELD_LIST){
									short dicId = ((OMMFieldList)data).getDictId();
									FieldDictionary dic = GenericOMMParser.getDictionary(dicId);
									if(dic == null){
										log.error("获取字典失败，字典ID=" + dicId);
										return;
									}
									Quote quote = null;
									Date now = new Date();
									if(1 == contractMap.get(name).getPriceAlgorithm()){
										FidDef fiddef = null;
										OMMData oData = null;
										for (Iterator iter = ((OMMIterable)data).iterator(); iter.hasNext();){
											Object entryObj = iter.next();
											if (entryObj instanceof OMMFieldEntry) {
												OMMFieldEntry entry = (OMMFieldEntry) entryObj;
												System.out.println("contract name=" + name + ";field name="+dic.getFidDef(entry.getFieldId()).getName());
												if(entry.getFieldId() == Configuration.TRDPRC_1){
													quote = new Quote();
													fiddef = dic.getFidDef(entry.getFieldId());
													if (fiddef != null){
														oData = entry.getData(fiddef.getOMMType());
														BigDecimal price = new BigDecimal(oData.toString());
														quote.exchangeCode = contractMap.get(name).getExchangeCode();
														quote.reutersCode = name;
														quote.newPrice = price;
														quote.askPrice = price;
														quote.priceTime = now;
													}													
												}else{
													MarketPrice marketPrc = this.marketPriceMap.get(contractMap.get(name).getExchangeCode());
													switch(entry.getFieldId()){
													case Configuration.HST_CLOSE:
														fiddef = dic.getFidDef(entry.getFieldId());
														oData = null;
														if (fiddef != null){
															oData = entry.getData(fiddef.getOMMType());
															marketPrc.setyClosePrice(new BigDecimal(oData.toString()));
														}
														break;
													}
												}
											}
										}
										if(quote != null){											
											MarketPrice mktPrc = this.marketPriceMap.get(contractMap.get(name).getExchangeCode());
											if(mktPrc != null){														
												quote.highPrice = mktPrc.getHighPrice();
												quote.lowPrice = mktPrc.getLowPrice();
												quote.openPrice = mktPrc.getOpenPrice();
												quote.yClosePrice = mktPrc.getyClosePrice();
												quote.ySettlePrice = mktPrc.getySettlePrice();
											}
											dispatchQuote(quote, oData);
										}
									}else if(2 == contractMap.get(name).getPriceAlgorithm()){
										OMMData oData = null;
										boolean getBid = false;
										boolean getAsk = false;
										int scale = contractMap.get(name).getScale();
										FidDef fiddef = null;
										for (Iterator iter = ((OMMIterable)data).iterator(); iter.hasNext();){
											Object entryObj = iter.next();
											if (entryObj instanceof OMMFieldEntry) {
												OMMFieldEntry entry = (OMMFieldEntry) entryObj;
												if(entry.getFieldId() == Configuration.ASK){
													if(quote == null){
														quote = new Quote();
														quote.priceTime = now;
													}
													fiddef = dic.getFidDef(entry.getFieldId());
													if (fiddef != null){
														oData = entry.getData(fiddef.getOMMType());
														BigDecimal price = new BigDecimal(oData.toString());
														quote.askPrice = price;
														getAsk = true;
													}
												}else if(entry.getFieldId() == Configuration.BID){
													if(quote == null){
														quote = new Quote();
														quote.priceTime = now;
													}
													fiddef = dic.getFidDef(entry.getFieldId());
													if (fiddef != null){
														oData = entry.getData(fiddef.getOMMType());
														BigDecimal price = new BigDecimal(oData.toString());
														quote.bidPrice = price;
														getBid = true;
													}
												}else{
													MarketPrice marketPrc = this.marketPriceMap.get(contractMap.get(name).getExchangeCode());
													switch(entry.getFieldId()){
													case Configuration.HST_CLOSE:
														fiddef = dic.getFidDef(entry.getFieldId());
														oData = null;
														if (fiddef != null){
															oData = entry.getData(fiddef.getOMMType());
															marketPrc.setyClosePrice(new BigDecimal(oData.toString()));
														}
														break;
													}
												}
											}
										}
										if(quote != null){
											if(getAsk && getBid){
												quote.exchangeCode = contractMap.get(name).getExchangeCode();
												quote.reutersCode = name;
												quote.newPrice = quote.askPrice.add(quote.bidPrice).divide(new BigDecimal(2), scale, RoundingMode.HALF_UP);
												MarketPrice mktPrc = this.marketPriceMap.get(contractMap.get(name).getExchangeCode());
												if(mktPrc != null){														
													quote.highPrice = mktPrc.getHighPrice();
													quote.lowPrice = mktPrc.getLowPrice();
													quote.openPrice = mktPrc.getOpenPrice();
													quote.yClosePrice = mktPrc.getyClosePrice();
													quote.ySettlePrice = mktPrc.getySettlePrice();
												}
												dispatchQuote(quote, oData);
											}else{
												log.info("ask和bid价没有成对出现");
											}
										}
									}else if(3 == contractMap.get(name).getPriceAlgorithm()){
										FidDef fiddef = null;
										OMMData oData = null;
										for (Iterator iter = ((OMMIterable)data).iterator(); iter.hasNext();){
											Object entryObj = iter.next();
											if (entryObj instanceof OMMFieldEntry) {
												OMMFieldEntry entry = (OMMFieldEntry) entryObj;
												System.out.println("contract name=" + name + ";field name="+dic.getFidDef(entry.getFieldId()).getName());
												if(entry.getFieldId() == Configuration.BID){
													quote = new Quote();
													fiddef = dic.getFidDef(entry.getFieldId());
													if (fiddef != null){
														oData = entry.getData(fiddef.getOMMType());
														BigDecimal price = new BigDecimal(oData.toString());
														quote.exchangeCode = contractMap.get(name).getExchangeCode();
														quote.reutersCode = name;
														quote.newPrice = price;
														quote.askPrice = price;
														quote.priceTime = now;
													}													
												}
											}
										}
										if(quote != null){											
											MarketPrice mktPrc = this.marketPriceMap.get(contractMap.get(name).getExchangeCode());
											if(mktPrc != null){														
												quote.highPrice = mktPrc.getHighPrice();
												quote.lowPrice = mktPrc.getLowPrice();
												quote.openPrice = mktPrc.getOpenPrice();
												quote.yClosePrice = mktPrc.getyClosePrice();
												quote.ySettlePrice = mktPrc.getySettlePrice();
											}
											dispatchQuote(quote, oData);
										}
									}
								}
							}
						}
					}
				}
			}else if(msg.has(OMMMsg.HAS_ATTRIB_INFO) && msg.getMsgType() == MsgType.REFRESH_RESP){
				OMMAttribInfo attribInfo = msg.getAttribInfo();
				if(attribInfo != null && attribInfo.has(OMMAttribInfo.HAS_NAME)){
					String name = attribInfo.getName();
					System.out.println("get a msg, itemName=" + name);
					if(itemNames != null && itemNames.contains(name)){
						if(msg.getDataType() != OMMTypes.NO_DATA){
							OMMData data = msg.getPayload();
							if(OMMTypes.isAggregate(data.getType())){
								short dataType = data.getType();
								if(dataType == OMMTypes.FIELD_LIST){
									short dicId = ((OMMFieldList)data).getDictId();
									FieldDictionary dic = GenericOMMParser.getDictionary(dicId);
									if(dic == null){
										log.error("获取字典失败，字典ID=" + dicId);
										return;
									}
									FidDef fiddef = null;
									OMMData oData = null;
									MarketPrice marketPrc = new MarketPrice();
									for (Iterator iter = ((OMMIterable)data).iterator(); iter.hasNext();){
										Object entryObj = iter.next();
										if (entryObj instanceof OMMFieldEntry) {
											OMMFieldEntry entry = (OMMFieldEntry) entryObj;
											switch(entry.getFieldId()){
											case Configuration.HIGH_1:
												fiddef = dic.getFidDef(entry.getFieldId());
												oData = null;
												if (fiddef != null){
													oData = entry.getData(fiddef.getOMMType());
													marketPrc.setHighPrice(new BigDecimal(oData.toString()));
												}
												break;
											case Configuration.BID_HIGH_1:
												fiddef = dic.getFidDef(entry.getFieldId());
												oData = null;
												if (fiddef != null){
													oData = entry.getData(fiddef.getOMMType());
													marketPrc.setHighPrice(new BigDecimal(oData.toString()));
												}
												break;
											case Configuration.LOW_1:
												fiddef = dic.getFidDef(entry.getFieldId());
												oData = null;
												if (fiddef != null){
													oData = entry.getData(fiddef.getOMMType());
													marketPrc.setLowPrice(new BigDecimal(oData.toString()));
												}
												break;
											case Configuration.BID_LOW_1:
												fiddef = dic.getFidDef(entry.getFieldId());
												oData = null;
												if (fiddef != null){
													oData = entry.getData(fiddef.getOMMType());
													marketPrc.setLowPrice(new BigDecimal(oData.toString()));
												}
												break;
											case Configuration.HST_CLOSE:
												fiddef = dic.getFidDef(entry.getFieldId());
												oData = null;
												if (fiddef != null){
													oData = entry.getData(fiddef.getOMMType());
													marketPrc.setyClosePrice(new BigDecimal(oData.toString()));
												}
												break;
											case Configuration.CLOSE_BID:
												fiddef = dic.getFidDef(entry.getFieldId());
												oData = null;
												if (fiddef != null){
													oData = entry.getData(fiddef.getOMMType());
													marketPrc.setyClosePrice(new BigDecimal(oData.toString()));
												}
												break;
											case Configuration.SETTLE:
												fiddef = dic.getFidDef(entry.getFieldId());
												oData = null;
												if (fiddef != null){
													oData = entry.getData(fiddef.getOMMType());
													marketPrc.setySettlePrice(new BigDecimal(oData.toString()));
												}
												break;
											case Configuration.OPEN1:
												fiddef = dic.getFidDef(entry.getFieldId());
												oData = null;
												if (fiddef != null){
													oData = entry.getData(fiddef.getOMMType());
													marketPrc.setOpenPrice(new BigDecimal(oData.toString()));
												}
												break;
											case Configuration.OPEN_BID:
												fiddef = dic.getFidDef(entry.getFieldId());
												oData = null;
												if (fiddef != null){
													oData = entry.getData(fiddef.getOMMType());
													marketPrc.setOpenPrice(new BigDecimal(oData.toString()));
												}
												break;
											case Configuration.OPEN_PRC:
												fiddef = dic.getFidDef(entry.getFieldId());
												oData = null;
												if (fiddef != null){
													oData = entry.getData(fiddef.getOMMType());
													marketPrc.setOpenPrice(new BigDecimal(oData.toString()));
												}
												break;
											}
											this.marketPriceMap.put(contractMap.get(name).getExchangeCode(), marketPrc);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

}

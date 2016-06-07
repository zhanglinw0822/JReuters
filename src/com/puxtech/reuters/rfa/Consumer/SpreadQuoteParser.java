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

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Subscription;
import com.lmax.disruptor.RingBuffer;
import com.puxtech.reuters.rfa.Common.ConfigNode;
import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.Contract;
import com.puxtech.reuters.rfa.Common.Quote;
import com.puxtech.reuters.rfa.Common.QuoteSignal;
import com.puxtech.reuters.rfa.Common.QuoteSource;
import com.puxtech.reuters.rfa.Common.SpreadConfig;
import com.puxtech.reuters.rfa.Common.SubNode;
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

public final class SpreadQuoteParser {
	private static final Log log = LogFactory.getLog("moniter");

	private QuoteSource quoteSource = null;
	private SpreadConfig spreadConfig = null;
	private RFASpreadConsumer consumer = null;
	List<String> itemNames = new ArrayList<String>();
	Map<String, Contract> contractMap = new HashMap<String, Contract>();

	public QuoteSource getQuoteSource() {
		return quoteSource;
	}

	public void setQuoteSource(QuoteSource quoteSource) {
		this.quoteSource = quoteSource;
	}

	public void setConsumer(RFASpreadConsumer consumer) {
		this.consumer = consumer;
	}

	public List<String> getItemNames() {
		return itemNames;
	}
	
	public void setSpreadConfig(SpreadConfig spreadConfig) {
		this.spreadConfig = spreadConfig;
	}

	public void initSpreadCfg(){
		if(this.spreadConfig != null){
			for (ConfigNode configNode : this.spreadConfig.getConfigList()) {
				for(SubNode node : configNode.getNodeList()){
					String rfaCode = node.getContractCode();
					this.itemNames.add(rfaCode);
				}
			}
		}
		
	}

	public final void parseQuoteFromOMMMsg(OMMMsg msg) throws Exception{
		//		List<String> itemNames = Configuration.getInstance().getItemNames();
		//		Map<String, QuoteSignal> quoteSignMap = Configuration.getInstance().getQuoteSignMap();
		if(msg != null){//
			if(msg.has(OMMMsg.HAS_ATTRIB_INFO) /* && msg.getMsgType() != MsgType.REFRESH_RESP 注释掉，保证能从路透源接收到不刷新的字段数据*/){
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
									for (Iterator iter = ((OMMIterable)data).iterator(); iter.hasNext();){
										Object entryObj = iter.next();
										if (entryObj instanceof OMMFieldEntry) {
											OMMFieldEntry entry = (OMMFieldEntry) entryObj;
											fiddef = dic.getFidDef(entry.getFieldId());
											if (fiddef != null){
												//CLOSE1 和CLOSE2会在休市时清空，HST_CLOSE会在休市时间05:15-06:00期间发送，建议在这段时间取数。
												BigDecimal price = null;
												switch(entry.getFieldId()){
												case Configuration.TRDPRC_1:
													oData = entry.getData(fiddef.getOMMType());
													price = new BigDecimal(oData.toString());
													log.info("更新TRDPRC_1:"+name+"," + price);
													this.consumer.getLastPriceMap().put(name, price);
													break;
												case Configuration.CLOSE1:
													break;
												case Configuration.HST_CLOSE:
													oData = entry.getData(fiddef.getOMMType());
													price = new BigDecimal(oData.toString());
													log.info("更新HST_CLOSE:"+name+"," + price);
													this.consumer.getClosePriceMap().put(name, price);
													break;
												case Configuration.CLOSE_ASK:
													//暂不实现
													break;
												case Configuration.CLOSE_BID:
													//暂不实现
													break;
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
	}
}

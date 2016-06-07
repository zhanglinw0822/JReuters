package com.puxtech.reuters.rfa.Consumer;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reuters.rfa.common.Client;
import com.reuters.rfa.common.Event;
import com.reuters.rfa.common.Handle;
import com.reuters.rfa.internal.common.ComplEventMsg;
import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.Contract;
import com.puxtech.reuters.rfa.Common.ReutersConfig;
import com.puxtech.reuters.rfa.RelayServer.RelayServer;
import com.puxtech.reuters.rfa.utility.GenericOMMParser;
import com.reuters.rfa.omm.OMMMsg;
import com.reuters.rfa.omm.OMMPool;
import com.reuters.rfa.rdm.RDMInstrument;
import com.reuters.rfa.rdm.RDMMsgTypes;
import com.reuters.rfa.session.omm.OMMItemEvent;
import com.reuters.rfa.session.omm.OMMItemIntSpec;

// This class is a Client implementation that is utilized to handle item requests
// and responses between application and RFA.
// An instance of this class is created by QSConsumerDemo.
// This class performs the following functions:
// - Creates and encodes item request messages and registers the client (itself) 
// - with RFA (method sendRequest()). The registration will cause RFA
//  to send item open request. RFA will return back a handle instance.
// This application will request two items - TRI.N and MSFT.O.
// - Unregisters the client in RFA (method closeRequest()).
// - Processes events for this client (method processEvent()). processEvent() method
// must be implemented by a class that implements Client interface.
//
// The class keeps the following members:
// ArrayList<Handle> _itemHandles - handles returned by RFA on registering the items
//							application uses this handles to identify the items
// QSConsumerDemo _mainApp - main application class

public class ItemManager implements Client
{
	private Log log = null;
	final Log monLog = LogFactory.getLog("moniter");
	ArrayList<Handle> _itemHandles;
	QSConsumer _mainApp;
	List<String> itemList = new ArrayList<String>();
	ReutersConfig config = null;
	QuoteParser quoteParser = null;

	private	String	_className = "ItemManager";

	// constructor
	public ItemManager(QSConsumer mainApp)
	{
		_mainApp = mainApp;
		this.log=mainApp.getLog();
		_itemHandles = new ArrayList<Handle>();
		quoteParser = new QuoteParser();
		quoteParser.setQuoteSource(this._mainApp.getQuoteSource());
		quoteParser.initContracts();
	}

	// creates streaming request messages for items and register them to RFA
	public void sendRequest()
	{
		log.info(_className+".sendRequest: Sending item requests");
		String serviceName = _mainApp._serviceName;
		//        List<String> itemNames = new ArrayList<String>();
		List<Contract> contractCfgList = new ArrayList<Contract>();;
		try {
			contractCfgList = Configuration.getInstance().getQuoteSourceContractMap().get(_mainApp.getQuoteSource().getName());
			//			itemNames = Configuration.getInstance().getItemNames();
		} catch (Exception e) {
			log.error(e);
			log.info("获取RFA合约代码配置失败！进程终止！");
			System.exit(0);
		}
		short msgModelType = RDMMsgTypes.MARKET_PRICE;

		OMMItemIntSpec ommItemIntSpec = new OMMItemIntSpec();

		//Preparing item request message
		OMMPool pool = _mainApp.getPool();
		OMMMsg ommmsg = pool.acquireMsg();

		ommmsg.setMsgType(OMMMsg.MsgType.REQUEST);
		ommmsg.setMsgModelType(msgModelType);
		ommmsg.setIndicationFlags(OMMMsg.Indication.REFRESH | OMMMsg.Indication.ATTRIB_INFO_IN_UPDATES);
		ommmsg.setPriority((byte) 1, 1);

		// Setting OMMMsg with negotiated version info from login handle        
		if( _mainApp.getLoginHandle() != null )
		{
			ommmsg.setAssociatedMetaInfo(_mainApp.getLoginHandle());
		}

		// register for each item
		for(Contract contractCfg : contractCfgList){
			if(contractCfg.isVirtual()){				
				continue;
			}
			String itemName = contractCfg.getSourceCfg().get("ReutersCode");
			log.info(_className+": Subscribing to " + itemName);
			ommmsg.setAttribInfo(serviceName, itemName, RDMInstrument.NameType.RIC);
			//Set the message into interest spec
			ommItemIntSpec.setMsg(ommmsg);
			Handle  itemHandle = _mainApp.getOMMConsumer().registerClient(
					_mainApp.getEventQueue(), ommItemIntSpec, this, null);
			_itemHandles.add(itemHandle);
			RelayServer.getConsumerMap().put(contractCfg.getExchangeCode(), this._mainApp);
		}
//		for (int i = 0; i < itemNames.size(); i++)
//		{
//			String itemName = itemNames.get(i);
//			log.info(_className+": Subscribing to " + itemName);
//
//			ommmsg.setAttribInfo(serviceName, itemName, RDMInstrument.NameType.RIC);
//
//			//Set the message into interest spec
//			ommItemIntSpec.setMsg(ommmsg);
//			Handle  itemHandle = _mainApp.getOMMConsumer().registerClient(
//					_mainApp.getEventQueue(), ommItemIntSpec, this, null);
//			_itemHandles.add(itemHandle);
//		}
		pool.releaseMsg(ommmsg);
	}

	// Unregisters/unsubscribes login handle
	public void closeRequest()
	{
		while (!_itemHandles.isEmpty())
		{
			Handle itemHandle = _itemHandles.remove(0);
			_mainApp.getOMMConsumer().unregisterClient(itemHandle);
		}
		_itemHandles.clear();
	}

	// This is a Client method. When an event for this client is dispatched,
	// this method gets called.
	public void processEvent(Event event)
	{
		// Completion event indicates that the stream was closed by RFA    	
		if (event.getType() == Event.COMPLETION_EVENT) 
		{
			log.info(_className+": Receive a COMPLETION_EVENT, "+ event.getHandle());
//			ComplEventMsg ie = (ComplEventMsg) event;
//			OMMMsg respMsg = ie.getEventQueue().
//			try {
//				if(this.quoteParser != null){				
//					this.quoteParser.parseQuoteFromOMMMsg(respMsg);
//				}
//			} catch (Exception e) {
//				log.equals(e);
//				log.info("获取行情失败！");
//			}
			return;
		}

		// check for an event type; it should be item event.
		log.info(_className+".processEvent: Received Item Event");
		if (event.getType() != Event.OMM_ITEM_EVENT) 
		{
			log.info("ERROR: "+_className+" Received an unsupported Event type.");
			_mainApp.cleanup();
			return;
		}

		OMMItemEvent ie = (OMMItemEvent) event;
		OMMMsg respMsg = ie.getMsg();
		try {
			if(this.quoteParser != null){				
				this.quoteParser.parseQuoteFromOMMMsg(respMsg);
			}
		} catch (Exception e) {
			log.equals(e);
			log.info("获取行情失败！");
		}
		monLog.info("get a new ommmsg");
		GenericOMMParser.parse(respMsg, this._mainApp.getLog());
	}
}
package com.puxtech.reuters.rfa.Consumer;

import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.puxtech.reuters.rfa.Common.Configuration;
import com.puxtech.reuters.rfa.Common.LoggerPrintStream;
import com.puxtech.reuters.rfa.Common.QuoteSource;
import com.puxtech.reuters.rfa.utility.GenericOMMParser;
import com.reuters.rfa.common.Client;
import com.reuters.rfa.common.Event;
import com.reuters.rfa.common.Handle;
import com.reuters.rfa.omm.OMMElementList;
import com.reuters.rfa.omm.OMMEncoder;
import com.reuters.rfa.omm.OMMMsg;
import com.reuters.rfa.omm.OMMPool;
import com.reuters.rfa.omm.OMMState;
import com.reuters.rfa.omm.OMMTypes;
import com.reuters.rfa.rdm.RDMMsgTypes;
import com.reuters.rfa.rdm.RDMUser;
import com.reuters.rfa.session.omm.OMMItemEvent;
import com.reuters.rfa.session.omm.OMMItemIntSpec;

// This class is a Client implementation that is utilized to handle login activities
// between application and RFA.
// An instance of this class is created by QSConsumerDemo.
// This class performs the following functions:
// - Creates and encodes login request message (method encodeLoginReqMsg()).
// - Registers the client (itself) with RFA (method sendRequest()). The registration
// will cause RFA to send login request. RFA will return back a handle instance.
// - Unregisters the client in RFA (method closeRequest()).
// - Processes events for this client (method processEvent()). processEvent() method
// must be implemented by a class that implements Client interface.
//
// The class keeps the following members:
// Handle 	_loginHandle - a handle returned by RFA on registering the client
//							application uses this handle to identify this client
// QSConsumerDemo _mainApp - main application class

public class LoginClient implements Client
{
    Handle _loginHandle;
    QSConsumer _mainApp;
    Configuration config = Configuration.getInstance();
    String username = 
//    		"bjs_office";
    		"szrmds1";
    String application = "256";
    String position = "10.150.5.101/EP068";
    final Log log = LogFactory.getLog("rfa");

	private String _className = "LoginClient";
	private QuoteSource quoteSource = null;

	// constructor
	public LoginClient(QSConsumer mainApp)
    {
        _mainApp = mainApp;
        if(this._mainApp != null && this._mainApp.getQuoteSource() != null){
        	this.quoteSource = this._mainApp.getQuoteSource();
        	this.username = this.quoteSource.getProperty("UserName");
        	this.application = this.quoteSource.getProperty("Application");
        	this.position = this.quoteSource.getProperty("Position");
        }
        log.info("********************************Login Client Config***********************************\r\n");
        log.info("*                  UserName  : " + this.username + "                     *\r\n");
        log.info("*                  Application : " + this.application + "                     *\r\n");
        log.info("*                  Position      : " + this.position + "                     *\r\n");
        log.info("********************************Login Client Config***********************************\r\n");
    }
    
    // Gets encoded streaming request messages for Login and register it to RFA
    public void sendRequest()
    {
        OMMMsg ommmsg = encodeLoginReqMsg();
        OMMItemIntSpec ommItemIntSpec = new OMMItemIntSpec();
        ommItemIntSpec.setMsg(ommmsg);
        log.info(_className+": Sending login request");
        _loginHandle = _mainApp.getOMMConsumer().registerClient(
                        _mainApp.getEventQueue(), ommItemIntSpec, this, null);
    }

    // Encodes request message for login
    private OMMMsg encodeLoginReqMsg()
    {
        try { if(username == null || "".equals(username))username = System.getProperty("user.name"); }  catch( Exception e ) {}
        try { position = InetAddress.getLocalHost().getHostAddress() + "/" +
              InetAddress.getLocalHost().getHostName(); }  catch( Exception e ) {}
        OMMEncoder encoder = _mainApp.getEncoder();
        OMMPool pool = _mainApp.getPool();
        encoder.initialize(OMMTypes.MSG, 500);
        OMMMsg msg = pool.acquireMsg();
        msg.setMsgType(OMMMsg.MsgType.REQUEST);
        msg.setMsgModelType(RDMMsgTypes.LOGIN);
        msg.setIndicationFlags(OMMMsg.Indication.REFRESH);
        msg.setAttribInfo(null, username, RDMUser.NameType.USER_NAME);

        encoder.encodeMsgInit(msg, OMMTypes.ELEMENT_LIST, OMMTypes.NO_DATA);
        encoder.encodeElementListInit(OMMElementList.HAS_STANDARD_DATA, (short)0, (short) 0);
        encoder.encodeElementEntryInit(RDMUser.Attrib.ApplicationId, OMMTypes.ASCII_STRING);
        encoder.encodeString(application, OMMTypes.ASCII_STRING);
        encoder.encodeElementEntryInit(RDMUser.Attrib.Position, OMMTypes.ASCII_STRING);
        encoder.encodeString(position, OMMTypes.ASCII_STRING);
        encoder.encodeElementEntryInit(RDMUser.Attrib.Role, OMMTypes.UINT);
	    encoder.encodeUInt((long)RDMUser.Role.CONSUMER);
        encoder.encodeAggregateComplete();

        //Get the encoded message from the encoder
        OMMMsg encMsg = (OMMMsg)encoder.getEncodedObject();

        //Release the message that own by the application
        pool.releaseMsg(msg);

        return encMsg; //return the encoded message
    }

    // Unregisters/unsubscribes login handle
    public void closeRequest()
    {
    	if (_loginHandle != null) 
    	{
	        _mainApp.getOMMConsumer().unregisterClient(_loginHandle);
	        _loginHandle = null;
    	}
    }

    // This is a Client method. When an event for this client is dispatched,
    // this method gets called.
    public void processEvent(Event event)
    {
    	// Completion event indicates that the stream was closed by RFA
    	if (event.getType() == Event.COMPLETION_EVENT) 
    	{
    		log.info(_className+": Receive a COMPLETION_EVENT, "+ event.getHandle());
    		return;
    	}

    	log.info(_className+".processEvent: Received Login Response ");

        OMMItemEvent ie = (OMMItemEvent) event;
        OMMMsg respMsg = ie.getMsg();

        // The login is unsuccessful, RFA forwards the message from the network
        if (respMsg.isFinal()) 
        {
        	log.info(_className+": Login Response message is final.");
        	GenericOMMParser.parse(respMsg, this._mainApp.getLog());
        	_mainApp.loginFailure();
        	return;
        }

        // The login is successful, RFA forwards the message from the network
        if ((respMsg.getMsgType() == OMMMsg.MsgType.STATUS_RESP) &&
                (respMsg.has(OMMMsg.HAS_STATE)) &&
                (respMsg.getState().getStreamState() == OMMState.Stream.OPEN) &&
                (respMsg.getState().getDataState() == OMMState.Data.OK) )
        {
        	log.info(_className+": Received Login STATUS OK Response");
            GenericOMMParser.parse(respMsg, this._mainApp.getLog());
            _mainApp.processLogin();
        }
        else // This message is sent by RFA indicating that RFA is processing the login 
        {
        	log.info(_className+": Received Login Response - "+ OMMMsg.MsgType.toString(respMsg.getMsgType()));
            GenericOMMParser.parse(respMsg, this._mainApp.getLog());
        }
    }
    
    public Handle getHandle()
    {
    	return _loginHandle;
    }
}

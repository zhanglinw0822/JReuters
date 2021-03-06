package com.puxtech.reuters.rfa.Consumer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.reuters.rfa.common.Context;
import com.reuters.rfa.common.DispatchException;
import com.reuters.rfa.common.EventQueue;
import com.reuters.rfa.common.EventSource;
import com.reuters.rfa.common.Handle;
import com.reuters.rfa.dictionary.DictionaryException;
import com.puxtech.reuters.rfa.Common.QuoteSource;
import com.puxtech.reuters.rfa.Common.SpreadConfig;
import com.puxtech.reuters.rfa.Common.SpreadConsumer;
import com.puxtech.reuters.rfa.utility.*;
import com.reuters.rfa.omm.OMMEncoder;
import com.reuters.rfa.omm.OMMPool;
import com.reuters.rfa.session.Session;
import com.reuters.rfa.session.omm.OMMConsumer;

// This is a main class to run QuickStartConsumer application.
// The purpose of this application is to demonstrate an RFA OMM Consumer
// connecting to OMM Provider, requesting data for items and receiving 
// responses. 
// After the initial request the application will periodically dispatch
// events from the event queue. The events contain response messages with
// requested data.
//
// This program accepts one command line parameter. This parameter is used 
// as a service name. For example if the name of the service is "DF_ONE", 
// and the application is started by a bat file, the command is shown below:
// QuickStartConsumer.bat DF_ONE
//
// The program creates an instance of this application and invokes the following steps:
// 1. Startup and initialization (method init())
// 2. Login (method login())
// 3. Item request (method itemReq()) This method is called when application received 
//    login response.
// 4. dispatch events (method run())
// 5. Shutdown and cleanup (method cleanup) This method is called when error is detected or
//    the run time expired.
//
// The application keeps the following members:
// Session _session - a session serving this application
// EventQueue _eventQueue - an event queue created by this application
//							to use for passing events from RFA to application
// OMMConsumer _consumer - an instance of event source representing this application
//							in the session
// LoginClient _loginClient - an instance of client class responsible for
//							handling the login messages
// ItemManager _itemManager - an instance of client class responsible for
//							handling the item messages
// OMMPool _pool - a memory pool, managed by RFA, allocated by application,
//							to use for application created message objects
// OMMEncoder _encoder - an instance of encoder allocated by the application
//							from the _pool. Application uses this encoder to
//							encode messages. An encoder does not have to be 
//							a member of the application, instead, can be used as 
//							a local variable. The recommended way is to have 
//							encoder as a member.
// String _serviceName - a string representing name of a service. It will be used to 
// 							request items. It is initially set to "DIRECT_FEED" but
//							can be modified by including command line parameter.
public class RFASpreadConsumer implements SpreadConsumer, Runnable
{
	Log log = LogFactory.getLog("rfa");
	// Configuration
	protected String            _sessionName = "myNamespace::myOMMViewSess";
	protected String            _eventQueueName = "myEventQueue";
	protected String            _consumerName = "myOMMConsumer";
	protected String fieldDictionaryFilename = System.getProperty("user.dir") + "/RDM/RDMFieldDictionary";
	protected String enumDictionaryFilename = System.getProperty("user.dir") + "/RDM/enumtype.def";
	protected String			   _serviceName = "IDN_RDF";
	//RFA objects
	protected Session              _session;
	protected EventQueue           _eventQueue;
	protected OMMConsumer          _consumer;
	protected SpreadLoginClient          _loginClient;
	protected SpreadItemManager          _itemManager;
	protected OMMEncoder           _encoder;
	protected OMMPool              _pool;
	private QuoteSource quoteSource = null;
	private SpreadConfig spreadConfig = null;
	private Map<String, BigDecimal> closePriceMap = new HashMap<String, BigDecimal>();
	private Map<String, BigDecimal> lastPriceMap = new HashMap<String, BigDecimal>();

	// class constructor
	public RFASpreadConsumer(){}

	private void initSpreadCfg(){
		if(this.quoteSource != null){
			this.fieldDictionaryFilename = System.getProperty("user.dir") + this.quoteSource.getProperty("FieldDic");
			this.enumDictionaryFilename = System.getProperty("user.dir") + this.quoteSource.getProperty("EnumDic");
			this._sessionName = this.quoteSource.getProperty("SessionName");
			this._serviceName = this.quoteSource.getProperty("ServiceName");
			String logName = this.quoteSource.getProperty("LoggerName");
			System.out.println("log name=" + logName);
			this.log = LogFactory.getLog(logName);
		}
		if(this.log != null){
			log.info("*****************************************************************************\r\n");
			log.info("*          Begin Spread Consumer                                                                  *\r\n");
			log.info("*****************************************************************************\r\n");
			log.info("*******************************Load Consumer Config Completed*********************************\r\n");
			log.info("*               SessionName: " + _sessionName + "               *\r\n");
			log.info("*               ServiceName : " + _serviceName + "               *\r\n");
			log.info("*               FieldDicFile    : " + fieldDictionaryFilename + "               *\r\n");
			log.info("*               EnumDicFile  : " + enumDictionaryFilename + "               *\r\n");
			log.info("*******************************Load Consumer Config Completed*********************************\r\n");
		}
	}

	public Log getLog() {
		return log;
	}
	public QuoteSource getQuoteSource() {
		return quoteSource;
	}

	public void setQuoteSource(QuoteSource quoteSource) {
		this.quoteSource = quoteSource;
	}

	// This method is responsible for initialization
	// As shown in QuickStartGuide, the initialization includes the following steps:
	// 1. Initialize context
	// 2. Create event queue
	// 3. Initialize logger
	// 4. Acquire session
	// 5. Create event source
	// 6. Load dictionaries
	// It also instantiates application specific objects: memory pool, encoder.
	public void init()
	{
		initSpreadCfg();
		// 1. Initialize context
		Context.initialize();

		// 2. Create an Event Queue
		_eventQueue = EventQueue.create( _eventQueueName);

		// 3. Initialize logger
		// The application utilizes logger from RFA. User sets the level 
		// to the desired value. The default is "Info".
		/*Logger logger = Logger.getLogger("com.reuters.rfa");
        Level level = Level.INFO;
        logger.setLevel(level);
        Handler[] handlers = logger.getHandlers();

        if(handlers.length == 0) 
        {
        	Handler handler = new ConsoleHandler();
        	handler.setLevel(level); 
        	logger.addHandler(handler);            	
        }

        for( int index = 0; index < handlers.length; index++ )
        {
        	handlers[index].setLevel(level);
        }*/

		// 4. Acquire a Session
		_session = Session.acquire( _sessionName );
		if ( _session == null )
		{
			log.info( "Could not acquire session." );
			Context.uninitialize();
			System.exit(1);
		}

		// 5. Create an OMMConsumer event source
		_consumer = (OMMConsumer) _session.createEventSource(EventSource.OMM_CONSUMER, _consumerName, true);

		// 6. Load dictionaries
		// Application may choose to down-load the enumtype.def and RWFFldDictionary
		// This example program loads the dictionaries from files.

		//TODO字典文件路径，需要按实际情况修改

		try 
		{
			GenericOMMParser.initializeDictionary(fieldDictionaryFilename, enumDictionaryFilename);
		}
		catch (DictionaryException ex) 
		{
			log.info("ERROR: Unable to initialize dictionaries");
			log.info(ex.getMessage());
			if(ex.getCause() != null)
				System.err.println(": " + ex.getCause().getMessage());
			cleanup();
			return;
		}

		//Create a OMMPool.
		_pool = OMMPool.create();

		//Create an OMMEncoder
		_encoder = _pool.acquireEncoder();
	}

	// This method utilizes the LoginClient class to send login request 
	public void login()
	{
		//Initialize client for login domain.
		_loginClient = new SpreadLoginClient(this);

		//Send login request
		_loginClient.sendRequest();
	}

	// This method is called by _loginClient upon receiving successful login response.
	public void processLogin()
	{
		log.info("QSConsumerDemo"+" Login successful");
		// The application successfully logged in 
		// Now we can send the item(s) request
		itemRequests();
	}

	// This method is called when the login was not successful
	// The application exits
	public void loginFailure()
	{
		log.info("OMMConsumerDemo"+": Login has been denied / rejected / closed");
		log.info("OMMConsumerDemo"+": Preparing to clean up and exiting");
		_loginClient = null;
		cleanup();
	}

	// This method utilizes ItemManager class to request items
	void itemRequests()
	{
		//Initialize item manager for item domains
		_itemManager = new SpreadItemManager(this);

		// Send requests
		_itemManager.sendRequest();
	}

	private boolean flag = true;
	private boolean isRestart = false;
	// This method dispatches events
	public void runConsumer()
	{
		// The run time is set arbitrary to 120 seconds
		long runTime = 120;
		// save start time to measure run time
		long startTime = System.currentTimeMillis();

		while( !isRestart && flag)
			try
		{
				_eventQueue.dispatch(100);
		}
		catch (DispatchException de )
		{
			log.info("EventQueue has been deactivated");
		}
		runTime = System.currentTimeMillis() - startTime;
		log.info(runTime + " seconds elapsed, " + getClass().toString() + " cleaning up");
	}

	// This method cleans up resources when the application exits
	// As shown in QuickStartGuide, the shutdown and cleanup includes the following steps:
	// 1. Deactivate event queue
	// 2. Unregister item interest
	// 3. Unregister login
	// 4. Destroy event queue
	// 5. Destroy event source
	// 6. Release session
	// 7. Uninitialize context
	public void cleanup()
	{
		log.info(Context.string());

		// 1. Deactivate event queue
		_eventQueue.deactivate();

		// 2. Unregister item interest
		if (_itemManager != null)
			_itemManager.closeRequest();

		// 3. Unregister login
		if (_loginClient != null)
			_loginClient.closeRequest();

		// 4. Destroy event queue
		_eventQueue.destroy();

		// 5. Destroy event source
		if (_consumer != null)
			_consumer.destroy();

		// 6. Release session
		_session.release();

		// 7. Uninitialize context
		Context.uninitialize();

		log.info(getClass().toString() + " exiting");
//		System.exit(0);
	}


	public EventQueue getEventQueue()
	{
		return _eventQueue;
	}

	public OMMConsumer getOMMConsumer()
	{
		return _consumer;
	}

	public OMMEncoder getEncoder()
	{
		return _encoder;
	}

	public OMMPool getPool()
	{
		return _pool;
	}

	public Handle getLoginHandle()
	{
		if ( _loginClient != null )
		{
			return _loginClient.getHandle();
		}

		return null;
	}

	protected String getServiceName()
	{
		return _serviceName;
	}

	// This is a main method of the QuickStartConsumer application.
	// A serviceName is hard coded to "DIRECT_FEED" but can be overridden by a string 
	// passed to the demo as a command line parameter.
	// Refer to QuickStartGuide for the RFA OMM Consumer application life cycle.
	// The steps shown in the guide are as follows:
	// Startup and initialization
	// Login
	// Item requests
	// Dispatch events
	// Shutdown and cleanup
	public static void main( String argv[] )
	{
		// Create a demo instance
		RFASpreadConsumer demo = new RFASpreadConsumer();

		// If the user is connecting to the Enterprise Platform, the serviceName 
		// should be set to the service name that is offered by the provider.
		// The name is passed as a command line parameter.
		if ((argv != null) && (argv.length > 0))
		{
			demo._serviceName = argv[0];
		}

		// Startup and initialization
		demo.init();

		// Login
		// and Item request
		// Item requests is done after application received login response.
		// The method itemRequests is called from processLogin method.
		demo.login();

		//Dispatch events
		demo.run();

		// Shutdown and cleanup
		demo.cleanup();
	}

	@Override
	public void run() {
		while (true) {
			init();
			login();
			runConsumer();
			cleanup();
			try {
				Thread.sleep(1000);
				if(!this.flag){
					break;
				}
			} catch (InterruptedException e) {
				log.error(e);
			}
		}		
	}

	public void shutdown() {
		this.flag = false;
	}

	public void restart() {
		this.isRestart = true;
	}

	@Override
	public Map<String, BigDecimal> getClosePriceMap() {
		return closePriceMap;
	}

	@Override
	public Map<String, BigDecimal> getLastPriceMap() {
		return lastPriceMap;
	}

	@Override
	public void setSpreadConfig(SpreadConfig spreadConfig) {
		this.spreadConfig = spreadConfig;
	}
	
	public SpreadConfig getSpreadConfig(){
		return this.spreadConfig;
	}

}

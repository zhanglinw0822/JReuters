package com.puxtech.reuters.rfa.Consumer;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.reuters.rfa.common.Context;
import com.reuters.rfa.common.DispatchException;
import com.reuters.rfa.common.EventQueue;
import com.reuters.rfa.common.EventSource;
import com.reuters.rfa.common.Handle;
import com.reuters.rfa.common.QualityOfServiceRequest;
import com.reuters.rfa.common.StandardPrincipalIdentity;
import com.reuters.rfa.common.TokenizedPrincipalIdentity;
import com.puxtech.reuters.rfa.utility.*;
import com.reuters.rfa.session.MarketDataItemSub;
import com.reuters.rfa.session.MarketDataSubscriber;
import com.reuters.rfa.session.MarketDataSubscriberInterestSpec;
import com.reuters.rfa.session.Session;

/**
 * MDSubDemo demonstrates how to subscribe to one or more items using RFA's
 * Market Data Subscription model.
 **/

public class Consumer
{

    // Configuration
    protected String _serviceName;
    protected LinkedList<String> _itemNames;
    protected LinkedList<String> _subjectNames;
    protected int _runTime;
    protected boolean _dynamicStream;
    protected int _bestRate, _bestTimeliness;
    protected int _worstRate, _worstTimeliness;
    protected String _sessionName;
    protected boolean _mounttpi;

    // RFA objects
    protected StandardPrincipalIdentity _standardPI;
    protected TokenizedPrincipalIdentity _tokenizedPI;
    protected Session _session;
    protected EventQueue _eventQueue;
    protected MarketDataSubscriber _marketDataSubscriber;
    protected Handle _mdsClientHandle;
    protected Handle _itemHandle;
    @Override
    public String toString(){
    	return "_serviceName="+_serviceName + "\r\n";
    }

    /**
     * The constructor initializes variables using the command line arguments.
     * It also creates the principal identity used to permission market data.
     */
    public Consumer()
    {
        // Read options from the command line
        _serviceName = CommandLine.variable("serviceName");
        String itemNames = CommandLine.variable("itemName");
        StringTokenizer st = new StringTokenizer(itemNames, ",");
        _itemNames = new LinkedList<String>();
        while (st.hasMoreTokens())
            _itemNames.add(st.nextToken().trim());
        String subjectNames = CommandLine.variable("subjectName");
        StringTokenizer st1 = new StringTokenizer(subjectNames, ",");
        _subjectNames = new LinkedList<String>();
        while (st1.hasMoreTokens())
            _subjectNames.add(st1.nextToken());
        

        boolean debug = CommandLine.booleanVariable("debug");
        if (debug)
        {
            // Enable debug logging
            Logger logger = Logger.getLogger("com.reuters.rfa");
            logger.setLevel(Level.FINE);
            Handler[] handlers = logger.getHandlers();

            if (handlers.length == 0)
            {
                Handler handler = new ConsoleHandler();
                handler.setLevel(Level.FINE);
                logger.addHandler(handler);
            }

            for (int index = 0; index < handlers.length; index++)
            {
                handlers[index].setLevel(Level.FINE);
            }
        }

        _runTime = CommandLine.intVariable("runTime");
        _dynamicStream = CommandLine.booleanVariable("dynamicStream");
        _bestRate = CommandLine.intVariable("bestRate");
        _bestTimeliness = CommandLine.intVariable("bestTimeliness");
        _worstRate = CommandLine.intVariable("worstRate");
        _worstTimeliness = CommandLine.intVariable("worstTimeliness");
        _mounttpi = CommandLine.booleanVariable("mounttpi");
        createPrincipals();
    }

    /**
     * Initializes context, creates the session, creates a client
     * MDSubDemoClient that can handle event callbacks. Also creates the
     * MarketDataSubscriber event source with appropriate
     * MarketDataSubscriberInterestSpec and subscribes to all the items
     * specified in the command line.
     */
    public void init()
    {
        Context.initialize();
        // Create a Session
        _sessionName = CommandLine.variable("session");
        if (_mounttpi)
            _session = Session.acquire(_sessionName, _tokenizedPI);
        else
            _session = Session.acquire(_sessionName);
        if (_session == null)
        {
            System.out.println("Could not acquire session.");
            System.exit(1);
        }
        System.out.println("RFA Version: " + Context.getRFAVersionInfo().getProductVersion());

        // Create an object to receive event callbacks
       // DataParser myClient = new DataParser(this);

        // Create an Event Queue
        _eventQueue = EventQueue.create("myEventQueue");

        // Create a MarketDataSubscriber event source
        // The usage of a StandardPrincipalIdentity when creating an EventSource is deprecated starting in 7.2
        // and will be removed completely in a future release. 
        _marketDataSubscriber = (MarketDataSubscriber)_session
                .createEventSource(EventSource.MARKET_DATA_SUBSCRIBER, "myMarketDataSubscriber",
                                   true, _standardPI);

        // Register for service and connection status
        MarketDataSubscriberInterestSpec marketDataSubscriberInterestSpec = new MarketDataSubscriberInterestSpec();
        marketDataSubscriberInterestSpec.setMarketDataSvcInterest(true);
        marketDataSubscriberInterestSpec.setConnectionInterest(true);
        marketDataSubscriberInterestSpec.setEntitlementsInterest(true);
       // _mdsClientHandle = _marketDataSubscriber.registerClient(_eventQueue, marketDataSubscriberInterestSpec, myClient, null);

        // Set the QOS request information
        QualityOfServiceRequest qosr = new QualityOfServiceRequest();
        qosr.setBestRate(_bestRate);
        qosr.setBestTimeliness(_bestTimeliness);
        qosr.setWorstRate(_worstRate);
        qosr.setWorstTimeliness(_worstTimeliness);
        if (_dynamicStream)
        {
            qosr.setStreamProperty(QualityOfServiceRequest.DYNAMIC_STREAM);
        }
        // else use static stream (default)
        if (_subjectNames.size() != 0)
        {
            Iterator<String> iter1 = _subjectNames.iterator();
            while (iter1.hasNext())
            {
                String subjectName = (String)iter1.next();
                System.out.println("Subscribing to " + subjectName);
                MarketDataItemSub marketDataItemSub = new MarketDataItemSub();
                marketDataItemSub.setSubject(subjectName);
                marketDataItemSub.setRequestedQualityOfService(qosr);
               // _marketDataSubscriber.subscribe(_eventQueue, marketDataItemSub, myClient, null);
            }
        }
        else
        {
            Iterator<String> iter = _itemNames.iterator();
            while (iter.hasNext())
            {
                String itemName = (String)iter.next();
                System.out.println("Subscribing to " + itemName);
                MarketDataItemSub marketDataItemSub = new MarketDataItemSub();
                marketDataItemSub.setItemName(itemName);
                marketDataItemSub.setServiceName(_serviceName);
                marketDataItemSub.setRequestedQualityOfService(qosr);
               // _marketDataSubscriber.subscribe(_eventQueue, marketDataItemSub, myClient, null);
            }
        }
    }

    /**
     * Dispatch events from the event queue for the duration given by runTime.
     */
    public void run()
    {
        // Dispatch item events for a while
//        long startTime = System.currentTimeMillis();

//        while ((System.currentTimeMillis() - startTime) < _runTime * 1000)
          while (true){        	  
        	  try
        	  {
        		  _eventQueue.dispatch(100);
        	  }
        	  catch (DispatchException de)
        	  {
        		  System.out.println("Queue deactivated");
        		  return;
        	  }
          }
        //System.out.println(Context.string());
        //System.out.println(_runTime + " seconds elapsed, " + getClass().toString() + " exiting");

    }

    /**
     * Cleanup before exit.
     */
    public void cleanup()
    {
        _marketDataSubscriber.unsubscribeAll();
        _marketDataSubscriber.unregisterClient(_mdsClientHandle);
        _marketDataSubscriber.destroy();
        _eventQueue.deactivate();
        _session.release();
        Context.uninitialize();
    }

    /**
     * Create TokenizedPrincipalIdentity and StandardPrincipalIdentity
     */
    public void createPrincipals()
    {
        String user = CommandLine.variable("user");
        String position = CommandLine.variable("position");
        String application = CommandLine.variable("application");

        _tokenizedPI = new TokenizedPrincipalIdentity();
        String tokenString = user;
        if (!application.equals(""))
        {
            tokenString = tokenString + "+" + application;
            if (!position.equals(""))
                tokenString = tokenString + "+" + position;
        }
        _tokenizedPI.setBuffer(tokenString.getBytes());
        _standardPI = new StandardPrincipalIdentity();
        _standardPI.setName(user);
        _standardPI.setPosition(position);
        _standardPI.setAppName(application);
    }

    /**
     * Add command line options for this example.
     */
    public static void addCommandLineOptions()
    {
        CommandLine.addOption("session", "SSLNamespace::pageSSLSession",
                              "Session name to use. Default myNamespace::mySession");
        CommandLine.addOption("serviceName", "IDN_SELECTFEED", "Service to use. Default IDN_RDF");
        CommandLine.addOption("itemName", "CLv1,LCOv1",
                              "Items to open. ',' separated list of items to open. Default TRI.N");
        CommandLine.addOption("subjectName", "",
                              "4-Parts subjects to open, ',' separated list of subjects to open - default \"\"");
        CommandLine.addOption("debug", "false", "Option to enable debug logging - default false");
        CommandLine.addOption("runTime", 60,
                              "number of seconds to dispatch before exiting program - default 60");
        String username = "bjs_office"; 
//        		"TY6_03_RHB_SWVPNDEMO";
////        try
////        {
////            username = System.getProperty("user.name");
////        }
//        catch (Exception e)
//        {
//        }
        CommandLine.addOption("user", username,
                              "DACS username for login - default system property or \"guest\"");
        String defaultPosition = "localhost"; 
//        		"10.150.5.101/EP068";
//        try
//        {
//            defaultPosition = InetAddress.getLocalHost().getHostAddress() + "/"
//                    + InetAddress.getLocalHost().getHostName();
//        }
//        catch (Exception e)
//        {
//        }
        CommandLine.addOption("position", defaultPosition,
                              "DACS position for login - default system property or \"1.1.1.1/net\"");
        CommandLine.addOption("application", "256", "DACS application ID for login - default 256");
        CommandLine.addOption("dynamicStream", "false",
                              "Enable dynamic QOS request stream - default false");
        CommandLine.addOption("bestRate", "0",
                              "Specify the acceptable best rate in the QOS request - default 0(tick-by-tick)");
        CommandLine.addOption("bestTimeliness", "0",
                              "specify the acceptable best timeliness in the QOS request - default 0(realtime)");
        CommandLine.addOption("worstRate", "2147483647",
                              "specify the acceptable worst rate in the QOS request - default 2147483647(slowest rate)");
        CommandLine.addOption("worstTimeliness", "2147483647",
                              "specify the acceptable worst timeliness in the QOS request - default 2147483647(max delayed)");
        CommandLine.addOption("mounttpi",
                              "true",
                              "Enable testing of TPI (Tokenized Principal Identity) - default false uses the -user option as the Tokenized Principal.");
    }

    /**
     * Initialize command line options, instantiate MDSubDemo, initialize it,
     * run it for given duration and finally cleanup.
     */
    public static void main(String argv[])
    {
        addCommandLineOptions();
        CommandLine.setArguments(argv);

        Consumer demo = new Consumer();
        demo.init();
        demo.run();
        demo.cleanup();
    }

}

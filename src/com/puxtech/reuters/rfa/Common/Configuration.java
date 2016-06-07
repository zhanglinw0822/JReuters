package com.puxtech.reuters.rfa.Common;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.puxtech.reuters.rfa.utility.MD5;

public class Configuration {
	public static final int TRDPRC_1 = 6;
	public static final int OPEN1 = 47;
	public static final int OPEN_PRC = 19;
	public static final int OPEN_BID = 57;
	
	public static final int CLOSE_BID = 60;
	public static final int CLOSE_ASK = 61;
	public static final int HST_CLOSE = 21;
	public static final int CLOSE1 = 50;
	
	public static final int BID_HIGH_1 = 203;
	public static final int HIGH_1 = 12;
	
	public static final int BID_LOW_1 = 204;
	public static final int LOW_1 = 13;
	
	public static final int SETTLE = 70;
	
	public static final int NETCHNG_1 = 11;
	public static final int BID = 22;
	public static final int ASK = 25;
	private static final Log log = LogFactory.getLog("moniter");
	private static final String defaultConfigFilePath = System.getProperty("user.dir") + "/appConfig.xml";
	private static Configuration configInstance = null;
	public static Configuration getInstance(){
		if (configInstance == null) {
//			synchronized (configInstance) {
				try {
					configInstance = new Configuration(defaultConfigFilePath);
				} catch (Exception e) {
					e.printStackTrace();
					log.error(e);
					log.info("初始化配置文件失败！");
					System.exit(0);
				}
//			}
		}
		return configInstance;
	}
	
	public static void refreshConfiguration(){
		synchronized (configInstance) {
			if(configInstance != null){
				try {
					log.info("刷新配置文件开始！");
					configInstance = new Configuration(defaultConfigFilePath);
					log.info("刷新配置文件成功！");
				} catch (Exception e) {
					log.error(e);
					log.info("刷新配置文件失败！");
				}
			}
		}
	}
	
	public static void backupConfigurationFile(){
		if(configInstance != null && configInstance.configFile != null){
			File backupFile = new File(defaultConfigFilePath + "." + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
			if(backupFile.exists()){
				backupFile.delete();
			}
			OutputStreamWriter writer = null;
			InputStreamReader reader = null;
			try {
				backupFile.createNewFile();
				writer = new OutputStreamWriter(new FileOutputStream(backupFile),"utf-8");
				reader = new InputStreamReader(new FileInputStream(configInstance.configFile), "utf-8");
				int readLength = -1;
				char[] buff = new char[1024];
				while((readLength = reader.read(buff)) != -1){
					writer.write(buff, 0, readLength);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if(writer != null){
					try {
						writer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if(reader != null){
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
		}
	}
	
	public static void updateConfigurationFile(){
		backupConfigurationFile();
		if(configInstance != null && configInstance.doc != null && configInstance.configFile != null){
			try {
				OutputFormat format = OutputFormat.createPrettyPrint();
				XMLWriter writer = new XMLWriter(new OutputStreamWriter(new FileOutputStream(configInstance.configFile),"utf-8"),format);
				writer.write(configInstance.doc);
				writer.close();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	private int localPort = 9901;
	private String logHead = "";
	private int logFlag = 1;
	private String quoteFormat = "";
	private String spreadFormat = "";
	private int monitorInterval = 20000;
	private List<String> itemNames = new ArrayList<String>();
	private List<QuoteSignal> dcQuoteSignList = new ArrayList<QuoteSignal>();
	private List<QuoteSignal> virtualSignList = new ArrayList<QuoteSignal>();
	
	private Map<String, QuoteSignal> quoteSignMap = new HashMap<String, QuoteSignal>();
	private List<QuoteSource> quoteSourceList = new ArrayList<QuoteSource>();
	private Map<String, QuoteSource> quoteSourceMap = new HashMap<String, QuoteSource>();
	private List<Contract> contractList = new ArrayList<Contract>();
	private Map<String, List<Contract>> quoteSourceContractMap = new HashMap<String, List<Contract>>();
	
	private String configFilePath = "";
	private File configFile = null;
	private Document doc = null;
	private String SourcePath = "";
	private String cronExpress = "";
	public Configuration(String configFilePath) throws Exception {
		super();
		if(configFilePath == null){
			throw new Exception("配置文件路径未定义!");
		}
		this.configFilePath = configFilePath;
		configFile = new File(this.configFilePath);
		doc = new SAXReader().read(configFile);
		initGlobalCfg();
		initQuoteSourceCfg();
		initContractCfg();
		initLogCfg();
		initSpreadCfg(quoteSource);
	}
	
	public Document getDoc() {
		return doc;
	}

	public Element getGlobalConfig(){
		Element globalConfigEle = null;
		if(this.doc != null){
			globalConfigEle = doc.getRootElement().element("Config").element("GlobalConfig");
		}
		return globalConfigEle;
	}
	
	//初始化参数配置 begin
	
	private String quoteSource;

	public String getQuoteSource() {
		return quoteSource;
	}
	
	private void initGlobalCfg(){
		Element globalCfgEle = this.getGlobalConfig();
		if(globalCfgEle != null){
			if(globalCfgEle.elementText("LocalPort") != null && globalCfgEle.elementText("LocalPort").matches("\\d+")){
				this.setLocalPort(Integer.valueOf(globalCfgEle.elementText("LocalPort")));
			}
			if(globalCfgEle.elementText("LogHead") != null && globalCfgEle.elementText("LogHead").length() > 0){
				this.setLogHead(globalCfgEle.elementText("LogHead"));
			}
			if(globalCfgEle.elementText("logFlag") != null && globalCfgEle.elementText("logFlag").matches("\\d+")){
				this.setLogFlag(Integer.valueOf(globalCfgEle.elementText("logFlag")));
			}
			if(globalCfgEle.elementText("QuoteFormat") != null && globalCfgEle.elementText("QuoteFormat").length() > 0){
				this.setQuoteFormat(globalCfgEle.elementText("QuoteFormat"));
			}
			if(globalCfgEle.elementText("SpreadFormat") != null && globalCfgEle.elementText("SpreadFormat").length() > 0){
				this.setSpreadFormat(globalCfgEle.elementText("SpreadFormat"));
			}
			if(globalCfgEle.elementText("MonitorInterval") != null && globalCfgEle.elementText("MonitorInterval").length() > 0){
				this.setMonitorInterval(Integer.valueOf(globalCfgEle.elementText("MonitorInterval")));
			}
			if(globalCfgEle.elementText("SourcePath") != null && globalCfgEle.elementText("SourcePath").length() > 0){
				this.setSourcePath(globalCfgEle.elementText("SourcePath"));
			}
			if(globalCfgEle.elementText("cronExpress") != null && globalCfgEle.elementText("cronExpress").length() > 0){
				this.setCronExpress(globalCfgEle.elementText("cronExpress"));
			}
			if(globalCfgEle.elementText("QuoteSource") != null && globalCfgEle.elementText("QuoteSource").length() > 0){
				this.quoteSource = globalCfgEle.elementText("QuoteSource");
			}
		}
	}
	//初始化参数配置 end
	
	public List<Element> getQuoteSourceConfigs(){
		List<Element> quoteSourceConfigEle = null;
		if(this.doc != null){
			quoteSourceConfigEle = doc.getRootElement().element("Config").elements("QuoteSource");
		}
		return quoteSourceConfigEle;
	}
	
	private void initQuoteSourceCfg(){
		List<Element> quoteSourceCfgElements = this.getQuoteSourceConfigs();
		for(Element quoteSourceCfg : quoteSourceCfgElements){
			String quoteSourceName = quoteSourceCfg.attributeValue("name");
			List<Element> propertyEle = quoteSourceCfg.elements();
			QuoteSource quoteSource = new QuoteSource();
			quoteSource.setName(quoteSourceName);
			if(propertyEle != null && propertyEle.size() > 0){
				for(Element pro : propertyEle){
					quoteSource.setProperty(pro.getName(), pro.getTextTrim());
				}
			}
			if(quoteSourceName != null){
				this.quoteSourceMap.put(quoteSourceName, quoteSource);
				if(this.quoteSourceContractMap.get(quoteSourceName) == null){
					this.quoteSourceContractMap.put(quoteSourceName, new ArrayList<Contract>());
				}
			}
		}
	}
	
	private Element getSpreadConfigs(){
		Element spreadConfigEle = null;
		if(this.doc != null){
			spreadConfigEle = doc.getRootElement().element("Config").element("Spread");
		}
		return spreadConfigEle;
	}
	
	private List<Element> getOffSetConfigs(){
		List<Element> offSetConfigEle = null;
		if(this.doc != null){
			offSetConfigEle = doc.getRootElement().element("Config").elements("OffSetConfig");
		}
		return offSetConfigEle;
	}
	
	SpreadConfig spreadConfig = null;
	private void initSpreadCfg(String quoteSourceName){
		//从读xml方式修改为读数据库方式 bein
		/*
		Element spreadCfgElements = getSpreadConfigs();
		if(spreadCfgElements != null){
			spreadConfig = new SpreadConfig(spreadCfgElements);
		}
		*/
		//从读xml方式修改为读数据库方式 emd
		spreadConfig = new SpreadConfig(quoteSourceName);
	}
	
	public SpreadConfig getSpreadConfig() {
		return spreadConfig;
	}

	private List<Element> getContractConfigs(){
		List<Element> contractConfigEle = null;
		if(this.doc != null){
			contractConfigEle = doc.getRootElement().element("Config").element("Contracts").elements("Contract");
		}
		return contractConfigEle;
	}
	
	private void initContractCfg() throws Exception{
		List<Element> contractCfgElements = this.getContractConfigs();
		for(Element contractCfg : contractCfgElements){
			Contract contract = new Contract();
			contract.setSourceName(contractCfg.elementText("SourceName") != null ? contractCfg.elementText("SourceName") : "");
			contract.setExchangeCode(contractCfg.elementText("ExchangeCode") != null ? contractCfg.elementText("ExchangeCode") : "");
			contract.setPriceAlgorithm(contractCfg.elementText("PriceAlgorithm") != null ? Integer.valueOf(contractCfg.elementText("PriceAlgorithm")) : 0);
			contract.setScale(contractCfg.element("PriceAlgorithm").attributeValue("scale") != null ? Integer.valueOf(contractCfg.element("PriceAlgorithm").attributeValue("scale"))  : 0);
//			contract.setOffset(contractCfg.elementText("Offset") != null && !contractCfg.elementText("Offset").isEmpty() ? new BigDecimal(contractCfg.elementText("Offset")) : new BigDecimal("0"));
			contract.setFilterName(contractCfg.elementText("Filter") != null ? contractCfg.elementText("Filter") : "");
			List<Element> sourceCfgs = contractCfg.element("SourceConfig").elements();
			for(Element sourceCfg : sourceCfgs){
				contract.appendSourceCfg(sourceCfg.getName(), sourceCfg.getTextTrim());
			}
			Element vPriceGenCfg = contractCfg.element("VirtualPriceGenerator");
			if(vPriceGenCfg != null && "true".equalsIgnoreCase(vPriceGenCfg.attributeValue("Enabled"))){
					contract.setVirtual(true);
			}
			this.contractList.add(contract);
			List<Contract> contractList = this.quoteSourceContractMap.get(contract.getSourceName());
			if(contractList != null){
				contractList.add(contract);
			}else{
				log.info("name=" + contract.getSourceName() + "的quoteSource未配置!");
			}
		}
	}
	
	public int getLocalPort() {
		return localPort;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}
	
	public String getLogHead() {
		return logHead;
	}

	public void setLogHead(String logHead) {
		this.logHead = logHead;
	}

	public int getLogFlag() {
		return logFlag;
	}

	public void setLogFlag(int logFlag) {
		this.logFlag = logFlag;
	}
	
	public String getQuoteFormat() {
		return quoteFormat;
	}

	public void setQuoteFormat(String quoteFormat) {
		this.quoteFormat = quoteFormat;
	}
	
	public int getMonitorInterval() {
		return monitorInterval;
	}

	public void setMonitorInterval(int monitorInterval) {
		this.monitorInterval = monitorInterval;
	}

	public List<QuoteSignal> getDcQuoteSignList() {
		return dcQuoteSignList;
	}
	
	public List<QuoteSignal> getVirtualSignList() {
		return virtualSignList;
	}

	public Map<String, QuoteSignal> getQuoteSignMap() {
		return quoteSignMap;
	}

	private void initLogCfg(){
		Properties pro;
		for(Contract contract : this.contractList){			
			String itemName = contract.getExchangeCode();
			pro = new Properties();
			pro.put("log4j.logger." + itemName, "INFO, " + itemName + "log");
			pro.put("log4j.additivity." + itemName,"false");
			pro.put("log4j.appender." + itemName + "log","org.apache.log4j.DailyRollingFileAppender");
			pro.put("log4j.appender." + itemName + "log.file","./logs/" + itemName + ".log");
			pro.put("log4j.appender." + itemName + "log.DatePattern","'.'yyyy-MM-dd");
			pro.put("log4j.appender." + itemName + "log.Threshold","INFO");
			pro.put("log4j.appender." + itemName + "log.layout","com.puxtech.reuters.rfa.Common.MyPatternLayout");
			pro.put("log4j.appender." + itemName + "log.layout.ConversionPattern","%m");
			pro.put("log4j.appender." + itemName + "log.layout.Header", this.logHead.trim().length() > 0 ? this.logHead + "\r\n" : "\u5546\u54C1\u4EE3\u7801,\u8DEF\u900F\u4EE3\u7801,\u65F6\u95F4,ASK,BID,\u4EF7\u683C,\u4E0A\u4E00\u53E3\u6709\u6548\u4EF7\u683C,\u6DA8\u8DCC\u5E45,\u662F\u5426\u88AB\u8FC7\u6EE4\r\n");
			//未计算价差数据日志
			String itemRawName = itemName + "_RAW";
			pro.put("log4j.logger." + itemRawName, "INFO, " + itemRawName + "log");
			pro.put("log4j.additivity." + itemRawName,"false");
			pro.put("log4j.appender." + itemRawName + "log","org.apache.log4j.DailyRollingFileAppender");
			pro.put("log4j.appender." + itemRawName + "log.file","./logs/" + itemRawName + ".log");
			pro.put("log4j.appender." + itemRawName + "log.DatePattern","'.'yyyy-MM-dd");
			pro.put("log4j.appender." + itemRawName + "log.Threshold","INFO");
			pro.put("log4j.appender." + itemRawName + "log.layout","com.puxtech.reuters.rfa.Common.MyPatternLayout");
			pro.put("log4j.appender." + itemRawName + "log.layout.ConversionPattern","%m");
			pro.put("log4j.appender." + itemRawName + "log.layout.Header", this.logHead.trim().length() > 0 ? this.logHead + "\r\n" : "\u5546\u54C1\u4EE3\u7801,\u8DEF\u900F\u4EE3\u7801,\u65F6\u95F4,ASK,BID,\u4EF7\u683C,\u4E0A\u4E00\u53E3\u6709\u6548\u4EF7\u683C,\u6DA8\u8DCC\u5E45,\u662F\u5426\u88AB\u8FC7\u6EE4\r\n");
			
			PropertyConfigurator.configure(pro);
			log.info("logger (" + itemName + ") configure finish!");
		}
		for(QuoteSource quoteSource : this.quoteSourceMap.values()){
			if(quoteSource.getProperty("LoggerName") != null && !"".equals(quoteSource.getProperty("LoggerName"))){
				String loggerName = quoteSource.getProperty("LoggerName");
				pro = new Properties();
				pro.put("log4j.logger." + loggerName, "INFO, " + loggerName + "log");
				pro.put("log4j.additivity." + loggerName,"false");
				pro.put("log4j.appender." + loggerName + "log","org.apache.log4j.DailyRollingFileAppender");
				pro.put("log4j.appender." + loggerName + "log.file","./logs/" + loggerName + ".log");
				pro.put("log4j.appender." + loggerName + "log.DatePattern","'.'yyyy-MM-dd");
				pro.put("log4j.appender." + loggerName + "log.Threshold","INFO");
				pro.put("log4j.appender." + loggerName + "log.layout","com.puxtech.reuters.rfa.Common.MyPatternLayout");
				pro.put("log4j.appender." + loggerName + "log.layout.ConversionPattern","%d{yyyy-MM-dd HH:mm:ss.SSS} - %m");
//				pro.put("log4j.appender." + loggerName + "log.layout.Header", this.logHead);
				//\u5546\u54C1\u4EE3\u7801,\u8DEF\u900F\u4EE3\u7801,\u65F6\u95F4,ASK,BID,\u4EF7\u683C,\u4E0A\u4E00\u53E3\u6709\u6548\u4EF7\u683C,\u6DA8\u8DCC\u5E45,\u662F\u5426\u88AB\u8FC7\u6EE4\r\n
				PropertyConfigurator.configure(pro);
				log.info("logger (" + loggerName + ") configure finish!");
			}
		}
	}
	
	public Element getFilterConfig(){
		Element filterConfigEle = null;
		if(this.doc != null){
			filterConfigEle = doc.getRootElement().element("Config").element("Filters");
		}
		return filterConfigEle;
	}

	public List<QuoteSource> getQuoteSourceList() {
		return quoteSourceList;
	}

	public Map<String, QuoteSource> getQuoteSourceMap() {
		return quoteSourceMap;
	}

	public Map<String, List<Contract>> getQuoteSourceContractMap() {
		return quoteSourceContractMap;
	}

	public List<Contract> getContractList() {
		return contractList;
	}

	public String getSourcePath() {
		return SourcePath;
	}

	public String getCronExpress() {
		return cronExpress;
	}

	public void setSourcePath(String sourcePath) {
		SourcePath = sourcePath;
	}

	public void setCronExpress(String cronExpress) {
		this.cronExpress = cronExpress;
	}

	public File getConfigFile() {
		return configFile;
	}

	public String getSpreadFormat() {
		return spreadFormat;
	}

	public void setSpreadFormat(String spreadFormat) {
		this.spreadFormat = spreadFormat;
	}
}
package com.puxtech.reuters.rfa.Common;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.puxtech.reuters.rfa.Publisher.TCPIoProcessorImpl;
import com.puxtech.reuters.rfa.RelayServer.RelayServer;

public class LoggerPrintStream extends PrintStream {
	private Log log = LogFactory.getLog(LoggerPrintStream.class);
	private static PrintStream ps = null;
	public static final PrintStream getConsoleLogger(Log log){
		PrintStream ps = null;
			try {
				ps = new LoggerPrintStream(log);
			} catch (FileNotFoundException e) {
				log.error("¹¹ÔìLoggerPrintStream³ö´í", e);
			}
		return ps;
	}
	public LoggerPrintStream(Log log) throws FileNotFoundException {
		super(System.getProperty("user.dir") + "/log.lck");
		this.log=log;
	}
	@Override
	public void println(String s) {
		log.error(s);
		log.error("\r\n");
	}
	@Override
	public void println(double s) { 
		log.error(s);
		log.error("\r\n");
	}
	@Override
	public void println(float s) { 
		log.error(s);
		log.error("\r\n");
	}
	@Override
	public void println(int s) { 
		log.error(s);
		log.error("\r\n");
	}
	@Override
	public void println(long s) { 
		log.error(s);
		log.error("\r\n");
	}
	@Override
	public void println(char s) { 
		log.error(s);
		log.error("\r\n");
	}
	@Override
	public void println(Object s) { 
		log.error(s);
		log.error("\r\n");
	}
	@Override
	public void println(boolean bool){
		log.error(bool + "\r\n");
	}
	@Override
	public void println(char[] charArray){
		log.error("[");
		for(char ch : charArray){
			log.error(ch);
			log.error(",");
		}
		log.error("]");
		log.error("\r\n");
	}
	@Override
	public void println(){
		log.error("\r\n");
	}
	@Override
	public void print(String s) {
		log.error(s);
	}
	@Override
	public void print(double s) { 
		log.error(s);
	}
	@Override
	public void print(float s) { 
		log.error(s);
	}
	@Override
	public void print(int s) { 
		log.error(s);
	}
	@Override
	public void print(long s) { 
		log.error(s);
	}
	@Override
	public void print(char s) { 
		log.error(s);
	}
	@Override
	public void print(Object s) { 
		log.error(s);
	}
	@Override
	public void print(boolean bool){
		log.error(bool);
	}
	@Override
	public void print(char[] charArray){
		log.error("[");
		for(char ch : charArray){
			log.error(ch);
			log.error(",");
		}
		log.error("]");
	}
}
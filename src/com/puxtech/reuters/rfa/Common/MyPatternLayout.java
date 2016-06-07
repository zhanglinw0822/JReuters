package com.puxtech.reuters.rfa.Common;

import org.apache.log4j.PatternLayout;

public class MyPatternLayout extends PatternLayout {
	
	public MyPatternLayout(){
		super();
	}
	public MyPatternLayout(String pattern){
		super(pattern);
	}
	private String header = null;
	public void setHeader(String header){
		this.header = header;
	}
	@Override
	public String getHeader() {
		return this.header;
	}
}

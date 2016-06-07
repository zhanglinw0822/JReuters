package com.puxtech.reuters.rfa.Common;

import java.util.Date;

public class SubNode implements Comparable<SubNode> {
	int index;
	String contractCode;
	Date attenBeginTime;
	Date changeContractTime;
	Date attenEndTime;
	int diffPricePeriod;
	String exchangeCode;
	
	public String getExchangeCode() {
		return exchangeCode;
	}
	public void setExchangeCode(String exchangeCode) {
		this.exchangeCode = exchangeCode;
	}
	public int getDiffPricePeriod() {
		return diffPricePeriod;
	}
	public void setDiffPricePeriod(int diffPricePeriod) {
		this.diffPricePeriod = diffPricePeriod;
	}
	public String getContractCode() {
		return contractCode;
	}
	public void setContractCode(String contractCode) {
		this.contractCode = contractCode;
	}
	public Date getAttenBeginTime() {
		return attenBeginTime;
	}
	public void setAttenBeginTime(Date attenBeginTime) {
		this.attenBeginTime = attenBeginTime;
	}
	public Date getChangeContractTime() {
		return changeContractTime;
	}
	public void setChangeContractTime(Date changeContractTime) {
		this.changeContractTime = changeContractTime;
	}
	public Date getAttenEndTime() {
		return attenEndTime;
	}
	public void setAttenEndTime(Date attenEndTime) {
		this.attenEndTime = attenEndTime;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	@Override
	public int compareTo(SubNode o) {
		return this.index - o.index;
	}
	@Override
	public String toString() {
		return "SubNode [index=" + index + ", contractCode=" + contractCode
				+ ", attenBeginTime=" + attenBeginTime
				+ ", changeContractTime=" + changeContractTime
				+ ", attenEndTime=" + attenEndTime + ", diffPricePeriod="
				+ diffPricePeriod + ", exchangeCode=" + exchangeCode + "]";
	}
	
	
}

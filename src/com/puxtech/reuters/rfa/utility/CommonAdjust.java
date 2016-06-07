package com.puxtech.reuters.rfa.utility;

public enum CommonAdjust {
	INSTANCE;
	public boolean adjustPriceFlag() {
		if (Integer.parseInt(PropUtil.getProperty("autoAdjustPrice")) == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public static void main(String[] args) {
		System.out.println(CommonAdjust.INSTANCE.adjustPriceFlag());
	}
}

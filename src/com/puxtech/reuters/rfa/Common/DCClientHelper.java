package com.puxtech.reuters.rfa.Common;

public class DCClientHelper {
	static { 
        System.loadLibrary("DCClient"); 
    }  
	public native void registerCallBack(Object iface, String method, Object[] params);
	public native void SetSerAddr(String addr, int port, String user, String pwd);
	
	
}

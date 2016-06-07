package com.puxtech.dc.common;

import org.apache.commons.logging.Log;

public class DCHelper {
	static{
		System.loadLibrary("DCHelper");
	}
	public native boolean DC_Init();
	public native void DC_SetSerAddr(String addr, int port, String username, String password);
	public native boolean DC_Connect();
	public native void DC_Stop();
	public native void setCallBack(CallbackBehaviour callbackBehaviour, Log logger);

	// 更新标志
	public static final int EDCPUFTime		= 0x01;
	public static final int EDCPUFMilli	= 0x02;
	public static final int EDCPUFType		= 0x04;

	public static final int EDCPUFSendMarket	= 0x010;
	public static final int EDCPUFName			= 0x020;
	public static final int EDCPUFCode			= 0x040;
	public static final int EDCPUFTradeCode	= 0x080;

	public static final int EDCPUFNow		= 0x0100;
	public static final int EDCPUFOpen		= 0x0200;
	public static final int EDCPUFHigh		= 0x0400;
	public static final int EDCPUFLow		= 0x0800;

	public static final int EDCPUFAvg			= 0x01000;
	public static final int EDCPUFSettle		= 0x02000;
	public static final int EDCPUFLastClose	= 0x04000;
	public static final int EDCPUFLastSettle	= 0x08000;	

	public static final int EDCPUFVolume	= 0x010000;
	public static final int EDCPUFAmount	= 0x020000;
	public static final int EDCPUFHold		= 0x040000;
}

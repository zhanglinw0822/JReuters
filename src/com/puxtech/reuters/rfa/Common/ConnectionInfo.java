package com.puxtech.reuters.rfa.Common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

public class ConnectionInfo {
	IoSession session;
	Date connectTime;
	int status;
	List<WriteFuture> sendFutures = Collections.synchronizedList(new ArrayList<WriteFuture>());
	
	public void init(){
		if(this.session != null){
			this.session.setAttribute("futureList", this.sendFutures);
		}
	}
	public IoSession getSession() {
		return session;
	}
	public void setSession(IoSession session) {
		this.session = session;
	}
	public Date getConnectTime() {
		return connectTime;
	}
	public void setConnectTime(Date connectTime) {
		this.connectTime = connectTime;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public List<WriteFuture> getSendFutures() {
		return sendFutures;
	}
	public void setSendFutures(List<WriteFuture> sendFutures) {
		this.sendFutures = sendFutures;
	}
}

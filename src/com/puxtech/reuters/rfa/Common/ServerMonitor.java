package com.puxtech.reuters.rfa.Common;

import java.util.*;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoServiceStatistics;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.puxtech.reuters.rfa.Publisher.TCPServerByMina;
import com.puxtech.reuters.rfa.RelayServer.QuoteEventOffSetHandler;
public class ServerMonitor implements Runnable{
	private static final List<IoSession> sessionList = new ArrayList<IoSession>();
	private static final Logger monitorLog = LoggerFactory.getLogger("moniter");
	public static void appendSession(IoSession session){
		if(session != null){
			session.setAttribute("futureList", Collections.synchronizedList(new ArrayList<WriteFuture>()));
			synchronized (sessionList) {
				sessionList.add(session);
			}
		}
	}
	
	public static List<IoSession> getSessionList(){
		return sessionList;
	}
	
	@Override
	public void run() {
		monitorLog.info("服务监控开始...");
		while(true){
			try {
				synchronized (sessionList) {
					int sessionCount = sessionList.size();
					monitorLog.info("当前连接数：" + sessionCount);
					List<IoSession> closedSessions = new ArrayList<IoSession>(); 
					for(int i = 0; i < sessionCount; i++){
						List<WriteFuture> futureList = new ArrayList<WriteFuture>();
						List<WriteFuture> futureListTemp = (List<WriteFuture>) sessionList.get(i).getAttribute("futureList");
						futureList.addAll(futureListTemp);
						monitorLog.info("*******************连接 " + (i + 1) + "开始*******************");
						monitorLog.info("当前连接状态： " + (sessionList.get(i).isConnected() ? "正常" : "关闭"));
						if(!sessionList.get(i).isConnected()){
							closedSessions.add(sessionList.get(i));
						}else{							
							monitorLog.info("客户端地址： " + sessionList.get(i).getRemoteAddress().toString());
							monitorLog.info("当前传输状态： " + (sessionList.get(i).isWriterIdle() ? "空闲" : "正常"));
							monitorLog.info("当前发送队列消息数：" + sessionList.get(i).getScheduledWriteMessages());
							monitorLog.info("当前发送队列大小：" + sessionList.get(i).getScheduledWriteBytes());
							monitorLog.info("连接建立时间：" + sessionList.get(i).getCreationTime());
							monitorLog.info("最近一次IO时间：" + sessionList.get(i).getLastIoTime());
							monitorLog.info("最近一次空闲时间：" + sessionList.get(i).getLastWriterIdleTime());
						}
						List<WriteFuture> doneFutures = new ArrayList<WriteFuture>();
						int j = 0;
						for(WriteFuture future : futureList){
							if(future.isDone()){
								doneFutures.add(future);
								//发送操作完成
								if(!future.isWritten()){
									//发送失败
									Throwable error = future.getException();
									if(error != null){
										monitorLog.info("*******************发送异常 " + j++ + "*******************");
										StackTraceElement[] elements = error.getStackTrace();
										StringBuffer sb = new StringBuffer();
										sb.append("Cause by " + error.getClass().getName() + " :" + error.getMessage() + "\r\n");
										for(StackTraceElement element : elements){
											sb.append(element.getClassName() + element.getMethodName() + "(" + element.getFileName() + ":" + element.getLineNumber() + ")\r\n");
										}
										monitorLog.info(sb.toString());
										monitorLog.info("*****************************************************");
									}
								}
							}
						}
						synchronized (futureListTemp) {
							futureListTemp.removeAll(doneFutures);
						}
						monitorLog.info("*******************连接 " + i + "结束*******************");
					}
					sessionList.removeAll(closedSessions);
				}
				Map offsetMap = QuoteEventOffSetHandler.getOffsetMap();
				if(offsetMap != null){
					monitorLog.info("**************************偏移量监控开始**************************");
					synchronized (offsetMap) {						
						for(Object key : offsetMap.keySet()){
							monitorLog.info("合约代码：" + key);
							monitorLog.info("偏移量：" + offsetMap.get(key).toString());
						}
					}
					monitorLog.info("**************************偏移量监控结束**************************");					
				}
			} catch (Exception e) {
				monitorLog.info("监控过程发生异常！", e);
			} finally {
				try {
					Thread.sleep(Configuration.getInstance().getMonitorInterval());
				} catch (InterruptedException e) {
					monitorLog.info("monitor sleep been Interrupted !");
				}
			}
			
//			IoServiceStatistics statistics = TCPServerByMina.getStatistics();
		}
	}
}
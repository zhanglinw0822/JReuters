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
		monitorLog.info("�����ؿ�ʼ...");
		while(true){
			try {
				synchronized (sessionList) {
					int sessionCount = sessionList.size();
					monitorLog.info("��ǰ��������" + sessionCount);
					List<IoSession> closedSessions = new ArrayList<IoSession>(); 
					for(int i = 0; i < sessionCount; i++){
						List<WriteFuture> futureList = new ArrayList<WriteFuture>();
						List<WriteFuture> futureListTemp = (List<WriteFuture>) sessionList.get(i).getAttribute("futureList");
						futureList.addAll(futureListTemp);
						monitorLog.info("*******************���� " + (i + 1) + "��ʼ*******************");
						monitorLog.info("��ǰ����״̬�� " + (sessionList.get(i).isConnected() ? "����" : "�ر�"));
						if(!sessionList.get(i).isConnected()){
							closedSessions.add(sessionList.get(i));
						}else{							
							monitorLog.info("�ͻ��˵�ַ�� " + sessionList.get(i).getRemoteAddress().toString());
							monitorLog.info("��ǰ����״̬�� " + (sessionList.get(i).isWriterIdle() ? "����" : "����"));
							monitorLog.info("��ǰ���Ͷ�����Ϣ����" + sessionList.get(i).getScheduledWriteMessages());
							monitorLog.info("��ǰ���Ͷ��д�С��" + sessionList.get(i).getScheduledWriteBytes());
							monitorLog.info("���ӽ���ʱ�䣺" + sessionList.get(i).getCreationTime());
							monitorLog.info("���һ��IOʱ�䣺" + sessionList.get(i).getLastIoTime());
							monitorLog.info("���һ�ο���ʱ�䣺" + sessionList.get(i).getLastWriterIdleTime());
						}
						List<WriteFuture> doneFutures = new ArrayList<WriteFuture>();
						int j = 0;
						for(WriteFuture future : futureList){
							if(future.isDone()){
								doneFutures.add(future);
								//���Ͳ������
								if(!future.isWritten()){
									//����ʧ��
									Throwable error = future.getException();
									if(error != null){
										monitorLog.info("*******************�����쳣 " + j++ + "*******************");
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
						monitorLog.info("*******************���� " + i + "����*******************");
					}
					sessionList.removeAll(closedSessions);
				}
				Map offsetMap = QuoteEventOffSetHandler.getOffsetMap();
				if(offsetMap != null){
					monitorLog.info("**************************ƫ������ؿ�ʼ**************************");
					synchronized (offsetMap) {						
						for(Object key : offsetMap.keySet()){
							monitorLog.info("��Լ���룺" + key);
							monitorLog.info("ƫ������" + offsetMap.get(key).toString());
						}
					}
					monitorLog.info("**************************ƫ������ؽ���**************************");					
				}
			} catch (Exception e) {
				monitorLog.info("��ع��̷����쳣��", e);
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
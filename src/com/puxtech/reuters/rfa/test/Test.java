package com.puxtech.reuters.rfa.test;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Test {

	public Test() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args) {
		try {
			Socket serverSocket = new Socket("10.100.32.206", 50001);
			serverSocket.setSoTimeout(30 * 1000);
			DataInputStream input = new DataInputStream(new BufferedInputStream(serverSocket.getInputStream()));
			while(true){
				String msg = receive(input);
				System.out.println(msg);
				Thread.sleep(1000);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String receive(DataInputStream input) {
		String msg = "";
		ByteArrayOutputStream array = null;
		DataOutputStream outputArray = null;
		try {
			array = new ByteArrayOutputStream();
			outputArray = new DataOutputStream(array);
			byte b = input.readByte();// ��ʼ�ַ� 0xff
//			System.out.println("b=" + b);
			// �����ַ�0x00
			while (0x00 != (b = input.readByte())) {
//				System.out.println("b1=" + b);
				outputArray.write(b);
			}
			outputArray.flush();
			byte[] buf = array.toByteArray();
			msg = new String(buf, "GBK");
		} catch (IOException e) {
			System.out.println("��������"+"�쳣" + e);
			msg = null;
		} catch (Exception e) {
			System.out.println("��������"+"�쳣" + e);

			msg = null;
		} finally {
			if (null != outputArray) {
				try {
					outputArray.close();
				} catch (IOException e) {
					System.out.println("�ر�������"+"�쳣" + e);
				}
			}
			if (null != array) {
				try {
					array.close();
				} catch (IOException e) {
					System.out.println("�ر�ByteArrayOutputStream"+"�쳣" + e);
				}
			}
		}
		return msg;
	}


}

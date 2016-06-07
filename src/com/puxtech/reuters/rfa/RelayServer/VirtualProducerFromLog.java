package com.puxtech.reuters.rfa.RelayServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class VirtualProducerFromLog implements Runnable {

	private static final String logFilePath = "";
	@Override
	public void run() {
		BufferedReader reader = null;
		try {
			File logFile = new File(logFilePath);
			StringBuffer sb = new StringBuffer();
			if(logFile.isFile()){
				reader = new BufferedReader(new FileReader(logFile));
				String line = null;
				while((line = reader.readLine()) != null){
					if(line.contains("Fri Jan")){
						if(sb.length() > 0){
							produce(sb.toString());
						}
					}

					
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(reader != null){
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void produce(String str){
		
	}

}

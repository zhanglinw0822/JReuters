package com.puxtech.reuters.rfa.Consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ODSPackage {
	private Map<Integer, String> params = new HashMap<Integer, String>();
	
	public ODSPackage(){
		
	}
	
	public ODSPackage(String content){
		for(String entry : content.split("[|]")){
			if(entry != null && entry.indexOf("=") > 0){
				String[] pair = entry.split("=");
				if(pair[0].matches("\\d+")){
					params.put(Integer.valueOf(pair[0]), pair[1]);
				}
			}
		}
	}
	
	public String getParam(Integer key){
		return params.get(key);
	}
	
	public int getParamInt(Integer key){
		return Integer.valueOf(this.getParam(key));
	}
	
	public void appendParam(Integer key, String value){
		this.params.put(key, value);
	}
	
	@Override
	public String toString() {
		StringBuffer sb = null;
		for(Entry<Integer, String> entry : params.entrySet()){
			if(sb == null){
				sb = new StringBuffer();
			}else{
				sb.append("|");
			}
			sb.append(entry.getKey() + "=" + entry.getValue());
		}
		sb.append("\r");
		return sb.toString();
	}
}

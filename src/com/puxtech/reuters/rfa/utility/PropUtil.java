package com.puxtech.reuters.rfa.utility;

import java.io.IOException;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * 
 */

/**
 * @author Smith
 * 
 */
public class PropUtil {
	
	/**
	 * 从properties中读取key的value，如果没找到返回null�?
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static String getProperty(String key)  {
		try {
			Properties prop = new Properties();
			InputStream in = new PropUtil().getClass().getResourceAsStream("/config.properties");
			prop.load(in);
			Set keyValue = prop.keySet();
			if (keyValue.contains(key)) {
				return prop.getProperty(key);
			} else {
				return null;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}

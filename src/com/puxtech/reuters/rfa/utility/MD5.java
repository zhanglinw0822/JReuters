package com.puxtech.reuters.rfa.utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;

public class MD5 {
	/**
	 * 用户密码加密算法,采用java 的MD5加密算法
	 * @param userId 用户名
	 * @param pwd 用户输入的密码
	 */
//	public final static String getMD5(String userId, String pwd) {
//		try {
//			char[] userIdArray = toChar(userId);
//			byte[] strTemp = pwd.getBytes();
//			MessageDigest mdTemp = MessageDigest.getInstance("MD5");
//			mdTemp.update(strTemp);
//			byte[] md = mdTemp.digest();
//			int j = md.length;
//			char str[] = new char[j * 2];
//			int k = 0;
//			for (int i = 0; i < j; i++) {
//				byte byte0 = md[i];
//				char[] hexDigits = complement(userIdArray);
//				str[k++] = hexDigits[byte0 >>> 3 & 0xf];
//				str[k++] = hexDigits[byte0 & 0xf];
//			}
//			return new String(str);
//		} catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		}
//	}

	/**
	 * 因为加密算法中采用用户名做为摘要,而MD5算法中的摘要必须为16位,所以在用户不足16位时
	 * 程序自动填满16位字符,以'z'填充
	 */

//	public static char[] complement(char[] v) throws Exception {
//		char[] c = new char[16];
//		if (v == null) {
//			throw new Exception("密码转换时,摘要无效!");
//		}
//		if (v.length < 16) {
//			for (int i = 0; i < v.length; i++) {
//				c[i] = v[i];
//			}
//			for (int i = v.length; i < 16; i++) {
//				c[i] = 'z';
//			}
//			return c;
//		} else {
//			return v;
//		}
//	}

	/**
	 * 将用户名转换为字符数组
	 * @param userId 用户名
	 */
//	public static char[] toChar(String userId) {
//		char[] result = new char[userId.length()];
//		for (int i = 0; i < result.length; i++) {
//			result[i] = userId.charAt(i);
//		}
//		return result;
//	}
	
	 /**
     * 根据算法对字符串加密
     * @param 加密的字符串
     * @param 算法
     * @return 加密后的字符串
     */
    public static String encodePassword(String password, String algorithm) {
        byte[] unencodedPassword = password.getBytes();

        MessageDigest md = null;

        try {
            // first create an instance, given the provider
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            return password;
        }

        md.reset();

        // call the update method one or more times
        // (useful when you don't know the size of your data, eg. stream)
        md.update(unencodedPassword);

        // now calculate the hash
        byte[] encodedPassword = md.digest();

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < encodedPassword.length; i++) {
            if ((encodedPassword[i] & 0xff) < 0x10) {
                buf.append("0");
            }

            buf.append(Long.toString(encodedPassword[i] & 0xff, 16));
        }

        return buf.toString();
    }
    
    public static String encodePassword(byte[] source, String algorithm) {

        MessageDigest md = null;

        try {
            // first create an instance, given the provider
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            return null;
        }

        md.reset();

        // call the update method one or more times
        // (useful when you don't know the size of your data, eg. stream)
        md.update(source);

        // now calculate the hash
        byte[] encodedPassword = md.digest();

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < encodedPassword.length; i++) {
            if ((encodedPassword[i] & 0xff) < 0x10) {
                buf.append("0");
            }

            buf.append(Long.toString(encodedPassword[i] & 0xff, 16));
        }

        return buf.toString();
    }
    
    
    public final static String getMD5(String userId, String pwd) {
    	return encodePassword(userId+pwd,"MD5");
    }

    public final static String getMD5(byte[] source) {
    	return encodePassword(source,"MD5");
    }
    
    public final static String getFileMD5(File file){
		byte[] fileBytes = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if(file != null && file.exists()){
			byte[] buff = new byte[1024];
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(file);
				int readLength = -1;
				while((readLength = fis.read(buff)) != -1){
					baos.write(buff, 0, readLength);
				}
				fileBytes = baos.toByteArray();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if(fis != null){
					try {
						fis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		if(fileBytes != null){
			return MD5.getMD5(fileBytes);
		}else{
			return null;
		}
	}
}

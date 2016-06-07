package com.puxtech.reuters.rfa.utility;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
	private static String yyyy_MM_dd  = "yyyy-MM-dd";
	private static String yyyyMMdd = "yyyyMMdd";
	
	/**
	 * str --- > date(yyyy-MM-dd)
	 * @param strDate
	 * @return
	 * @throws ParseException
	 */
	public synchronized static Date formatYyyy_MM_ddDate(String strDate) {
		Date aDate = null;
		aDate = convertStringToDate(yyyy_MM_dd, strDate);
		return aDate;
	}
	
	/**
	 * str --- > date(yyyy-MM-dd)
	 * @param strDate
	 * @return
	 * @throws ParseException
	 */
	public synchronized static Date formatYyyyyMMddDate(String strDate){
		Date aDate = null;
		aDate = convertStringToDate(yyyyMMdd, strDate);
		return aDate;
	}
	/**
	 * date ---->str
	 * @param strDate
	 * @return
	 */
	public synchronized static String parseYyyyyMMddDate(Date date,String format){
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}
	
	private synchronized static final Date convertStringToDate(String aMask, String strDate) {
		SimpleDateFormat df = null;
		Date date = null;
		df = new SimpleDateFormat(aMask);
		try {
			date = df.parse(strDate);
		} catch (ParseException pe) {
			pe.printStackTrace();
		}
		return (date);
	}
	
	/**
	 * 比较2个日期大小.  date1>date2  返回1  date1=date2返回0  date1<date2返回-1
	 * @param date1
	 * @param date2
	 * @return
	 */
	public synchronized static int compareDate(Date date1,Date date2){
		if(date1.before(date2)){
			return -1;
		}else if(date1.equals(date2)){
			return 0;
		}else if(date1.after(date2)){
			return 1;
		}
		return 0;
	}
	
	/**
	 * 返回最新日期，格式：yyyyMMdd
	 * @return
	 */
	public synchronized static String getNewestDate(){
		SimpleDateFormat df =  new SimpleDateFormat(yyyyMMdd);
		return df.format(Calendar.getInstance().getTime());
	}
	
	/**
	 * srcDate(yyyyMMdd) + n天
	 * @param srcDate
	 * @param n
	 * @return
	 */
	public synchronized static String addDays(String srcDate,int n){
		Date date = formatYyyyyMMddDate(srcDate);
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.DAY_OF_YEAR, n);
		SimpleDateFormat sdf = new SimpleDateFormat(yyyyMMdd);
		return sdf.format(c.getTime());
		
	}
	
	/**
	 * 返回当前日期和参数日期的时间差的绝对值,单位（秒）
	 * @param date
	 * @return
	 */
	public synchronized static Long getDateDiff(Date date){
		return Math.abs(Calendar.getInstance().getTimeInMillis()/1000 - date.getTime()/1000);
	}
	
	public static void main(String[] args) throws Exception {
//		Date date1 = DateUtils.formatYyyy_MM_ddDate("2015-04-20");
//		Date date2 = DateUtils.formatYyyy_MM_ddDate("2015-04-19");
//		System.out.println(DateUtils.compareDate(date1,date2));
//		
//		System.out.println(DateUtils.getNewestDate());
//		
//		System.out.println(DateUtils.addDays(DateUtils.getNewestDate(), 1));
		  Calendar l1 = Calendar.getInstance();
		Thread.sleep(3000);
		System.out.println(DateUtils.getDateDiff(l1.getTime()));
	}
}

package com.puxtech.reuters.rfa.Filter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;

import com.puxtech.reuters.rfa.Common.Quote;

public class FirstQuoteFilterStrategy extends FilterStrategy {
	private Date today = null;
	private List<Date> monTimeList = new ArrayList<Date>();
	private List<Date> tueTimeList = new ArrayList<Date>();
	private List<Date> wedTimeList = new ArrayList<Date>();
	private List<Date> thuTimeList = new ArrayList<Date>();
	private List<Date> friTimeList = new ArrayList<Date>();
	private List<Date> satTimeList = new ArrayList<Date>();
	private List<Date> sunTimeList = new ArrayList<Date>();
	
	private List<Boolean> timeFlags = new ArrayList<Boolean>();

	private static SimpleDateFormat formater = new SimpleDateFormat("dd");
	private static SimpleDateFormat timeFormater = new SimpleDateFormat("HH:mm");
	private static SimpleDateFormat dateFormater = new SimpleDateFormat("E", Locale.US);

	public void initTimeFlags(int size){
		this.timeFlags = new ArrayList<Boolean>();
		for(int i = 0; i < size; i++){
			this.timeFlags.add(true);
		}
	}

	private List<Date> getTimeList(Date priceTime){
		String dateStr = dateFormater.format(priceTime);
		if("Mon".equals(dateStr)){
			return this.monTimeList;
		}
		if("Tue".equals(dateStr)){
			return this.tueTimeList;
		}
		if("Wed".equals(dateStr)){
			return this.wedTimeList;
		}
		if("Thu".equals(dateStr)){
			return this.thuTimeList;
		}
		if("Fri".equals(dateStr)){
			return this.friTimeList;
		}
		if("Sat".equals(dateStr)){
			return this.satTimeList;
		}
		if("Sun".equals(dateStr)){
			return this.sunTimeList;
		}
		return new ArrayList<Date>();
	}
	
	@Override
	public boolean isFilter(Quote quote) {
		if(quote == null){
			return false;
		}
		List<Date> timeList = this.getTimeList(quote.priceTime);
		if(today != null && !formater.format(quote.priceTime).equals(formater.format(today))){
			//���죬���ü�ʱ
			this.initTimeFlags(timeList.size());
		}else{
			//�����������������û�б仯
			
		}
		for(int i = 0; i < this.timeFlags.size(); i++){
			boolean needFilter = this.timeFlags.get(i);
			if(needFilter){
				//��ǰʱ�����δ����
				try {
					Date now = timeFormater.parse(timeFormater.format(new Date()));
					Date checkTime = timeList.get(i);
					if(now.compareTo(checkTime) >= 0){
						
						return true;
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}else{
				//��ǰʱ����ѹ���
				
			}
			
		}
		
		
		if(today == null){
			//��������ʱ����һ�ڼ۹��ˣ�����¼��ǰ����
			today = quote.priceTime;
			return true;
		}else{
			if(formater.format(quote.priceTime).equals(formater.format(today))){
				//��ǰ����û�з����仯ʱ��������
				return false;
			}else{
				//��ǰ���ڷ����仯������һ�ʣ���¼��ǰ����
				today = quote.priceTime;
				return true;
			}
		}
	}

	@Override
	public void appendRule(Rule rule, QuotePool quotePool) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setUnlimitRule(Rule unlimitRule) {
		// TODO Auto-generated method stub

	}

	@Override
	public void resetBenchMark() {
		// TODO Auto-generated method stub
		
	}
}

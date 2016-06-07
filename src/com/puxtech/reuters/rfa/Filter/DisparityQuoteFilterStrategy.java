package com.puxtech.reuters.rfa.Filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.puxtech.reuters.rfa.Common.Quote;

public class DisparityQuoteFilterStrategy extends FilterStrategy {
	private static final Log moniterLog = LogFactory.getLog("moniter");
	private List<Rule> roleList = new ArrayList<Rule>();
	private Rule unlimitRule = null;
	private int[] filtedCounts = {};
	
	public Rule getUnlimitRule() {
		return unlimitRule;
	}
	@Override
	public void setUnlimitRule(Rule unlimitRule) {
		this.unlimitRule = unlimitRule;
	}

	public DisparityQuoteFilterStrategy() {
		super();
	}

	public void appendRule(Rule rule, QuotePool quotePool){
		this.roleList.add(rule);
		this.filtedCounts = new int[this.roleList.size()];
		//roleList���ݰٷֱȴ�С�����ź��򣬲���������˳��
		Collections.sort(this.roleList, new Comparator<Rule>() {
			@Override
			public int compare(Rule o1, Rule o2) {
				return o1.getPercent().compareTo(o2.getPercent());
			}
		});
	}

	@Override
	public boolean isFilter(Quote quote){
		if(quote == null){
			return false;
		}
		if(unlimitRule != null && unlimitRule.match(quote, benchMark)){
			return true;
		}
		int step = -1;//��¼��ǰ�۸��䵽�ڼ������ݣ�roleList���¼�ѹ��˱�����filtedCounts�������±����
		boolean isFilter = false;
		for(int i = 0; i < roleList.size(); i++){
			Rule rule = roleList.get(i);
			if(rule.match(quote, benchMark)){//���жϰٷֱ��Ƿ����
				this.filtedCounts[i] += 1;//�����ϣ���ǰ�����ѹ�����+1
				step = i;//���뵱ǰ����
			}
		}
		if(step < 0){//���н��ݾ������ϣ��ٷֱ�С����ͽ��ݣ�������
			isFilter = false;
			//���»�׼��
			benchMark = quote.newPrice;
			//�ѹ��˱�������
			for(int i = 0; i < this.filtedCounts.length; i++){
				this.filtedCounts[i] = 0;
			}
		}else{
			//����stepָ���Ľ��ݣ��жϸý����ѹ������Ƿ�С�ڵ���Ӧ������,Ӧ�������ڶ�ȡ����ʱ��-1���˴��ж�С�ڵ���
			if(this.filtedCounts[step] <= roleList.get(step).getFilteCount()){
				isFilter = true;//��ǰ���ݻ���ʣ�࣬����
			}else{
				isFilter = false;//��ǰ����������������
				//���»�׼��
				benchMark = quote.newPrice;
				//�ѹ��˱�������
				for(int i = 0; i < this.filtedCounts.length; i++){
					this.filtedCounts[i] = 0;
				}
			}
		}
		return isFilter;
	}
	public List<Rule> getRoleList() {
		return roleList;
	}
	@Override
	public void resetBenchMark() {
		benchMark = null;
	}
}
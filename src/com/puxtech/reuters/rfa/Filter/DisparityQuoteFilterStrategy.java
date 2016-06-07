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
		//roleList根据百分比从小到大排好序，不依赖配置顺序
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
		int step = -1;//记录当前价格落到第几个阶梯，roleList与记录已过滤笔数的filtedCounts数组以下标对齐
		boolean isFilter = false;
		for(int i = 0; i < roleList.size(); i++){
			Rule rule = roleList.get(i);
			if(rule.match(quote, benchMark)){//仅判断百分比是否符合
				this.filtedCounts[i] += 1;//若符合，则当前阶梯已过滤数+1
				step = i;//落入当前阶梯
			}
		}
		if(step < 0){//所有阶梯均不符合，百分比小于最低阶梯，不过滤
			isFilter = false;
			//更新基准价
			benchMark = quote.newPrice;
			//已过滤笔数清零
			for(int i = 0; i < this.filtedCounts.length; i++){
				this.filtedCounts[i] = 0;
			}
		}else{
			//落入step指定的阶梯，判断该阶梯已过滤数是否小于等于应过滤数,应过滤数在读取配置时已-1，此处判断小于等于
			if(this.filtedCounts[step] <= roleList.get(step).getFilteCount()){
				isFilter = true;//当前阶梯还有剩余，过滤
			}else{
				isFilter = false;//当前阶梯已满，不过滤
				//更新基准价
				benchMark = quote.newPrice;
				//已过滤笔数清零
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
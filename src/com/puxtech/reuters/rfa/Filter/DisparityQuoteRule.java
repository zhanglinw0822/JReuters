package com.puxtech.reuters.rfa.Filter;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.dom4j.Element;

import com.puxtech.reuters.rfa.Common.Quote;
import com.puxtech.reuters.rfa.Common.QuoteOperator;

public class DisparityQuoteRule extends Rule {
	private BigDecimal percent;
	private QuoteOperator operator;
	private int filteCount = 0;
	private int filteredCount = 0;
	public DisparityQuoteRule() {
		super();
	}
@Override
public int getFilteCount() {
		return filteCount;
	}

	public void setFilteCount(int filteCount) {
		this.filteCount = filteCount;
	}

	public int getFilteredCount() {
		return filteredCount;
	}
	public void setFilteredCount(int filteredCount) {
		this.filteredCount = filteredCount;
	}
	
	public void clearFilteredCount(){
		this.filteredCount=0;
	}
	public void addFilteredCount(int count){
		this.filteredCount += count;
	}
	//	public Element getRuleConfigElement() {
//		return ruleConfigElement;
//	}
//
//	public void setRuleConfigElement(Element ruleConfigElement) {
//		this.ruleConfigElement = ruleConfigElement;
//	}
	public BigDecimal getPercent() {
		return percent;
	}

	public void setPercent(BigDecimal percent) {
		this.percent = percent;
	}

	public QuoteOperator getOperator() {
		return operator;
	}

	public void setOperator(QuoteOperator operator) {
		this.operator = operator;
	}

	@Override
	public boolean match(Quote quote, BigDecimal benchMark) {
//		BigDecimal benchMark = .get(quote.exchangeCode);
		if(benchMark == null){
			return false;
		}
		boolean isMatch = false;
		BigDecimal curPercent = quote.newPrice.subtract(benchMark).abs().divide(benchMark, 4, RoundingMode.DOWN).multiply(BigDecimal.valueOf(100));//计算公式：取绝对值（当前价-基准价）/基准价 * 100
//		quote.prevPrice=FilterStrategy.benchMarkMap.get(quote.exchangeCode);
		quote.spread=curPercent;
//		System.out.println("newPrice=" + quote.newPrice + ";benchPrice=" + benchMark +";curPercent="+curPercent + ";strategyPercent=" + percent);
		switch (operator) {
		case GREATERTHAN:
			isMatch = curPercent.compareTo(percent) == 1;
			break;
		case EQUAL:
			isMatch = curPercent.compareTo(percent) == 0;
			break;
		case GREATEROREQUAL:
			isMatch = curPercent.compareTo(percent) != -1;
			break;
		case LESSOREQUAL:
			isMatch = curPercent.compareTo(percent) != 1;
			break;
		case LESSTHAN:
			isMatch = curPercent.compareTo(percent) == -1;
			break;
		default:
			break;
		}
//		if(isMatch) this.filteredCount += 1;
//		isMatch = isMatch && this.filteredCount <= this.filteCount;
//		System.out.println("benchMark=" + benchMark + ";nowQuote=" + quote.newPrice + ";curPercent" + curPercent + "percent=" + percent + "isMatch=" + isMatch + "filteCount=" + this.filteCount + "filteredCount=" + this.filteredCount);
		return isMatch;
	}
}
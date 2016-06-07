package com.puxtech.reuters.rfa.Common;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Quote {
	static public long idSeed = 1;
	static private Date idSeedTime = new Date();
	static private SimpleDateFormat dateFormat = new SimpleDateFormat("dd");
	static public long generateQuoteID(){
		if(dateFormat.format(new Date()) != null && !dateFormat.format(new Date()).equals(dateFormat.format(idSeedTime))){
			//跨天
			idSeed = 1;
		}
		idSeedTime = new Date();
		return idSeed++;
	}
	static SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	static public Quote generateQuote(String quoteStr){
		Quote quote = null;
		if(quoteStr != null){
			String[] str = quoteStr.split(",");
			if(str.length == 9){
				try {
					quote = new Quote();
					quote.exchangeCode = str[0].trim();
					quote.reutersCode = str[1].trim();
					quote.priceTime = dateFormater.parse(str[2].trim());
					quote.askPrice = new BigDecimal(str[3].trim());
					quote.bidPrice = new BigDecimal(str[4].trim());
					quote.newPrice = new BigDecimal(str[5].trim());
				} catch (ParseException e) {
					quote = null;
				}
			}else if (str.length == 20){
				try {
					quote = new Quote();
					quote.exchangeCode = str[0].trim();
					quote.reutersCode = str[1].trim();
					quote.priceTime = dateFormater.parse(str[2].trim());
					quote.askPrice = new BigDecimal(str[3].trim());
					quote.bidPrice = new BigDecimal(str[4].trim());
					quote.newPrice = new BigDecimal(str[5].trim());
					quote.tradeQty = new Long(str[8].trim());
					quote.yClosePrice = new BigDecimal(str[9].trim());
					quote.ySettlePrice = new BigDecimal(str[10].trim());
					quote.openPrice = new BigDecimal(str[11].trim());
					quote.highPrice = new BigDecimal(str[12].trim());
					quote.lowPrice = new BigDecimal(str[13].trim());
					quote.hold = new Long(str[14].trim());
					quote.holdDif = new Long(str[15].trim());
					quote.range = new BigDecimal(str[16].trim());
					quote.amp = new BigDecimal(str[17].trim());
					quote.change = new BigDecimal(str[18].trim());
				} catch (ParseException e) {
					quote = null;
				}
			}
		}
		return quote;
	}
	public long id;
	public BigDecimal bidPrice = new BigDecimal(0.0);
	public BigDecimal askPrice = new BigDecimal(0.0);
	public BigDecimal newPrice = new BigDecimal(0.0);
	public BigDecimal prevPrice = new BigDecimal("0.0");
	public BigDecimal spread = new BigDecimal("0.000");
	public BigDecimal ySettlePrice = new BigDecimal("0.0");
	public BigDecimal yClosePrice = new BigDecimal("0.0");
	public BigDecimal openPrice = new BigDecimal("0.0");
	public BigDecimal highPrice = new BigDecimal("0.0");
	public BigDecimal lowPrice = new BigDecimal("0.0");
	public Long hold = 0L;
	public Long holdDif = 0L;
	public BigDecimal range = new BigDecimal("0.0");         //幅度
	public BigDecimal amp = new BigDecimal("0.0");           //振幅
	public BigDecimal change = new BigDecimal("0.0");       //涨跌
	public Long tradeQty = 0L;
	public Date priceTime;
	public String reutersCode;
	public String exchangeCode;
	public String dcCode;
	public String dcMarket;
	public boolean isFilter = false;
	public boolean transferCompleted = false;
	private int handledFlag;
	public int getHandledFlag() {
		return handledFlag;
	}
	public void setOffsetHandled(){
		handledFlag = handledFlag | 8 ;//第四位设为1
	}
	public void setFilterHandled(){
		handledFlag = handledFlag | 4;//第三位设为1
	}
	public void setIDHandled(){
		handledFlag = handledFlag | 2;//第二位设为1
	}
	public void setLogHandled(){
		handledFlag = handledFlag | 1;//末位设为1
	}
	/**
	 * 是否可以发送
	 * @return
	 */
	public boolean canSend(){
		if ((handledFlag & 14) == 14) {
			return true;
		}
		return false;
	}
	public Quote(){
		this.id = System.nanoTime();
	}
	@Override
	public String toString(){
		String filterStr = isFilter ? "Y" : "N";
		String quoteStr = "";
		switch(Configuration.getInstance().getLogFlag()){
		case 1:
			quoteStr = exchangeCode + "," + reutersCode + "," + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(priceTime) + "," + 
			QuoteEncoder.getPriceFormat().format(askPrice) +"," + 
			QuoteEncoder.getPriceFormat().format(bidPrice) + "," + 
			QuoteEncoder.getPriceFormat().format(newPrice) + "," + prevPrice + "," + spread + "%," + filterStr;
			break;
		case 2:
			quoteStr = exchangeCode + "," + reutersCode + "," + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(priceTime) + "," + 
			QuoteEncoder.getPriceFormat().format(askPrice) +"," + 
			QuoteEncoder.getPriceFormat().format(bidPrice) + "," + 
			QuoteEncoder.getPriceFormat().format(newPrice) + "," + prevPrice + "," + spread + "%," + tradeQty + "," + yClosePrice + "," + ySettlePrice + "," + openPrice + "," 
			+ highPrice + "," + lowPrice + "," + hold + "," + holdDif + "," + range + "," + amp + "," + change + "," + filterStr;
			break;
		}
		return quoteStr;
	}
}
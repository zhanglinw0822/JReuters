package com.puxtech.reuters.rfa.Common;
import java.text.AttributedCharacterIterator;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class QuoteEncoder
{
  /*
   * %Date%         日期
   * %Time%         时间
   * %quoteID%    行情ID
   * %COI%           商品ID
   * %newPr%        最新价
   * %bidPr%         bid价
   * %askPr%         ask价
   * %TrQty%         成交量
   * %yClose%        昨收
   * %ySettle%       昨结
   * %Open%         开盘价
   * %High%          最高价
   * %Low%           最低价
   * %hold%          持仓量
   * %hDiff%         仓差
   * %range%        幅度
   * %amp%          振幅
   * %change%     涨跌
   * */
  private static NumberFormat priceFormat = NumberFormat.getNumberInstance();
  static
  {
    priceFormat.setMaximumFractionDigits(4);
    priceFormat.setMinimumFractionDigits(4);
  }

  private static String formatNumber(double price)
  {
    StringBuffer sb = new StringBuffer();
    if ((priceFormat instanceof DecimalFormat)) {
      DecimalFormat format = (DecimalFormat)priceFormat;
      AttributedCharacterIterator iterator = format.formatToCharacterIterator(new Double(price));
      for (char c = iterator.first(); c != 65535; c = iterator.next()) {
        if (c != ',') {
          sb.append(c);
        }
      }
    }
    System.out.println("price=" + sb.toString());
    return sb.toString();
  }
  public static NumberFormat getPriceFormat() {
    return priceFormat;
  }

  public byte[] encode(Quote quote)
  {
    byte[] quoteBytes = new byte[0];
    if (quote != null) {
    	String idStr = String.format("%03d", quote.id % 1000);
      try {
    	  String quoteFormat = Configuration.getInstance().getQuoteFormat();
    	  if(quoteFormat != null && quoteFormat.length() > 0){
    		  String quoteStr = quoteFormat.replaceAll("%quoteID%", idStr).replaceAll("%COI%", quote.exchangeCode).replaceAll("%Date%", new SimpleDateFormat("yyyyMMdd").format(quote.priceTime))
    				  .replaceAll("%Time%", new SimpleDateFormat("HHmmss.SSS").format(quote.priceTime)).replaceAll("%newPr%", formatNumber(quote.newPrice.doubleValue()))
    				  .replaceAll("%bidPr%", formatNumber(quote.bidPrice.doubleValue())).replaceAll("%askPr%", formatNumber(quote.askPrice.doubleValue()));
    		  if(Configuration.getInstance().getLogFlag() == 2){
    			  quoteStr.replaceAll("%TrQty%", formatNumber(quote.tradeQty))
    			  .replaceAll("%yClose%", formatNumber(quote.yClosePrice.doubleValue())).replaceAll("%ySettle%", formatNumber(quote.ySettlePrice.doubleValue())).replaceAll("%Open%", formatNumber(quote.openPrice.doubleValue()))
    			  .replaceAll("%High%", formatNumber(quote.highPrice.doubleValue())).replaceAll("%Low%", formatNumber(quote.lowPrice.doubleValue())).replaceAll("%hold%", formatNumber(quote.hold))
    			  .replaceAll("%hDiff%", formatNumber(quote.holdDif)).replaceAll("%range%", formatNumber(quote.range.doubleValue())).replaceAll("%amp%", formatNumber(quote.amp.doubleValue()))
    			  .replaceAll("%change%", formatNumber(quote.change.doubleValue()));
    		  }
    		  quoteBytes = quoteStr.getBytes();
    		  quoteBytes[(quoteBytes.length - 1)] = 0;
    	  }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return quoteBytes;
  }
}
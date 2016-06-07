package com.puxtech.reuters.rfa.Filter;

import com.puxtech.reuters.rfa.Common.Quote;
import com.puxtech.reuters.rfa.Common.QuoteOperator;

public class QuotePool {
	private int size;
	private Quote[] quoteArray;
	
	public QuotePool(int size) {
		super();
		this.size = size;
		quoteArray = new Quote[size - 1];
	}
	
	public boolean put(Quote newQuote){
		for(int i = 0; i < quoteArray.length; i++){
			if(quoteArray[i] == null){
				quoteArray[i] = newQuote;
//				System.out.println("quotePool size=" + size + ";not full");
				return true;
			}
		}
//		this.clear();
//		System.out.println("quotePool size=" + size + ";full and cleared!");
		return false;
	}
	
	public void clear(){
		for(int i = 0; i < quoteArray.length; i++){
			quoteArray[i] = null;
		}
	}

	public int getSize() {
		return size;
	}
	
}
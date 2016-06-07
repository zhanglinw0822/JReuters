package com.puxtech.reuters.rfa.Common;

import java.math.BigDecimal;

public class Calculator {
	private static Calculator calculator;
	public static Calculator getInstance(){
		if(calculator == null){
			calculator = new Calculator();
		}
		return calculator;
	}
	private Calculator(){}
	BigDecimal big;
}

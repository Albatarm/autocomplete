package com.albatarm.autocomplete.app;

import com.albatarm.lang.calculatorLexer;
import com.albatarm.lang.calculatorParser;

public class CalculatorLang2 extends AbstractLang<calculatorLexer, calculatorParser> {

	public CalculatorLang2() {
		super(calculatorLexer::new, calculatorParser.ruleNames, calculatorLexer.VOCABULARY, calculatorParser._ATN);
	}

}

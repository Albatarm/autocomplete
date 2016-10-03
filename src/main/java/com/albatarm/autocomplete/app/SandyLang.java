package com.albatarm.autocomplete.app;

import com.sandy.SandyLexer;
import com.sandy.SandyParser;

public class SandyLang extends AbstractLang<SandyLexer, SandyParser> {

	public SandyLang() {
		super(SandyLexer::new, SandyParser.ruleNames, SandyLexer.VOCABULARY, SandyParser._ATN);
	}

}

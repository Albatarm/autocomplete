package com.albatarm.autocomplete.app;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.albatarm.lang.CalcLexer;
import com.albatarm.lang.CalcParser;

public class CalcLang extends AbstractLangEx<CalcLexer> {
    
    private static final Set<Integer> SEPARATORS = new HashSet<>(Arrays.asList(
            CalcLexer.RPAREN,
            CalcLexer.LPAREN,
            CalcLexer.EQ,
            CalcLexer.DIV,
            CalcLexer.GT,
            CalcLexer.LT,
            CalcLexer.MINUS,
            CalcLexer.PLUS,
            CalcLexer.TIMES,
            CalcLexer.POW
    ));
    
    public CalcLang() {
        super(
                "src/main/antlr4/com/albatarm/lang/Calc.g4",
                "target/generated-sources/antlr4/Calc.tokens",
                SEPARATORS,
                "equation",
                CalcParser.tokenNames, 
                CalcLexer::new
        );
    }

}

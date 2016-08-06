package com.albatarm.autocomplete.app;

import com.albatarm.lang.calculatorLexer;
import com.albatarm.lang.calculatorParser;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CalculatorLang extends AbstractLang<calculatorLexer> {
    
    private static final Set<Integer> SEPARATORS = new HashSet<>(Arrays.asList(
            calculatorLexer.RPAREN,
            calculatorLexer.LPAREN,
            calculatorLexer.EQ,
            calculatorLexer.DIV,
            calculatorLexer.GT,
            calculatorLexer.LT,
            calculatorLexer.MINUS,
            calculatorLexer.PLUS,
            calculatorLexer.TIMES,
            calculatorLexer.POW
    ));
    
    public CalculatorLang() {
        super(
                "src/main/antlr4/com/albatarm/lang/calculator.g4",
                "target/generated-sources/antlr4/calculator.tokens",
                SEPARATORS,
                "equation",
                calculatorParser.tokenNames, 
                calculatorLexer::new
        );
    }

}

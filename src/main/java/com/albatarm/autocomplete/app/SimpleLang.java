package com.albatarm.autocomplete.app;

import com.albatarm.lang.SimpleLexer;
import com.albatarm.lang.SimpleParser;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SimpleLang extends AbstractLang<SimpleLexer> {
    
    private static final Set<Integer> SEPARATORS = new HashSet<>(Arrays.asList(
            SimpleLexer.PLUS
    ));
    
    public SimpleLang() {
        super(
                "src/main/antlr4/com/albatarm/lang/simple.g4",
                "target/generated-sources/antlr4/simple.tokens",
                SEPARATORS,
                "root",
                SimpleParser.tokenNames,
                SimpleLexer::new
        );
    }
    
}

package com.albatarm.autocomplete.app;

import com.albatarm.lang.OtherSimpleLexer;
import com.albatarm.lang.OtherSimpleParser;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class OtherSimpleLang extends AbstractLang<OtherSimpleLexer> {
    
    private static final Set<Integer> SEPARATORS = new HashSet<>(Arrays.asList(
            OtherSimpleLexer.PLUS
    ));

    protected OtherSimpleLang() {
        super(
                "src/main/antlr4/com/albatarm/lang/OtherSimple.g4", 
                "target/generated-sources/antlr4/OtherSimple.tokens", 
                SEPARATORS, 
                "root", 
                OtherSimpleParser.tokenNames, 
                OtherSimpleLexer::new
        );
    }

}

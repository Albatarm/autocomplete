package com.albatarm.autocomplete;

public class GrammarNode {
    
    private static final int INVALID_TOKEN = 0;

    private final boolean isTerminal = true;
    private final boolean isRequired = true;
    private final boolean multiple = false;
    private final boolean any = false;
    
    private final int tokenRef = INVALID_TOKEN;
    private final String ruleRef;
    
}


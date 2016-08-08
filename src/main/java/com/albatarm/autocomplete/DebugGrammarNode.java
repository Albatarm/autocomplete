package com.albatarm.autocomplete;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class DebugGrammarNode extends GrammarNode {
    
    private static class Builder extends GrammarNode.Builder {
        
        private final TokenNamer namer;
        
        private Builder(TokenNamer namer) {
            this.namer = namer;
        }
        
        public GrammarNode build() {
            return new DebugGrammarNode(terminal, required, multiple, any, tokenRef, ruleRef, namer);
        }
        
    }

    private final TokenNamer namer;

    public DebugGrammarNode(boolean terminal, boolean required, boolean multiple, boolean any, int tokenRef, String ruleRef, TokenNamer namer) {
        super(terminal, required, multiple, any, tokenRef, ruleRef);
        this.namer = namer;
    }
    
    @Override
    public String toString() {
        String tokenName = namer.getTokenName(getTokenRef());
        ArrayList<String> attr = new ArrayList<>();
        if (isTerminal()) {
            attr.add("terminal");
        }
        if (isRequired()) {
            attr.add("required");
        }
        if (isMultiple()) {
            attr.add("multiple");
        }
        if (isAny()) {
            attr.add("any");
        }
        return String.format("Node{token:%s, rule:%s, attr:}", tokenName == null ? "ø" : tokenName, getRuleRef(), attr.stream().collect(Collectors.joining("+")));
    }

    public static Builder builder(TokenNamer namer) {
        return new Builder(namer);
    }
    
}

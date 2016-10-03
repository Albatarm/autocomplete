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
        ArrayList<String> attrs = new ArrayList<>();
        if (isRequired()) {
            attrs.add("required");
        }
        if (isMultiple()) {
            attrs.add("multiple");
        }
        if (isAny()) {
            attrs.add("any");
        }
        String type = isTerminal() ? "Token" : "Rule";
        Object value = isTerminal() ? namer.getTokenName(getTokenRef()) : getRuleRef();
        String attr = attrs.isEmpty() ? "" : attrs.stream().collect(Collectors.joining("+", ", ", ""));
        return String.format("%s{%s%s}", type, value, attr);
    }

    public static Builder builder(TokenNamer namer) {
        return new Builder(namer);
    }
    
}

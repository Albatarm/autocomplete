package com.albatarm.autocomplete;

import com.albatarm.autocomplete.GrammarNode.Builder;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class DebugAutoCompleterFactory extends AutoCompleterFactory {
    
    private static class Namer implements TokenNamer {
        
        private final HashMap<Integer, String> tokenNames = new HashMap<>();

        @Override
        public String getTokenName(int tokenId) {
            return tokenNames.get(tokenId);
        }
        
        public void add(int tokenId, String name) {
            tokenNames.put(tokenId, name);
        }
        
    }

    private Namer namer = new Namer();
    
    @Override
    protected void init() {
        super.init();
        namer = new Namer();
    }
    
    @Override
    protected void addToken(String name, int tokenId) {
        super.addToken(name, tokenId);
        namer.add(tokenId, name);
    }
    
    @Override
    protected Builder createGrammarNodeBuilder() {
        return DebugGrammarNode.builder(namer);
    }
    
    @Override
    protected RuleAlternatives createRuleAlternatives(boolean optimized, List<GrammarSequence> sequences, Set<Integer> tokens) {
        return new DebugRuleAlternatives(optimized, sequences, tokens, namer);
    }

}

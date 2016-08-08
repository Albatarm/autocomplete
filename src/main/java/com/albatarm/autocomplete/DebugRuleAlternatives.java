package com.albatarm.autocomplete;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DebugRuleAlternatives extends RuleAlternatives {

    private final TokenNamer namer;

    public DebugRuleAlternatives(boolean optimized, List<GrammarSequence> sequences, Set<Integer> tokens, TokenNamer namer) {
        super(optimized, sequences, tokens);
        this.namer = namer;
    }
    
    @Override
    public String toString() {
        return "RuleAlterniatives{" + (isOptimized() ? "optim" : "not-optim") + ", tokens=" + getTokens().stream().map(namer::getTokenName).collect(Collectors.joining(", ", "[", "]")) + ", seqs=" + getSequences() + "}";
    }

}

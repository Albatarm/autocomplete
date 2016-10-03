package com.albatarm.autocomplete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RuleAlternatives {
    
    private final List<GrammarSequence> sequences;
    private final Set<Integer> set;
    private final boolean optimized;
    
    public RuleAlternatives(boolean optimized, List<GrammarSequence> sequences, Set<Integer> tokens) {
        this.optimized = optimized;
        this.sequences = new ArrayList<>(sequences);
        this.set = new HashSet<>(tokens);
        if ((optimized && !sequences.isEmpty()) || (!optimized && sequences.isEmpty())) {
        	throw new IllegalStateException();
        }
    }
    
    public boolean isOptimized() {
        return optimized;
    }
    
    public Set<Integer> getTokens() {
        return Collections.unmodifiableSet(set);
    }
    
    public List<GrammarSequence> getSequences() {
        return Collections.unmodifiableList(sequences);
    }
    
    @Override
    public String toString() {
        return "RuleAlterniatives{optim=" + optimized + ", " + set + ", " + sequences + "}";
    }
    
}

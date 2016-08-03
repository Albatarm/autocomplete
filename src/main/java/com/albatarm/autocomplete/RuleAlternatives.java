package com.albatarm.autocomplete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RuleAlternatives {
    
    private final List<GrammarSequence> sequences = new ArrayList<>();
    private final Set<Integer> set = new HashSet<>();
    
    private boolean optimized = true;
    
    public void setOptimized(boolean optimized) {
        this.optimized = optimized;
    }
    
    public boolean isOptimized() {
        return optimized;
    }
    
    public void addToken(int token) {
        set.add(token);
    }
    
    public void addSequence(GrammarSequence sequence) {
        //TODO alternatives.sequence.push_back(sequence);
        sequences.add(sequence);
    }
    
    public Set<Integer> getSet() {
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

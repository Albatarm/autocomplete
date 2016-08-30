package com.albatarm.autocomplete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GrammarSequence {

    private final List<GrammarNode> nodes;
    
    public GrammarSequence(List<GrammarNode> nodes) {
        this.nodes = new ArrayList<>(nodes);
    }
    
    @Override
    public String toString() {
        return "Sequence{" + nodes.toString() + "}";
    }
    
    public List<GrammarNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
    
}

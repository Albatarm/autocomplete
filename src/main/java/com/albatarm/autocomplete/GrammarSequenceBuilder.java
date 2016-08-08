package com.albatarm.autocomplete;

import java.util.ArrayList;

public final class GrammarSequenceBuilder {

    private final ArrayList<GrammarNode> nodes = new ArrayList<>();
    
    public GrammarSequenceBuilder add(GrammarNode node) {
        nodes.add(node);
        return this;
    }
    
    public GrammarSequence build() {
        return new GrammarSequence(nodes);
    }
    
}

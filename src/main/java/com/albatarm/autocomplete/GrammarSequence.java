package com.albatarm.autocomplete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GrammarSequence {

    // MySql
    private int minVersion;
    // MySql
    private int maxVersion;
    
    // MySql
    private int activeSqlModes = -1;
    // MySql
    private int inactiveSqlModes = -1;
    
    private final List<GrammarNode> nodes = new ArrayList<>();
    
    public void addNode(GrammarNode node) {
        //TODO sequence.nodes.push_back(node);
        nodes.add(node);
    }
    
    @Override
    public String toString() {
        return "Sequence{" + nodes.toString() + "}";
    }
    
    public List<GrammarNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
    
}

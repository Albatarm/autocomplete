package com.albatarm.autocomplete;

import java.util.ArrayList;
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
    
}

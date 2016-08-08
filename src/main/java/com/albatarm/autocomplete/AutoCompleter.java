package com.albatarm.autocomplete;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.antlr.v4.parse.ANTLRLexer;

public class AutoCompleter {

    // TODO
    private static final int DOT_SYMBOL = ANTLRLexer.DOT;

    // Full grammar
    private final Map<String, RuleAlternatives> rules;

    // Rules with a special meaning (e.g. "table_ref").
    private final Set<String> specialRules;
    // Rules we don't provide completion with (e.g. "label").
    private final Set<String> ignoredRules;

    public AutoCompleter(Map<String, RuleAlternatives> rules, Set<String> specialRules, Set<String> ignoredRules) {
        this.rules = new HashMap<>(rules);
        this.specialRules = new HashSet<>(specialRules);
        this.ignoredRules = new HashSet<>(ignoredRules);
    }
    
    public void print() {
        rules.forEach((name, rule) -> {
            System.out.println(name + " :  ");
            System.out.println(rule);
        });
    }

    public RuleAlternatives getRuleAlternatives(String rule) {
        return rules.get(rule);
    }
    
    public Set<String> getIgnoredRules() {
        return Collections.unmodifiableSet(ignoredRules);
    }
    
    public Set<String> getSpecialRules() {
        return Collections.unmodifiableSet(specialRules);
    }

}

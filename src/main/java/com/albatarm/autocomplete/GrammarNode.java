package com.albatarm.autocomplete;

public class GrammarNode {

    private static final int INVALID_TOKEN = 0;

    private boolean isTerminal = true;
    private boolean isRequired = true;
    private boolean multiple = false;
    private boolean any = false;

    private int tokenRef = INVALID_TOKEN;
    private String ruleRef;

    public void setRequired(boolean b) {
        this.isRequired  = b;
    }

    public void setMultiple(boolean b) {
        this.multiple = b;
    }

    public void setTerminal(boolean b) {
        this.isTerminal = b;
    }

    public void setTokenRef(int tokenIdFromName) {
        this.tokenRef = tokenIdFromName;
    }

    public void setRuleRef(String text) {
        this.ruleRef = text;
    }

    public void setAny(boolean b) {
        this.any = b;
    }
    
    public boolean isAny() {
        return any;
    }
    
    public boolean isMultiple() {
        return multiple;
    }
    
    public boolean isRequired() {
        return isRequired;
    }
    
    public boolean isTerminal() {
        return isTerminal;
    }
    
    public int getTokenRef() {
        return tokenRef;
    }
    
    public String getRuleRef() {
        return ruleRef;
    }
    
    @Override
    public String toString() {
        return String.format("Node{%s, %s, term=%s, req=%s, mul=%s, any=%s}", tokenRef, ruleRef, isTerminal, isRequired, multiple, any);
    }

}

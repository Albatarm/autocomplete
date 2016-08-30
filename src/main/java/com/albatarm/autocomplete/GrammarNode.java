package com.albatarm.autocomplete;

public class GrammarNode {
    
    public static class Builder {

        private static final int INVALID_TOKEN = 0;

        protected boolean terminal = true;
        protected boolean required = true;
        protected boolean multiple = false;
        protected boolean any = false;

        protected int tokenRef = INVALID_TOKEN;
        protected String ruleRef;
        
        protected Builder() {
        }
        
        public void setRequired(boolean b) {
            this.required  = b;
        }

        public void setMultiple(boolean b) {
            this.multiple = b;
        }

        public void setTerminal(boolean b) {
            this.terminal = b;
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
        
        public GrammarNode build() {
        	if (terminal) {
        		if (ruleRef != null) {
        			throw new IllegalStateException();
        		}
        	} else {
        		if (tokenRef != 0) {
        			throw new IllegalStateException();
        		}
        	}
            return new GrammarNode(terminal, required, multiple, any, tokenRef, ruleRef);
        }
        
    }

    private final boolean terminal;
    private final boolean required;
    private final boolean multiple;
    private final boolean any;

    private final int tokenRef;
    private final String ruleRef;
    
    public GrammarNode(boolean terminal, boolean required, boolean multiple, boolean any, int tokenRef, String ruleRef) {
        this.terminal = terminal;
        this.required = required;
        this.multiple = multiple;
        this.any = any;
        this.tokenRef = tokenRef;
        this.ruleRef = ruleRef;
    }

    public boolean isAny() {
        return any;
    }
    
    public boolean isMultiple() {
        return multiple;
    }
    
    public boolean isRequired() {
        return required;
    }
    
    public boolean isTerminal() {
        return terminal;
    }
    
    public int getTokenRef() {
        return tokenRef;
    }
    
    public String getRuleRef() {
        return ruleRef;
    }
    
    @Override
    public String toString() {
        return String.format("Node{%s, %s, term=%s, req=%s, mul=%s, any=%s}", tokenRef, ruleRef, terminal, required, multiple, any);
    }
    
    public static Builder builder() {
        return new Builder();
    }

}

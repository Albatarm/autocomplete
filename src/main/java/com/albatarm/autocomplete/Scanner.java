package com.albatarm.autocomplete;

import java.util.ArrayList;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

public class Scanner<T extends Lexer> {
    
    private int tokenIndex;
    private final ArrayList<Token> tokens = new ArrayList<>();

    public void reset {
        
    }
    
    public Token getToken() {
        
    }
    
    public int getTokenType() {
        
    }
    
    public int getTokenLine() {
        
    }
    
    public int getTokenStart() {
        
    }
    
    public int getTokenEnd() {
        
    }
    
    public int getTokenChannel() {
        
    }
    
    public String getTokenText() {
        
    }
    
    public void next() {
        next(true);
    }
    
    public void next(boolean skipHidden) {
        
    }
    
    public void previous() {
        previous(true);
    }

    public void previous(boolean skipHidden) {
        
    }
    
    public boolean skipIf(int token) {
        
    }
    
    public int getPosition() {
        
    }
    
    public void seek(int position) {
        
    }
    
    public void seek(int line, int offset) {
        
    }
    
    public int lookAround(int offset, boolean ignoreHidden) {
        
    }
    
    public boolean isType(int type) {
        
    }
    
}

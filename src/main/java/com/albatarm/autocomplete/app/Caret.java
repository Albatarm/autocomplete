package com.albatarm.autocomplete.app;

import java.util.Objects;

public final class Caret {
    private int line;
    private int offset;
    
    private Caret(int line, int offset) {
        this.line = line;
        this.offset = offset;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getOffset() {
        return offset;
    }
    
    @Override
    public boolean equals(Object obj) {
    	if (obj == this) {
    		return true;
    	}
    	if (obj instanceof Caret) {
    		Caret other = (Caret) obj;
    		return line == other.line && offset == other.offset;
    	}
    	return false;
    }
    
    @Override
    public int hashCode() {
    	return Objects.hash(line, offset);
    }
    
    public static Caret at(int line, int offset) {
    	return new Caret(line, offset);
    }
    
    public static Caret atStart() {
    	return new Caret(1, 0);
    }
    
    public static Caret of(String text, int position) {
        int line = 1;
        int lastLinePos = 0;
        for (int i=0; i<text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n') {
                line++;
                lastLinePos = i;
            }
        }
        return new Caret(line, position - lastLinePos);
    }
}
package com.albatarm.autocomplete.app;

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
    
    public static Caret from(String text, int position) {
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
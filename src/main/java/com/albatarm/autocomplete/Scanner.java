package com.albatarm.autocomplete;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.IntPredicate;
import java.util.stream.Stream;
import org.antlr.v4.parse.ANTLRLexer;
import org.antlr.v4.runtime.Lexer;

public class Scanner<T extends Lexer> implements Iterable<Token> {
    
    private final class ScannerToken implements Token {

        private final org.antlr.v4.runtime.Token token;
        
        public ScannerToken(org.antlr.v4.runtime.Token token) {
            this.token = token;
        }
        
        @Override
        public int getType() {
            return token.getType();
        }

        @Override
        public String getText() {
            return token.getText();
        }
        
        @Override
        public String toString() {
        	int type = getType();
        	return type == T.EOF ? "EOF" : lexer.getVocabulary().getDisplayName(type) + "{" + getText() + "}";
        }
        
    }
    
    private int tokenIndex;
    private final ArrayList<org.antlr.v4.runtime.Token> tokens = new ArrayList<>();
    private final IntPredicate separatorPredicate;
	private final T lexer;

    public Scanner(T lexer, IntPredicate separatorPredicate) {
        this.lexer = lexer;
		this.separatorPredicate = separatorPredicate;
        // Cache the tokens. There's always at least one token: the EOF token.
        // It might seem counter productive to load all tokens upfront, but this makes many
        // things a lot simpler or even possible. The token stream used by a parser does exactly the same.
        this.tokenIndex = 0;
        while (true) {
            org.antlr.v4.runtime.Token token = lexer.nextToken();
            tokens.add(token);
            if (token.getType() == ANTLRLexer.EOF) {
                break;
            }
        }
    }
    
    public void reset() {
        tokenIndex = 0;
    }
    
    private org.antlr.v4.runtime.Token getCurrentToken() {
        return tokens.get(tokenIndex);
    }
    
    public int getTokenType() {
        return getCurrentToken().getType();
    }
    
    public int getTokenLine() {
        return getCurrentToken().getLine();
    }
    
    public int getTokenStart() {
        return getCurrentToken().getCharPositionInLine();
    }
    
    public int getTokenEnd() {
        org.antlr.v4.runtime.Token token = getCurrentToken();
        return token.getCharPositionInLine() + (token.getStopIndex() - token.getStartIndex()) + 1;
    }
    
    public int getTokenChannel() {
        return getCurrentToken().getChannel();
    }
    
    public String getTokenText() {
        return getCurrentToken().getText();
    }
    
    public Token getToken() {
    	return new ScannerToken(getCurrentToken());
    }
    
    public void next() {
        next(true);
    }
    
    public void next(boolean skipHidden) {
        while (tokenIndex < tokens.size() - 1) {
            ++tokenIndex;
            if (tokens.get(tokenIndex).getChannel() == 0 || !skipHidden) {
                break;
            }
        }
    }
    
    public int getPosition() {
        return tokenIndex;
    }
    
    public void seek(int position) {
        tokenIndex = position;
        if (position >= tokens.size()) {
            tokenIndex = tokens.size() - 1;
        }
    }
    
    public boolean isType(int type) {
        return getTokenType() == type;
    }
    
    public boolean isSeparator() {
        return separatorPredicate.test(getTokenType());
    }
    
    @Override
    public Iterator<Token> iterator() {
        return stream().iterator();
    }
    
    public Stream<Token> stream() {
        return tokens.stream().map(ScannerToken::new);
    }

}

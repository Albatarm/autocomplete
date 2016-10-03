package com.albatarm.autocomplete.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.antlr.v4.parse.ANTLRLexer;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.atn.ATN;

import com.albatarm.autocomplete.CompletionProposal;
import com.albatarm.autocomplete.Scanner;
import com.albatarm.autocomplete.antlr.AntlrAutoCompletionSuggester;

public class AbstractLang<L extends Lexer, P extends Parser> implements Lang {
	
	private static final int CARET_TOKEN_TYPE = -42;
	
	private final Function<CharStream, L> lexerFactory;
	private final AntlrAutoCompletionSuggester suggester;

	public AbstractLang(Function<CharStream, L> lexerFactory, String[] ruleNames, Vocabulary vocabulary, ATN atn) {
		this.lexerFactory = lexerFactory;
		this.suggester = new AntlrAutoCompletionSuggester(Arrays.asList(ruleNames), vocabulary, atn);
	}

	@Override
	public CompletionProposal compile2(String source, Caret caret) {
		ANTLRInputStream input = new ANTLRInputStream(source);
        L lexer = lexerFactory.apply(input);
        Set<Integer> possible = suggester.collectCandidates(toList(lexer));
        Set<String> tokens = possible.stream().map(type -> {
        	if (type < 1) {
        		return "EOF";
        	} else {
        		return lexer.getRuleNames()[type - 1];
        	}
        }).collect(Collectors.toSet());
        //Set<String> tokens = possible.stream().map(String::valueOf).collect(Collectors.toSet());
        return new CompletionProposal(tokens, "lol", false);
	}
	
	private List<org.antlr.v4.runtime.Token> toList(L lexer) {
		ArrayList<org.antlr.v4.runtime.Token> result = new ArrayList<>();
		while (true) {
            org.antlr.v4.runtime.Token next = lexer.nextToken();
            if (next.getChannel() != 0) {
            	continue;
            }
            if (next.getType() < 0) {
            	next = new CommonToken(CARET_TOKEN_TYPE);
            }
            result.add(next);
            if (next.getType() < 0) {
                break;
            }
        }
		return result;
	}
	
}

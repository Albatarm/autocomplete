package com.albatarm.autocomplete.app;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.antlr.runtime.RecognitionException;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.albatarm.autocomplete.AutoCompleter;
import com.albatarm.autocomplete.AutoCompleterFactory;
import com.albatarm.autocomplete.AutoCompletionContext;
import com.albatarm.autocomplete.CompletionProposal;
import com.albatarm.autocomplete.DebugAutoCompleterFactory;
import com.albatarm.autocomplete.Scanner;

public class AbstractLang<T extends Lexer> implements Lang {
	
	private static final Logger LOG = LogManager.getLogger(AbstractLang.class);
    
    private final AutoCompleter completer;
    private final Set<Integer> separators;
    private final String rootRule;
    private final String[] tokenNames;
    private final Function<CharStream, T> lexerFactory;
    
    protected AbstractLang(String grammarFile, String tokensFile, Set<Integer> separators, String rootRule, String[] tokenNames, Function<CharStream, T> lexerFactory) {
    	this(grammarFile, null, tokensFile, separators, rootRule, tokenNames, lexerFactory);
    }
    
    protected AbstractLang(String grammarFile, String importDir, String tokensFile, Set<Integer> separators, String rootRule, String[] tokenNames, Function<CharStream, T> lexerFactory) {
        this.separators = separators;
        this.rootRule = rootRule;
        this.tokenNames = tokenNames;
        this.lexerFactory = lexerFactory;
        try {
            DebugAutoCompleterFactory factory = new DebugAutoCompleterFactory(importDir == null ? null : Paths.get(importDir));
            configure(factory);
            completer = factory.generate(Paths.get(grammarFile), Paths.get(tokensFile));
        } catch (IOException | RecognitionException e) {
            throw new IllegalStateException(e);
        }
    }
    
    protected void configure(AutoCompleterFactory factory) {
    }

    @Override
    public AutoCompleter getAutoCompleter() {
        return completer;
    }

    @Override
    public AutoCompletionContext<?> compile(String source, Caret caret) {
        ANTLRInputStream input = new ANTLRInputStream(source);
        T lexer = lexerFactory.apply(input);
        Scanner<T> scanner = new Scanner<>(lexer, separators::contains);
        LOG.debug(() -> scanner.stream().map(Object::toString).collect(Collectors.joining(" ")));
        return new AutoCompletionContext<>(completer, Arrays.asList(tokenNames), scanner, rootRule, caret.getLine(), caret.getOffset());
    }
    
    @Override
    public CompletionProposal compile2(String source, Caret caret) {
    	AutoCompletionContext<?> ctx = compile(source, caret);
    	boolean fullyParsed = ctx.collectCandidates();
    	return new CompletionProposal(ctx.getCompletionCandidates(), ctx.getCurrentToken(), fullyParsed);
    }
    
    @Override
    public void parse(String source) {
    	ANTLRInputStream input = new ANTLRInputStream(source);
        T lexer = lexerFactory.apply(input);
        Scanner<T> scanner = new Scanner<>(lexer, separators::contains);
        LOG.debug(() -> scanner.stream().map(Object::toString).collect(Collectors.joining(" ")));
    }

}

package com.albatarm.autocomplete.app;

import com.albatarm.autocomplete.AutoCompleter;
import com.albatarm.autocomplete.AutoCompletionContext;
import com.albatarm.autocomplete.Scanner;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import org.antlr.runtime.RecognitionException;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;

public class AbstractLang<T extends Lexer> implements Lang {
    
    private final AutoCompleter completer;
    private final Set<Integer> separators;
    private final String rootRule;
    private final String[] tokenNames;
    private final Function<CharStream, T> factory;
    
    protected AbstractLang(String grammarFile, String tokensFile, Set<Integer> separators, String rootRule, String[] tokenNames, Function<CharStream, T> factory) {
        this.separators = separators;
        this.rootRule = rootRule;
        this.tokenNames = tokenNames;
        this.factory = factory;
        try {
            completer = new AutoCompleter(Paths.get(grammarFile), Paths.get(tokensFile));
        } catch (IOException | RecognitionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public AutoCompleter getAutoCompleter() {
        return completer;
    }

    @Override
    public AutoCompletionContext<?> compile(String source, Caret caret) {
        ANTLRInputStream input = new ANTLRInputStream(source);
        T lexer = factory.apply(input);
        Scanner<T> scanner = new Scanner<>(lexer, separators::contains);
        return new AutoCompletionContext<>(completer, Arrays.asList(tokenNames), scanner, rootRule, caret.getLine(), caret.getOffset());
    }

}

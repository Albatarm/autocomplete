package com.albatarm.autocomplete.app;

import com.albatarm.autocomplete.AutoCompleter;
import com.albatarm.autocomplete.AutoCompletionContext;
import com.albatarm.autocomplete.DebugAutoCompleterFactory;
import com.albatarm.autocomplete.Scanner;
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

public class AbstractLang<T extends Lexer> implements Lang {
    
    private final AutoCompleter completer;
    private final Set<Integer> separators;
    private final String rootRule;
    private final String[] tokenNames;
    private final Function<CharStream, T> lexerFactory;
    
    protected AbstractLang(String grammarFile, String tokensFile, Set<Integer> separators, String rootRule, String[] tokenNames, Function<CharStream, T> lexerFactory) {
        this.separators = separators;
        this.rootRule = rootRule;
        this.tokenNames = tokenNames;
        this.lexerFactory = lexerFactory;
        try {
            DebugAutoCompleterFactory factory = new DebugAutoCompleterFactory();
            completer = factory.generate(Paths.get(grammarFile), Paths.get(tokensFile));
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
        T lexer = lexerFactory.apply(input);
        Scanner<T> scanner = new Scanner<>(lexer, separators::contains);
        System.out.println(scanner.stream().map(Object::toString).collect(Collectors.joining(" ")));
        return new AutoCompletionContext<>(completer, Arrays.asList(tokenNames), scanner, rootRule, caret.getLine(), caret.getOffset());
    }

}

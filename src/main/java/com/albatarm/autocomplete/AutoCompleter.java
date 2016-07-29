package com.albatarm.autocomplete;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.BaseTree;
import org.antlr.v4.parse.ANTLRLexer;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.parse.GrammarASTAdaptor;
import org.antlr.v4.parse.v3TreeGrammarException;
import org.antlr.v4.tool.ast.GrammarAST;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AutoCompleter {
    
    private static final int ANTLR4_TOKEN_EOF = -1;
    
    private static final Logger LOG = LogManager.getLogger(AutoCompleter.class);

    // Full grammar 
    private final Map<String, RuleAlternatives> rules = new HashMap<>();
    // token names to ids
    private final Map<String, Integer> tokenMap = new HashMap<>();
    
    // Rules with a special meaning (e.g. "table_ref").
    private final Set<String> specialRules = new HashSet<>();
    // Rules we don't provide completion with (e.g. "label").
    private final Set<String> ignoredRules = new HashSet<>();
    // Tokens we don't want to show up (e.g. operators).
    private final Set<String> ignoredTokens = new HashSet<>();
    
    public AutoCompleter(Path grammarFilename, Path tokenFilename) throws IOException, RecognitionException {
        init();
        LOG.debug("Parsing tokens file: {}", tokenFilename);
        readTokens(tokenFilename);
        LOG.debug("Parsing grammar file: {}", grammarFilename);
        readGrammar(grammarFilename);
        LOG.debug("Ending");
    }
    
    private void init() {
        // fill specialRules, ignoredRules, ignoredTokens
    }
    
    private void readTokens(Path filename) throws IOException {
        try (Stream<String> lines = Files.lines(filename, StandardCharsets.UTF_8)) {
            lines.filter(s -> !s.isEmpty()).forEach(line -> {
                int pos = line.lastIndexOf('=');
                LOG.debug(() -> "line " + line);
                String tokenName = line.substring(0, pos);
                int tokenId = Integer.parseInt(line.substring(pos + 1));
                tokenMap.put(tokenName, tokenId);
            });
        }
        tokenMap.put("EOF", ANTLR4_TOKEN_EOF);
    }
    
    private void readGrammar(Path filename) throws IOException, RecognitionException {
        try (InputStream inputstream = Files.newInputStream(filename)) {
            ANTLRInputStream input = new ANTLRInputStream(inputstream);
            GrammarASTAdaptor adaptor = new GrammarASTAdaptor(input);
            
            ANTLRLexer lexer = new ANTLRLexer(input);
            CommonTokenStream tokenStream = new CommonTokenStream(lexer);
            ANTLRParser parser = new ANTLRParser(tokenStream);
            parser.setTreeAdaptor(adaptor);
            try {
                ParserRuleReturnScope r = parser.grammarSpec();
                GrammarAST tree = (GrammarAST)r.getTree();
                
                if (tree.getType() == ANTLRParser.PARSER || tree.getType() == ANTLRParser.COMBINED) {
                    for (int index = 0; index < tree.getChildCount(); index++) {
                        BaseTree child = (BaseTree) tree.getChild(index);
                        // not sure
                        if (child.getType() == ANTLRParser.TOKEN_REF) {
                            traverseRule(child);
                        }
                    }
                }
            } catch (v3TreeGrammarException e) {
                LOG.error("error", e);
            }
        }
    }

    private void traverseRule(BaseTree rule) {
        BaseTree child = (BaseTree) rule.getChild(0);
        String tokenText = child.getText();
        if (Character.isLowerCase(tokenText.charAt(0))) {
            System.out.println("lol");
        }
    }
    
    public static void main(String[] args) throws IOException, RecognitionException {
        AutoCompleter ctx = new AutoCompleter(Paths.get("src/main/antlr4/calculator.g4"), Paths.get("target/generated-sources/antlr4/calculator.tokens"));
    }
    
}


package com.albatarm.autocomplete;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AutocompleterContext {
    
    private static final int ANTLR4_TOKEN_EOF = -1;
    
    private static final Logger LOG = LogManager.getLogger(AutocompleterContext.class);

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
    
    public AutocompleterContext(Path grammarFilename, Path tokenFilename) {
        init();
        LOG.debug("Parsing tokens file: {}", tokenFilename);
        readTokens(tokenFilename);
        LOG.debug("Parsing grammar file: {}", grammarFilename);
        readGrammar(grammarFilename);
    }
    
    private void init() {
        // fill specialRules, ignoredRules, ignoredTokens
    }
    
    private void readTokens(Path filename) throws IOException {
        try (Stream<String> lines = Files.lines(filename, StandardCharsets.UTF_8)) {
            lines.filter(s -> !s.isEmpty()).forEach(line -> {
                int pos = line.lastIndexOf('=');
                String tokenName = line.substring(0, pos);
                int tokenId = Integer.parseInt(line.substring(pos + 1));
                tokenMap.put(tokenName, tokenId);
            });
        }
        tokenMap.put("EOF", ANTLR4_TOKEN_EOF);
    }
    
    private void readGrammar(Path filename) {
        
    }
    
}


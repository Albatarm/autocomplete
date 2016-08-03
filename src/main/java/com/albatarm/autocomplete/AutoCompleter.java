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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.ParserRuleReturnScope;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.BaseTree;
import org.antlr.runtime.tree.Tree;
import org.antlr.v4.parse.ANTLRLexer;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.parse.GrammarASTAdaptor;
import org.antlr.v4.parse.v3TreeGrammarException;
import org.antlr.v4.tool.ast.AltAST;
import org.antlr.v4.tool.ast.BlockAST;
import org.antlr.v4.tool.ast.GrammarAST;
import org.antlr.v4.tool.ast.RuleAST;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AutoCompleter {

    private static final int ANTLR4_TOKEN_EOF = -1;

    // TODO
    private static final int DOT_SYMBOL = ANTLRLexer.DOT;

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
        rules.forEach((name, rule) -> {
            System.out.println(name + " :  ");
            System.out.println(rule);
        });
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
                GrammarAST tree = (GrammarAST) r.getTree();
                LOG.debug(() -> "tree type = " + tree.getType());
                if (tree.getType() == ANTLRParser.GRAMMAR) {
                    for (int index = 0; index < tree.getChildCount(); index++) {
                        BaseTree child = (BaseTree) tree.getChild(index);
                        // not sure
                        LOG.debug("child " + child.getType());
                        if (child.getType() == ANTLRParser.RULES) {
                            traverseRules(child);
                        }
                    }
                }
            } catch (v3TreeGrammarException e) {
                LOG.error("error", e);
            }
        }
    }

    private void traverseRules(BaseTree tree) {
        LOG.debug("traverseRules " + tree.getClass());
        for (int index = 0; index < tree.getChildCount(); index++) {
            BaseTree child = (BaseTree) tree.getChild(index);
            // LOG.debug("rules index is " + child.getType());
            if (child.getType() == ANTLRParser.RULE) {
                traverseRule((RuleAST) child);
            }
        }

        /*
         * BaseTree child = (BaseTree) rule.getChild(0); String tokenText = child.getText(); if (Character.isLowerCase(tokenText.charAt(0))) {
         * System.out.println("lol"); }
         */
    }

    private void traverseRule(RuleAST rule) {
        LOG.debug("traverseRule " + rule.getRuleName());

        if (!rule.isLexerRule()) {
            Tree block = rule.getFirstChildWithType(ANTLRParser.BLOCK);

            traverseBlock((BlockAST) block, rule.getRuleName());

            /*
             * for (int index = 0; index < tree.getChildCount(); index++) { BaseTree child = (BaseTree) tree.getChild(index); LOG.debug("rules index is " +
             * child.getType()); if (child.getType() == ANTLRParser.TOKEN_REF) { //traverseRule(child); } }
             */
        }

    }

    private int getTokenIdFromName(String name) {
        return Objects.requireNonNull(tokenMap.get(name));
    }

    private void traverseBlock(BlockAST block, String name) {
        LOG.debug("traverseBlock for rule " + name);
        // A block is either a rule body or a part enclosed by parentheses.
        // A block consists of a number of alternatives which are stored as the
        // content of that block
        // under the given name.
        RuleAlternatives alternatives = new RuleAlternatives();

        // Check if we can create an optimized alternatives variant which simply
        // uses a set, so we can
        // test a match with a single operation.
        // To make this work the block must consist solely of single terminal
        // token alternatives without
        // any predicate.
        for (int index = 0; index < block.getChildCount(); index++) {
            BaseTree alt = (BaseTree) block.getChild(index);
            LOG.debug("block child is " + alt.getType());

            // 2 nodes at most: the single terminal + EOA. Gated semantic
            // predicates are child nodes of that
            // alt node too, so they automatically get checked here too.

            if (alt.getType() == ANTLRParser.ALT && alt.getChildCount() > 2) {
                alternatives.setOptimized(false);
                break;
            }

            // Check also the type of the first node. We only accept terminals
            // (no rule ref or closures).
            BaseTree child = (BaseTree) alt.getChild(0);
            if (child.getType() != ANTLRParser.TOKEN_REF) {
                alternatives.setOptimized(false);
                break;
            }

        }

        if (alternatives.isOptimized()) {
            for (BaseTree alt : block.getAllChildrenWithType(ANTLRParser.ALT)) {
                Tree child = alt.getChild(0);
                alternatives.addToken(getTokenIdFromName(child.getText()));
            }

        } else {
            // One less child in the loop as the list is always ended by a EOB
            // node.
            for (BaseTree alt : block.getAllChildrenWithType(ANTLRParser.ALT)) {
                String altName = name + "_alt" + alt.getChildIndex();
                GrammarSequence sequence = traverseAlternative((AltAST) alt, altName);
                alternatives.addSequence(sequence);
            }

        }
        rules.put(name, alternatives);
    }

    private GrammarSequence traverseAlternative(AltAST alt, String name) {
        LOG.debug("traverseAlternative " + alt + ", " + name + ", " + alt.getClass().getName());

        GrammarSequence sequence = new GrammarSequence();
        int index = 0;

        // Check for special nodes first.
        Tree ch = alt.getChild(0);
        switch (ch.getType()) {
            case ANTLRParser.SEMPRED:
            // TODO case ANTLRParser.GATED_SEMPRED_V3TOK
            {
                // See if we can extract version info or SQL mode condition from
                // that.
                ++index;
                String predicate = ch.getText();

                // A predicate has the form "{... text ... }?".
                // TODO predicate = predicate.substr(1, predicate.size() - 3);
                predicate = predicate.substring(1, predicate.length() - 3);

                parsePredicate(predicate, sequence);
                break;
            }
            case ANTLRParser.SYNPRED: // A syntactic predicate converted to a
                                      // semantic predicate.
                ++index; // Not needed for our work, so we can ignore it.
                break;
            case ANTLRParser.EPSILON: // An empty alternative.
                return sequence;
            default:
                LOG.debug("child -> " + ch.getType());
                break;
        }

        // One less child node as the alt is always ended by an EOA node.
        for (; index < alt.getChildCount(); ++index) {
            Tree child = alt.getChild(index);
            GrammarNode node = new GrammarNode();

            int type = child.getType();

            // Ignore ROOT/BANG nodes (they are just tree construction markup).
            /*
             * if (type == ANTLRParser.ROOT || type == ANTLRParser.BANG) { child = child.getChild(0); type = child.getType(); }
             */

            switch (type) {
                case ANTLRParser.OPTIONAL:
                case ANTLRParser.CLOSURE:
                case ANTLRParser.POSITIVE_CLOSURE: {
                    node.setRequired(type != ANTLRParser.OPTIONAL && type != ANTLRParser.CLOSURE);
                    node.setMultiple(type == ANTLRParser.CLOSURE || type == ANTLRParser.POSITIVE_CLOSURE);

                    child = child.getChild(0);

                    // See if this block only contains a single alt with a
                    // single child node.
                    // If so optimize and make this single child node directly
                    // the current node.
                    boolean optimized = false;
                    if (child.getChildCount() == 2) { // 2 because there's always that EOB child node.
                        Tree childAlt = child.getChild(0);
                        if (childAlt.getChildCount() == 2) { // 2 because there's always that EOA child node.
                            optimized = true;
                            child = childAlt.getChild(0);
                            int childType = child.getType();
                            switch (childType) {
                                case ANTLRParser.LEXER_CHAR_SET: // TODO CHAR_LITERAL
                                case ANTLRParser.STRING_LITERAL:
                                case ANTLRParser.TOKEN_REF: {
                                    LOG.debug("char string literal -> " + type);
                                    node.setTerminal(true);
                                    String childName = child.getText();
                                    if (childType == ANTLRParser.LEXER_CHAR_SET || childType == ANTLRParser.STRING_LITERAL) { // TODO CHAR_LITERAL
                                        childName = unquote(childName);
                                    }
                                    node.setTokenRef(getTokenIdFromName(childName));
                                    break;
                                }
                                case ANTLRParser.RULE_REF: {
                                    node.setTerminal(false);
                                    node.setRuleRef(child.getText());
                                    break;
                                }
                                default: {
                                    throw new IllegalStateException("Unhandled type : " + type + " in alternative : " + name);
                                }
                            }
                        }
                    }

                    if (!optimized) {
                        String blockName = name + "_block" + index;
                        traverseBlock((BlockAST) child, blockName);

                        node.setTerminal(false);
                        node.setRuleRef(blockName);
                    }
                    break;
                }
                case ANTLRParser.LEXER_CHAR_SET: // TODO CHAR_LITERAL
                case ANTLRParser.STRING_LITERAL:
                case ANTLRParser.TOKEN_REF: {
                    LOG.debug("char string literal -> " + type);
                    node.setTerminal(true);
                    String name2 = child.getText();
                    if (type == ANTLRParser.LEXER_CHAR_SET || type == ANTLRParser.STRING_LITERAL) // TODO CHAR_LITERAL
                    {
                        name2 = unquote(name2);
                    }
                    node.setTokenRef(getTokenIdFromName(name2));
                    break;
                }

                case ANTLRParser.RULE_REF: {
                    node.setTerminal(false);
                    node.setRuleRef(child.getText());
                    break;
                }

                case ANTLRParser.BLOCK: {
                    String blockName = name + "_block" + index;
                    traverseBlock((BlockAST) child, blockName);

                    node.setTerminal(false);
                    node.setRuleRef(blockName);
                    break;
                }

                case ANTLRParser.DOT:// Match any token, except EOF.
                {
                    node.setTerminal(true);
                    node.setAny(true);
                    node.setTokenRef(DOT_SYMBOL); // Just a dummy (one of the
                                                  // ignore tokens), so it
                                                  // doesn't appear in the list.
                    break;
                }

                case ANTLRParser.ASSIGN: // TODO LABEL_ASSIGN_V3TOK
                {
                    // A variable assignment, instead of a token or rule
                    // reference.
                    // The reference is the second part of the assignment.
                    Tree token = child.getChild(1);
                    node.setTerminal(true);

                    switch (token.getType()) {
                        case ANTLRParser.DOT: {
                            node.setAny(true);
                            node.setTokenRef(DOT_SYMBOL);
                            break;
                        }

                        case ANTLRParser.LEXER_CHAR_SET: // TODO CHAR_LITERAL
                        case ANTLRParser.STRING_LITERAL:
                        case ANTLRParser.TOKEN_REF: {
                            LOG.debug("char string literal -> " + type);
                            String tokenText = token.getText();
                            if (type == ANTLRParser.LEXER_CHAR_SET || type == ANTLRParser.STRING_LITERAL) { // TODO CHAR_LITERAL
                                tokenText = unquote(tokenText);
                            }
                            node.setTokenRef(getTokenIdFromName(tokenText));
                        }

                        default: {
                            throw new IllegalStateException("Unhandled type: " + type + " in label assignment : " + name);
                        }
                    }
                    break;
                }

                case ANTLRParser.SEMPRED: {
                    // A validating semantic predicate - ignore.
                    // Might be necessary to handle one day, when we use such a
                    // predicate to
                    // control parts with dynamic conditions.
                    continue;
                }

                default: {
                    throw new IllegalStateException("Unhandled type: " + type + " in alternative : " + name);
                }
            }

            sequence.addNode(node);

        }

        return sequence;
    }

    public RuleAlternatives getRuleAlternatives(String rule) {
        return rules.get(rule);
    }
    
    private static String unquote(String s) {
        LOG.debug(() -> "unquote " + s);
        return s;
    }

    private void parsePredicate(String predicate, GrammarSequence sequence) {
        LOG.debug("parsePredicate [{}]", predicate);
    }

    public static void main(String[] args) throws IOException, RecognitionException {
        AutoCompleter ctx = new AutoCompleter(Paths.get("src/main/antlr4/calculator.g4"), Paths.get("target/generated-sources/antlr4/calculator.tokens"));
    }

}

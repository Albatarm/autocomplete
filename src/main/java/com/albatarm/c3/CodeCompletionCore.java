package com.albatarm.c3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.PredicateTransition;
import org.antlr.v4.runtime.atn.RuleTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.albatarm.c3.collection.ArrayIntList;
import com.albatarm.c3.collection.IntList;
import com.albatarm.c3.collection.IntLists;

public class CodeCompletionCore {

    private static final Logger LOG = LogManager.getLogger(CodeCompletionCore.class);

    private static final String[] ATN_STATE_TYPE_MAP = {
            "invalid",
            "basic",
            "rule start",
            "block start",
            "plus block start",
            "star block start",
            "token start",
            "rule stop",
            "block end",
            "star loop back",
            "star loop entry",
            "plus loop back",
            "loop end"
    };

    // Not dependent on SHOW_DEBUG_OUTPUT. Prints the collected rules + tokens to terminal.
    private static final boolean SHOW_RESULT = false;
    // Enables printing ATN state info to terminal.
    private static final boolean SHOW_DEBUG_OUTPUT = true;
    // Only relevant when SHOW_DEBUG_OUTPUT is true. Enables transition printing for a state.
    private static final boolean DEBUG_OUTPUT_WITH_TRANSITIONS = false;
    // Also depends on SHOW_DEBUG_OUTPUT. Enables call stack printing for each rule recursion.
    private static final boolean SHOW_RULE_STACK = false;

    // Tailoring of the result.
    // Tokens which should not appear in the candidates set.
    private Set<Integer> ignoredTokens = new HashSet<>();
    // Rules which replace any candidate token they contain.
    private Set<Integer> preferredRules = new HashSet<>();

    // This allows to return descriptive rules (e.g. className, instead of ID/identifier).

    private final Parser parser;
    private final ATN atn;
    private final Vocabulary vocabulary;
    private final String[] ruleNames;

    private final IntList tokens = new ArrayIntList();

    private int tokenStartIndex = 0;
    private int statesProcessed = 0;

    // A mapping of rule index + token stream position to end token positions.
    // A rule which has been visited before with the same input position will always produce the same output positions.
    private Map<Integer, Map<Integer, Set<Integer>>> shortcutMap = new HashMap<>();
    private Candidates.Builder candidates; // The collected candidates (rules and tokens).

    private static Map<String, Map<Integer, FollowSetsHolder>> followSetsByATN = new HashMap<>();

    public CodeCompletionCore(Parser parser) {
        this.parser = parser;
        this.atn = parser.getATN();
        this.vocabulary = parser.getVocabulary();
        this.ruleNames = parser.getRuleNames();
    }

    public void setIgnoredTokens(int... ignoredTokens) {
        this.ignoredTokens.clear();
        for (int token : ignoredTokens) {
            this.ignoredTokens.add(token);
        }
    }

    public void setPreferredRules(int... preferredRules) {
        this.preferredRules.clear();
        for (int rule : preferredRules) {
            this.preferredRules.add(rule);
        }
    }

    /**
     * This is the main entry point. The caret token index specifies the token stream index for the token which currently
     * covers the caret (or any other position you want to get code completion candidates for).
     * Optionally you can pass in a parser rule context which limits the ATN walk to only that or called rules. This can significantly
     * speed up the retrieval process but might miss some candidates (if they are outside of the given context).
     */
    public Candidates collectCandidates(int caretTokenIndex, ParserRuleContext context) {
        shortcutMap.clear();
        candidates = Candidates.builder();
        statesProcessed = 0;

        tokenStartIndex = context == null ? 0 : context.getStart().getTokenIndex();
        TokenStream tokenStream = parser.getInputStream();

        int currentIndex = tokenStream.index();
        tokenStream.seek(tokenStartIndex);
        tokens.clear();
        int offset = 1;
        while (true) {
            Token token = tokenStream.LT(offset++);
            tokens.add(token.getType());
            if (token.getTokenIndex() >= caretTokenIndex || token.getType() == Token.EOF) {
                break;
            }
        }
        tokenStream.seek(currentIndex);

        IntList callStack = new ArrayIntList();
        int startRule = context == null ? 0 : context.getRuleIndex();
        processRule(atn.ruleToStartState[startRule], 0, callStack, "");

        if (SHOW_RESULT) {
            LOG.debug("States processed: {}", statesProcessed);
        }

        return candidates.build();
    }

    /**
     * Walks the ATN for a single rule only. It returns the token stream position for each path that could be matched in this rule.
     * The result can be empty in case we hit only non-epsilon transitions that didn't match the current input or if we
     * hit the caret position.
     */
    private Set<Integer> processRule(ATNState startState, int tokenIndex, IntList callStack, String indentation) {
        // Start with rule specific handling before going into the ATN walk.

        // Check first if we've taken this path with the same input before.
        Map<Integer, Set<Integer>> positionMap = shortcutMap.get(startState.ruleIndex);
        if (positionMap == null) {
            positionMap = new HashMap<>();
            shortcutMap.put(startState.ruleIndex, positionMap);
        } else {
            if (positionMap.containsKey(tokenIndex)) {
                if (SHOW_DEBUG_OUTPUT) {
                    LOG.debug("======> shortcut");
                }
                return positionMap.get(tokenIndex);
            }
        }

        Set<Integer> result = new HashSet<>();

        // For rule start states we determine and cache the follow set, which gives us 3 advantages:
        // 1) We can quickly check if a symbol would be matched when we follow that rule. We can so check in advance
        //    and can save us all the intermediate steps if there is no match.
        // 2) We'll have all symbols that are collectable already together when we are at the caret when entering a rule.
        // 3) We get this lookup for free with any 2nd or further visit of the same rule, which often happens
        //    in non trivial grammars, especially with (recursive) expressions and of course when invoking code completion
        //    multiple times.
        Map<Integer, FollowSetsHolder> setsPerState = followSetsByATN.get(parser.getClass().getName());
        if (setsPerState == null) {
            setsPerState = new HashMap<>();
            followSetsByATN.put(parser.getClass().getName(), setsPerState);
        }

        FollowSetsHolder followSets = setsPerState.computeIfAbsent(startState.stateNumber, key -> new FollowSetsHolder(determineFollowSets(startState, atn.ruleToStopState[startState.ruleIndex])));

        callStack.push(startState.ruleIndex);
        int currentSymbol = tokens.get(tokenIndex);

        if (tokenIndex >= tokens.size() - 1) { // At caret?
            if (preferredRules.contains(startState.ruleIndex)) {
                // No need to go deeper when collecting entries and we reach a rule that we want to collect anyway.
                translateToRuleIndex(callStack);
            } else {
                // Convert all follow sets to either single symbols or their associated preferred rule and add
                // the result to our candidates list.
                for (FollowSetWithPath set : followSets.getSets()) {
                    IntList fullPath = new ArrayIntList(callStack);
                    fullPath.addAll(set.getPath());
                    if (!translateToRuleIndex(fullPath)) {
                        for (Integer symbol : set.getIntervals().toList()) {
                            if (!ignoredTokens.contains(symbol)) {
                                if (SHOW_DEBUG_OUTPUT) {
                                    LOG.debug("=====> collected: {}", vocabulary.getDisplayName(symbol));
                                }
                                if (!candidates.containsToken(symbol)) {
                                    candidates.putToken(symbol, set.getFollowing());
                                } else {
                                    // More than one following list for the same symbol.
                                    if (!Objects.equals(candidates.getToken(symbol), set.getFollowing())) {
                                        candidates.putToken(symbol, IntLists.empty());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            callStack.pop();
            return result;
        } else {
            // Process the rule if we either could pass it without consuming anything (epsilon transition)
            // or if the current input symbol will be matched somewhere after this entry point.
            // Otherwise stop here.
            if (!followSets.getCombined().contains(Token.EPSILON) && !followSets.getCombined().contains(currentSymbol)) {
                callStack.pop();
                return result;
            }
        }

        // The current state execution pipeline contains all yet-to-be-processed ATN states in this rule.
        // For each such state we store the token index + a list of rules that lead to it.
        Deque<PipelineEntry> statePipeline = new ArrayDeque<>();

        // Bootstrap the pipeline.
        statePipeline.addLast(new PipelineEntry(startState, tokenIndex));

        while (!statePipeline.isEmpty()) {
            PipelineEntry currentEntry = statePipeline.removeLast();
            ++statesProcessed;

            currentSymbol = tokens.get(currentEntry.getTokenIndex());

            boolean atCaret = currentEntry.getTokenIndex() >= tokens.size() - 1;
            if (SHOW_DEBUG_OUTPUT) {
                printDescription(indentation, currentEntry.getState(), generateBaseDescription(currentEntry.getState()), currentEntry.getTokenIndex());
                if (SHOW_RULE_STACK) {
                    printRuleState(callStack);
                }
            }

            switch (currentEntry.getState().getStateType()) {
                case ATNState.RULE_START: // Happens only for the first state in this rule, not subrules.
                    indentation += "  ";
                    break;
                case ATNState.RULE_STOP:
                    // Record the token index we are at, to report it to the caller.
                    result.add(currentEntry.getTokenIndex());
                    continue;
            }

            Transition[] transitions = currentEntry.getState().getTransitions();
            for (Transition transition : transitions) {
                switch (transition.getSerializationType()) {
                    case Transition.RULE: {
                        Set<Integer> endStatus = processRule(transition.target, currentEntry.getTokenIndex(), callStack, indentation);
                        for (Integer position : endStatus) {
                            statePipeline.addLast(new PipelineEntry(((RuleTransition) transition).followState, position));
                        }
                        break;
                    }

                    case Transition.PREDICATE: {
                        if (checkPredicate((PredicateTransition) transition)) {
                            statePipeline.addLast(new PipelineEntry(transition.target, currentEntry.getTokenIndex()));
                        }
                        break;
                    }

                    case Transition.WILDCARD: {
                        if (atCaret) {
                            if (!translateToRuleIndex(callStack)) {
                                for (Integer token : IntervalSet.of(Token.MIN_USER_TOKEN_TYPE, atn.maxTokenType).toList()) {
                                    if (ignoredTokens.contains(token)) {
                                        candidates.putToken(token, IntLists.empty());
                                    }
                                }
                            }
                        } else {
                            statePipeline.addLast(new PipelineEntry(transition.target, currentEntry.getTokenIndex() + 1));
                        }
                        break;
                    }

                    default: {
                        if (transition.isEpsilon()) {
                            // Jump over simple states with a single outgoing epsilon transition.
                            statePipeline.addLast(new PipelineEntry(transition.target, currentEntry.getTokenIndex()));
                            continue;
                        }

                        IntervalSet set = transition.label();
                        if (set != null && set.size() > 0) {
                            if (transition.getSerializationType() == Transition.NOT_SET) {
                                set = set.complement(IntervalSet.of(Token.MIN_USER_TOKEN_TYPE, atn.maxTokenType));
                            }
                            if (atCaret) {
                                if (!translateToRuleIndex(callStack)) {
                                    List<Integer> list = set.toList();
                                    boolean addFollowing = list.size() == 1;
                                    for (Integer symbol : list) {
                                        if (!ignoredTokens.contains(symbol)) {
                                            if (SHOW_DEBUG_OUTPUT) {
                                                LOG.debug("=====> collected: {}", vocabulary.getDisplayName(symbol));
                                            }

                                            if (addFollowing) {
                                                candidates.putToken(symbol, getFollowingTokens(transition));
                                            } else {
                                                candidates.putToken(symbol, IntLists.empty());
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (set.contains(currentSymbol)) {
                                    if (SHOW_DEBUG_OUTPUT) {
                                        LOG.debug("=====> consumed: {}", vocabulary.getDisplayName(currentSymbol));
                                    }
                                    statePipeline.addLast(new PipelineEntry(transition.target, currentEntry.getTokenIndex() + 1));
                                }
                            }
                        }
                    }
                }
            }
        }

        callStack.pop();

        // Cache the result, for later lookup to avoid duplicate walks.
        positionMap.put(tokenIndex, result);

        return result;
    }

    /**
     * Entry point for the recursive follow set collection function.
     */
    private List<FollowSetWithPath> determineFollowSets(ATNState start, ATNState stop) {
        ArrayList<FollowSetWithPath> result = new ArrayList<>();
        Set<ATNState> seen = new HashSet<>();
        IntList ruleStack = new ArrayIntList();
        collectFollowSets(start, stop, result, seen, ruleStack);

        return result;
    }

    /**
     * Check if the predicate associated with the given transition evaluates to true.
     */
    private boolean checkPredicate(PredicateTransition transition) {
        return transition.getPredicate().eval(parser, ParserRuleContext.EMPTY);
    }

    /**
     * This method follows the given transition and collects all symbols within the same rule that directly follow it
     * without intermediate transitions to other rules and only if there is a single symbol for a transition.
     */
    private IntList getFollowingTokens(Transition transition) {
        LOG.debug("getFollowingTokens {}", transition);
        IntList result = new ArrayIntList();

        //ArrayList<ATNState> seen = new ArrayList<>();
        Deque<ATNState> pipeline = new ArrayDeque<>();
        pipeline.addLast(transition.target);

        while (!pipeline.isEmpty()) {
            LOG.debug("   pipeline={}", pipeline);
            ATNState state = pipeline.removeLast();
            LOG.debug("   remove {}", state);

            for (Transition trans : Objects.requireNonNull(state).getTransitions()) {
                if (trans.getSerializationType() == Transition.ATOM) {
                    if (!trans.isEpsilon()) {
                        List<Integer> list = Objects.requireNonNull(trans.label()).toList();
                        LOG.debug("    list = {}", list);
                        if (list.size() == 1 && !ignoredTokens.contains(list.get(0))) {
                            result.add(list.get(0));
                            pipeline.addLast(trans.target);
                            LOG.debug("   add {}", trans.target);
                        }
                    } else {
                        pipeline.addLast(trans.target);
                        LOG.debug("   add2 {}", trans.target);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Walks the rule chain upwards to see if that matches any of the preferred rules.
     * If found, that rule is added to the collection candidates and true is returned.
     */
    private boolean translateToRuleIndex(IntList ruleStack) {
        if (preferredRules.isEmpty()) {
            return false;
        }

        // Loop over the rule stack from highest to lowest rule level. This way we properly handle the higher rule
        // if it contains a lower one that is also a preferred rule.
        for (int i = 0; i < ruleStack.size(); i++) {
            if (preferredRules.contains(ruleStack.get(i))) {
                // Add the rule to our candidates list along with the current rule path,
                // but only if there isn't already an entry like that.
                IntList path = new ArrayIntList(ruleStack.subList(0, i));
                boolean addNew = true;
                for (Map.Entry<Integer, IntList> rule : candidates.getRules().entrySet()) {
                    if ((rule.getKey()) != ruleStack.get(i) || rule.getValue().size() != path.size()) {
                        continue;
                    }
                    // Found an entry for this rule. Same path? If so don't add a new (duplicate) entry.
                    if (path.every((v, j) -> v == rule.getValue().get(j))) {
                        addNew = false;
                        break;
                    }
                }

                if (addNew) {
                    candidates.putRule(ruleStack.get(i), path);
                    if (SHOW_DEBUG_OUTPUT) {
                        LOG.debug("=====> collected: {}", ruleNames[i]);
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Collects possible tokens which could be matched following the given ATN state. This is essentially the same
     * algorithm as used in the LL1Analyzer class, but here we consider predicates also and use no parser rule context.
     */
    private void collectFollowSets(ATNState s, ATNState stopState, List<FollowSetWithPath> followSets, Set<ATNState> seen, IntList ruleStack) {
        LOG.debug("collectFollowSets s={} stop={} followSets={} seen={} ruleStack={}", s, stopState, followSets, seen, ruleStack);
        if (seen.contains(s)) {
            return;
        }

        seen.add(s);

        if (s == stopState || s.getStateType() == ATNState.RULE_STOP) {
            FollowSetWithPath set = new FollowSetWithPath(IntervalSet.of(Token.EPSILON), new ArrayIntList(ruleStack));
            followSets.add(set);
            return;
        }

        for (Transition transition : s.getTransitions()) {
            final int serializationType = transition.getSerializationType();
            if (serializationType == Transition.RULE) {
                RuleTransition ruleTransition = (RuleTransition) transition;
                if (ruleStack.indexOf(ruleTransition.target.ruleIndex) != -1) {
                    continue;
                }

                ruleStack.push(ruleTransition.target.ruleIndex);
                collectFollowSets(transition.target, stopState, followSets, seen, ruleStack);
                ruleStack.pop();
            } else if (serializationType == Transition.PREDICATE) {
                if (checkPredicate((PredicateTransition) transition)) {
                    collectFollowSets(transition.target, stopState, followSets, seen, ruleStack);
                }
            } else if (transition.isEpsilon()) {
                collectFollowSets(transition.target, stopState, followSets, seen, ruleStack);
            } else if (serializationType == Transition.WILDCARD) {
                FollowSetWithPath set = new FollowSetWithPath(IntervalSet.of(Token.MIN_USER_TOKEN_TYPE, atn.maxTokenType), new ArrayIntList(ruleStack));
                followSets.add(set);
            } else {
                IntervalSet label = transition.label();
                if (label != null && label.size() > 0) {
                    if (serializationType == Transition.NOT_SET) {
                        label = label.complement(IntervalSet.of(Token.MIN_USER_TOKEN_TYPE, atn.maxTokenType));
                    }
                    FollowSetWithPath set = new FollowSetWithPath(label, new ArrayIntList(ruleStack), getFollowingTokens(transition));
                    followSets.add(set);
                }
            }
        }
    }

    private String generateBaseDescription(ATNState state) {
        String stateValue = state.stateNumber == ATNState.INVALID_STATE_NUMBER ? "Invalid" : String.valueOf(state.stateNumber);
        return "[" + stateValue + " " + ATN_STATE_TYPE_MAP[state.getStateType()] + "] in " + ruleNames[state.ruleIndex];
    }

    private void printDescription(String currentIndent, ATNState state, String baseDescription, int tokenIndex) {
        StringBuilder output = new StringBuilder(currentIndent);
        StringBuilder transitionDescription = new StringBuilder();
        if (DEBUG_OUTPUT_WITH_TRANSITIONS) {
            for (Transition transition : state.getTransitions()) {
                StringBuilder labels = new StringBuilder();
                IntList symbols = transition.label() != null ? IntLists.wrap(transition.label().toIntegerList()) : IntLists.empty();
                if (symbols.size() > 2) {
                    // Only print start and end symbols to avoid large lists in debug output.
                    labels.setLength(0);
                    labels.append(vocabulary.getDisplayName(symbols.get(0))).append(" .. ").append(vocabulary.getDisplayName(symbols.get(symbols.size() - 1)));
                } else {
                    for (int i = 0; i < symbols.size(); i++) {
                        if (labels.length() > 0) {
                            labels.append(", ");
                        }
                        int symbol = symbols.get(i);
                        labels.append(vocabulary.getDisplayName(symbol));
                    }
                }
                if (labels.length() == 0) {
                    labels.setLength(0);
                    labels.append("ε");
                }
                transitionDescription.append("\n").append(currentIndent).append("\t(").append(labels).append(") [")
                        .append(transition.target.stateNumber).append(' ')
                        .append(ATN_STATE_TYPE_MAP[transition.target.getStateType()]).append("] in ")
                        .append(ruleNames[transition.target.ruleIndex]);
            }
        }

        if (tokenIndex >= tokens.size() - 1) {
            output.append("<<").append(tokenStartIndex + tokenIndex).append(">> ");
        } else {
            output.append("<").append(tokenStartIndex + tokenIndex).append("> ");
        }
        LOG.debug("{}Current state: {}{}", output, baseDescription, transitionDescription);
    }

    private void printRuleState(IntList stack) {
        if (stack.isEmpty()) {
            LOG.debug("<empty stack>");
            return;
        }

        stack.forEach(rule -> LOG.debug("{}", ruleNames[rule]));
    }

}
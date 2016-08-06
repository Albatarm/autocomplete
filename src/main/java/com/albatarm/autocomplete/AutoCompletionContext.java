package com.albatarm.autocomplete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.runtime.Lexer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AutoCompletionContext<T extends Lexer> {

    enum RunState {
        Matching, CollectionPending
    }
    
    private static final Logger LOG = LogManager.getLogger(AutoCompletionContext.class);

    private final AutoCompleter rulesHolder;

    private String typedPart;
    private final List<String> tokenNames;
    private final Deque<String> walkStack = new LinkedList<>(); // The rules as they are being matched or collected from.
    // It's a deque instead of a stack as we need to iterate over it.

    private final Scanner<T> scanner;
    private Set<String> completionCandidates = new HashSet<>();

    private final int caretLine;
    private final int caretOffset;

    private RunState runState;

    private final String rootRule;

    // A hierarchical view of all table references in the code, updated constantly during the match process.
    // Organized as stack to be able to easily remove sets of references when changing nesting level.
    //private Deque<List<TableReference>> referencesStack;

    // A flat list of possible references. Kinda snapshot of the references stack at the point when collection
    // begins (the stack is cleaned up while bubbling up, after the collection process).
    // Additionally, it gets also all references after the caret.
    //private List<TableReference> references;
    
    public AutoCompletionContext(AutoCompleter rulesHolder, List<String> tokenNames, Scanner<T> scanner, String rootRule, int caretLine, int caretOffset) {
        this.rulesHolder = rulesHolder;
        this.rootRule = rootRule;
        this.caretLine = caretLine;
        this.caretOffset = caretOffset;
        this.tokenNames = new ArrayList<>(tokenNames);
        this.scanner = scanner; // Has all the data necessary for scanning already.
    }

    /**
     * Uses the given scanner (with set input) to collect a set of possible completion candidates at the given line + offset.
     *
     * @returns true if the input could fully be matched (happens usually only if the given caret is after the text and can be used to test if the algorithm
     *          parses queries fully).
     *
     *          Actual candidates are stored in the completion_candidates member set.
     *
     */
    public boolean collectCandidates() {
        runState = RunState.Matching;

        if (scanner.getTokenChannel() != 0) {
          scanner.next(true);
        }

        //referencesStack.push_back(std::vector<TableReference>()); // For the root level of table references.
        boolean matched = matchRule(rootRule);

        // If a column reference is required then we have to continue scanning the query for table references.
        if (completionCandidates.contains("column_ref"))
        {
          //collectRemainingTableReferences();
          takeReferencesSnapshot(); // Move references from stack to the ref map.
        }

        return matched;
    }
    
    public Set<String> getCompletionCandidates() {
        return Collections.unmodifiableSet(completionCandidates);
    }

    private boolean matchRule(String rule) {
        if (runState != RunState.Matching) { // Sanity check - should never happen at this point.
            return false;
        }

        if (isTokenEndAfterCaret()) {
            //LOG.debug("matchRule::isTokenEndAfterCaret -> collecting from rule");
            collectFromRule(rule);
            return false;
        }

        walkStack.addFirst(rule);

        int highestTokenIndex = 0;
        RunState resultState = runState;
        boolean matchedAtLeastOnce = false;

        // The longest match wins.
        RuleAlternatives alts = rulesHolder.getRuleAlternatives(rule);
        if (alts.isOptimized()) {
            // In the optimized case we have neither predicates nor sequences.
            // We match a single terminal only, out of a set of alternative terminals.
            if (alts.getSet().contains(scanner.getTokenType())) {
                matchedAtLeastOnce = true;
                scanner.next(true);
                if (isTokenEndAfterCaret()) {
                    resultState = RunState.CollectionPending;
                }
            }
        } else {
            boolean canSeek = false;
            for (GrammarSequence alt : alts.getSequences()) {
                // First run a predicate check if this alt can be considered at all.
                /*
                 * if ((alt.min_version > server_version) || (server_version > alt.max_version)) continue;
                 * 
                 * if ((alt.active_sql_modes > -1) && (alt.active_sql_modes & sql_mode) != alt.active_sql_modes) continue;
                 * 
                 * if ((alt.inactive_sql_modes > -1) && (alt.inactive_sql_modes & sql_mode) != 0) continue;
                 */

                // When attempting to match one alt out of a list pick the one with the longest match.
                // Reset the run state each time to have the base matching done first (in case a previous alt did collect).
                int marker = scanner.getPosition();
                runState = RunState.Matching;
                boolean matched = matchAlternative(alt);
                if (matched) {
                    matchedAtLeastOnce = true;
                }
                if (matched || runState != RunState.Matching) {
                    canSeek = true;
                    if (scanner.getPosition() > highestTokenIndex) {
                        highestTokenIndex = scanner.getPosition();
                        resultState = runState;
                    }
                }
                scanner.seek(marker);

            }
            if (canSeek) {
                scanner.seek(highestTokenIndex); // Move to the end of the longest match.
            }

        }

        runState = resultState;
        walkStack.pop();

        return matchedAtLeastOnce;
    }

    private boolean isTokenEndAfterCaret() {
        if (scanner.isType(ANTLRParser.EOF)) {
            return true;
        }
        assert (scanner.getTokenLine() > 0);
        if (scanner.getTokenLine() > caretLine) {
            return true;
        }
        if (scanner.getTokenLine() < caretLine) {
            return false;
        }

        // This determination is a bit tricky as it depends on the type of the token.
        // For letters (like when typing a keyword) all positions directly attached to a letter must be
        // considered within the token (as we could extend it).
        // For example each vertical bar is a position within the token: |F|R|O|M|
        // Not so with tokens that can separate other tokens without the need of a whitespace (comma etc.).

        boolean result;
        if (scanner.isSeparator()) {
            result = scanner.getTokenEnd() > caretOffset;
        } else {
            result = scanner.getTokenEnd() >= caretOffset;
        }
        return result;
    }

    private boolean matchAlternative(GrammarSequence sequence) {
        // An empty sequence per se matches anything without consuming input.
        if (sequence.getNodes().isEmpty()) {
            return true;
        }

        int i = 0;
        while (true) {
            // Set to true if the current node allows multiple occurrences and was matched at least once.
            boolean matchedLoop = false;
            // Skip any optional nodes if they don't match the current input.
            boolean matched;
            GrammarNode node;
            do {
                node = sequence.getNodes().get(i);
                matched = match(node, scanner.getTokenType());

                // If that match call caused the collection to start then don't continue with matching here.
                if (runState != RunState.Matching) {
                    if (runState == RunState.CollectionPending) {
                        // We start collecting at the current node if it allows multiple matches (to include candidates from the
                        // current rule). However this can prematurely stop the collection, since it might contain mandatory nodes.
                        // But since we matched it already at least once we also have to include tokens directly following it.
                        // Hence two calls for collect_from_alternative. The second call might include again already added candidates
                        // but duplicates are sorted out automatically.
                        if (node.isMultiple()) {
                            collectFromAlternative(sequence, i);
                        }
                        collectFromAlternative(sequence, i + 1);
                    }
                    return matched && hasMatchedAllMandatoryTokens(sequence, i); // Return true only if we fully matched the sequence.
                }

                if (matched && node.isMultiple()) {
                    matchedLoop = true;
                }

                if (matched || node.isRequired()) {
                    break;
                }

                // Did not match an optional part. That's ok, skip this then.
                ++i;
                if (i == sequence.getNodes().size()) { // Done with the sequence?
                    return true;
                }
            } while (true);

            // Important note:
            // We still have an unsolved problem here, which has to do with ignored rules that are part of special rules
            // (e.g. a qualified identifier in an object reference).
            // Normally we walk up the match stack to see if we can include the special rule in such a case. However, if that
            // ignored rule ends with an optional part we cannot say currently if the current caret position is to be considered
            // still as part of that ignored rule or must be seen as part of the following one:
            // "qualifier. |" vs "identifier |" (with | being the caret).
            // In the first case we have to include the special rule, while in the second case we must not.
            //
            // However this is a very special case and we solve this currently by testing for the DOT symbol, but this
            // solution is not universal.
            if (matched) {
                // Load next token if the grammar node is a terminal node.
                // Otherwise the match() call will have advanced the input position already.
                if (node.isTerminal()) {
                    int lastToken = scanner.getTokenType();
                    scanner.next(true);
                    if (isTokenEndAfterCaret()) {
                        takeReferencesSnapshot();

                        // XXX: hack, need a better way to find out when we have to include the special rule from the stack.
                        // Using a fixed token look-back might not be valid for all languages.
                        /*
                         * if (lastToken == DOT_SYMBOL) { for (std::deque<std::string>::const_iterator iterator = walk_stack.begin(); iterator !=
                         * walk_stack.end(); ++iterator) { if (rules_holder.special_rules.find(*iterator) != rules_holder.special_rules.end()) {
                         * completion_candidates.insert(*iterator); run_state = RunStateMatching; return hasMatchedAllMandatoryTokens(sequence, i); } } }
                         */

                        collectFromAlternative(sequence, node.isMultiple() ? i : i + 1);

                        return hasMatchedAllMandatoryTokens(sequence, i);
                    }
                } else {
                    // Similar here for non-terminals.
                    if (isTokenEndAfterCaret()) {
                        takeReferencesSnapshot();
                        collectFromAlternative(sequence, node.isMultiple() ? i : i + 1);

                        return hasMatchedAllMandatoryTokens(sequence, i);
                    }
                }

                // If the current grammar node can be matched multiple times try as often as you can.
                // This is the greedy approach and default in ANTLR. At the moment we don't support non-greedy matches
                // as we don't use them in MySQL parser rules.
                if (!scanner.isType(ANTLRParser.EOF) && node.isMultiple()) {
                    while (true) {
                        matched = match(node, scanner.getTokenType());

                        // If we get a pending collection state here then it means the match() call caused a candidate collection
                        // to start and reached the end of the node which contains at least one path that allows to match
                        // more tokens after itself.
                        // So, we have to continue collecting candidates after the current node.
                        if (runState == RunState.CollectionPending) {
                            collectFromAlternative(sequence, i); // No check needed for multiple occurences (always the case here).
                            collectFromAlternative(sequence, i + 1); // Same double collection as above.

                            // If this collection run reached an end it means we are done here.
                            // Otherwise we might still need more candidates to collect because this node or its subnodes are all
                            // optional too.
                            if (runState != RunState.CollectionPending) {
                                return hasMatchedAllMandatoryTokens(sequence, i);
                            }

                            if (!matched) {
                                break;
                            }
                        } else {
                            if (!matched) {
                                break;
                            }

                            if (node.isTerminal()) {
                                scanner.next(true);
                                if (isTokenEndAfterCaret()) {
                                    takeReferencesSnapshot();
                                    collectFromAlternative(sequence, i + 1);
                                    return hasMatchedAllMandatoryTokens(sequence, i);
                                }
                            }

                            if (scanner.isType(ANTLRParser.EOF)) {
                                break;
                            }
                        }
                    }
                }
            } else {
                // No match, but could be end of a grammar node loop.
                if (!matchedLoop) {
                    return false;
                }
            }

            ++i;
            if (i == sequence.getNodes().size()) {
                break;
            }
        }
        return true;
    }

    private void takeReferencesSnapshot() {
        // TODO Auto-generated method stub
    }

    /**
     * Returns true if the given input token matches the given grammar node. This may involve recursive rule matching.
     */
    private boolean match(GrammarNode node, int tokenType) {
        if (node.isTerminal()) {
            return (node.getTokenRef() == tokenType) || (node.isAny() && !scanner.isType(ANTLRParser.EOF));
        } else {
            return matchRule(node.getRuleRef());
        }
    }

    /**
     * Returns true if the given index is at the end of the sequence or at a point where only optional parts follow.
     */
    private boolean hasMatchedAllMandatoryTokens(GrammarSequence sequence, int index) {
        if (index + 1 == sequence.getNodes().size()) {
            return true;
        }
        for (GrammarNode node : sequence.getNodes()) {
            if (node.isRequired()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Collects possibly reachable tokens from all alternatives in the given rule.
     */
    private void collectFromRule(String rule) {
        // Don't go deeper if we have one of the special or ignored rules.
        if (rulesHolder.getSpecialRules().contains(rule)) {
            completionCandidates.add(rule);
            runState = RunState.Matching;
            return;
        }

        // Don't collect anything from an ignored rule.
        if (rulesHolder.getIgnoredRules().contains(rule)) {
            runState = RunState.Matching;
            return;
        }

        // Any other rule goes here.
        RunState combinedState = RunState.Matching;
        RuleAlternatives alts = rulesHolder.getRuleAlternatives(rule);
        if (alts.isOptimized()) {
            // Insert only tokens we are interested in.
            for (int i : alts.getSet()) {
                String tokenRef = tokenNames.get(i);
                boolean ignored = rulesHolder.getIgnoredRules().contains(tokenRef);
                if (!ignored) {
                    completionCandidates.add(tokenRef);
                }
            }

            runState = RunState.Matching;
            return;
        } else {
            for (GrammarSequence seq : alts.getSequences()) {
                // First run a predicate check if this alt can be considered at all.
                /*
                 * if ((i->min_version > server_version) || (server_version > i->max_version)) continue;
                 * 
                 * if ((i->active_sql_modes > -1) && (i->active_sql_modes & sql_mode) != i->active_sql_modes) continue;
                 * 
                 * if ((i->inactive_sql_modes > -1) && (i->inactive_sql_modes & sql_mode) != 0) continue;
                 */

                collectFromAlternative(seq, 0);
                if (runState == RunState.CollectionPending) {
                    combinedState = RunState.CollectionPending;
                }
            }
            runState = combinedState;
        }
    }

    /**
     * Collects all tokens that can be reached in the sequence from the given start point. There can be more than one if there are optional rules. Returns true
     * if the sequence between the starting point and the end consists only of optional tokens or there aren't any at all.
     */
    private void collectFromAlternative(GrammarSequence sequence, int startIndex) {
        for (int i = startIndex; i < sequence.getNodes().size(); ++i) {
            GrammarNode node = sequence.getNodes().get(i);
            if (node.isTerminal() && node.getTokenRef() == ANTLRParser.EOF) {
                runState = RunState.Matching;
                break;
            }

            if (node.isTerminal()) {
                // Insert only tokens we are interested in.
                String tokenRef = tokenNames.get(node.getTokenRef());
                boolean ignored = rulesHolder.getIgnoredRules().contains(tokenRef);
                boolean exists = completionCandidates.contains(tokenRef);
                if (!ignored && !exists) {
                    completionCandidates.add(tokenRef);
                }
                if (node.isRequired()) {
                    // Also collect following tokens into this candidate, until we find the end of the sequence
                    // or a token that is either not required or can appear multiple times.
                    String tokenRefs = tokenRef;
                    if (!ignored && !node.isMultiple()) {
                        while (++i < sequence.getNodes().size()) {
                            GrammarNode node2 = sequence.getNodes().get(i);
                            if (!node2.isTerminal() || !node2.isRequired() || node2.isMultiple()) {
                                break;
                            }
                            tokenRefs += " " + tokenNames.get(node2.getTokenRef());
                        }

                        if (tokenRefs.length() > tokenRef.length()) {
                            if (!exists) {
                                completionCandidates.remove(tokenRef);
                            }
                            completionCandidates.add(tokenRefs);
                        }
                    }

                    // If we found a required token then we are done with this alternative.
                    // That doesn't mean that we cannot start another collection run somewhere else. Just not in this alt anymore
                    // (and those rules that include this alt).
                    runState = RunState.Matching;
                    return;
                }
            } else {
                collectFromRule(node.getRuleRef());
                if (node.isRequired() && runState != RunState.CollectionPending) {
                    return;
                }
            }
        }

        // If we reach this point then we have found only optional parts, so the parent must continue collecting.
        runState = RunState.CollectionPending;
    }
}

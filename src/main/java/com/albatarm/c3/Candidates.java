package com.albatarm.c3;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.Vocabulary;

import com.albatarm.c3.collection.ArrayIntList;
import com.albatarm.c3.collection.IntList;

// All the candidates which have been found. Tokens and rules are separated (both use a numeric value).
// Token entries include a list of tokens that directly follow them (see also the "following" member in the FollowSetWithPath class).
public class Candidates {

    public static class Builder {

        private final HashMap<Integer, IntList> tokens = new HashMap<>();
        private final HashMap<Integer, IntList> rules = new HashMap<>();

        private Builder() {
        }

        public IntList getToken(int token) {
            return tokens.get(token);
        }

        public Builder putToken(int token, IntList list) {
            tokens.put(token, new ArrayIntList(list));
            return this;
        }

        public boolean containsToken(int token) {
            return tokens.containsKey(token);
        }

        public Builder putRule(int rule, IntList list) {
            rules.put(rule, new ArrayIntList(list));
            return this;
        }

        public Map<Integer, IntList> getRules() {
            return Collections.unmodifiableMap(rules);
        }

        public Candidates build() {
            return new Candidates(tokens, rules);
        }

    }

    private final Map<Integer, IntList> tokens;
    private final Map<Integer, IntList> rules;

    public Candidates(Map<Integer, IntList> tokens, Map<Integer, IntList> rules) {
        this.tokens = tokens;
        this.rules = rules;
    }

    public String toString(String[] ruleNames, Vocabulary vocabulary) {
        StringBuilder sb = new StringBuilder();
        sb.append("Candidate rules:\n");
        rules.forEach((rule, tks) -> {
            sb.append("   ").append(ruleNames[rule]).append(" => ").append(toString(vocabulary, tks)).append('\n');
        });

        sb.append('\n').append("Candidate tokens:\n");
        tokens.forEach((token, tks) -> {
            sb.append("   ").append(vocabulary.getSymbolicName(token)).append(" (").append(token).append(") => ").append(toString(vocabulary, tks)).append('\n');
        });

        return sb.toString();
    }

    private static String toString(Vocabulary vocabulary, IntList tokens) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(vocabulary.getDisplayName(tokens.get(i)));
        }
        return sb.toString();
        //return tokens.stream().mapToObj(vocabulary::getDisplayName).collect(Collectors.joining(", "));
    }

    public static Builder builder() {
        return new Builder();
    }

}

package com.albatarm.autocomplete.antlr;

import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.atn.ActionTransition;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.EpsilonTransition;
import org.antlr.v4.runtime.atn.PrecedencePredicateTransition;
import org.antlr.v4.runtime.atn.RuleTransition;
import org.antlr.v4.runtime.atn.SetTransition;
import org.antlr.v4.runtime.atn.Transition;

public class Transitions {
	
	public static String describe(Transition t, List<String> ruleNames, Vocabulary vocabulary) {
		String result;
		if (t instanceof EpsilonTransition) {
			result = "(e)";
		} else if (t instanceof RuleTransition) {
			result = String.format("rule %s precedence %d", ruleNames.get(((RuleTransition) t).ruleIndex), ((RuleTransition) t).precedence);
		} else if (t instanceof AtomTransition) {
			result = String.format("atom(%s)", vocabulary.getSymbolicName(((AtomTransition) t).label));
		} else if (t instanceof SetTransition) {
			result = String.format("set(%s)", ((SetTransition) t).set.toList().stream().map(vocabulary::getSymbolicName).collect(Collectors.joining(", ")));
		} else if (t instanceof ActionTransition) {
			result = "action";
		} else if (t instanceof PrecedencePredicateTransition) {
			result = String.format("precedence predicate %s", ((PrecedencePredicateTransition) t).precedence);
		} else {
			result = String.format("UNKNOWN %s", t.getClass().getSimpleName());
		}
		return result;
	}

}

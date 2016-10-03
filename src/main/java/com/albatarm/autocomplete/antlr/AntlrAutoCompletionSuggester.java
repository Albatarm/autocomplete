package com.albatarm.autocomplete.antlr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.SetTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AntlrAutoCompletionSuggester {
	
	private static class MyTokenStream {
		
		private final List<Token> tokens;
		private final int start;
		
		public MyTokenStream(List<Token> tokens) {
			this(tokens, 0);
		}

		public MyTokenStream(List<Token> tokens, int start) {
			this.tokens = tokens;
			this.start = start;
		}
		
		public Token next() {
			return start >= tokens.size() ? new CommonToken(-1) : tokens.get(start);
		}
		
		public boolean atCaret() {
			return next().getType() < 0;
		}
		
		public MyTokenStream move() {
			return new MyTokenStream(tokens, start + 1);
		}
		
	}
	
	private static final Logger LOG = LogManager.getLogger(AntlrAutoCompletionSuggester.class);
	
	private final List<String> ruleNames;
	private final Vocabulary vocabulary;
	private final ATN atn;
	
	private static interface Collector {
		void collect(int tokenType);
	}
	
	public AntlrAutoCompletionSuggester(List<String> ruleNames, Vocabulary vocabulary, ATN atn) {
		this.ruleNames = ruleNames;
		this.vocabulary = vocabulary;
		this.atn = atn;
	}
	
	public <T extends Lexer> Set<Integer> collectCandidates(List<Token> tokens) {
		HashSet<Integer> candidates = new HashSet<>();
		process(atn.states.get(0), new MyTokenStream(tokens), candidates::add, new ParserStack());
		return candidates;
	}

	private void process(ATNState state, MyTokenStream tokens, 
			Collector collector, ParserStack parserStack) {
		process(state, tokens, collector, parserStack, new HashSet<>(), Arrays.asList("start"));
	}
	
	private void process(ATNState state, MyTokenStream tokens, 
			Collector collector, ParserStack parserStack, Set<Integer> alreadyPassed, List<String> history) {
		//LOG.debug("history size {}", history.size());
		
		final boolean atCaret = tokens.atCaret();
		ParserStack.Result stackRes = parserStack.process(state);
		if (!stackRes.hasError()) {
			return;
		}
		
		for (Transition transition : state.getTransitions()) {
			//final String desc = describe(ruleNames, vocabulary, state, transition);
			String desc = "";
			if (transition.isEpsilon()) {
				if (!alreadyPassed.contains(transition.target.stateNumber)) {
					alreadyPassed.add(transition.target.stateNumber);
					process(transition.target, tokens, collector, stackRes.getStack(),
							alreadyPassed, plus(history, desc));
					/*process(transition.target, tokens, collector, stackRes.getStack(),
							plus(alreadyPassed, transition.target.stateNumber), plus(history, desc));*/
							
				}
			} else if (transition instanceof AtomTransition) {
				Token nextTokenType = tokens.next();
				if (atCaret) {
					if (isCompatibleWithStack(transition.target, parserStack)) {
						collector.collect(((AtomTransition) transition).label);
					}
				} else {
					if (nextTokenType.getType() == ((AtomTransition) transition).label) {
						process(transition.target, tokens.move(), collector, stackRes.getStack(), new HashSet<Integer>(), plus(history, desc));
					}
				}
			} else if (transition instanceof SetTransition) {
				Token nextTokenType = tokens.next();
				transition.label().toList().forEach(sym -> {
					if (atCaret) {
						if (isCompatibleWithStack(transition.target, parserStack)) {
							collector.collect(sym);
						}
					} else {
						if (nextTokenType.getType() == sym) {
							process(transition.target, tokens.move(), collector, stackRes.getStack(), new HashSet<>(), plus(history, desc));
						}
					}
				});
			} else {
				throw new UnsupportedOperationException(transition.getClass().getCanonicalName());
			}
		}
	}
	
	private String describe(List<String> ruleNames, Vocabulary vocabulary, ATNState s, Transition t) {
		return String.format("[%d] %s TR %s", s.stateNumber, ATNStates.describe(s), Transitions.describe(t, ruleNames, vocabulary));
	}
	
	private boolean isCompatibleWithStack(ATNState state, ParserStack parserStack) {
		ParserStack.Result res = parserStack.process(state);
		if (!res.hasError()) {
			return false;
		}
		if (state.epsilonOnlyTransitions) {
			return Arrays.stream(state.getTransitions()).anyMatch(it -> isCompatibleWithStack(it.target, res.getStack()));
		} else {
			return true;
		}
	}
	
	
	
	
	private static <E> Set<E> plus(Set<E> set, E value) {
		HashSet<E> result = new HashSet<>(set);
		result.add(value);
		return result;
	}
	
	private static <E> List<E> plus(List<E> list, E value) {
		ArrayList<E> result = new ArrayList<>(list);
		result.add(value);
		return result;
	}
	
}

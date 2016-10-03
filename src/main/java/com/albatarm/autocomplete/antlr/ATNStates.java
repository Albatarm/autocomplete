package com.albatarm.autocomplete.antlr;

import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.BasicBlockStartState;
import org.antlr.v4.runtime.atn.BasicState;
import org.antlr.v4.runtime.atn.BlockEndState;
import org.antlr.v4.runtime.atn.LoopEndState;
import org.antlr.v4.runtime.atn.PlusBlockStartState;
import org.antlr.v4.runtime.atn.PlusLoopbackState;
import org.antlr.v4.runtime.atn.RuleStartState;
import org.antlr.v4.runtime.atn.RuleStopState;
import org.antlr.v4.runtime.atn.StarBlockStartState;
import org.antlr.v4.runtime.atn.StarLoopEntryState;
import org.antlr.v4.runtime.atn.StarLoopbackState;

public class ATNStates {

	public static String describe(ATNState s) {
		String result;
		if (s instanceof RuleStartState) {
			result = String.format("rule start (stop -> %s) isLeftRec %s  (ruleIndex=%d)", ((RuleStartState) s).stopState, ((RuleStartState) s).isLeftRecursiveRule, s.ruleIndex);
		} else if (s instanceof RuleStopState) {
			result = String.format("rule stop (ruleIndex=%d)", s.ruleIndex);
		} else if (s instanceof BasicState) {
			result = "basic";
		} else if (s instanceof PlusBlockStartState) {
			result = String.format("plus block start (loopback %s)", ((PlusBlockStartState) s).loopBackState);
		} else if (s instanceof StarBlockStartState) {
			result = "star block start";
		} else if (s instanceof StarLoopEntryState) {
			result = String.format("star loop entry start (loopback %s) prec %s", ((StarLoopEntryState) s).loopBackState, ((StarLoopEntryState) s).isPrecedenceDecision);
		} else if (s instanceof StarLoopbackState) {
			result = "star loopback";
		} else if (s instanceof BasicBlockStartState) {
			result = "basic block start";
		} else if (s instanceof BlockEndState) {
			result = String.format("block end (start %s)", ((BlockEndState) s).startState);
		} else if (s instanceof PlusLoopbackState) {
			result = "plus loopback";
		} else if (s instanceof LoopEndState) {
			result = String.format("loop end (loopback %s)", ((LoopEndState) s).loopBackState);
		} else {
			result = String.format("UNKNOWN %s", s.getClass().getSimpleName());
		}
		return result;
	}
	
}

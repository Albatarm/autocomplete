package com.albatarm.autocomplete;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class CompletionProposal {

	private final Set<String> candidates;
	private final String currentToken;
	private final boolean fullyParsed;
	
	public CompletionProposal(Set<String> candidates, String currentToken, boolean fullyParsed) {
		this.candidates = new HashSet<>(candidates);
		this.currentToken = currentToken;
		this.fullyParsed = fullyParsed;
	}

	public Set<String> getCandidates() {
		return Collections.unmodifiableSet(candidates);
	}

	public String getCurrentToken() {
		return currentToken;
	}

	public boolean isFullyParsed() {
		return fullyParsed;
	}
	
}

package com.albatarm.autocomplete;

import java.util.Set;

public interface AutoCompleteProvider {
	boolean collectCandidates();
	Set<String> getCandidates();
}

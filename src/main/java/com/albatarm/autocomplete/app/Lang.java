package com.albatarm.autocomplete.app;

import com.albatarm.autocomplete.CompletionProposal;

public interface Lang {
	CompletionProposal compile2(String source, Caret caret);
}

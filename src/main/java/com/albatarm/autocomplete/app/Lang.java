package com.albatarm.autocomplete.app;

import com.albatarm.autocomplete.AutoCompleter;
import com.albatarm.autocomplete.AutoCompletionContext;
import com.albatarm.autocomplete.CompletionProposal;

public interface Lang {
    AutoCompleter getAutoCompleter();
    AutoCompletionContext<?> compile(String source, Caret caret);
    CompletionProposal compile2(String source, Caret caret);
    void parse(String source);
}

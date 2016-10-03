package com.albatarm.autocomplete.app;

import com.albatarm.autocomplete.AutoCompleter;
import com.albatarm.autocomplete.AutoCompletionContext;

public interface LangEx extends Lang {
    AutoCompleter getAutoCompleter();
    AutoCompletionContext<?> compile(String source, Caret caret);
    void parse(String source);
}

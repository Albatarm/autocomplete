package com.albatarm.autocomplete.app;

import com.albatarm.autocomplete.AutoCompleter;
import com.albatarm.autocomplete.AutoCompletionContext;

public interface Lang {
    AutoCompleter getAutoCompleter();
    AutoCompletionContext<?> compile(String source, Caret caret);
}

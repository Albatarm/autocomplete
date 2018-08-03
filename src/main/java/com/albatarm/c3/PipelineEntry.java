package com.albatarm.c3;

import org.antlr.v4.runtime.atn.ATNState;

public class PipelineEntry {

    private final ATNState state;
    private final int tokenIndex;

    public PipelineEntry(ATNState state, int tokenIndex) {
        super();
        this.state = state;
        this.tokenIndex = tokenIndex;
    }

    public ATNState getState() {
        return state;
    }

    public int getTokenIndex() {
        return tokenIndex;
    }

}

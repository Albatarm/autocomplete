package com.albatarm.c3;

import java.util.List;

import org.antlr.v4.runtime.misc.IntervalSet;

import com.google.common.collect.ImmutableList;

// A list of follow sets (for a given state number) + all of them combined for quick hit tests.
// This data is static in nature (because the used ATN states are part of a static struct: the ATN).
// Hence it can be shared between all C3 instances, however it dependes on the actual parser class (type).
public class FollowSetsHolder {

    private final ImmutableList<FollowSetWithPath> sets;
    private final IntervalSet combined = new IntervalSet();

    public FollowSetsHolder(List<FollowSetWithPath> sets) {
        this.sets = ImmutableList.copyOf(sets);
        for (FollowSetWithPath set : sets) {
            combined.addAll(set.getIntervals());
        }
    }

    public IntervalSet getCombined() {
        return combined;
    }

    public List<FollowSetWithPath> getSets() {
        return sets;
    }

}

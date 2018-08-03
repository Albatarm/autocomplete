package com.albatarm.c3;

import org.antlr.v4.runtime.misc.IntervalSet;

import com.albatarm.c3.collection.IntList;
import com.albatarm.c3.collection.IntLists;

// A record for a follow set along with the path at which this set was found.
// If there is only a single symbol in the interval set then we also collect and store tokens which follow
// this symbol directly in its rule (i.e. there is no intermediate rule transition). Only single label transitions
// are considered. This is useful if you have a chain of tokens which can be suggested as a whole, because there is
// a fixed sequence in the grammar.
public class FollowSetWithPath {

    private final IntervalSet intervals;
    private final IntList path;
    private final IntList following;

    public FollowSetWithPath(IntervalSet intervals, IntList path) {
        this(intervals, path, IntLists.empty());
    }

    public FollowSetWithPath(IntervalSet intervals, IntList path, IntList following) {
        this.intervals = intervals;
        this.path = path;
        this.following = following;
    }

    public IntervalSet getIntervals() {
        return intervals;
    }

    public IntList getPath() {
        return IntLists.unmodifiable(path);
    }

    public IntList getFollowing() {
        return IntLists.unmodifiable(following);
    }

}

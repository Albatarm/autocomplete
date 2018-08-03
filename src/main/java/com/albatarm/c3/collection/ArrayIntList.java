package com.albatarm.c3.collection;

import org.antlr.v4.runtime.misc.IntegerList;

public final class ArrayIntList extends IntegerList implements IntList {

    public ArrayIntList() {
        super();
    }

    public ArrayIntList(IntList list) {
        addAll(list);
    }

    @Override
    public void addAll(IntList list) {
        list.forEach(this::add);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IntList)) {
            return false;
        }
        return IntLists.equals(this, (IntList) o);
    }

    @Override
    public int hashCode() {
        return IntLists.hash(this);
    }

    @Override
    public String toString() {
        return IntLists.toString(this);
    }

}

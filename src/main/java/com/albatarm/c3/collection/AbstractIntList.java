package com.albatarm.c3.collection;

import java.util.Collection;

public abstract class AbstractIntList implements IntList {

    @Override
    public void addAll(int[] values) {
        for (int value : values) {
            add(value);
        }
    }

    @Override
    public void addAll(IntList list) {
        list.forEach(this::add);
    }

    @Override
    public void addAll(Collection<Integer> collection) {
        for (Integer value : collection) {
            add(value);
        }
    }

    @Override
    public boolean contains(int value) {
        for (int i = 0; i < size(); i++) {
            if (get(i) == value) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int[] toArray() {
        int[] result = new int[size()];
        for (int i = 0; i < size(); i++) {
            result[i] = get(i);
        }
        return result;
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

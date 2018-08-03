package com.albatarm.c3.collection;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import org.antlr.v4.runtime.misc.IntegerList;

public final class IntLists {

    private static class EmptyIntList extends AbstractIntList {

        @Override
        public void add(int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int get(int index) {
            throw new NoSuchElementException();
        }

        @Override
        public int set(int index, int newValue) {
            throw new NoSuchElementException();
        }

        @Override
        public int removeAt(int index) {
            throw new NoSuchElementException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void clear() {
        }

    }

    private static class UnmodifiableIntList implements IntList {

        private final IntList delegate;

        public UnmodifiableIntList(IntList delegate) {
            this.delegate = delegate;
        }

        @Override
        public void add(int value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addAll(int[] values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addAll(IntList list) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addAll(Collection<Integer> collection) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int get(int index) {
            return delegate.get(index);
        }

        @Override
        public boolean contains(int value) {
            return delegate.contains(value);
        }

        @Override
        public int set(int index, int newValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int removeAt(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int[] toArray() {
            return delegate.toArray();
        }

    }

    private static class IntegerListWrapper implements IntList {

        private final IntegerList delegate;

        public IntegerListWrapper(IntegerList delegate) {
            this.delegate = delegate;
        }

        @Override
        public void add(int value) {
            delegate.add(value);
        }

        @Override
        public void addAll(int[] values) {
            delegate.addAll(values);
        }

        @Override
        public void addAll(IntList list) {
            list.forEach(delegate::add);
        }

        @Override
        public void addAll(Collection<Integer> collection) {
            delegate.addAll(collection);
        }

        @Override
        public int get(int index) {
            return delegate.get(index);
        }

        @Override
        public boolean contains(int value) {
            return delegate.contains(value);
        }

        @Override
        public int set(int index, int newValue) {
            return delegate.set(index, newValue);
        }

        @Override
        public int removeAt(int index) {
            return delegate.removeAt(index);
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public int[] toArray() {
            return delegate.toArray();
        }

        @Override
        public String toString() {
            return IntLists.toString(this);
        }

    }

    private static class Itr implements PrimitiveIterator.OfInt {

        private final IntList list;
        private int cursor = 0;

        public Itr(IntList list) {
            this.list = list;
        }

        @Override
        public boolean hasNext() {
            return cursor != list.size();
        }

        @Override
        public int nextInt() {
            int i = cursor;
            if (i >= list.size()) {
                throw new NoSuchElementException();
            }
            int next = list.get(i);
            cursor = i + 1;
            return next;
        }

    }

    private static final EmptyIntList EMPTY = new EmptyIntList();

    private IntLists() {
    }

    public static IntList empty() {
        return EMPTY;
    }

    public static IntList unmodifiable(IntList list) {
        return new UnmodifiableIntList(list);
    }

    public static IntList wrap(IntegerList list) {
        return new IntegerListWrapper(list);
    }

    public static boolean equals(IntList a, IntList b) {
        if (a == b) {
            return true;
        }
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            int val1 = a.get(i);
            int val2 = b.get(i);
            if (val1 != val2) {
                return false;
            }
        }
        return true;
    }

    public static int hash(IntList list) {
        int hashCode = 1;
        for (int i = 0; i < list.size(); i++) {
            hashCode = 31 * hashCode + list.get(i);
        }
        return hashCode;
    }

    public static String toString(IntList list) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(list.get(i));
        }
        return sb.append(']').toString();
    }

    static PrimitiveIterator.OfInt iterator(IntList list) {
        return new Itr(list);
    }

}

package com.albatarm.c3.collection;

import java.util.Collection;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public interface IntList {

    void add(int value);

    void addAll(int[] values);

    void addAll(IntList list);

    void addAll(Collection<Integer> collection);

    int get(int index);

    boolean contains(int value);

    int set(int index, int newValue);

    int removeAt(int index);

    boolean isEmpty();

    int size();

    void clear();

    int[] toArray();

    default void forEach(IntConsumer consumer) {
        for (int i = 0; i < size(); i++) {
            consumer.accept(get(i));
        }
    }

    default void push(int value) {
        add(value);
    }

    default int pop() {
        int index = size() - 1;
        int value = get(index);
        removeAt(index);
        return value;
    }

    default int indexOf(int value) {
        for (int index = 0; index < size(); index++) {
            if (value == get(index)) {
                return index;
            }
        }
        return -1;
    }

    default IntList subList(int fromIndex, int toIndex) {
        return new SubList(this, fromIndex, toIndex);
    }

    default boolean every(IntListPredicate predicate) {
        for (int index = 0; index < size(); index++) {
            if (!predicate.test(get(index), index)) {
                return false;
            }
        }
        return true;
    }

    default PrimitiveIterator.OfInt iterator() {
        return IntLists.iterator(this);
    }

    default IntStream stream() {
        return StreamSupport.intStream(Spliterators.spliterator(iterator(), size(), Spliterator.IMMUTABLE & Spliterator.SIZED & Spliterator.ORDERED), false);
    }

}

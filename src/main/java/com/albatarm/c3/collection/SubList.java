package com.albatarm.c3.collection;

class SubList extends AbstractIntList {

    private final IntList l;
    private final int offset;
    private int size;

    SubList(IntList list, int fromIndex, int toIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        }
        if (toIndex > list.size()) {
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        }
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex +
                    ") > toIndex(" + toIndex + ")");
        }
        l = list;
        offset = fromIndex;
        size = toIndex - fromIndex;
    }

    @Override
    public void add(int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int get(int index) {
        rangeCheck(index);
        return l.get(index + offset);
    }

    @Override
    public int set(int index, int newValue) {
        rangeCheck(index);
        return l.set(index + offset, newValue);
    }

    @Override
    public int removeAt(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    private void rangeCheck(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }
    }

    private String outOfBoundsMsg(int index) {
        return "Index: " + index + ", Size: " + size;
    }

}

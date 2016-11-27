package com.zigurs.karlis.utils.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Simple array iterator.
 *
 * @param <T> type of array elements
 */
public final class ArrayIterator<T> implements Iterator<T> {

    private final T[] array;
    private int index;

    ArrayIterator(final T[] elements) {
        Objects.requireNonNull(elements);
        array = elements;
    }

    @Override
    public boolean hasNext() {
        return index < array.length;
    }

    @Override
    public T next() {
        if (index >= array.length)
            throw new NoSuchElementException("all items already retrieved");

        return array[index++];
    }
}

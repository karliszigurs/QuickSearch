/*
 *                                     //
 * Copyright 2016 Karlis Zigurs (http://zigurs.com)
 *                                   //
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zigurs.karlis.utils.search;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;

/**
 * <strong>Immutable</strong>, array backed, {@link Set} of unique (as determined by
 * their respective {@link Object#equals(Object)} method), non-{@code null} elements.
 * <p>
 * The rationale is to have as lightweight as possible memory profile and iteration cost
 * while retaining {@link Set} semantics - in this case at cost of modifying operations.
 * Fully obeys JDK Collections {@link Set} semantics.
 * <p>
 * Comes with helper functions {@link ImmutableSet#add(ImmutableSet, Comparable)} and
 * {@link ImmutableSet#remove(ImmutableSet, Comparable)} that create a new {@link ImmutableSet}
 * instance after applying requested modification (leaving the original {@link ImmutableSet} intact).
 * <p>
 * Calls to any modifying operations on an instance will throw an exception.
 * <p>
 * This implementation does not permit {@code null} elements.
 * <p>
 * This implementation is thread-safe.
 *
 * @author Karlis Zigurs, 2016
 */
@SuppressWarnings("unchecked")
public class ImmutableSet<T extends Comparable<T>> extends AbstractSet<T> {

    /**
     * Static, shareable and empty iterator.
     */
    private static final Iterator EMPTY_ITERATOR = new Iterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }
    };

    /**
     * Reusable instance for empty sets.
     */
    private static final ImmutableSet EMPTY_SET = new ImmutableSet(new Comparable[0]) {
        @Override
        public Iterator iterator() {
            return EMPTY_ITERATOR;
        }
    };

    /*
     * Implementation
     */

    /**
     * Array with elements in this set.
     */
    private final T[] array;

    /**
     * Cached hashcode
     */
    private int cachedHashCode = 0;

    private ImmutableSet(final T[] array) {
        this(array, true);
    }

    private ImmutableSet(final T[] array, boolean shouldSort) {
        Objects.requireNonNull(array);

        if (shouldSort)
            Arrays.parallelSort(array);

        this.array = array;
    }

    @Override
    public int size() {
        return array.length;
    }

    @Override
    public boolean contains(final Object o) {
        if (o == null || !(o instanceof Comparable))
            return false;

        return Arrays.binarySearch(array, o) > -1;
    }

    @Override
    public Iterator<T> iterator() {
        return new ImmutableSetIterator(array);
    }

    @Override
    public void forEach(final Consumer<? super T> consumer) {
        Objects.requireNonNull(consumer);

        for (T element : array)
            consumer.accept(element);
    }

    /**
     * Follows {@link AbstractSet#hashCode()} semantics - hash code is calculated from
     * the elements in the set and will be equal to other sets that contain the
     * same (hashcodes of) elements.
     *
     * @return calculated hashcode
     */
    @Override
    public int hashCode() {
        if (cachedHashCode != 0)
            return cachedHashCode;

        int h = 0;
        for (T element : array)
            h += element.hashCode();

        cachedHashCode = h; // cache for future calls
        return cachedHashCode;
    }

    /**
     * Follows {@link AbstractSet#equals(Object)} semantics.
     *
     * @param that object to compare to
     * @return {@code true} if compared to set consisting of equal elements
     */
    @Override
    public boolean equals(Object that) {
        if (that == this)
            return true;

        if (!(that instanceof Set))
            return false;

        Set<?> set = (Set<?>) that;

        return (set.size() == size())
                && containsAll(set);
    }

    /*
     * Extended functionality
     */

    /**
     * Convenience call for parallel processing to split the set in
     * two halves in an efficient fashion.
     *
     * @return Array of new ImmutableSet instances of size none (if empty),
     * one (if single element) or two instances representing halves (if 2+ elements present).
     */
    public ImmutableSet<T>[] split() {
        int size = size();

        if (size == 0)
            return new ImmutableSet[0];

        if (size == 1)
            return new ImmutableSet[]{this};

        int leftHalf = size / 2;

        ImmutableSet<T> left = new ImmutableSet<>(Arrays.copyOfRange(array, 0, leftHalf));
        ImmutableSet<T> right = new ImmutableSet<>(Arrays.copyOfRange(array, leftHalf, size));

        return new ImmutableSet[]{left, right};
    }

    /*
     * Helpers
     */

    /**
     * Empty set of given type.
     *
     * @param <T> type
     * @return empty set
     */
    public static <T extends Comparable<T>> ImmutableSet<T> emptySet() {
        return (ImmutableSet<T>) EMPTY_SET;
    }

    /**
     * Set of single element.
     *
     * @param element element to wrap
     * @param <T>     type
     * @return set containing single element
     */
    public static <T extends Comparable<T>> ImmutableSet<T> singletonSet(final T element) {
        Objects.requireNonNull(element);

        return new ImmutableSet<>((T[]) new Comparable[]{element});
    }

    /**
     * Create a new {@link ImmutableSet} by adding an item to existing set.
     *
     * @param set        set to add to
     * @param newElement element to add
     * @param <T>        type of items in set
     * @return new instance with the element added or original set if element was already present
     */
    public static <T extends Comparable<T>> ImmutableSet<T> add(final ImmutableSet<T> set,
                                                                final T newElement) {
        Objects.requireNonNull(set);
        Objects.requireNonNull(newElement);

        if (set.isEmpty())
            return singletonSet(newElement);

        int insertionPoint = Arrays.binarySearch(set.array, newElement);

        if (insertionPoint > -1)
            return set; // already contains

        /*
         * Might as well add it then... since we've just done the binary search insertion point lookup
         */

        insertionPoint = (-insertionPoint) - 1;

        T[] destination = (T[]) Array.newInstance(Comparable.class, set.array.length + 1);
        System.arraycopy(set.array, 0, destination, 0, insertionPoint); // copy up to
        System.arraycopy(set.array, insertionPoint, destination, insertionPoint + 1, set.array.length - insertionPoint);
        destination[insertionPoint] = newElement;

        return new ImmutableSet<>(destination, false);
    }

    /**
     * Create an {@link ImmutableSet} instance by removing an element
     * from an existing {@link ImmutableSet}.
     *
     * @param set             non-{@code null} set
     * @param elementToRemove element to remove
     * @param <T>             type of items in set
     * @return new instance with specified item removed or supplied set instance if specified item was not present
     */
    public static <T extends Comparable<T>> ImmutableSet<T> remove(final ImmutableSet<T> set,
                                                                   final T elementToRemove) {
        Objects.requireNonNull(set);
        Objects.requireNonNull(elementToRemove);

        if (set.isEmpty())
            return set;

        // Check if present
        int insertionPoint = Arrays.binarySearch(set.array, elementToRemove);
        if (insertionPoint < 0)
            return set; // not present

        // present, but the only element? Ok.
        if (set.size() == 1)
            return emptySet();

        /*
         * Remove from a known position
         */

        T[] destination = (T[]) Array.newInstance(Comparable.class, set.array.length - 1);

        System.arraycopy(set.array, 0, destination, 0, insertionPoint);
        System.arraycopy(set.array, insertionPoint + 1, destination, insertionPoint, destination.length - insertionPoint);

        return new ImmutableSet<>(destination, false);
    }

    /**
     * Create a {@link ImmutableSet} of unique, non-{@code null} elements
     * from the specified {@link Collection}.
     *
     * @param source source collection
     * @param <T>    type
     * @return immutable set of unique, non-null elements
     */
    public static <T extends Comparable<T>> ImmutableSet<T> fromCollection(final Collection<? extends T> source) {
        Objects.requireNonNull(source);

        if (source.isEmpty())
            return emptySet();

        if (source instanceof ImmutableSet)
            return (ImmutableSet<T>) source;

        if (source instanceof Set) {
            Set set = (Set) source;
            set.remove(null);
            return new ImmutableSet<>((T[]) set.toArray((T[]) Array.newInstance(Comparable.class, set.size())));
        }

        /* and brute force fallback */
        Set<T> set = new HashSet<>();

        set.addAll(source);
        set.remove(null);

        return new ImmutableSet<>(set.toArray((T[]) Array.newInstance(Comparable.class, set.size())));
    }

    /**
     * Create an {@link ImmutableSet} instance consisting of union of two {@link Collection}s.
     *
     * @param left  left source collection
     * @param right right source collection
     * @param <T>   type
     * @return set of all unique, non-null elements from both collections
     */
    public static <T extends Comparable<T>> ImmutableSet<T> fromCollections(final Collection<? extends T> left,
                                                                            final Collection<? extends T> right) {
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);

        /*
         * Avoid any actual work, if we can help it
         */
        if (left.isEmpty() && right.isEmpty())
            return emptySet();

        if (left.isEmpty())
            return fromCollection(right);

        if (right.isEmpty())
            return fromCollection(left);

        /* brute force it is */
        Set<T> set = new HashSet<>();

        set.addAll(left);
        set.addAll(right);
        set.remove(null);

        return new ImmutableSet<>(set.toArray((T[]) Array.newInstance(Comparable.class, set.size())));
    }

    /**
     * Helper to determine equality using cheapest possible cost.
     * <p>
     * Compares by reference, then discards if {@link Object#hashCode()}s differ
     * and only calls {@link Object#equals(Object)} as a last resort.
     *
     * @param left  item to compare
     * @param right item to compare
     * @param <T>   type
     * @return true if {@code left.equals(right)}
     */
    private static <T extends Comparable<T>> boolean itemsAreEqual(T left, T right) {
        return left == right ||
                (left.hashCode() == right.hashCode() &&
                        left.equals(right));
    }

    /**
     * Simple array iterator
     *
     * @param <T> type of array elements
     */
    private static class ImmutableSetIterator<T> implements Iterator<T> {

        private final T[] innerArray;
        private int index = 0;

        private ImmutableSetIterator(T[] array) {
            Objects.requireNonNull(array);

            innerArray = array;
        }

        @Override
        public boolean hasNext() {
            return index < innerArray.length;
        }

        @Override
        public T next() {
            if (index >= innerArray.length) {
                throw new NoSuchElementException();
            }
            return innerArray[index++];
        }
    }
}

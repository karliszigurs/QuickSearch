/*
 * Copyright 2016 Karlis Zigurs
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

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

/**
 * Array backed implementation of a set (yes, it is wrong. Very).
 * <p>
 * The rationale is to make memory profile and reads as lightweight as possible
 * even if it comes at the expense of modifying operations. If you can, try to use
 * .forEach to iterate over the set as it avoids allocating an iterator instance.
 * <p>
 * Since it is to be used to contain non-null, unique, values Set interface seems to be most appropriate.
 * <p>
 * Comes at a cost of being a bit thread unsafe, modifying itself, etc.
 * <p>
 * To be used in QuickSearch and behind write lock _ONLY_!
 * <p>
 * This class does not permit <tt>null</tt> elements.
 *
 * @param <T> type this set instance will operate on
 */
@SuppressWarnings("unchecked")
public class ImmutableSet<T> extends AbstractSet<T> {

    /**
     * Static, shareable empty iterator.
     */
    private static final Iterator NULL_ITERATOR = new Iterator() {
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
     * Pass around the same instance for empty sets. Yay for less allocations.
     */
    private static final ImmutableSet EMPTY_SET = new ImmutableSet(new Object[0]) {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @NotNull
        @Override
        public Iterator iterator() {
            return NULL_ITERATOR;
        }

        @Override
        public void forEach(Consumer action) {
            /* No operation */
        }

        @NotNull
        @Override
        public ImmutableSet safeCopy() {
            return EMPTY_SET;
        }
    };
    /**
     * Array with elements in this set.
     */
    private final T[] array;
    /**
     * Cached hashcode
     */
    private int hashCode = 0;

    private ImmutableSet(@NotNull final T[] array) {
        this.array = array;
    }

    /**
     * Empty set of given type.
     *
     * @param <S> type
     * @return empty set
     */
    @NotNull
    public static <S> ImmutableSet<S> empty() {
        return (ImmutableSet<S>) EMPTY_SET;
    }

    /**
     * Set with one member.
     *
     * @param item item to wrap in set
     * @param <S>  type
     * @return set of type with specified member
     */
    @NotNull
    public static <S> ImmutableSet<S> fromSingle(@NotNull final S item) {
        Objects.requireNonNull(item);

        return new ImmutableSet<>((S[]) new Object[]{item});
    }

    /**
     * Expand given collection with a specified item and return a set.
     * <p>
     * Given as convenience method that performs better if operating on
     * ImmutableSet already.
     *
     * @param source base collection
     * @param item   item to add
     * @param <S>    type
     * @return set of items
     */
    @NotNull
    public static <S> ImmutableSet<S> addAndCreate(@NotNull final Collection<? extends S> source,
                                                   @NotNull final S item) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(item);

        if (source.isEmpty())
            return new ImmutableSet<>((S[]) new Object[]{item});

        if (source instanceof ImmutableSet) {
            ImmutableSet<S> set = (ImmutableSet<S>) source;

            if (set.contains(item))
                return set;

            Object[] destination = new Object[set.array.length + 1];
            System.arraycopy(set.array, 0, destination, 0, set.array.length);
            destination[destination.length - 1] = item;

            return new ImmutableSet<>((S[]) destination);
        } else {
            Set<S> set = new HashSet<>();
            set.addAll(source);
            set.add(item);
            set = removeNullFromSet(set);
            return new ImmutableSet<>((S[]) set.toArray());
        }
    }

    /**
     * Create a set consisting of supplied collection with the specified item removed.
     * Again, ideally operating on a ImmutableSet itself as the source collection.
     *
     * @param source      source collection
     * @param surplusItem item to remove
     * @param <S>         type
     * @return set of original collection items minus specified item
     */
    @NotNull
    public static <S> ImmutableSet<S> removeAndCreate(@NotNull final Collection<? extends S> source,
                                                      @NotNull final S surplusItem) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(surplusItem);

        if (source instanceof ImmutableSet) {
            return removeViaCompacting((ImmutableSet<S>) source, surplusItem);
        } else {
            Set<S> set = new HashSet<>();
            set.addAll(source);
            set.remove(surplusItem);
            set = removeNullFromSet(set);
            return new ImmutableSet<>((S[]) set.toArray());
        }
    }

    private static <S> ImmutableSet<S> removeViaCompacting(@NotNull final ImmutableSet<S> set,
                                                           @NotNull final S surplusItem) {
        if (set.isEmpty())
            return set;

        S[] source = set.array;
        Object[] destination = new Object[source.length - 1];

        int destPtr = 0;
        for (int i = 0; i < source.length; i++) {
            if (itemsEqual(source[i], surplusItem)) {
                if (destPtr < destination.length) // more to copy
                    System.arraycopy(source, i + 1, destination, i, source.length - (i + 1));
                return new ImmutableSet<>((S[]) destination);
            } else if (destPtr < destination.length) { // end of copy without match
                destination[destPtr++] = source[i];
            }
        }

        /* No items were removed, set unchanged */
        return set;
    }

    /**
     * Create a read only set from a specified collection.
     *
     * @param source source collection
     * @param <S>    type
     * @return read only set
     */
    @NotNull
    public static <S> ImmutableSet<S> fromCollection(@NotNull final Collection<? extends S> source) {
        Objects.requireNonNull(source);

        if (source.isEmpty())
            return empty();

        if (source instanceof ImmutableSet)
            return (ImmutableSet<S>) source;

        if (source instanceof Set) {
            Set set = removeNullFromSet((Set) source);
            return new ImmutableSet<>((S[]) set.toArray());
        }

        Set<S> set = new HashSet<>();
        set.addAll(source);
        set = removeNullFromSet(set);
        return new ImmutableSet<>((S[]) set.toArray());
    }

    /**
     * Create set from union of two collections.
     *
     * @param source  first source collection
     * @param source2 second source collection
     * @param <S>     type
     * @return set of unique items across both collections
     */
    @NotNull
    public static <S> ImmutableSet<S> fromCollections(@NotNull final Collection<? extends S> source,
                                                      @NotNull final Collection<? extends S> source2) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(source2);

        /*
         * Avoid any actual work, if we can help it
         */
        boolean sourceEmpty = source.isEmpty();
        boolean source2Empty = source2.isEmpty();

        if (sourceEmpty && source2Empty)
            return empty();

        if (sourceEmpty)
            return fromCollection(source2);

        if (source2Empty)
            return fromCollection(source);

        /*
         * Ah well, brute force it is.
         */
        Set<S> set = new HashSet<>();
        set.addAll(source);
        set.addAll(source2);
        set = removeNullFromSet(set);
        return new ImmutableSet<>((S[]) set.toArray());
    }

    private static Set removeNullFromSet(Set source) {
        source.remove(null);
        return source;
    }

    private static <S> boolean itemsEqual(S one, S two) {
        return one == two ||
                (one.hashCode() == two.hashCode() &&
                        one.equals(two));
    }

    @Override
    public int size() {
        return array.length;
    }

    @Override
    public boolean contains(final Object o) {
        Objects.requireNonNull(o);

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < array.length; i++) {
            /* Try cheaper checks first */
            if (array[i] == o || (array[i].hashCode() == o.hashCode() && array[i].equals(o))) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new ArrayIterator(array);
    }

    @Override
    public void forEach(final Consumer<? super T> action) {
        Objects.requireNonNull(action);

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < array.length; i++) {
            action.accept(array[i]);
        }
    }

    @NotNull
    public ImmutableSet<T> safeCopy() {
        return new ImmutableSet<>(Arrays.copyOf(array, array.length));
    }

    /**
     * Follows AbstractSet.hashCode() semantics, that is - hash code is calculated from
     * the elements in the set and will be equal to other sets that contain the same (hashcodes of)
     * elements.
     *
     * @return calculated hashcode
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Override
    public int hashCode() {
        if (hashCode != 0)
            return hashCode;

        /*
         * It must be dully noted that as we have an array
         * to operate on directly, it's far more efficient
         * than iterators, streams or foreach.
         */

        int h = 0;
        for (int i = 0; i < array.length; i++)
            h += array[i].hashCode();

        hashCode = h;
        return hashCode;
    }

    /**
     * Follows AbstractSet.equals() semantics. That is, it is compared as a set
     * and matches if it contains the same elements.
     *
     * @param o object to compare to
     * @return if matches AbstractSet.equals() semantics
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Set))
            return false;

        Set<?> set = (Set<?>) o;

        return set.size() == size() &&
                containsAll(set);
    }

    private static class ArrayIterator<I> implements Iterator<I> {

        private final I[] innerArray;
        private int index = 0;

        public ArrayIterator(I[] array) {
            innerArray = array;
        }

        @Override
        public boolean hasNext() {
            return index < innerArray.length;
        }

        @Override
        public I next() {
            if (index >= innerArray.length) {
                throw new NoSuchElementException();
            }
            return innerArray[index++];
        }
    }
}

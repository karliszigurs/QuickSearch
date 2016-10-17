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
 *
 * @param <T>
 */
@SuppressWarnings("unchecked")
public final class ReadOnlySet<T> extends AbstractSet<T> {

    private static final ReadOnlySet EMPTY_SET = new ReadOnlySet(new Object[0]);

    /**
     * Empty set of given type.
     *
     * @param <S> type
     * @return empty set
     */
    @NotNull
    public static <S> ReadOnlySet<S> empty() {
        return (ReadOnlySet<S>) EMPTY_SET;
    }

    /**
     * Set with one member.
     *
     * @param item item to wrap in set
     * @param <S> type
     * @return set of type with specified member
     */
    @NotNull
    public static <S> ReadOnlySet<S> fromSingle(@NotNull S item) {
        Objects.requireNonNull(item);

        return new ReadOnlySet<>((S[]) new Object[]{item});
    }

    /**
     * Expand given collection with a specified item and return a set.
     *
     * Given as convenience method that performs better if operating on
     * ReadOnlySet already.
     *
     * @param source base collection
     * @param item item to add
     * @param <S> type
     * @return set of items
     */
    @NotNull
    public static <S> ReadOnlySet<S> addAndCreate(@NotNull Collection<? extends S> source,
                                                  @NotNull S item) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(item);

        if (source.isEmpty())
            return new ReadOnlySet<>((S[]) new Object[]{item});

        if (source instanceof ReadOnlySet) {
            ReadOnlySet<S> set = (ReadOnlySet<S>) source;

            if (set.contains(item)) {
                return set;
            }

            Object[] arr = new Object[set.array.length + 1];
            System.arraycopy(set.array, 0, arr, 0, set.array.length);
            arr[arr.length - 1] = item;

            set.array = (S[]) arr;
            return set;
        } else {
            HashSet<S> set = new HashSet<>();
            set.addAll(source);
            set.add(item);
            return new ReadOnlySet<>((S[]) set.toArray());
        }
    }

    /**
     * Create a set consisting of supplied collection with the specified item removed.
     * Again, ideally operating on a ReadOnlySet itself as the source collection.
     *
     * @param source source collection
     * @param surplusItem item to remove
     * @param <S> type
     * @return set of original collection items minus specified item
     */
    @NotNull
    public static <S> ReadOnlySet<S> removeAndCreate(@NotNull Collection<? extends S> source,
                                                     @NotNull S surplusItem) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(surplusItem);

        if (source instanceof ReadOnlySet) {
            ReadOnlySet<S> set = (ReadOnlySet<S>) source;

            if (set.size() == 1) {
                if (set.array[0].equals(surplusItem)) {
                    return empty();
                } else {
                    return set; // trying to remove an item we know isn't there? I'd argue that that hints at a bug.
                }
            } else {
                /* Do it the hard way, by compacting the array */
                for (int i = 0; i < set.array.length; i++) {
                    if (set.array[i].hashCode() == surplusItem.hashCode() && set.array[i].equals(surplusItem)) {
                        System.arraycopy(set.array, i + 1, set.array, i, set.array.length - (i + 1));
                        set.array = Arrays.copyOf(set.array, set.array.length - 1);
                        return set;
                    }
                }
            }
            return set; // No item was removed
        } else {
            HashSet<S> set = new HashSet<>();

            set.addAll(source);
            set.remove(surplusItem);

            return new ReadOnlySet<>((S[]) set.toArray());
        }
    }

    /**
     * Create a read only set from a specified collection.
     *
     * @param source source collection
     * @param <S> type
     * @return read only set
     */
    @NotNull
    public static <S> ReadOnlySet<S> fromCollection(@NotNull Collection<? extends S> source) {
        Objects.requireNonNull(source);

        if (source.isEmpty()) {
            return empty();
        }

        HashSet<S> set = new HashSet<>();
        set.addAll(source);
        return new ReadOnlySet<>((S[]) set.toArray());
    }

    /**
     * Create set from union of two collections.
     *
     * @param source first source collection
     * @param source2 second source collection
     * @param <S> type
     * @return set of unique items across both collections
     */
    @NotNull
    public static <S> ReadOnlySet<S> fromCollections(@NotNull Collection<? extends S> source,
                                                     @NotNull Collection<? extends S> source2) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(source2);

        if (source.isEmpty() && source2.isEmpty())
            return empty();

        HashSet<S> set = new HashSet<>();
        set.addAll(source);
        set.addAll(source2);
        return new ReadOnlySet<>((S[]) set.toArray());
    }

    /*
     * Implementation below.
     */

    private T[] array;

    private ReadOnlySet(@NotNull T[] array) {
        this.array = array;
    }

    @Override
    public int size() {
        return array.length;
    }

    @Override
    public boolean contains(Object o) {
        Objects.requireNonNull(o);

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < array.length; i++) {
            /* Try to skip more by cheaper hash check first */
            if (array[i].hashCode() == o.hashCode() && array[i].equals(o)) {
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
    public void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action);

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < array.length; i++) {
            action.accept(array[i]);
        }
    }

    @NotNull
    public ReadOnlySet<T> safeCopy() {
        return new ReadOnlySet<>(Arrays.copyOf(array, array.length));
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
            return innerArray[index++];
        }
    }
}

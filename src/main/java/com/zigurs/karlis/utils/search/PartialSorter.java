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

/**
 * Purpose built sort discarding known beyond-the-cut elements early.
 * Trades the cost of manual insertion against the cost of having to sort whole array.
 * <p>
 * Comparable to built in sort functions on smaller datasets (&lt;1000 elements), but
 * significantly outperforms built-in sort algorithms on larger datasets (which may be important
 * when sorting 5000 matching items by score to serve only top 3).
 */
public class PartialSorter {

    /*
     * Magical number where we switch from using array to using TreeSet.
     *
     * Determined after profiling / benchmarking scaling curves and consulting with pixies.
     */
    private static final int ARRAY_SORT_LIMIT = 101;

    /**
     * Sort function which delegates the sorting to likely more efficient
     * internal sorting implementation for the given input size and desired
     * results set size
     *
     * @param input          collection to select elements from
     * @param limitResultsTo maximum size of generated ordered list, negative or 0 will return empty list
     * @param comparator     comparator to use (or use Comparator.naturalOrder())
     * @param <X>            type of objects to sort
     * @return sorted list consisting of first (up to limitResultsTo) elements in specified comparator order
     */
    public static <X> List<X> sortAndLimit(@NotNull final Collection<? extends X> input,
                                           final int limitResultsTo,
                                           @NotNull final Comparator<X> comparator) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(comparator);

        if (limitResultsTo < 1 || input.isEmpty())
            return Collections.emptyList();

        if (limitResultsTo < ARRAY_SORT_LIMIT)
            return sortAndLimitWithArray(input, limitResultsTo, comparator);
        else
            return sortAndLimitWithTreeSet(input, limitResultsTo, comparator);
    }

    private static <X> List<X> sortAndLimitWithTreeSet(@NotNull final Collection<? extends X> input,
                                                      final int limitResultsTo,
                                                      @NotNull final Comparator<X> comparator) {
        TreeSet<X> results = new TreeSet<>(comparator);

        X lastEntry = null;

        for (X entry : input) {
            if (results.size() < limitResultsTo) {
                results.add(entry);
                lastEntry = results.last();
            } else if (comparator.compare(entry, lastEntry) < 0) {
                results.add(entry);
                results.remove(results.last());
                lastEntry = results.last();
            }
        }

        return new ArrayList<>(results);
    }

    private static <X> List<X> sortAndLimitWithArray(@NotNull final Collection<? extends X> input,
                                                    final int limitResultsTo,
                                                    @NotNull final Comparator<X> comparator) {
        //noinspection unchecked
        X[] array = (X[]) new Object[Math.min(input.size(), limitResultsTo)];

        X lastEntry = null;

        for (X entry : input) {
            if (lastEntry == null) { //handle initial population
                for (int pos = 0; pos < array.length; pos++) {
                    if (array[pos] == null || comparator.compare(entry, array[pos]) < 0) {
                        System.arraycopy(array, pos, array, pos + 1, array.length - (pos + 1));
                        array[pos] = entry;
                        break;
                    }
                }
                lastEntry = array[array.length - 1];
            } else if (comparator.compare(entry, lastEntry) < 0) {
                for (int pos = 0; pos < array.length; pos++) {
                    if (comparator.compare(entry, array[pos]) < 0) {
                        System.arraycopy(array, pos, array, pos + 1, array.length - (pos + 1));
                        array[pos] = entry;
                        break;
                    }
                }
                lastEntry = array[array.length - 1];
            }
        }

        return Arrays.asList(array);
    }
}

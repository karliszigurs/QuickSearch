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

    /**
     * Sort function
     *
     * @param input          collection to select elements from
     * @param limitResultsTo maximum size of generated ordered list
     * @param comparator     comparator to use (or use Comparator.naturalOrder())
     * @param <X>            type of objects to sort
     * @return sorted list consisting of first (up to limitResultsTo) elements in specified comparator order
     */
    public static <X> List<X> sortAndLimit(@NotNull final Collection<? extends X> input,
                                           final int limitResultsTo,
                                           @NotNull final Comparator<X> comparator) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(comparator);

        final int maxResults = Math.max(limitResultsTo, 0); // Safety check that limit is not negative
        LinkedList<X> result = new LinkedList<>();

        for (X entry : input) {
            if (result.size() < maxResults) {
                insertInListInOrderedPos(result, entry, comparator);
            } else if (comparator.compare(entry, result.getLast()) < 0) {
                insertInListInOrderedPos(result, entry, comparator);
                result.removeLast();
            }
        }

        return result;
    }

    private static <X> void insertInListInOrderedPos(@NotNull List<X> result,
                                                     @NotNull X entry,
                                                     @NotNull Comparator<X> comparator) {
        for (int pos = 0; pos < result.size(); pos++) {
            if (comparator.compare(entry, result.get(pos)) < 0) {
                result.add(pos, entry);
                return;
            }
        }
        // If not added already (and returned there), append to end of the list
        result.add(entry);
    }
}

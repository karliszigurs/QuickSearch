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
package com.zigurs.karlis.utils.search.parallel;

import com.zigurs.karlis.utils.collections.ImmutableSet;

import java.util.Map;
import java.util.concurrent.RecursiveTask;
import java.util.function.Function;

import static com.zigurs.karlis.utils.search.QuickSearch.intersectMaps;

/**
 * Fork-join task computing the intersection of maps (summing values)
 * provided for all specified keywords.
 *
 * @param <T> type of keys of maps being intersected
 */
public class IntersectionTask<T> extends RecursiveTask<Map<T, Double>> {

    private final ImmutableSet<String> keywords;
    private final Function<String, Map<T, Double>> supplierFunction;

    /**
     * Constructor.
     *
     * @param keywords         Immutable set of keywords this task should split down and accumulate
     * @param supplierFunction supplier of maps to intersect for given keyword
     */
    public IntersectionTask(final ImmutableSet<String> keywords,
                            final Function<String, Map<T, Double>> supplierFunction) {
        this.keywords = keywords;
        this.supplierFunction = supplierFunction;
    }

    @Override
    protected Map<T, Double> compute() {
        if (keywords.size() == 1)
            return supplierFunction.apply(keywords.iterator().next());

        ImmutableSet<String>[] splits = keywords.split();

        IntersectionTask<T> left = new IntersectionTask<>(splits[0], supplierFunction);
        left.fork();

        IntersectionTask<T> right = new IntersectionTask<>(splits[1], supplierFunction);
        right.fork();

        Map<T, Double> leftMap = left.join();

        if (leftMap.isEmpty()) {
            right.cancel(true); // Worth a try...
            return leftMap;
        }

        Map<T, Double> rightMap = right.join();

        if (rightMap.isEmpty())
            return rightMap;

        return intersectMaps(leftMap, rightMap);
    }
}

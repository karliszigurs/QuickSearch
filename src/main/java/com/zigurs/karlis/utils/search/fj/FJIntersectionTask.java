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
package com.zigurs.karlis.utils.search.fj;

import com.zigurs.karlis.utils.search.ImmutableSet;

import java.util.Map;
import java.util.concurrent.RecursiveTask;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FJIntersectionTask<T> extends RecursiveTask<Map<T, Double>> {

    private final ImmutableSet<String> keywords;
    private final Function<String, Map<T, Double>> supplierFunction;
    private final BiFunction<Map<T, Double>, Map<T, Double>, Map<T, Double>> mergeFunction;

    public FJIntersectionTask(final ImmutableSet<String> keywords,
                              final Function<String, Map<T, Double>> supplierFunction,
                              final BiFunction<Map<T, Double>, Map<T, Double>, Map<T, Double>> mergeFunction) {
        this.keywords = keywords;
        this.supplierFunction = supplierFunction;
        this.mergeFunction = mergeFunction;
    }

    @Override
    protected Map<T, Double> compute() {
        if (keywords.size() == 1)
            return supplierFunction.apply(keywords.getSingleElement());

        ImmutableSet<String>[] splits = keywords.split();

        FJIntersectionTask<T> left = new FJIntersectionTask<>(splits[0], supplierFunction, mergeFunction);
        left.fork();

        FJIntersectionTask<T> right = new FJIntersectionTask<>(splits[1], supplierFunction, mergeFunction);
        right.fork();

        Map<T, Double> leftMap = left.join();

        if (leftMap.isEmpty()) {
            right.cancel(true); // Worth a try...
            return leftMap;
        }

        Map<T, Double> rightMap = right.join();

        if (rightMap.isEmpty())
            return rightMap;

        return mergeFunction.apply(leftMap, rightMap);
    }
}

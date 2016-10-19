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
package com.zigurs.karlis.utils.search.model;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Container for augumented search result result containing the keywords
 * associated with the result and the calculated search result score.
 *
 * @param <T> wrapped response result type
 */
public class Item<T> {

    @NotNull
    private final T result;
    @NotNull
    private final Set<String> itemKeywords;

    private final double score;

    /**
     * Construct an instance.
     *
     * @param result       result
     * @param itemKeywords set of non-null keywords associated with result
     * @param score        search result score for the result
     */
    public Item(@NotNull T result, @NotNull Set<String> itemKeywords, double score) {
        this.result = result;
        this.itemKeywords = itemKeywords;
        this.score = score;
    }

    /**
     * Query.
     *
     * @return wrapped result
     */
    @NotNull
    public T getResult() {
        return result;
    }

    /**
     * Query.
     *
     * @return result keywords, if supplied
     */
    @NotNull
    public Set<String> getItemKeywords() {
        return itemKeywords;
    }

    /**
     * Query.
     *
     * @return final score for the result in particular search
     */
    public double getScore() {
        return score;
    }
}

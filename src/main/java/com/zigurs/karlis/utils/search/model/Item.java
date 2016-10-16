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

import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Container for augumented search result item containing the keywords
 * associated with the item and the calculated search result score.
 *
 * @param <T> wrapped response item type
 */
public class Item<T> {

    @NotNull
    private final T item;
    @NotNull
    private final Set<String> itemKeywords;

    private final double score;

    /**
     * Construct an instance.
     *
     * @param item         item
     * @param itemKeywords set of non-null keywords associated with item
     * @param score        search result score for the item
     */
    public Item(@NotNull T item, @NotNull Set<String> itemKeywords, double score) {
        this.item = item;
        this.itemKeywords = ImmutableSet.copyOf(itemKeywords);
        this.score = score;
    }

    /**
     * Query.
     *
     * @return wrapped item
     */
    @NotNull
    public T getItem() {
        return item;
    }

    /**
     * Query.
     *
     * @return item keywords, if supplied
     */
    @NotNull
    public Set<String> getItemKeywords() {
        return itemKeywords;
    }

    /**
     * Query.
     *
     * @return final score for the item in particular search
     */
    public double getScore() {
        return score;
    }
}

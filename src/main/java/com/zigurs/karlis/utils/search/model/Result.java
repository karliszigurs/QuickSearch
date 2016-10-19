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

import java.util.List;

/**
 * Container for augmented search results.
 *
 * @param <T> wrapped response item type
 */
public class Result<T> {

    @NotNull
    private final String searchString;
    @NotNull
    private final List<Item<T>> responseItems;

    /**
     * Construct an instance
     *
     * @param searchString  search string that generated this result set
     * @param responseItems found items
     */
    public Result(@NotNull final String searchString, @NotNull final List<Item<T>> responseItems) {
        this.searchString = searchString;
        this.responseItems = responseItems;
    }

    /**
     * Query.
     *
     * @return original search string
     */
    @NotNull
    public String getSearchString() {
        return searchString;
    }

    /**
     * Query.
     *
     * @return list of 0 to n top scoring search items
     */
    @NotNull
    public List<Item<T>> getResponseItems() {
        return responseItems;
    }
}

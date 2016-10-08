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

/**
 * Container for quick search instance stats
 */
public class Stats {

    private final int items;
    private final int keywords;
    private final int fragments;

    /**
     * Construct an instance.
     *
     * @param items in the search instance
     * @param keywords in the search instance
     * @param fragments in the search instance
     */
    public Stats(int items, int keywords, int fragments) {
        this.items = items;
        this.keywords = keywords;
        this.fragments = fragments;
    }

    /**
     * Query.
     *
     * @return number of items
     */
    public int getItems() {
        return items;
    }

    /**
     * Query.
     *
     * @return number of keywords
     */
    public int getKeywords() {
        return keywords;
    }

    /**
     * Query.
     *
     * @return fragments in the instance
     */
    public int getFragments() {
        return fragments;
    }
}

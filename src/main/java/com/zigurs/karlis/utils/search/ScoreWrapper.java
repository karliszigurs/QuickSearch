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

/**
 * Internal utility class. Package access for testing purposes.
 * <p>
 * Track item score during result set generation.
 */
public class ScoreWrapper<T> {

    @NotNull
    private final T item;
    private final double score;

    public ScoreWrapper(@NotNull final T item, final double score) {
        this.item = item;
        this.score = score;
    }

    @NotNull
    public T unwrap() {
        return item;
    }

    public double getScore() {
        return score;
    }
}

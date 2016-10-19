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
 * Internal utility class. Package level for testing access.
 * <p>
 * Internally we want to cache possibly heavy hashCode calculations
 * as we'll be juggling the objects between sets a bit.
 */
final class HashWrapper<T> {

    @NotNull
    private final T item;

    private final int hashCode;

    HashWrapper(@NotNull T item) {
        this.item = item;
        hashCode = item.hashCode();
    }

    @NotNull
    T unwrap() {
        return item;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object that) {
        return (that instanceof HashWrapper) && hashCode() == that.hashCode() && item.equals(((HashWrapper) that).unwrap());
    }
}

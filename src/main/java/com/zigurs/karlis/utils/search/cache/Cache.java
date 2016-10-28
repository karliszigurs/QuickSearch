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
package com.zigurs.karlis.utils.search.cache;

import java.util.function.Function;

/**
 * Simple cache interface. Supplies a map from GraphNode key or reads
 * (and possibly caches it) from the specified supplier.
 *
 * @param <K> type of key used in this cache
 * @param <V> corresponding payload returned by this cache
 */
public interface Cache<K, V> {

    V getFromCacheOrSupplier(K rootNode, Function<K, V> supplier);

    void clearCache();

    CacheStatistics getStatistics();

}

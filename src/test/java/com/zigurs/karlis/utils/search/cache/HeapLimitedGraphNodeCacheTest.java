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
package com.zigurs.karlis.utils.search.cache;

import com.zigurs.karlis.utils.search.GraphNode;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class HeapLimitedGraphNodeCacheTest {

    private Cache<GraphNode<String>, Map<String, Double>> cache;

    private final Function<GraphNode<String>, Map<String, Double>> supplierFunction = (keyword) -> {
        Map<String, Double> map = new HashMap<>();

        for (int i = 0; i < 10_000; i++) {
            map.put("entry" + i, (double) i);
        }

        return map;
    };

    @Before
    public void setUp() throws Exception {
        cache = new HeapLimitedGraphNodeCache<>(1024 * 1024);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructFalse() {
        cache = new HeapLimitedGraphNodeCache<>(Integer.MIN_VALUE);
    }

    @Test
    public void queryUncacheable() {
        cache.getFromCacheOrSupplier(new GraphNode<>("verylongname"), supplierFunction);
        assertEquals(1, cache.getStatistics().getUncacheable());
        assertEquals(true, cache.getStatistics().isEnabled());
    }

    @Test
    public void queryCached() {
        cache.getFromCacheOrSupplier(new GraphNode<>("hey"), supplierFunction);
        cache.getFromCacheOrSupplier(new GraphNode<>("hey"), supplierFunction);
        assertEquals(1, cache.getStatistics().getHits());
        assertEquals(true, cache.getStatistics().isEnabled());
    }

    @Test
    public void forceTrimAndDisable() {
        for (int i = 0; i < 27; i++) {
            cache.getFromCacheOrSupplier(new GraphNode<>(String.valueOf('a' + (i % 24))), supplierFunction);
            cache.getFromCacheOrSupplier(new GraphNode<>("a"), supplierFunction);
            cache.getFromCacheOrSupplier(new GraphNode<>("b"), supplierFunction);
            cache.getFromCacheOrSupplier(new GraphNode<>("c"), supplierFunction);
            cache.getFromCacheOrSupplier(new GraphNode<>("d"), supplierFunction);
        }

        assertEquals(0, cache.getStatistics().getSize());
        assertEquals(10, cache.getStatistics().getEvictions());
        assertFalse(cache.getStatistics().isEnabled());
    }
}
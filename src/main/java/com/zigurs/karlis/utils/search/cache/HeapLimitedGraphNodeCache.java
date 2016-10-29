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

import com.zigurs.karlis.utils.search.GraphNode;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;

/**
 * Simple, strong-referenced cache implementation that tries to limit its memory
 * use (based on an educated guess of cost of references).
 * <p>
 * During use it will scale down what it caches every time it hits the set
 * cache limit ultimately disabling (and purging) itself if it keeps hitting the limits.
 */
public class HeapLimitedGraphNodeCache<T> implements Cache<GraphNode<T>, Map<T, Double>> {

    /*
     * Cache map itself. As we are locking over it anyway, no benefit from using concurrent
     * implementations. LinkedHashMap because it offers access order iteration.
     */
    private final Map<String, Map<T, Double>> cache = new LinkedHashMap<>(128, 0.75f, true);

    /*
     * A lock, needed since we are trying to track the contents of the
     * maps, not the count of items in the cache map itself.
     */
    private final StampedLock mapLock = new StampedLock();

    /*
     * Track and manage size
     */
    private final int MAX_ALLOWED_ENTRIES;
    private long currentEntries = 0L;
    private long keyLengthLimit = 10;
    private boolean cacheDisabled = false;

    /*
     * Statistics. Not thread safe, but I'm not
     * too worried about % of stats being missed.
     */
    private long hits = 0;
    private long misses = 0;
    private long uncacheable = 0;
    private long evictions = 0;

    /**
     * Create a cache instance with specified size limit in bytes.
     * <p>
     * Constructor will throw a fit if a size of less than 1 is specified.
     *
     * @param cacheLimitInBytes heap use limit hint
     */
    public HeapLimitedGraphNodeCache(final int cacheLimitInBytes) {
        if (cacheLimitInBytes < 1)
            throw new IllegalArgumentException("Illegal cache size specified");

        /*
         * Average of 60 bytes per entry as empirically measured, may
         * differ slightly in ether direction depending on the exact dataset.
         *
         * Shouldn't depend on the cached object size as we are only
         * adding a reference to it in the original graph tree.
         */
        MAX_ALLOWED_ENTRIES = cacheLimitInBytes / 60;
    }

    /**
     * Look for a result ether from cache or from the supplied function.
     * <p>
     * Supplier of the function should consider that it may (and will) be read concurrently by multiple
     * threads, repeatedly for the same value and there is no guarantee that a content returned to
     * function invocation will be the same references that will be returned to the original caller.
     *
     * @param node     key node
     * @param supplier function to invoke for result if no hits are found in cache
     * @return result ether from cache or from the supplied function
     */
    @Override
    @NotNull
    public Map<T, Double> getFromCacheOrSupplier(@NotNull final GraphNode<T> node,
                                                 @NotNull final Function<GraphNode<T>, Map<T, Double>> supplier) {
        boolean cacheable;

        /*
         * Optimistic case, try to read from cache and return immediately
         */
        long readStamp = mapLock.readLock();
        try {
            cacheable = isCacheable(node.getFragment());
            if (cacheable) {
                Map<T, Double> cached = cache.get(node.getFragment());

                if (cached != null) {
                    hits++;
                    return cached;
                }
            }
        } finally {
            mapLock.unlockRead(readStamp);
        }

        /*
         * If we reach here there was ether a cache miss or
         * the keyword is too long to be cached.
         *
         * First - see if we should just pass the call through...
         */
        if (!cacheable) {
            uncacheable++;
            return supplier.apply(node);
        }

        /*
         * That wasn't it. We need to try to write into the cache...
         */

        long writeStamp = mapLock.writeLock();
        try {
            misses++;
            Map<T, Double> cached = cache.get(node.getFragment());

            /*
             * Another thread already populated it, we can return
             */
            if (cached != null) {
                hits++;
                return cached;
            }

            /*
             * Continue to populate...
             */
            Map<T, Double> newResult = Collections.unmodifiableMap(supplier.apply(node));

            cache.put(node.getFragment(), newResult);

            currentEntries += newResult.size();

            if (currentEntries > MAX_ALLOWED_ENTRIES)
                trimCache();

            return newResult;
        } finally {
            mapLock.unlockWrite(writeStamp);
        }
    }

    private boolean isCacheable(@NotNull final String key) {
        return !cacheDisabled && key.length() <= keyLengthLimit;
    }

    // Called only after write lock has been acquired
    private void trimCache() {
        /*
         * We have burst the limit with the current key length,
         * scale back what we cache (or disable cache if already
         * at the shortest keys).
         */
        if (keyLengthLimit > 0) {
            keyLengthLimit--;
            cacheDisabled = keyLengthLimit < 1;

            if (cacheDisabled) {
                cache.clear(); // DO NOT call clearCache() here, lock may be non-re-entrant
                currentEntries = 0L;
                return;
            }
        }

        /*
         * We just want to check things at the end of the queue,
         * so that least accessed items could be trimmed.
         */
        Deque<Map.Entry<String, Map<T, Double>>> stack = new LinkedBlockingDeque<>(cache.size());
        cache.entrySet().forEach(stack::push);

        while (currentEntries > MAX_ALLOWED_ENTRIES) {
            Map.Entry<String, Map<T, Double>> entry = stack.pop();
            cache.remove(entry.getKey());
            currentEntries -= entry.getValue().size();
            evictions++;
        }

        /*
         * Also clear the cache of all elements that are
         * now over the threshold of cacheable.
         */
        stack.forEach(entry -> {
            if (!isCacheable(entry.getKey())) {
                cache.remove(entry.getKey());
                currentEntries -= entry.getValue().size();
                evictions++;
            }
        });
    }

    @Override
    public void clearCache() {
        long stamp = mapLock.readLock();
        try {
            if (!cache.isEmpty()) {
                stamp = mapLock.tryConvertToWriteLock(stamp);

                if (stamp == 0L)
                    stamp = mapLock.writeLock();

                cache.clear();
                currentEntries = 0L;
            }
        } finally {
            mapLock.unlock(stamp);
        }
    }

    @Override
    public CacheStatistics getStatistics() {
        return new CacheStatistics() {
            @Override
            public long getHits() {
                return hits;
            }

            @Override
            public long getMisses() {
                return misses;
            }

            @Override
            public long getUncacheable() {
                return uncacheable;
            }

            @Override
            public long getEvictions() {
                return evictions;
            }

            @Override
            public boolean isEnabled() {
                return !cacheDisabled;
            }

            @Override
            public long getSize() {
                return currentEntries;
            }
        };
    }
}

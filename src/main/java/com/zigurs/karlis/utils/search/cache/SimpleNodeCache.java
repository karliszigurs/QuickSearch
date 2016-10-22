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

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;

/**
 * Simple, adaptive-ish cache that will scale back what it caches each time it hits the
 * limit it is allowed to cache.
 * <p>
 * It will eventually disable itself if it finds itself unable to cache even the shortest keys
 * to avoid trashing the cache on every request.
 */
public class SimpleNodeCache<T> implements Cache<T> {

    /*
     * LinkedHashMap to take advantage of the last accessed functionality.
     * Might be replaced in the future, as it's fairly heavyweight code in there...
     */
    private final Map<String, Map<T, Double>> cache = new LinkedHashMap<>(128, 0.75f, true);

    /*
     * And a lock, as we are using a thread-unsafe map implementation.
     */
    private final StampedLock mapLock = new StampedLock();

    /*
     * Track and manage size
     */
    private final int MAX_ALLOWED_ENTRIES;
    private final AtomicLong currentEntries = new AtomicLong(0);
    private long keyLengthLimit = 10;
    private boolean cacheDisabled = false;

    /*
     * Statistics. Not thread safe, but I'm not
     * too worried about any recording being missed.
     */
    private long hits = 0;
    private long misses = 0;
    private long uncacheable = 0;
    private long evictions = 0;

    public SimpleNodeCache(final int cacheLimitInBytes) {
        /*
         * Average of 60 bytes per entry as empirically measured, may
         * differ slightly in ether direction depending on the exact dataset.
         *
         * Shouldn't depend on the cached object size as we are only
         * adding a reference to it in the original graph tree.
         */
        MAX_ALLOWED_ENTRIES = cacheLimitInBytes / 60;
    }

    @Override
    public Map<T, Double> getFromCacheOrSupplier(@NotNull final GraphNode<T> node,
                                                 @NotNull final Function<GraphNode<T>, Map<T, Double>> supplier) {
        // this isn't quite proper thread safe yet...
        if (!cacheDisabled && isCacheable(node.getFragment())) {
            Map<T, Double> cached;
            long readLock = mapLock.readLock();
            try {
                cached = cache.get(node.getFragment());
            } finally {
                mapLock.unlockRead(readLock);
            }

            if (cached == null) {
                misses++;
                cached = supplier.apply(node);
                long writeLock = mapLock.writeLock();
                try {
                    cache.put(node.getFragment(), new HashMap<>(cached));
                    currentEntries.addAndGet(cached.size());
                    checkAndTrimToSize();
                } finally {
                    mapLock.unlockWrite(writeLock);
                }
            } else {
                hits++;
            }

            return cached;
        } else {
            uncacheable++;
            return supplier.apply(node);
        }
    }

    private void checkAndTrimToSize() {
        if (currentEntries.get() > MAX_ALLOWED_ENTRIES) {
            /*
             * We have burst the limit with the current key length,
             * scale back what we cache (or disable cache if already
             * at the shortest keys).
             */
            boolean trimmedAllowedSize = false;

            if (keyLengthLimit > 0) {
                keyLengthLimit--;
                cacheDisabled = keyLengthLimit < 1;
                trimmedAllowedSize = true;
            }

            /*
             * We just want to check things at the end of the queue,
             * so that least accessed items could be trimmed.
             */
            Deque<Map.Entry<String, Map<T, Double>>> stack = new ArrayDeque<>(cache.size());
            cache.entrySet().forEach(stack::push);

            while (currentEntries.get() > MAX_ALLOWED_ENTRIES) {
                Map.Entry<String, Map<T, Double>> entry = stack.pop();
                cache.remove(entry.getKey());
                currentEntries.addAndGet(-entry.getValue().size());
                evictions++;
            }

            /*
             * Also clear the cache of all elements that are
             * now under the threshold of cacheable.
             */
            while (trimmedAllowedSize && !stack.isEmpty()) {
                Map.Entry<String, Map<T, Double>> entry = stack.pop();
                if (!isCacheable(entry.getKey())) {
                    cache.remove(entry.getKey());
                    currentEntries.addAndGet(-entry.getValue().size());
                    evictions++;
                }
            }
        }
    }

    private boolean isCacheable(@NotNull final String key) {
        return key.length() <= keyLengthLimit;
    }

    @Override
    public void clear() {
        long writeLock = mapLock.writeLock();
        try {
            if (!cache.isEmpty() || currentEntries.get() > 0) {
                cache.clear();
                currentEntries.set(0);
            }
        } finally {
            mapLock.unlockWrite(writeLock);
        }
    }

    @Override
    public String getCacheStats() {
        return String.format("{ \"hits\": %d, " +
                        "\"misses\": %d, " +
                        "\"uncacheable\": %d, " +
                        "\"evictions\": %d, " +
                        "\"size\": %d, " +
                        "\"keysCached\": %d, " +
                        "\"maxSize\": %d, " +
                        "\"disabled\": %b, " +
                        "\"keyLimit\": %d }",
                hits,
                misses,
                uncacheable,
                evictions,
                currentEntries.get(),
                cache.size(),
                MAX_ALLOWED_ENTRIES,
                keyLengthLimit < 1,
                keyLengthLimit);
    }
}

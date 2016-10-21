package com.zigurs.karlis.utils.search.cache;

import com.zigurs.karlis.utils.search.GraphNode;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Simple, semi-adaptive cache that will scale back what it caches each time it hits the
 * limit it is allowed to cache.
 */
public class NodeTreeCache<T> implements Cache<T> {

    private final Map<String, Map<T, Double>> cache = new ConcurrentHashMap<>();

    private final int maxAllowedEntries;

    private long currentEntries = 0;
    private long currentCacheLimit = 5;

    public NodeTreeCache(int cacheLimitInBytes) {
        maxAllowedEntries = cacheLimitInBytes / 200;
    }

    @Override
    public Map<T, Double> apply(@NotNull GraphNode<T> node,
                                @NotNull Function<GraphNode<T>, Map<T, Double>> supplier) {
        if (isCacheable(node.getFragment())) {
            Map<T, Double> cached = cache.get(node.getFragment());

            if (cached == null) {
                cached = supplier.apply(node);
                cache.put(node.getFragment(), new HashMap<>(cached));
                currentEntries += cached.size();

                if (currentEntries > maxAllowedEntries) {
                    trimCache();
                    if (currentCacheLimit > 0) {
                        currentCacheLimit--;
                    }
                }
            }

            return cached;
        } else {
            return supplier.apply(node);
        }
    }

    private boolean isCacheable(String key) {
        return key.length() <= currentCacheLimit;
    }

    private void trimCache() {
        if (currentEntries > 0) {
            cache.keySet().stream()
                    .sorted(Comparator.reverseOrder())
                    .forEach(key -> {
                        if (currentEntries > maxAllowedEntries) {
                            System.out.println("Removing " + key);
                            Map<T, Double> map = cache.get(key);
                            currentEntries -= map.size();
                            cache.remove(key);
                        }
                    });
        }
    }

    @Override
    public Object call() throws Exception {
        clear();
        return null;
    }

    public void clear() {
        if (currentEntries > 0) {
            cache.clear();
            currentEntries = 0;
        }
    }
}

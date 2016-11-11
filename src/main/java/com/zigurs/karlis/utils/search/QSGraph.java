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
package com.zigurs.karlis.utils.search;

import com.zigurs.karlis.utils.search.cache.Cache;
import com.zigurs.karlis.utils.search.cache.CacheStatistics;
import com.zigurs.karlis.utils.search.cache.HeapLimitedGraphNodeCache;
import com.zigurs.karlis.utils.search.model.Stats;

import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiFunction;

public class QSGraph<T> {

    /**
     * Cache of visited items for any particular graph node.
     */
    private final Cache<GraphNode<T>, Map<T, Double>> cache;

    /**
     * Map providing quick entry point to a particular node in the graph.
     */
    private final Map<String, GraphNode<T>> fragmentsNodesMap = new HashMap<>();

    /**
     * Mapping between an item and the keywords associated with it. Both a helper
     * and a requirement to unmap nodes upon item removal.
     */
    private final Map<T, ImmutableSet<String>> itemKeywordsMap = new HashMap<>();

    /**
     * Stamped lock governing access to the graph modifying functions.
     */
    private final StampedLock stampedLock = new StampedLock();

    /**
     * Construct an instance with a specified cache limit - 0 for disabled,
     * -1 for unlimited, positive integer for target cache size limit in bytes.
     *
     * @param cacheLimit max cache size limit in bytes
     */
    public QSGraph(int cacheLimit) {
        if (cacheLimit > 0)
            cache = new HeapLimitedGraphNodeCache<>(cacheLimit);
        else
            cache = null;
    }

    /*
     * Public interface
     */

    /**
     * Add an item to the graph and construct graph nodes for the specified keywords.
     * <p>
     * If the corresponding nodes already exist, the item will simply be added as a leaf.
     *
     * @param item             item to add
     * @param suppliedKeywords keywords to construct graph for
     */
    public void registerItem(final T item,
                             final Set<String> suppliedKeywords) {
        long writeLock = stampedLock.writeLock();
        try {
            suppliedKeywords.forEach(keyword -> createAndRegisterNode(null, keyword, item));

            if (itemKeywordsMap.containsKey(item))
                itemKeywordsMap.put(item, ImmutableSet.mergeCollections(itemKeywordsMap.get(item), suppliedKeywords));
            else
                itemKeywordsMap.put(item, ImmutableSet.fromCollection(suppliedKeywords));

            clearCache();
        } finally {
            stampedLock.unlockWrite(writeLock);
        }
    }

    /**
     * Remove an item from the map and remove any node mappings that become empty
     * upon the items removal (determined using stored associated keywords of the item).
     *
     * @param item item to remove
     */
    public void unregisterItem(final T item) {
        long writeLock = stampedLock.writeLock();
        try {
            if (itemKeywordsMap.containsKey(item)) {
                for (String keyword : itemKeywordsMap.get(item)) {
                    GraphNode<T> keywordNode = fragmentsNodesMap.get(keyword);

                    keywordNode.removeItem(item);

                    if (keywordNode.getItems().isEmpty())
                        removeEdge(keywordNode, null);
                }
            }

            itemKeywordsMap.remove(item);
            clearCache();
        } finally {
            stampedLock.unlockWrite(writeLock);
        }
    }

    /**
     * Walk the graph accumulating encountered items in the map with the highest score
     * (according to the supplied scoring {@code BiFunction<String, String, Double>})
     * encountered at any visit to an item.
     *
     * @param fragment       keyword or keyword fragment to start walk from
     * @param scorerFunction function that will be called with the supplied fragment and node identity to score match
     * @return map of accumulated items with their highest score encountered during walk (may be empty)
     */
    public Map<T, Double> walkAndScore(final String fragment,
                                       final BiFunction<String, String, Double> scorerFunction) {
        long readLock = stampedLock.readLock();
        try {

            GraphNode<T> root = fragmentsNodesMap.get(fragment);

            if (root == null)
                return Collections.emptyMap();
            else
                return walkAndScore(root, scorerFunction);

        } finally {
            stampedLock.unlockRead(readLock);
        }
    }

    /**
     * Retrieve the stored keywords set associated with the item
     * (or empty set if the item mapping is not recognized).
     * <p>
     * Warning - there's a good chance that the returned set will be
     * immutable in some form or another, caller should make a copy
     * before operating on it.
     *
     * @param item previously registered item
     * @return set of associated keywords
     */
    public Set<String> getItemKeywords(T item) {
        // TODO - can we safely avoid locking here? I think we can...
        ImmutableSet<String> keywords = itemKeywordsMap.get(item);

        if (keywords == null)
            return Collections.emptySet();

        return keywords;
    }

    /**
     * Clear this graph. Resets the graph to a virgin state as if it has been
     * just constructed.
     */
    public void clear() {
        long writeLock = stampedLock.writeLock();
        try {
            fragmentsNodesMap.clear();
            itemKeywordsMap.clear();
            clearCache();
        } finally {
            stampedLock.unlockWrite(writeLock);
        }
    }

    /**
     * Retrieve some basic statistics about the size of this graph.
     *
     * @return stats object containing sizes of internal collections
     */
    public Stats getStats() {
        // TODO - again, ignoring locking here
        return new Stats(
                itemKeywordsMap.size(),
                fragmentsNodesMap.size()
        );
    }

    /**
     * Retrieve some basic cache statistics if cache is enabled, empty {@link Optional} otherwise.
     *
     * @return {@link Optional} of {@link CacheStatistics} if cache is active
     */
    public Optional<CacheStatistics> getCacheStats() {
        // TODO - again, ignoring locking here
        if (cache != null)
            return Optional.of(cache.getStats());
        else
            return Optional.empty();
    }

    /**
     * Helper function to enable callers to understand if it's safe to mangle
     * maps returned from the graph (if cache is disabled).
     * <p>
     * TODO - horrible, horrible, get rid of this!
     *
     * @return true if the cache is enabled
     */
    public boolean isCacheEnabled() {
        return cache != null;
    }

    /*
     * Implementation code
     */

    private void clearCache() {
        if (cache != null)
            cache.clear();
    }

    private void createAndRegisterNode(final GraphNode<T> parent,
                                       final String identity,
                                       final T item) {
        GraphNode<T> node = fragmentsNodesMap.get(identity);

        if (node == null) {
            final String internedIdentity = identity.intern();

            node = new GraphNode<>(internedIdentity);
            fragmentsNodesMap.put(internedIdentity, node);

            // And proceed to add child nodes
            if (node.getFragment().length() > 1) {
                createAndRegisterNode(node, internedIdentity.substring(0, identity.length() - 1), null);
                createAndRegisterNode(node, internedIdentity.substring(1), null);
            }
        }

        if (item != null)
            node.addItem(item);

        if (parent != null)
            node.addParent(parent);
    }

    private void removeEdge(final GraphNode<T> node,
                            final GraphNode<T> parent) {
        if (node == null) //already removed
            return;

        if (parent != null)
            node.removeParent(parent);

        // No getParents or getItems means that there's nothing here to find, proceed onwards
        if (node.getParents().isEmpty() && node.getItems().isEmpty()) {
            fragmentsNodesMap.remove(node.getFragment());

            if (node.getFragment().length() > 1) {
                removeEdge(fragmentsNodesMap.get(node.getFragment().substring(0, node.getFragment().length() - 1)), node);
                removeEdge(fragmentsNodesMap.get(node.getFragment().substring(1)), node);
            }
        }
    }

    /*
     * Graph walking
     */

    private Map<T, Double> walkAndScore(final GraphNode<T> root,
                                        final BiFunction<String, String, Double> scorerFunction) {
        final Map<T, Double> accumulator = new LinkedHashMap<>(root.getItemsSizeHint() > 0 ? root.getItemsSizeHint() : 16);
        final Set<String> visitsTracker = new HashSet<>(root.getNodesSizeHint() > 0 ? root.getNodesSizeHint() : 16);

        Map<T, Double> result;

        if (cache != null)
            result = cache.getFromCacheOrSupplier(root, rootNode -> walkAndScore(rootNode.getFragment(), rootNode, accumulator, visitsTracker, scorerFunction));
        else
            result = walkAndScore(root.getFragment(), root, accumulator, visitsTracker, scorerFunction);

        /* Store size hints to prevent rehash operations on repeat visits */
        root.setItemsSizeHint(result.size());
        root.setNodesSizeHint(visitsTracker.size());

        return result;
    }

    private Map<T, Double> walkAndScore(final String originalFragment,
                                        final GraphNode<T> node,
                                        final Map<T, Double> accumulated,
                                        final Set<String> visited,
                                        final BiFunction<String, String, Double> keywordMatchScorer) {
        visited.add(node.getFragment());

        if (!node.getItems().isEmpty()) {
            Double score = keywordMatchScorer.apply(originalFragment, node.getFragment());
            node.getItems().forEach(item -> accumulated.merge(item, score, (d1, d2) -> d1.compareTo(d2) > 0 ? d1 : d2));
        }

        node.getParents().forEach(parent -> {
            if (!visited.contains(parent.getFragment())) {
                walkAndScore(originalFragment, parent, accumulated, visited, keywordMatchScorer);
            }
        });

        return accumulated;
    }
}

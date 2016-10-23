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

import com.zigurs.karlis.utils.search.cache.Cache;
import com.zigurs.karlis.utils.search.cache.SimpleNodeCache;
import com.zigurs.karlis.utils.search.model.Item;
import com.zigurs.karlis.utils.search.model.Result;
import com.zigurs.karlis.utils.search.model.Stats;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zigurs.karlis.utils.search.QuickSearch.ACCUMULATION_POLICY.UNION;
import static com.zigurs.karlis.utils.search.QuickSearch.UNMATCHED_POLICY.BACKTRACKING;

/**
 * Simple and lightweight in-memory quick search provider.
 * <p>
 * Fit for low latency querying of small to medium sized datasets (limited by memory) to enable users
 * immediately see the top hits for their partially entered search string. Based on production experience
 * this approach is well perceived by users and their ability to see the top hits immediately allows
 * them to adjust their queries on the fly getting to the desired result faster.
 * <p>
 * By implementing this functionality directly in the app or corresponding backend the overall complexity of the
 * project can be significantly reduced - there is no need to care about maintaining search infrastructure, servers,
 * software or APIs.
 * <p>
 * Example uses can include:
 * <ul>
 * <li>Selecting from a list of existing contacts</li>
 * <li>Looking for a particular city (associating it with known aliases, landmarks, state, etc)</li>
 * <li>Searching for an item in an online (book) shop</li>
 * <li>Used in background to highlight items that match the (partial) keywords entered. A.la. OSX System Preferences search</li>
 * <li>Navigating large navigation trees, in example all sporting events for a year</li>
 * </ul>
 * <p>
 * Typical use case would be including it in ether application or a web server, maintaining the
 * data set (ether via provided add and remove methods or by clearing and repopulating the search contents
 * completely) and exposing an API to user that accepts a free-form input and returns corresponding matching items.
 * <p>
 * Each entry is associated with a number of keywords that are not exposed to user, therefore it is possible to add
 * name aliases or item class descriptions to keywords. Same applies to letting users discover items by unique identifiers
 * or alternate spellings.
 * <p>
 * An example contacts list is provided as example (entry followed by assigned keywords):
 * <table summary="">
 * <tr><th>Item</th><th>Supplied keywords</th></tr>
 * <tr><td>"Jane Doe, 1234"</td><td>"Jane Doe Marketing Manager SEO Community MySpace 1234"</td></tr>
 * <tr><td>"Alice Stuggard, 9473"</td><td>"Alice Stuggard Tech Cryptography Manager RSA 9473"</td></tr>
 * <tr><td>"Robert Howard, 6866"</td><td>"Robert Bob Howard Tech Necromancy Summoning Undead Cryptography BOFH RSA DOD Laundry 6866"</td></tr>
 * <tr><td>"Eve Moneypenny, 9223"</td><td>"Eve Moneypenny Accounting Manager Q OSA 9223"</td></tr>
 * </table>
 * <p>
 * In the example above if the user enters <code><strong>"Mana"</strong></code> he will be served a list of Jane,
 * Alice and Eve as their keyword <code><strong>"Manager"</strong></code> is matched by
 * <code><strong>"Mana"</strong></code>. Now user should see that the result set is sufficiently narrow and
 * can tailor his search further by continuing on to type <code><strong>"Mana a"</strong></code> - which will lead
 * to Alice and Eve being promoted to top of results. Alice because of her name match and Eve because of her department.
 * <code><strong>"Mana acc"</strong></code> will narrow the results to Eve only as she is only one in the search set
 * that can match both <code><strong>*mana*</strong></code> and <code><strong>*acc*</strong></code>.
 * <p>
 * Example use:
 * <p>
 * <code>QuickSearch&lt;String&gt; qs = new QuickSearch&lt;&gt;();<br>
 * qs.addItem("Villain", "Roy Batty Lord Voldemort Colonel Kurtz");<br>
 * qs.addItem("Hero", "Walt Kowalksi Jake Blues Shaun");<br>
 * System.out.println(qs.findItem("walk")); // finds "Hero"</code>
 * <p>
 * Concurrency - This class is thread safe. Implementation is completely passive
 * and can be deployed horizontally as identical datasets will produce identical search results.
 *
 * @author Karlis Zigurs, 2016
 */
public class QuickSearch<T> {

    /**
     * Matching policy to apply to unmatched keywords. In case of EXACT only
     * exact supplied keyword matches will be considered, in case of BACKTRACKING
     * any keywords with no matches will be incrementally shortened until first
     * candidate match is found (e.g. supplied 'terminal' will be shortened until it
     * reaches 'ter' where it can match against 'terra').
     */
    public enum UNMATCHED_POLICY {
        EXACT, BACKTRACKING
    }

    /**
     * If multiple keywords are supplied select strategy to accumulate result set.
     * <p>
     * UNION will consider all items found for each keyword in the result,
     * INTERSECTION will consider only items that are matched by all the supplied
     * keywords.
     * <p>
     * INTERSECTION is significantly more performant as it discards
     * candidates as early as possible.
     */
    public enum ACCUMULATION_POLICY {
        UNION, INTERSECTION
    }

    /**
     * Function to 'clean up' supplied keyword and user input strings. We assume that the input is
     * going to be ether free form or malformed, therefore this allows to apply required actions to generate
     * a 'clean' set of keywords from the input string.
     * <p>
     * In example both "one two,three-four" and "one$two%three^four" as inputs will produce
     * set of 4 strings [one,two,three,four] on the output.
     */
    public static final Function<String, Set<String>> DEFAULT_KEYWORDS_EXTRACTOR =
            s -> Arrays.stream(s.replaceAll("[^\\w]+", " ").split("[\\s]+")).collect(Collectors.toSet());

    /**
     * Function to sanitize search keywords before using them internally. Applied to both keywords
     * supplied with items and to user input before performing search.
     * <p>
     * Rationale is to allow somewhat relaxed free-form text input (e.g. phone devices automatically capitalising
     * entered keywords) and extra capability to remap special characters to their latin alphabet equivalents.
     * <p>
     * The normalized representation has no specific requirements, this is just a convenience method.
     * Simply returning the supplied string will mean that the search results contain only exact (and case
     * sensitive) matches. It is also possible to return empty strings here, in which case the supplied
     * keyword will be ignored.
     * <p>
     * Example transformations:
     * <table summary="">
     * <tr><th>Original</th><th>Transformed</th><th>Reason</th></tr>
     * <tr><td><code>"New York"</code></td><td><code>"new york"</code></td><td>remove upper case</td></tr>
     * <tr><td><code>"Pythøn"</code></td><td><code>"python"</code></td><td>replace special characters</td></tr>
     * <tr><td><code>"HERMSGERVØRDENBRØTBØRDA"</code></td><td><code>"hermsgervordenbrotborda"</code></td><td>it could happen...</td></tr>
     * <tr><td><code>"Россия"</code></td><td><code>"rossiya"</code></td><td>translate cyrilic alphabet to latin</td></tr>
     * </table>
     * <p>
     * Default implementation assumes that String.trim().toLowerCase() is sufficient.
     */
    public static final Function<String, String> DEFAULT_KEYWORD_NORMALIZER = s -> s.trim().toLowerCase();

    /**
     * Function scoring user supplied input against corresponding keywords associated with search items.
     * <p>
     * An example invocations might request to compare <code><strong>"swe"</strong></code> against
     * <code><strong>"sweater"</strong></code> or <code><strong>"count"</strong></code> against
     * <code><strong>"accounting"</strong></code>.
     * <p>
     * Default implementation returns the ratio between search term and keyword lengths with additional boost
     * if the search term matches beginning of the keyword.
     * <p>
     * In example, while matching user input against known keyword "password", the following will be calculated:
     * <ul>
     * <li>Input "pa" -&gt; low match (0.25), but boosted (+1) due to matching start of the keyword.</li>
     * <li>Input "swo" -&gt; low match (0.37), not boosted</li>
     * <li>Input "assword" -&gt; high match (0.87), not boosted</li>
     * <li>Input "password" -&gt; high match (1), also boosted by matching the beginning of the line (+1)</li>
     * </ul>
     * <p>
     * All keywords supplied by user are scored against all matching keywords associated with a searchable item.
     * Items rank in the results is determined by the sum of all score results.
     */
    public static final BiFunction<String, String, Double> DEFAULT_MATCH_SCORER = (keywordMatch, keyword) -> {
        double matchScore = (double) keywordMatch.length() / (double) keyword.length(); // reaches maximum if lengths match (both are identical)

        // bonus boost for start of term
        if (keyword.startsWith(keywordMatch))
            matchScore += 1.0;

        return matchScore;
    };

    /*
     * Instance properties
     */

    @NotNull
    private final BiFunction<String, String, Double> keywordMatchScorer;
    @NotNull
    private final Function<String, String> keywordNormalizer;
    @NotNull
    private final Function<String, Set<String>> keywordsExtractor;
    @NotNull
    private final UNMATCHED_POLICY unmatchedPolicy;
    @NotNull
    private final QuickSearch.ACCUMULATION_POLICY accumulationPolicy;
    @NotNull
    private final Map<String, GraphNode<T>> fragmentsItemsTree = new HashMap<>();
    @NotNull
    private final Map<T, ImmutableSet<String>> itemKeywordsMap = new HashMap<>();
    @NotNull
    private final StampedLock lock = new StampedLock();
    @Nullable
    private final Cache<T> cache;

    /**
     * Private constructor, use builder instead.
     *
     * @param builder supplies configuration
     */
    private QuickSearch(@NotNull final QuickSearchBuilder builder) {
        keywordsExtractor = builder.keywordsExtractor;
        keywordNormalizer = builder.keywordNormalizer;
        keywordMatchScorer = builder.keywordMatchScorer;

        Objects.requireNonNull(keywordsExtractor);
        Objects.requireNonNull(keywordNormalizer);
        Objects.requireNonNull(keywordMatchScorer);

        unmatchedPolicy = builder.unmatchedPolicy;
        accumulationPolicy = builder.accumulationPolicy;

        Objects.requireNonNull(unmatchedPolicy);
        Objects.requireNonNull(accumulationPolicy);

        /*
         * Quick sanity check on the supplied functions to ensure
         * they confirm to behavior expected internally.
         */

        testKeywordsExtractorFunction(keywordsExtractor);
        testKeywordNormalizerFunction(keywordNormalizer);
        testKeywordMatchScorerFunction(keywordMatchScorer);

        /*
         * Wire in cache interceptors, if supplied. Otherwise
         * provide internal supplier and null clearer.
         */

        if (builder.cacheLimit > 0)
            this.cache = new SimpleNodeCache<>(builder.cacheLimit);
        else
            cache = null;
    }

    /*
     * Public interface
     */

    /**
     * Add an item with corresponding keywords, e.g. an online store item Shoe with
     * keywords <code><strong>"Shoe Red 10 Converse cheap free"</strong></code>.
     * <p>
     * You can expand the keywords stored against an item by adding it again with extra keywords.
     * If the item is already in the database any new keywords will be mapped to it.
     *
     * @param item     Item to return for search results
     * @param keywords Arbitrary list of keywords separated by space, comma, special characters, freeform text...
     * @return True if the item was added, false if no keywords to map against the item were found (therefore item was not added)
     */
    public boolean addItem(@Nullable final T item, @Nullable final String keywords) {
        if (item == null || keywords == null || keywords.isEmpty())
            return false;

        Set<String> keywordsSet = prepareKeywords(keywords, true);

        if (keywordsSet.isEmpty())
            return false;

        long writeLock = lock.writeLock();
        try {
            addItemImpl(item, keywordsSet);
        } finally {
            lock.unlockWrite(writeLock);
        }
        return true;
    }

    /**
     * Remove an item, if it exists. Calling this method ensures that specified item
     * and any keywords it was associated with is gone.
     * <p>
     * Or it does nothing if no such item was present.
     *
     * @param item Item to remove
     */
    public void removeItem(@Nullable final T item) {
        if (item == null)
            return;

        long writeLock = lock.writeLock();
        try {
            removeItemImpl(item);
        } finally {
            lock.unlockWrite(writeLock);
        }
    }

    /**
     * Find top matching item for the supplied search string
     *
     * @param searchString Raw search string
     * @return Optional containing (or not) the top scoring item
     */
    @NotNull
    public Optional<T> findItem(@Nullable final String searchString) {
        if (isInvalidRequest(searchString, 1))
            return Optional.empty();

        Set<String> searchKeywords = prepareKeywords(searchString, false);

        if (searchKeywords.isEmpty())
            return Optional.empty();

        List<ScoreWrapper<T>> results;

        long readLock = lock.readLock();
        try {
            results = findItemsImpl(searchKeywords, 1);
        } finally {
            lock.unlockRead(readLock);
        }

        if (results.isEmpty())
            return Optional.empty();
        else
            return Optional.of(results.get(0).unwrap());
    }

    /**
     * Find top n items matching the supplied search string. Supplied string will be processed by
     * keyword extracting and normalizing functions before used for search.
     *
     * @param searchString     Raw search string, e.g. "new york pizza"
     * @param numberOfTopItems Number of items the returned result should be limited to
     * @return List of 0 to numberOfTopItems elements
     */
    @NotNull
    public List<T> findItems(@Nullable final String searchString, final int numberOfTopItems) {
        if (isInvalidRequest(searchString, numberOfTopItems))
            return Collections.emptyList();

        Set<String> searchKeywords = prepareKeywords(searchString, false);

        if (searchKeywords.isEmpty())
            return Collections.emptyList();

        List<ScoreWrapper<T>> results;

        long readLock = lock.readLock();
        try {
            results = findItemsImpl(searchKeywords, numberOfTopItems);
        } finally {
            lock.unlockRead(readLock);
        }

        if (results.isEmpty()) {
            return Collections.emptyList();
        } else {
            return results.stream()
                    .map(ScoreWrapper::unwrap)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Find top matching item for the supplied search string and return it
     * wrapped in the augumented response object.
     *
     * @param searchString Raw search string
     * @return Possibly empty Optional wrapping item, keywords and score
     */
    @NotNull
    public Optional<Item<T>> findItemWithDetail(@Nullable final String searchString) {
        if (isInvalidRequest(searchString, 1))
            return Optional.empty();

        Set<String> searchKeywords = prepareKeywords(searchString, false);

        if (searchKeywords.isEmpty())
            return Optional.empty();


        long readLock = lock.readLock();
        try {
            List<ScoreWrapper<T>> results = findItemsImpl(searchKeywords, 1);

            if (results.isEmpty()) {
                return Optional.empty();
            } else {
                ScoreWrapper<T> w = results.get(0);
                return Optional.of(
                        new Item<>(
                                w.unwrap(),
                                itemKeywordsMap.get(w.unwrap()).safeCopy(),
                                w.getScore()
                        )
                );
            }
        } finally {
            lock.unlockRead(readLock);
        }
    }

    /**
     * Request an augumented result containing the search string, scores for all items
     * and list of keywords matched (can be used to provide hints to user).
     *
     * @param searchString     Raw search string, e.g. "new york pizza"
     * @param numberOfTopItems Number of items the result should be limited to
     * @return Result object containing 0 to n top scoring items and corresponding metadata
     */
    @NotNull
    public Result<T> findItemsWithDetail(@Nullable final String searchString, final int numberOfTopItems) {
        if (isInvalidRequest(searchString, numberOfTopItems))
            return new Result<>(searchString != null ? searchString : "", Collections.emptyList());

        Set<String> searchKeywords = prepareKeywords(searchString, false);

        if (searchKeywords.isEmpty())
            return new Result<>(searchString, Collections.emptyList());

        long readLock = lock.readLock();
        try {
            List<ScoreWrapper<T>> results = findItemsImpl(searchKeywords, numberOfTopItems);

            if (results.isEmpty()) {
                return new Result<>(searchString, Collections.emptyList());
            } else {
                // Could be moved out of locked block if it wasn't for the keywords lookup...
                return new Result<>(
                        searchString,
                        results.stream()
                                .map(i -> new Item<>(
                                        i.unwrap(),
                                        itemKeywordsMap.get(i.unwrap()).safeCopy(),
                                        i.getScore())
                                )
                                .collect(Collectors.toList())
                );
            }
        } finally {
            lock.unlockRead(readLock);
        }
    }

    /**
     * Clear the search database.
     */
    public void clear() {
        long writeLock = lock.writeLock();
        try {
            fragmentsItemsTree.clear();
            itemKeywordsMap.clear();
            clearCache();
        } finally {
            lock.unlockWrite(writeLock);
        }
    }

    /**
     * Returns an overview of contained maps sizes.
     *
     * @return stats listing number of items, keywords and fragments known
     */
    @NotNull
    public Stats getStats() {
        Stats stats;

        long readLock = lock.readLock();
        try {
            stats = new Stats(
                    itemKeywordsMap.size(),
                    fragmentsItemsTree.size()
            );
        } finally {
            lock.unlockRead(readLock);
        }

        return stats;
    }

    public String getCacheStats() {
        return cache != null ? cache.getCacheStats() : "";
    }

    /*
     * Implementation methods
     */

    private boolean isInvalidRequest(@Nullable final String searchString, final int numItems) {
        return searchString == null || searchString.isEmpty() || numItems < 1;
    }

    @NotNull
    private List<ScoreWrapper<T>> findItemsImpl(@NotNull final Set<String> searchKeywords,
                                                final int maxItemsToList) {
        Map<T, Double> matches = findAndScoreImpl(searchKeywords);

        if (matches.size() > maxItemsToList) {
            /*
             * Use custom sort if the candidates list is larger than number of items we
             * need to report back. On large sets of results this can bring notable
             * improvements in speed when compared to built-in sorting methods.
             */
            return PartialSorter.sortAndLimit(matches.entrySet(), maxItemsToList, (o1, o2) -> o1.getValue().compareTo(o2.getValue()) < 0 ? 1 : -1).stream()
                    .map(e -> new ScoreWrapper<>(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        } else {
            return matches.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(maxItemsToList)
                    .map(e -> new ScoreWrapper<>(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }
    }

    @NotNull
    private Map<T, Double> findAndScoreImpl(@NotNull final Set<String> suppliedFragments) {
        if (accumulationPolicy == UNION)
            return findAndScoreUnionImpl(suppliedFragments);
        else // implied (withAccumulationPolicy == INTERSECTION)
            return findAndScoreIntersectionImpl(suppliedFragments);
    }

    @NotNull
    private Map<T, Double> findAndScoreUnionImpl(@NotNull final Set<String> searchFragments) {
        Map<T, Double> accumulatedItems = new LinkedHashMap<>();

        searchFragments.forEach(fragment ->
                walkAndScore(fragment).forEach((k, v) ->
                        accumulatedItems.merge(k, v, (d1, d2) -> d1 + d2))
        );

        return accumulatedItems;
    }

    @NotNull
    private Map<T, Double> findAndScoreIntersectionImpl(@NotNull final Set<String> suppliedFragments) {
        Map<T, Double> accumulatedItems = null;

        boolean firstFragment = true;

        for (String suppliedFragment : suppliedFragments) {
            Map<T, Double> fragmentItems = walkAndScore(suppliedFragment);

            if (fragmentItems.isEmpty())
                return fragmentItems; // Can fail early

            if (firstFragment) {
                accumulatedItems = fragmentItems;
                firstFragment = false;
            } else {
                accumulatedItems.keySet().retainAll(fragmentItems.keySet());
                accumulatedItems.entrySet().forEach(e -> e.setValue(e.getValue() + fragmentItems.get(e.getKey())));
            }
        }

        //noinspection ConstantConditions
        return accumulatedItems;
    }

    private Map<T, Double> walkAndScore(@NotNull final String fragment) {
        GraphNode<T> root = fragmentsItemsTree.get(fragment);

        if (root == null) {
            if (unmatchedPolicy == BACKTRACKING && fragment.length() > 1) {
                return walkAndScore(fragment.substring(0, fragment.length() - 1));
            } else {
                return Collections.emptyMap();
            }
        }

        Map<T, Double> result;
        final Map<T, Double> accumulator = new LinkedHashMap<>(root.getItemsSizeHint() > 0 ? root.getItemsSizeHint() : 16);
        final Set<String> visitsTracker = new HashSet<>(root.getNodesSizeHint() > 0 ? root.getNodesSizeHint() : 16);

        if (cache != null)
            result = cache.getFromCacheOrSupplier(root, rootNode -> walkAndScore(rootNode.getFragment(), rootNode, accumulator, visitsTracker));
        else
            result = walkAndScore(root.getFragment(), root, accumulator, visitsTracker);

        /* Store size hints to prevent rehash operations on repeat visits */
        root.setItemsSizeHint(result.size());
        root.setNodesSizeHint(visitsTracker.size());

        return result;
    }

    private Map<T, Double> walkAndScore(@NotNull final String originalFragment,
                                        @NotNull final GraphNode<T> node,
                                        @NotNull final Map<T, Double> accumulated,
                                        @NotNull final Set<String> visited) {
        visited.add(node.getFragment());

        if (!node.getItems().isEmpty()) {
            Double score = keywordMatchScorer.apply(originalFragment, node.getFragment());
            node.getItems().forEach(item -> accumulated.merge(item, score, (d1, d2) -> d1.compareTo(d2) > 0 ? d1 : d2));
        }

        node.getParents().forEach(parent -> {
            if (!visited.contains(parent.getFragment())) {
                walkAndScore(originalFragment, parent, accumulated, visited);
            }
        });

        return accumulated;
    }

    private void addItemImpl(@NotNull final T item,
                             @NotNull final Set<String> suppliedKeywords) {
        registerItem(item, suppliedKeywords);

        if (itemKeywordsMap.containsKey(item))
            itemKeywordsMap.put(item, ImmutableSet.fromCollections(itemKeywordsMap.get(item), suppliedKeywords));
        else
            itemKeywordsMap.put(item, ImmutableSet.fromCollection(suppliedKeywords));

        clearCache();
    }

    private void removeItemImpl(@NotNull final T item) {
        unregisterItem(item);
        itemKeywordsMap.remove(item);
        clearCache();
    }

    private void clearCache() {
        if (cache != null)
            cache.clear();
    }

    @NotNull
    private Set<String> prepareKeywords(@NotNull final String keywordsString, boolean internKeywords) {
        return ImmutableSet.fromCollection(
                keywordsExtractor.apply(keywordsString).stream()
                        .filter(s -> s != null)
                        .map(keywordNormalizer)
                        .filter(s -> !s.isEmpty())
                        .map(s -> internKeywords ? s.intern() : s)
                        .collect(Collectors.toSet()) // implies distinct
        );
    }

    /*
     * Constructor parameter function tests.
     */

    /**
     * Test keyword extractor function for valid set (can be empty)
     * returned for empty and present string inputs.
     *
     * @param function Extractor function under test
     * @throws IllegalArgumentException Thrown if there was a null output or an exception while processing test inputs
     */
    private void testKeywordsExtractorFunction(@NotNull final Function<String, Set<String>> function) {
        try {
            if (function.apply("") == null || function.apply("testinput") == null)
                throw new IllegalArgumentException("Keywords extractor function failed non-null result test");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception while testing keywords extractor function", e);
        }
    }

    /**
     * Test keyword normalizer function for non-null string output (can be empty)
     * returned for empty and present string inputs.
     *
     * @param function Normalizer function under test
     * @throws IllegalArgumentException Thrown if there was a null output or exception during test invocations
     */
    private void testKeywordNormalizerFunction(@NotNull final Function<String, String> function) {
        try {
            if (function.apply("") == null || function.apply("testinput") == null)
                throw new IllegalArgumentException("Keyword normalizer function failed non-null output test");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception while testing keyword normalizer function", e);
        }
    }

    /**
     * Test supplied scoring function for exceptions during scoring call.
     *
     * @param function Function under test
     * @throws IllegalArgumentException Thrown if there was an exception trying to score example inputs
     */
    private void testKeywordMatchScorerFunction(@NotNull final BiFunction<String, String, Double> function) {
        try {
            function.apply("testinput", "testinput");
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception while testing keyword match scorer function", e);
        }
    }

    /*
     * Tree/graph of keyword fragments
     */

    private void registerItem(@NotNull final T item,
                              @NotNull final Set<String> keywords) {
        keywords.forEach(keyword -> createAndRegisterNode(null, keyword, item));
    }

    private void createAndRegisterNode(@Nullable final GraphNode<T> parent,
                                       @NotNull final String identity,
                                       @Nullable final T item) {
        GraphNode<T> node = fragmentsItemsTree.get(identity);

        if (node == null) {
            node = new GraphNode<>(identity);
            fragmentsItemsTree.put(identity, node);

            // And proceed to add child nodes
            if (node.getFragment().length() > 1) {
                createAndRegisterNode(node, identity.substring(0, identity.length() - 1), null);
                createAndRegisterNode(node, identity.substring(1), null);
            }
        }

        if (item != null)
            node.addItem(item);

        if (parent != null)
            node.addParent(parent);
    }

    private void unregisterItem(@NotNull final T item) {
        if (itemKeywordsMap.containsKey(item)) {
            for (String keyword : itemKeywordsMap.get(item)) {
                GraphNode<T> keywordNode = fragmentsItemsTree.get(keyword);

                keywordNode.removeItem(item);

                if (keywordNode.getItems().isEmpty())
                    collapseEdge(keywordNode, null);
            }
        }
    }

    private void collapseEdge(@Nullable final GraphNode<T> node,
                              @Nullable final GraphNode<T> parent) {
        if (node == null) //already removed
            return;

        if (parent != null)
            node.removeParent(parent);

        // No getParents or getItems means that there's nothing here to find, proceed onwards
        if (node.getParents().isEmpty() && node.getItems().isEmpty()) {
            fragmentsItemsTree.remove(node.getFragment());

            if (node.getFragment().length() > 1) {
                collapseEdge(fragmentsItemsTree.get(node.getFragment().substring(0, node.getFragment().length() - 1)), node);
                collapseEdge(fragmentsItemsTree.get(node.getFragment().substring(1)), node);
            }
        }
    }

    /*
     * Configuration and builder
     */

    public static QuickSearchBuilder builder() {
        return new QuickSearchBuilder();
    }

    public static class QuickSearchBuilder {

        private static final int DEFAULT_CACHE_HEAP_LIMIT = 100 * 1024 * 1024;

        private BiFunction<String, String, Double> keywordMatchScorer = DEFAULT_MATCH_SCORER;
        private Function<String, String> keywordNormalizer = DEFAULT_KEYWORD_NORMALIZER;
        private Function<String, Set<String>> keywordsExtractor = DEFAULT_KEYWORDS_EXTRACTOR;
        private UNMATCHED_POLICY unmatchedPolicy = BACKTRACKING;
        private ACCUMULATION_POLICY accumulationPolicy = UNION;
        private int cacheLimit = 0;

        public QuickSearchBuilder withKeywordMatchScorer(BiFunction<String, String, Double> scorer) {
            keywordMatchScorer = scorer;
            return this;
        }

        public QuickSearchBuilder withKeywordNormalizer(Function<String, String> normalizer) {
            keywordNormalizer = normalizer;
            return this;
        }

        public QuickSearchBuilder withKeywordExtractor(Function<String, Set<String>> extractor) {
            keywordsExtractor = extractor;
            return this;
        }

        public QuickSearchBuilder withUnmatchedPolicy(UNMATCHED_POLICY policy) {
            unmatchedPolicy = policy;
            return this;
        }

        public QuickSearchBuilder withAccumulationPolicy(ACCUMULATION_POLICY policy) {
            accumulationPolicy = policy;
            return this;
        }

        public QuickSearchBuilder withCacheLimit(int limitInHeapBytes) {
            if (limitInHeapBytes == -1) {
                cacheLimit = Integer.MAX_VALUE;
                return this;
            }

            if (limitInHeapBytes < 1)
                return this;

            cacheLimit = limitInHeapBytes;
            return this;
        }

        public QuickSearchBuilder withCache() {
            return withCacheLimit(DEFAULT_CACHE_HEAP_LIMIT);
        }

        public <T> QuickSearch<T> build() {
            return new QuickSearch<>(this);
        }
    }
}
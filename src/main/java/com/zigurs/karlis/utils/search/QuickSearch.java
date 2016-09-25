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

import java.util.*;
import java.util.stream.Collectors;

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
 * <code>QuickSearch&lt;String&gt; qs = new QuickSearch&lt;&gt;();
 * qs.addItem("Villain", "Roy Batty Lord Voldemort Colonel Kurtz");
 * qs.addItem("Hero", "Walt Kowalksi Jake Blues Shaun");
 * qs.findItem("walk"); // finds "Hero"</code>
 * <p>
 * Concurrency - This class is thread safe (public functions are synchronised). Implementation is completely passive
 * and can be deployed horizontally as identical datasets will produce identical search results.
 *
 * @author Karlis Zigurs, 2016
 */
public class QuickSearch<T> {

    /**
     * Interface to 'clean up' supplied keyword and user input strings. We assume that the input is
     * going to be ether free form or malformed, therefore this allows to apply required actions to generate
     * a 'clean' set of keywords from the input string.
     */
    public interface KeywordsExtractor {

        /**
         * Convert the input string into a list of keywords to be used internally.
         *
         * @param inputString supplied keywords or search input string
         * @return Set of extracted keywords, can be empty if no viable keywords could be extracted.
         */
        Set<String> extract(String inputString);
    }

    /**
     * Default raw input keywords extractor. Replaces all non-word characters with whitespace and
     * splits the resulting string on whitespace boundaries.
     * <p>
     * In example both "one two,three-four" and "one$two%three^four" as inputs will produce
     * set of 4 strings [one,two,three,four] on the output.
     */
    public static final KeywordsExtractor DEFAULT_KEYWORDS_EXTRACTOR = (s) -> new HashSet<>(Arrays.asList(s.replaceAll("[^\\w]+", " ").split("[\\s]+")));

    /**
     * Interface to sanitize search keywords before using them internally. Applied to both keywords
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
     * Default implementation assumes that String::toLowerCase() is sufficient.
     */
    public interface KeywordNormalizer {

        /**
         * Called to request a final representation for the supplied keyword to be used internally.
         *
         * @param keyword original keyword
         * @return form to use internally or empty string if this keyword is to be ignored
         */
        String normalize(String keyword);
    }

    public static final KeywordNormalizer DEFAULT_KEYWORD_NORMALIZER = String::toLowerCase;

    /**
     * Interface providing scoring of user supplied input against corresponding keywords associated with search items.
     * <p>
     * An example invocations might request to compare <code><strong>"swe"</strong></code> against
     * <code><strong>"sweater"</strong></code> or <code><strong>"count"</strong></code> aganst
     * <code><strong>"accounting"</strong></code>.
     */
    public interface KeywordMatchScorer {
        /**
         * Score how well (likely incomplete) user input scores against identified matching item keyword.
         * <p>
         * Called multiple times for all user supplied strings against their matching keywords
         * associated with item and summed up to determine final item rank.
         *
         * @param keywordSubstring user supplied (partial) match as recognized internally
         * @param itemKeyword      full matching keyword associated with the item
         * @return arbitrary number scoring the match. Higher means closer match.
         */
        double score(String keywordSubstring, String itemKeyword);
    }

    /**
     * Default keyword match score implementation.
     * <p>
     * Returns the ratio between search term and keyword lengths with additional boost
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
    public static final KeywordMatchScorer DEFAULT_MATCH_SCORER = (keywordMatch, keyword) -> {
        double matchScore = (double) keywordMatch.length() / (double) keyword.length(); // reaches maximum if lengths match (both are identical)

        // bonus boost for start of term
        if (keyword.startsWith(keywordMatch))
            matchScore += 1.0;

        return matchScore;
    };

    private class ItemAndScoreWrapper implements Comparable<ItemAndScoreWrapper> {

        private final T item;
        private double score;

        ItemAndScoreWrapper(T item, double score) {
            this.item = item;
            this.score = score;
        }

        void incrementScoreBy(double add) {
            score += add;
        }

        double getScore() {
            return score;
        }

        T getItem() {
            return item;
        }

        @Override
        public int compareTo(ItemAndScoreWrapper o) {
            return Double.compare(this.score, o.score);
        }
    }

    /*
     * Variables
     */

    private final KeywordMatchScorer keywordMatchScorer;
    private final KeywordNormalizer keywordNormalizer;
    private final KeywordsExtractor keywordsExtractor;

    /**
     * Default for minimum keyword length. Any keywords shorter than this will be ignored internally, therefore,
     * a user supplied string <code><strong>"New y"</strong></code> will be treated the same as <code><strong>"new"</strong></code>.
     */
    public static final int DEFAULT_MINIMUM_KEYWORD_LENGTH = 2;
    private final int minimumKeywordLength;

    private final Map<String, List<T>> keywordsToItemsMap = new HashMap<>();
    private final Map<String, List<String>> substringsToKeywordsMap = new HashMap<>();
    private final Map<T, List<String>> itemKeywordsMap = new HashMap<>();

    /*
     * Constructors
     */

    /**
     * Constructs a QuickSearch instance using defaults for keywords extractor, normaliser, match scorer and
     * minimum keyword length.
     */
    public QuickSearch() {
        this(DEFAULT_KEYWORDS_EXTRACTOR, DEFAULT_KEYWORD_NORMALIZER, DEFAULT_MATCH_SCORER, DEFAULT_MINIMUM_KEYWORD_LENGTH);
    }

    /**
     * Constructs a QuickSearch instance with the provided keyword processing implementations and specified minimum
     * keyword length.
     *
     * @param keywordsExtractor    Extractor. {@link  KeywordsExtractor}
     * @param keywordNormalizer    Normalizer. {@link  KeywordNormalizer}
     * @param keywordMatchScorer   Scorer. {@link  KeywordMatchScorer}
     * @param minimumKeywordLength Minimum length for keywords internally. Any keywords shorter than specified will be ignored.
     */
    public QuickSearch(KeywordsExtractor keywordsExtractor, KeywordNormalizer keywordNormalizer, KeywordMatchScorer keywordMatchScorer, int minimumKeywordLength) {
        this.keywordsExtractor = keywordsExtractor;
        this.keywordNormalizer = keywordNormalizer;
        this.keywordMatchScorer = keywordMatchScorer;
        this.minimumKeywordLength = minimumKeywordLength;
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
    public synchronized boolean addItem(T item, String keywords) {
        return addItemImpl(item, prepareKeywords(keywords, true));
    }

    /**
     * Remove previously added item. Calling this method removes the item and its mapping of keywords from the database.
     *
     * @param item Item to remove
     * @return True if the item was removed, false if no such item was found
     */
    public synchronized boolean removeItem(T item) {
        return removeItemImpl(item);
    }

    /**
     * Find top matching item for the supplied search string
     *
     * @param searchString Raw search string
     * @return Top scoring item or null if no match was found
     */
    public synchronized T findItem(String searchString) {
        List<T> foundItems = findItems(searchString, 1);

        if (foundItems.size() > 0) {
            return foundItems.get(0);
        } else {
            return null;
        }
    }

    /**
     * Find top n items matching the supplied search string. Supplied string will be processed by
     * {@link KeywordsExtractor} and {@link KeywordNormalizer} before used for search, and any
     * extracted search keywords shorten than the specified minimum keyword length will be ignored.
     *
     * @param searchString     Raw search string, e.g. "new york pizza"
     * @param numberOfTopItems Number of items the returned result should be limited to
     * @return List of 0 to numberOfTopItems elements
     */
    public synchronized List<T> findItems(String searchString, int numberOfTopItems) {
        return findItemsImpl(searchString, numberOfTopItems);
    }

    /**
     * Clear the search database.
     */
    public synchronized void clear() {
        keywordsToItemsMap.clear();
        substringsToKeywordsMap.clear();
        itemKeywordsMap.clear();
    }

    /*
     * Implementation methods
     */

    private List<T> findItemsImpl(String searchString, int maxItemsToList) {
        Set<String> providedKeywords = prepareKeywords(searchString, false);

        // empty list if no matches found
        if (providedKeywords.isEmpty())
            return Collections.emptyList(); //No viable keywords found

        // search itself
        Map<T, ItemAndScoreWrapper> unsortedResults = findAndScoreImpl(providedKeywords);

        /*
         * Choose best results sorting approach based on how large a portion
         * of matches is to be delivered to client.
         *
         * Although less efficient on smaller sample sizes sorting the result set
         * manually brings circa 50-70% better throughput for queries that result in lot of hits
         * as we can avoid sorting (discarding early) a significant proportion of the results.
         *
         * Yay for not doing unnecessary work!
         */
        if (unsortedResults.size() > maxItemsToList * 2) {
            return resultsSortManual(maxItemsToList, unsortedResults);
        } else {
            return resultsSortAPI(maxItemsToList, unsortedResults);
        }
    }

    private List<T> resultsSortManual(int maxItemsToList, Map<T, ItemAndScoreWrapper> unsortedResults) {
        LinkedList<ItemAndScoreWrapper> topResults = new LinkedList<>();

        for (Map.Entry<T, ItemAndScoreWrapper> entry : unsortedResults.entrySet()) {
            ItemAndScoreWrapper score = entry.getValue();

            if (topResults.size() < maxItemsToList) {
                insertSorted(topResults, score);
            } else if (score.getScore() > topResults.getLast().getScore()) {
                insertSorted(topResults, score);
                topResults.removeLast();
            }
        }

        return topResults.stream()
                .map(ItemAndScoreWrapper::getItem)
                .collect(Collectors.toList());
    }

    private void insertSorted(LinkedList<ItemAndScoreWrapper> lList, ItemAndScoreWrapper score) {
        for (int i = 0; i < lList.size(); i++) {
            if (score.getScore() > lList.get(i).getScore()) {
                lList.add(i, score);
                return;
            }
        }
        lList.addLast(score);
    }

    private List<T> resultsSortAPI(int maxItemsToList, Map<T, ItemAndScoreWrapper> unsortedResults) {
        return unsortedResults.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .limit(maxItemsToList)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private Map<T, ItemAndScoreWrapper> findAndScoreImpl(Set<String> suppliedSearchKeywords) {
        // temp array to contain found matching items
        Map<T, ItemAndScoreWrapper> matchingItems = new HashMap<>();

        /*
         * Scoring happens here. Basic implementation that weights the
         * length of search term against matching keyword length and adds
         * a small bonus if the found keyword begins with the search term.
         */
        for (String keywordMatch : suppliedSearchKeywords) {
            List<String> matchingKeywords = substringsToKeywordsMap.get(keywordMatch);
            if (matchingKeywords == null) continue;

            for (String keyword : matchingKeywords) {
                List<T> items = keywordsToItemsMap.get(keyword);
                double score = keywordMatchScorer.score(keywordMatch, keyword);

                for (T item : items) {
                    ItemAndScoreWrapper itemAndScoreWrapper = matchingItems.get(item);

                    if (itemAndScoreWrapper == null) {
                        matchingItems.put(item, new ItemAndScoreWrapper(item, score));
                    } else {
                        itemAndScoreWrapper.incrementScoreBy(score);
                    }
                }
            }
        }

        return matchingItems;
    }

    private boolean addItemImpl(T item, Set<String> keywords) {
        if (keywords.size() == 0) {
            return false; // No valid keywords found, skip adding
        }

        // Populate search maps
        for (String keyword : keywords) {
            addItemToKeywordItemsList(item, keyword);
            mapKeywordSubstrings(keyword);
        }

        // Keep track of all the various keywords item has been assigned with (needed for item removal)
        List<String> knownKeywords = itemKeywordsMap.get(item);

        if (knownKeywords == null) {
            knownKeywords = new ArrayList<>();
            itemKeywordsMap.put(item, knownKeywords);
        }

        // Add keywords (or add keywords not already known if item already exists)
        for (String keyword : keywords) {
            if (!knownKeywords.contains(keyword)) {
                knownKeywords.add(keyword);
            }
        }

        return true;
    }

    private boolean removeItemImpl(T item) {
        if (!itemKeywordsMap.containsKey(item))
            return false;

        //  all known keywords for the item
        List<String> knownKeywords = itemKeywordsMap.get(item);

        if (knownKeywords == null)
            return false; // No such item found?

        // remove search term mappings
        for (String keyword : knownKeywords) {
            unmapKeywordSubstrings(keyword);
            removeItemFromKeywordItemsList(item, keyword);
        }

        // forget about the item
        itemKeywordsMap.remove(item);
        return true;
    }

    private void mapKeywordSubstrings(String keyword) {
        for (int i = 0; i < keyword.length(); i++) {
            for (int y = i + 1; y <= keyword.length(); y++) {
                String keywordSubstring = keyword.substring(i, y).trim();
                if (keywordSubstring.length() > 0) {
                    mapSingleKeywordSubstring(keyword, keywordSubstring);
                }
            }
        }
    }

    private void unmapKeywordSubstrings(String keyword) {
        for (int i = 0; i < keyword.length(); i++) {
            for (int y = i + 1; y <= keyword.length(); y++) {
                String keywordSubstring = keyword.substring(i, y).trim();
                if (keywordSubstring.length() > 0) {
                    unmapSingleKeywordSubstring(keyword, keywordSubstring);
                }
            }
        }
    }

    private void mapSingleKeywordSubstring(String keyword, String keywordSubstring) {
        List<String> substringKeywordsList = substringsToKeywordsMap.get(keywordSubstring);

        if (substringKeywordsList == null) {
            substringKeywordsList = new ArrayList<>();
            substringsToKeywordsMap.put(keywordSubstring, substringKeywordsList);
        }

        if (!substringKeywordsList.contains(keyword)) {
            substringKeywordsList.add(keyword);
        }
    }

    private void unmapSingleKeywordSubstring(String keyword, String keywordSubstring) {
        List<String> substringKeywordsList = substringsToKeywordsMap.get(keywordSubstring);

        if (substringKeywordsList != null) {
            substringKeywordsList.remove(keyword);

            if (substringKeywordsList.size() == 0) {
                substringsToKeywordsMap.remove(keywordSubstring);
            }
        }
    }

    private void addItemToKeywordItemsList(T item, String keyword) {
        List<T> keywordItems = keywordsToItemsMap.get(keyword);

        if (keywordItems == null) {
            keywordItems = new ArrayList<>();
            keywordsToItemsMap.put(keyword, keywordItems);
        }

        if (!keywordItems.contains(item)) {
            keywordItems.add(item);
        }
    }

    private void removeItemFromKeywordItemsList(T item, String keyword) {
        List<T> keywordItems = keywordsToItemsMap.get(keyword);

        if (keywordItems != null) {
            keywordItems.remove(item);

            if (keywordItems.size() == 0) {
                keywordsToItemsMap.remove(keyword);
            }
        }
    }

    private Set<String> prepareKeywords(String rawInput, boolean filterShorts) {
        return prepareKeywordsList(keywordsExtractor.extract(rawInput), filterShorts);
    }

    private Set<String> prepareKeywordsList(Set<String> keywords, boolean filterShorts) {
        return keywords.stream()
                .filter(kw -> kw != null && !kw.isEmpty())                          // prune empty and null keywords
                .map(keywordNormalizer::normalize)                                  // cleanup each keyword
                .map(String::trim)                                                  // also trim any whitespace
                .filter(s -> !filterShorts || s.length() >= minimumKeywordLength)   // filter out keywords that are too short
                .collect(Collectors.toSet());
    }
}

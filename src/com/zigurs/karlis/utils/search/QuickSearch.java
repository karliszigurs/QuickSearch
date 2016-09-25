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
 * A simple, lightweight in-memory quick-search provider.
 * <p>
 * Fit for low latency querying small to medium sized datasets (limited by memory) to enable users
 * immediately see the top hits for their partially entered search string. Based on field use
 * this approach is well perceived by users and their ability to see the top hits immediately allows
 * them to adjust their queries on the fly getting to the desired result faster.
 * <p>
 * By implementing this functionality directly in the app or corresponding backend the overall complexity of the
 * project can be significantly reduced - you don't need to care about maintaining search infrastructure, server,
 * software, API, but instead focus on cheaply repopulating the dataset on reset, every hour, or so.
 * <p>
 * Example uses can include:
 * - Selecting from a list of existing contacts
 * - Looking for a particular city (associating it with known aliases, landmarks, state, etc).
 * - Searching for an item in an online (book) shop
 * - Used in background to highlight items that match the (partial) keywords entered. A.la. OSX System Preferences search
 * - Navigating large navigation trees, in example all sporting events for a year
 * <p>
 * Typical use case would be including it in ether application or a web server, maintaining the
 * data set (ether via provided add and remove methods or by clearing and repopulating the search contents
 * completely) and exposing an API to user that accepts a free-form input and returns corresponding matching items.
 * <p>
 * Each entry is associated with a number of keywords that are not exposed to user, therefore a possibility to use
 * aliases or item class descriptions in keywords. Same applies to letting users discover items by unique identifiers
 * or alternate spellings.
 * <p>
 * An example contacts list is provided as example (entry followed by assigned keywords):
 * - "Jane Doe, 1234", "Jane Doe Marketing Manager SEO Community MySpace 1234"
 * - "Alice Stuggard, 9473", "Alice Stuggard Tech Cryptography Manager RSA 9473"
 * - "Robert Howard, 6866", "Robert Bob Howard Tech Necromancy Summoning Undead Cryptography BOFH RSA DOD Laundry 6866"
 * - "Eve Moneypenny, 9223", "Eve Moneypenny Accounting Manager Q OSA 9223"
 * <p>
 * In the above example if the user enters "Mana" he will be served a list of Jane, Alice and Eve as three
 * matches as their keywords "Manager" is matched by "Mana". By now user should realise that he already has all
 * managers covered in his result set and can tailor his search further by continuing to type "Mana a" - which
 * will lead to Alice and Eve being promoted to top of results. Alice because of her name match and Eve because
 * of department. Continuing to type "mana acc" reduces the results to Eve as she is only one in the search
 * set that can match both *mana* and *acc*.
 * <p>
 * Or, in example if you have a complaint, you can start typing "nec" or "dea" and immediately
 * be presented with Bob Howard as your suggested point of contact.
 * <p>
 * Performance:
 * The implementation focuses on speed of queries with a bit of consideration for retaining a maintainable code.
 * A typical server should be able to handle well in excess of a million queries per second on a single core
 * against a modestly sized dataset (e.g. couple of thousand entries).
 * <p>
 * This class is thread safe.
 * <p>
 * If you want to re-implement or use this code in a project, free or commercial, or different programming language
 * you are welcome to, but I'll appreciate a small credit.
 *
 * @param <T> Items contained within this search instance
 */
public class QuickSearch<T> {

    /**
     * Interface to normalize search keywords (both supplied when adding items and when preparing
     * search input for search iteration). Rationale is to allow slightly relaxed free-form text input
     * (e.g. phone devices automatically capitalising entered keywords) and possibility to remap
     * special characters to their latin alphabet equivalent, if desired.
     * <p>
     * Can convert supplied strings into simplest possible representation, e.g. by ensuring consistent case,
     * replacing special characters with their simplified form, and so on.
     * <p>
     * The normalized representation is not limited in any form or way, this is just a convenience method.
     * Simply returning the supplied string will mean that the search results contain only exact (and case sensitive) matches.
     * <p>
     * Example transformations:
     * New York -> new york                               //remove upper case
     * Pythøn   -> python                                 //replace special characters
     * HERMSGERVØRDENBRØTBØRDA -> hermsgervordenbrotborda //both of the above
     * Россия   -> rossiya                                //map cyrilic alphabet to latin
     * <p>
     * Default implementation assumes that String::toLowerCase() is sufficient.
     */
    public interface KeywordNormalizer {
        String normalize(String keyword);
    }

    public static final KeywordNormalizer DEFAULT_KEYWORD_NORMALIZER = String::toLowerCase;

    /**
     * Interface to score how well a supplied (sub)string matches against matching keyword,
     * e.g. "swe" against "sweater".
     * <p>
     * See DEFAULT_MATCH_SCORER for example implementation.
     */
    public interface KeywordMatchScorer {
        /**
         * Score how well (possibly incomplete) user input scores against identified matching item keyword.
         * <p>
         * Called multiple times for all user supplied strings against their matching keywords
         * associated with item and summed up to determine final item rank.
         *
         * @param keywordSubstring user supplied (partial) match
         * @param matchingKeyword  full matching keyword associated with the item
         * @return arbitrary number scoring the match
         */
        double score(String keywordSubstring, String matchingKeyword);
    }

    /**
     * Default match scorer implementation. Returns the ratio between search term and keyword
     * lengths with additional bonus boost if the search term matches beginning of the keyword.
     * <p>
     * In example, while matching user input against known keyword "password", the following will be calculated:
     * Input "pa" -> low match (0.25), but boosted (+1) due to matching start of the keyword.
     * Input "ass" -> low match (0.37), not boosted
     * Input "assword" -> high match (0.87), not boosted
     * Input "password" -> high match (1), also boosted by matching the beginning of the line (+1)
     * <p>
     * All keywords supplied by user are scored against all partially matching keywords associated
     * with a searchable item. Items rank in the results is determined by the sum of all score results.
     */
    public static final KeywordMatchScorer DEFAULT_MATCH_SCORER = (keywordMatch, keyword) -> {
        double matchScore = (double) keywordMatch.length() / (double) keyword.length(); // reaches maximum if lengths match (both are identical)

        // bonus boost for start of term
        if (keyword.startsWith(keywordMatch))
            matchScore += 1.0;

        return matchScore;
    };

    /**
     * Interface to 'clean' up raw search strings passed in. See DEFAULT_KEYWORDS_EXTRACTOR for explanation.
     */
    public interface KeywordsExtractor {
        Set<String> extract(String rawSearchString);
    }

    /**
     * Default raw input keywords extractor. Replaces all non-word characters with whitespace and
     * splits the resulting string on whitespace boundaries.
     * <p>
     * In example the following strings will result in identical keywords list [one,two,three,four]:
     * "one two,three-four"
     * "one$two%three^four"
     */
    public static final KeywordsExtractor DEFAULT_KEYWORDS_EXTRACTOR = (s) -> new HashSet<>(Arrays.asList(s.replaceAll("[^\\w]+", " ").split("[\\s]+")));

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

    private static final int DEFAULT_MINIMUM_KEYWORD_LENGTH = 2;
    private final int minimumKeywordLength;

    private final Map<String, List<T>> keywordsToItemsMap = new HashMap<>();
    private final Map<String, List<String>> substringsToKeywordsMap = new HashMap<>();
    private final Map<T, List<String>> itemKeywordsMap = new HashMap<>();

    /*
     * Constructors
     */

    public QuickSearch() {
        this(DEFAULT_KEYWORDS_EXTRACTOR, DEFAULT_KEYWORD_NORMALIZER, DEFAULT_MINIMUM_KEYWORD_LENGTH, DEFAULT_MATCH_SCORER);
    }

    public QuickSearch(KeywordsExtractor filter, KeywordNormalizer keywordNormalizer, int minimumKeywordLength, KeywordMatchScorer keywordMatchScorer) {
        this.keywordsExtractor = filter;
        this.keywordMatchScorer = keywordMatchScorer;
        this.minimumKeywordLength = minimumKeywordLength;
        this.keywordNormalizer = keywordNormalizer;
    }

    /*
     * Public interface
     */

    /**
     * Add (or expand - see below) an item with corresponding keywords,
     * e.g. an online store item Shoe with keywords "Shoe Red 10 Converse cheap free" ...
     * <p>
     * You can expand the current keywords stored against an item by adding it again with extra (or different)
     * keywords. If the item is already in the database any new keywords will be added to it.
     *
     * @param item     item to return for search results
     * @param keywords arbitrary list of keywords separated by space, comma, special characters, freeform text...
     * @return true if the item was added, false if no keywords to map against the item were found (item not added)
     */
    public synchronized boolean addItem(T item, String keywords) {
        return addItemImpl(item, prepareKeywords(keywords, true));
    }

    /**
     * Remove previously added item. Calling this method removes the item and its mapping
     * of keywords from the database.
     *
     * @param item to remove
     * @return true if the item was removed, false if no such item was found
     */
    public synchronized boolean removeItem(T item) {
        return removeItemImpl(item);
    }

    /**
     * Find top matching item for the supplied search string
     *
     * @param searchString raw search string
     * @return top scoring item or null if none found
     */
    public synchronized T findTopItem(String searchString) {
        List<T> foundItems = findItems(searchString, 1);

        if (foundItems.size() > 0) {
            return foundItems.get(0);
        } else {
            return null;
        }
    }

    /**
     * Find top n items matching supplied search string
     *
     * @param searchString     raw search string, e.g. "new york pizza"
     * @param numberOfTopItems number of items the returned result should be limited to
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
        Map<T, ItemAndScoreWrapper> unsortedResults = coreSearchImpl(providedKeywords);

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

    private Map<T, ItemAndScoreWrapper> coreSearchImpl(Set<String> suppliedSearchKeywords) {
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
        //  all known keywords for the item
        List<String> knownKeywords = itemKeywordsMap.get(item);

        if (knownKeywords == null) {
            return false; // No such item found
        }

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
                .map(keywordNormalizer::normalize           )                       // cleanup each keyword
                .map(String::trim)                                                  // also trim any whitespace
                .filter(s -> !filterShorts || s.length() >= minimumKeywordLength)   // filter out keywords that are too short
                .collect(Collectors.toSet());
    }
}

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

import com.zigurs.karlis.utils.search.model.Result;
import com.zigurs.karlis.utils.search.model.Stats;
import org.github.jamm.MemoryMeter;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import static com.zigurs.karlis.utils.search.QuickSearch.CANDIDATE_ACCUMULATION_POLICY.INTERSECTION;
import static com.zigurs.karlis.utils.search.QuickSearch.CANDIDATE_ACCUMULATION_POLICY.UNION;
import static com.zigurs.karlis.utils.search.QuickSearch.UNMATCHED_POLICY.BACKTRACKING;
import static com.zigurs.karlis.utils.search.QuickSearch.UNMATCHED_POLICY.EXACT;
import static org.junit.Assert.*;

public class QuickSearchTest {

    private static final class StoreItem {

        private final int itemIdentifier;
        private final String name;
        private final String category;
        private final String description;

        public StoreItem(int itemIdentifier, String name, String category, String description) {
            this.itemIdentifier = itemIdentifier;
            this.name = name;
            this.category = category;
            this.description = description;
        }

        public int getItemIdentifier() {
            return itemIdentifier;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return String.format("(%s (%d) \"%s\")",
                    getName(),
                    getItemIdentifier(),
                    getDescription()
            );
        }
    }

    private final static String[][] USA_STATES = {
            {"AL", "Alabama", "Montgomery", "December 14, 1819"},
            {"AK", "Alaska", "Juneau", "January 3, 1959"},
            {"AZ", "Arizona", "Phoenix", "February 14, 1912"},
            {"AR", "Arkansas", "Little Rock", "June 15, 1836"},
            {"CA", "California", "Sacramento", "September 9, 1850"},
            {"CO", "Colorado", "Denver", "August 1, 1876"},
            {"CT", "Connecticut", "Hartford", "January 9, 1788"},
            {"DE", "Delaware", "Dover", "December 7, 1787"},
            {"FL", "Florida", "Tallahassee", "March 3, 1845"},
            {"GA", "Georgia", "Atlanta", "January 2, 1788"},
            {"HI", "Hawaii", "Honolulu", "August 21, 1959"},
            {"ID", "Idaho", "Boise", "July 3, 1890"},
            {"IL", "Illinois", "Springfield", "December 3, 1818"},
            {"IN", "Indiana", "Indianapolis", "December 11, 1816"},
            {"IA", "Iowa", "Des Moines", "December 28, 1846"},
            {"KS", "Kansas", "Topeka", "January 29, 1861"},
            {"KY", "Kentucky", "Frankfort", "June 1, 1792"},
            {"LA", "Louisiana", "Baton Rouge", "April 30, 1812"},
            {"ME", "Maine", "Augusta", "March 15, 1820"},
            {"MD", "Maryland", "Annapolis", "April 28, 1788"},
            {"MA", "Massachusetts", "Boston", "February 6, 1788"},
            {"MI", "Michigan", "Lansing", "January 26, 1837"},
            {"MN", "Minnesota", "Saint Paul", "May 11, 1858"},
            {"MS", "Mississippi", "Jackson", "December 10, 1817"},
            {"MO", "Missouri", "Jefferson City", "August 10, 1821"},
            {"MT", "Montana", "Helena", "November 8, 1889"},
            {"NE", "Nebraska", "Lincoln", "March 1, 1867"},
            {"NV", "Nevada", "Carson City", "October 31, 1864"},
            {"NH", "New Hampshire", "Concord", "June 21, 1788"},
            {"NJ", "New Jersey", "Trenton", "December 18, 1787"},
            {"NM", "New Mexico", "Santa Fe", "January 6, 1912"},
            {"NY", "New York", "Albany", "July 26, 1788"},
            {"NC", "North Carolina", "Raleigh", "November 21, 1789"},
            {"ND", "North Dakota", "Bismarck", "November 2, 1889"},
            {"OH", "Ohio", "Columbus", "March 1, 1803"},
            {"OK", "Oklahoma", "Oklahoma City", "November 16, 1907"},
            {"OR", "Oregon", "Salem", "February 14, 1859"},
            {"PA", "Pennsylvania", "Harrisburg", "December 12, 1787"},
            {"RI", "Rhode Island", "Providence", "May 19, 1790"},
            {"SC", "South Carolina", "Columbia", "May 23, 1788"},
            {"SD", "South Dakota", "Pierre", "November 2, 1889"},
            {"TN", "Tennessee", "Nashville", "June 1, 1796"},
            {"TX", "Texas", "Austin", "December 29, 1845"},
            {"UT", "Utah", "Salt Lake City", "January 4, 1896"},
            {"VT", "Vermont", "Montpelier", "March 4, 1791"},
            {"VA", "Virginia", "Richmond", "June 25, 1788"},
            {"WA", "Washington", "Olympia", "November 11, 1889"},
            {"WV", "West Virginia", "Charleston", "June 20, 1863"},
            {"WI", "Wisconsin", "Madison", "May 29, 1848"},
            {"WY", "Wyoming", "Cheyenne", "July 10, 1890"}
    };

    private QuickSearch<String> searchInstance;

    @Before
    public void setUp() throws Exception {
        searchInstance = new QuickSearch<>();
    }

    @After
    public void tearDown() throws Exception {
        searchInstance = null;
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingExtractor() {
        QuickSearch<String> qs = new QuickSearch<>(
                null,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER
        );
        assertNotNull(qs.getStats());
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingNormaliser() {
        QuickSearch<String> qs = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                null,
                QuickSearch.DEFAULT_MATCH_SCORER
        );
        assertNotNull(qs.getStats());
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingScorer() {
        QuickSearch<String> qs = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                null
        );
        assertNotNull(qs.getStats());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidMatchingPolicy() {
        QuickSearch<String> qs = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER,
                null,
                QuickSearch.CANDIDATE_ACCUMULATION_POLICY.UNION
        );
        assertNotNull(qs.getStats());
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidCandidatePolicy() {
        QuickSearch<String> qs = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER,
                QuickSearch.UNMATCHED_POLICY.EXACT,
                null
        );
        assertNotNull(qs.getStats());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractingFunction() {
        new QuickSearch<>(
                s -> null,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractingFunction1() {
        new QuickSearch<String>(
                s -> s.length() > 0 ? null : Collections.emptySet(),
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractingFunction2() {
        new QuickSearch<String>(
                s -> {
                    throw new NullPointerException("testing");
                },
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER
        );
    }

    @Test
    public void testKeywordsFiltering() {
        new QuickSearch<String>(
                s -> new HashSet<>(Arrays.asList(s.split(","))),
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER
        ).findItem("red,black,,blue");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizerFunction() {
        new QuickSearch<String>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                s -> null,
                QuickSearch.DEFAULT_MATCH_SCORER
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizerFunction1() {
        new QuickSearch<String>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                s -> s.length() > 0 ? null : s,
                QuickSearch.DEFAULT_MATCH_SCORER
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizerFunction2() {
        new QuickSearch<String>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                s -> {
                    throw new NullPointerException("test");
                },
                QuickSearch.DEFAULT_MATCH_SCORER
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testScorerFunction() {
        new QuickSearch<String>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                (s1, s2) -> {
                    throw new IndexOutOfBoundsException("testing");
                }
        );
    }

    @Test
    public void itemAdded() throws Exception {
        addItem("item", "one two three");

        checkStats(1, 3, 23);
    }

    @Test
    public void missingKeywords() throws Exception {
        assertFalse("Add a new search item with no keywords?", searchInstance.addItem("item", ""));
    }

    @Test
    public void missingKeywords2() throws Exception {
        assertFalse("Add a new search item with no keywords?", searchInstance.addItem("item", "    "));
    }

    @Test
    public void emptyItemAdded2() throws Exception {
        addItem("", "one two three");
    }

    @Test
    public void nullItemAdded() throws Exception {
        assertFalse("Failed to add a new search item.", searchInstance.addItem(null, "one two three"));
    }

    @Test
    public void nullItemAdded2() throws Exception {
        assertFalse("Failed to add a new search item.", searchInstance.addItem("item", null));
    }

    @Test
    public void itemAddedOnce() {
        for (int i = 0; i < 10; i++) {
            String testString = "teststring5" + i;
            addItem(testString.substring(0, 5), testString.substring(0, 5));
        }

        checkStats(1, 1, 13);
    }

    @Test
    public void itemRemoved() throws Exception {
        addItem("toBeRemoved", "one two three");
        assertTrue("Failed to remove search item", searchInstance.removeItem("toBeRemoved"));
    }

    @Test
    public void itemNotRemoved() throws Exception {
        assertFalse("Failed to remove search item", searchInstance.removeItem("toBeRemoved"));
    }

    @Test
    public void itemRemovedNull() throws Exception {
        assertFalse("Failed to remove search item", searchInstance.removeItem(null));
    }

    @Test
    public void remove() throws Exception {
        addItem("test1", "onex two three");
        addItem("test2", "onexx two three");
        addItem("test3", "one two three");

        assertTrue("Unexpected result size", searchInstance.findItems("one", 10).size() == 3);

        for (int i = 1; i < 4; i++) {
            searchInstance.removeItem("test" + i);
        }

        assertTrue("Unexpected result size", searchInstance.findItems("one", 10).size() == 0);

        checkStats(0, 0, 0);
    }

    @Test
    public void removeTwice() throws Exception {
        addItem("test1", "onex two three");
        addItem("test2", "onexx two three");
        addItem("test3", "one two three");

        assertEquals("Unexpected result size", 3, searchInstance.findItems("one", 10).size());

        for (int i = 1; i < 4; i++) {
            searchInstance.removeItem("test" + i);
        }

        for (int i = 1; i < 4; i++) {
            searchInstance.removeItem("test" + i);
        }

        assertTrue("Unexpected result size", searchInstance.findItems("one", 10).size() == 0);
    }

    @Test
    public void itemFound() throws Exception {
        addItem("test", "one two three");
        assertNotNull("Search item not found", searchInstance.findItem("one"));
    }

    @Test
    public void itemNotFound() throws Exception {
        addItem("test", "one two three");
        assertFalse("Search item not found", searchInstance.findItem("").isPresent());
    }

    @Test
    public void itemNotFound8() throws Exception {
        addItem("test", "one two three");
        assertFalse("Search item not found", searchInstance.findItem("   ").isPresent());
    }

    @Test
    public void itemNotFound1() throws Exception {
        addItem("test", "one two three");
        assertTrue("Search item not found", searchInstance.findItems("", 1).isEmpty());
    }

    @Test
    public void itemNotFound2() throws Exception {
        addItem("test", "one two three");
        assertFalse("Search item not found", searchInstance.findItemWithDetail("").isPresent());
    }

    @Test
    public void itemNotFound10() throws Exception {
        addItem("test", "one two three");
        assertFalse("Search item not found", searchInstance.findItemWithDetail("    ").isPresent());
    }

    @Test
    public void itemNotFound9() throws Exception {
        addItem("test", "one two three");
        assertTrue("Search item not found", searchInstance.findItemsWithDetail("    ", 1).getResponseItems().isEmpty());
    }

    @Test
    public void itemNotFound3() throws Exception {
        addItem("test", "one two three");
        assertTrue("Search item not found", searchInstance.findItemsWithDetail("", 1).getResponseItems().isEmpty());
    }

    @Test
    public void itemNotFound4() throws Exception {
        addItem("test", "one two three");
        assertTrue("Search item not found", searchInstance.findItemsWithDetail("", 0).getResponseItems().isEmpty());
    }

    @Test
    public void itemNotFound5() throws Exception {
        addItem("test", "one two three");
        assertTrue("Search item not found", searchInstance.findItemsWithDetail(null, 1).getResponseItems().isEmpty());
    }

    @Test
    public void itemNotFound6() throws Exception {
        addItem("test", "one two three");
        assertFalse("Search item not found", searchInstance.findItemWithDetail("London").isPresent());
    }

    @Test
    public void itemNotFound7() throws Exception {
        addItem("test", "one two three");
        assertTrue("Search item not found", searchInstance.findItemsWithDetail("four five", 1).getResponseItems().isEmpty());
    }

    @Test
    public void nullItemNotFound() throws Exception {
        addItem("test", "one two three");
        assertFalse("Search item found", searchInstance.findItem(null).isPresent());
    }

    @Test
    public void augumentedItemFound() throws Exception {
        addItem("test", "one two three");
        assertTrue(searchInstance.findItemWithDetail("one").isPresent());
        assertEquals("test", searchInstance.findItemWithDetail("one").get().getItem());
    }

    @Test
    public void augumentedItemFoundWithNull() throws Exception {
        addItem("test", "one two three");
        assertFalse(searchInstance.findItemWithDetail(null).isPresent());
    }

    @Test
    public void findEmptyResult() throws Exception {
        addItem("test", "one two three test");
        assertTrue(searchInstance.findItemsWithDetail("test", 0).getResponseItems().isEmpty());
    }

    @Test
    public void findCompletelyUnrelated() throws Exception {
        addItem("test", "one two three");
        assertTrue(searchInstance.findItemsWithDetail("search engine", 0).getResponseItems().isEmpty());
        assertEquals("search engine", searchInstance.findItemsWithDetail("search engine", 0).getSearchString());
    }

    @Test
    public void itemsFound() throws Exception {
        addItem("test1", "one two three");
        addItem("test2", "one two three");
        addItem("test3", "one two three");

        assertTrue("Unexpected size", searchInstance.findItems("one", 10).size() == 3);
    }

    @Test
    public void itemsFoundInOrder() throws Exception {
        addItem("test1", "onex two three");
        addItem("test2", "one two three");
        addItem("test3", "onexx two three");
        addItem("test4", "onexxx two three");
        addItem("test5", "onexxxx two three");
        addItem("test6", "onexxxxx two three");
        addItem("test7", "onexxxxxx two three");
        addItem("test8", "onexxxxxxx two three");

        List<String> results = searchInstance.findItems("one", 3);
        assertEquals("Unexpected size", 3, results.size());
        assertEquals("test2", results.get(0));
        assertEquals("test1", results.get(1));
        assertEquals("test3", results.get(2));
    }

    @Test
    public void verifyScore() {
        addItem("test1", "one two three intersecting");
        addItem("test2", "one two three intersecting");
        addItem("test3", "one three");

        assertEquals(2.0, searchInstance.findItemWithDetail("two").get().getScore(), 0);
        assertEquals(4.0, searchInstance.findItemWithDetail("two three").get().getScore(), 0);
        assertEquals(4.5, searchInstance.findItemWithDetail("two three ecting").get().getScore(), 0);
        assertEquals(5.5, searchInstance.findItemWithDetail("two three inters").get().getScore(), 0);
    }

    @Test
    public void findAugumentedItems() throws Exception {
        addItem("test1", "one two three");
        addItem("test2", "one two three");
        addItem("test3", "one two three");

        Result<String> res = searchInstance.findItemsWithDetail("one", 10);

        assertEquals("Unexpected size", 3, res.getResponseItems().size());
        assertTrue("Missing keywords", searchInstance.findItemsWithDetail("one", 10)
                .getResponseItems().get(0).getItemKeywords().size() > 0);
    }

    @Test
    public void findNoItems() throws Exception {
        addItem("test1", "one two three");
        addItem("test2", "one two three");
        addItem("test3", "one two three");
        assertTrue("Unexpected results", searchInstance.findItems("       ", 10).isEmpty());
    }

    @Test
    public void findWithNullItems() throws Exception {
        addItem("test1", "one two three");
        addItem("test2", "one two three");
        addItem("test3", "one two three");
        assertTrue("Unexpected results", searchInstance.findItems(null, 10).isEmpty());
    }

    @Test
    public void findNoAugumentedItems() throws Exception {
        addItem("test1", "one two three");
        addItem("test2", "one two three");
        addItem("test3", "one two three");
        assertTrue("Unexpected results", searchInstance.findItemsWithDetail("       ", 10).getResponseItems().isEmpty());
    }

    @Test
    public void exerciseMultiStepMapping() throws Exception {
        String exerciseString = "aquickbrownfoxjumpsoverthelazydog";

        for (int i = 2; i < exerciseString.length(); i++) {
            addItem("test" + i, exerciseString.substring(0, i));
        }

        for (int i = 0; i < exerciseString.length() - 1; i++) {
            addItem("test" + i, exerciseString.substring(i, exerciseString.length()));
        }

        assertEquals(10, searchInstance.findItems("e ex exe exer exerc i is ise", 10).size());
    }

    @Test
    public void statsAreEmpty() throws Exception {
        checkStats(0, 0, 0);
    }

    @Test
    public void statsAreFull() throws Exception {
        String exerciseString = "aquickbrownfoxjumpsoverthelazydog";

        for (int i = 2; i < exerciseString.length(); i++) {
            addItem("test" + i, exerciseString.substring(0, i));
        }

        for (int i = 0; i < exerciseString.length() - 1; i++) {
            addItem("test" + i, exerciseString.substring(i, exerciseString.length()));
        }

        assertEquals(10, searchInstance.findItems("e ex exe exer exerc i is ise", 10).size());
        checkStats(33, 63, 554);
    }

    @Test
    public void alternativeScorerFunction() throws Exception {
        QuickSearch<String> alternativeConfig = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                (s1, s2) -> (double) (s1.length() * s1.length())
        );

        String exerciseString = "aquickbrownfoxjumpsoverthelazydog";

        for (int i = 0; i < exerciseString.length(); i++) {
            alternativeConfig.addItem("test" + i, exerciseString.substring(0, i));
        }

        for (int i = 0; i < exerciseString.length(); i++) {
            alternativeConfig.addItem("test" + i, exerciseString.substring(i, exerciseString.length()));
        }

        assertEquals(10, alternativeConfig.findItems("e ex exe exer exerc i is ise", 10).size());

        checkStats(alternativeConfig.getStats(), 33, 65, 554);
    }

    @Test
    public void expandKeywords() throws Exception {
        String exerciseString = "aquickbrownfoxjumpsoverthelazydog";

        for (int i = 2; i < exerciseString.length(); i++) {
            addItem("test" + i, exerciseString.substring(0, i));
        }

        for (int i = 0; i < exerciseString.length() - 1; i++) {
            addItem("test" + i, exerciseString.substring(i, exerciseString.length()));
        }

        assertEquals(10, searchInstance.findItems("e ex exe exer exerc i is ise", 10).size());

        Stats stats = searchInstance.getStats();

        //Repeat, but with different item names (forcing to expand the keywords
        for (int i = 2; i < exerciseString.length(); i++) {
            addItem("test" + (i % 3), exerciseString.substring(0, i));
        }

        for (int i = 0; i < exerciseString.length() - 1; i++) {
            addItem("test" + ((i % 3) + 3), exerciseString.substring(i, exerciseString.length()));
        }

        assertEquals(10, searchInstance.findItems("e ex exe exer exerc i is ise", 10).size());

        assertEquals(stats.getItems(), searchInstance.getStats().getItems());
        assertEquals(stats.getKeywords(), searchInstance.getStats().getKeywords());
        assertEquals(stats.getFragments(), searchInstance.getStats().getFragments());
    }

    @Test
    public void findItemsLimit() throws Exception {
        addItem("test1", "one two three");
        addItem("test2", "one two three");
        addItem("test3", "one two three");

        assertTrue("Unexpected size", searchInstance.findItems("one", 3).size() == 3);
        assertTrue("Unexpected size", searchInstance.findItems("one", 2).size() == 2);
        assertTrue("Unexpected size", searchInstance.findItems("one", 1).size() == 1);
        assertTrue("Unexpected size", searchInstance.findItems("one", 0).size() == 0);
    }

    @Test
    public void itemsRanking() throws Exception {
        addItem("test1", "onex two three");
        addItem("test2", "onexx two three");
        addItem("test3", "one two three");

        List<String> result = searchInstance.findItems("one", 10);

        assertTrue("Unexpected result size", result.size() == 3);

        assertEquals("test3", result.get(0));
        assertEquals("test1", result.get(1));
        assertEquals("test2", result.get(2));
    }

    @Test
    public void directMatching() throws Exception {
        addItem("test1", "keyword");
        addItem("test2", "keyboard");

        List<String> result = searchInstance.findItems("keyword", 10);

        assertTrue("Unexpected result size", result.size() == 1);
        assertEquals("test1", result.get(0));
    }

    @Test
    public void unionWorks() throws Exception {
        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four");
        addItem("test4", "three five");
        addItem("test5", "three six");
        addItem("test6", "three seven");

        List<String> result = searchInstance.findItems("two three", 10);
        assertTrue("Unexpected result size", result.size() == 6);

        List<String> result2 = searchInstance.findItems("three two", 10);
        assertTrue("Unexpected result size", result2.size() == 6);
    }

    @Test
    public void unionWorks1() throws Exception {
        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four");
        addItem("test4", "three five");
        addItem("test5", "three six");
        addItem("test6", "three seven");

        Result<String> res = searchInstance.findItemsWithDetail("two three", 10);

        assertEquals("Unexpected result size", 6, res.getResponseItems().size());
        assertEquals("Unexpected score", 4.0, res.getResponseItems().get(0).getScore(), 0.0);
    }

    @Test
    public void intersectionWorks() throws Exception {
        searchInstance = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER,
                BACKTRACKING,
                INTERSECTION);

        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four");
        addItem("test4", "three five");
        addItem("test5", "three six");
        addItem("test6", "three seven");
        addItem("test7", "zebra cat");

        assertEquals("Unexpected result size", 1, searchInstance.findItems("two three", 10).size());
        assertEquals("Unexpected result size", 2, searchInstance.findItems("two", 10).size());
        assertEquals("Unexpected result size", 1, searchInstance.findItems("three two", 10).size());
        assertEquals("Unexpected result size", 0, searchInstance.findItems("three cat two zebra", 10).size());
        assertEquals("Unexpected result size", 0, searchInstance.findItems("cat three two", 10).size());
    }

    @Test
    public void intersectionWorks1() throws Exception {
        searchInstance = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER,
                BACKTRACKING,
                INTERSECTION);

        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four");
        addItem("test7", "four cat");

        assertTrue("Unexpected results", searchInstance.findItems("cat three", 10).isEmpty());
        assertTrue("Unexpected results", searchInstance.findItems("cat three two", 10).isEmpty());
    }

    @Test
    public void intersectionWorks2() throws Exception {
        searchInstance = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER,
                BACKTRACKING,
                INTERSECTION);

        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four");

        assertTrue("Unexpected results", searchInstance.findItems("two four", 10).isEmpty());
    }

    @Test
    public void intersectionWorks3() throws Exception {
        searchInstance = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER,
                BACKTRACKING,
                INTERSECTION);

        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four");

        assertTrue("Unexpected results", searchInstance.findItems("five six", 10).isEmpty());
    }

    @Test
    public void intersectionWorks4() throws Exception {
        searchInstance = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER,
                BACKTRACKING,
                INTERSECTION);

        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four two");

        Result<String> res = searchInstance.findItemsWithDetail("two three", 10);

        assertEquals("Unexpected result size", 2, res.getResponseItems().size());
        assertEquals("Unexpected score", 4.0, res.getResponseItems().get(0).getScore(), 0.0);
        assertEquals("Unexpected score", 4.0, res.getResponseItems().get(1).getScore(), 0.0);
    }

    @Test
    public void backtrackMatchingStops() throws Exception {
        addItem("keyword", "keyword");
        addItem("keyboard", "keyboard");

        List<String> result = searchInstance.findItems("keywZ", 10);

        assertEquals("Unexpected result size", 1, result.size());
        assertEquals("keyword", result.get(0));
    }

    @Test
    public void backtrackMatchingContinues() throws Exception {
        addItem("keyword", "keyword one");
        addItem("keyboard", "keyboard");

        List<String> result = searchInstance.findItems("keyZZ", 10);

        assertEquals("Unexpected result size", 2, result.size());
        assertEquals("keyword", result.get(0));
        assertEquals("keyboard", result.get(1));
    }

    @Test
    public void exactMatching() throws Exception {
        searchInstance = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER,
                EXACT,
                UNION
        );

        addItem("keyword", "keyword");
        addItem("keyboard", "keyboard");

        List<String> result = searchInstance.findItems("keywZ", 10);

        assertTrue("Unexpected result size", result.isEmpty());
    }

    @Test
    public void clear() throws Exception {
        addItem("test1", "onex two three");
        addItem("test2", "onexx two three");
        addItem("test3", "one two three");

        searchInstance.clear();

        checkStats(0, 0, 0);

        assertFalse("Unexpected result", searchInstance.findItem("one").isPresent());
    }

    @Test
    public void objectsAsItems() {
        List<StoreItem> items = new LinkedList<>();
        String[] categories = new String[]{"Shoes", "Jackets", "Skirts"};

        for (int i = 0; i < 1_000; i++) {
            items.add(new StoreItem(i, "StoreItem", categories[i % 3],
                    String.format("Item%d %s", i, categories[i % 3])));
        }

        items.add(new StoreItem(1, "Lord of The Rings",
                "Fantasy", "tolkien fantasy hardbound middle earth lord of the rings"));

        // create a quick search for StoreItems
        QuickSearch<StoreItem> search = new QuickSearch<>();

        // populate quick search data
        for (StoreItem item : items) {
            search.addItem(item, item.getDescription());
        }

        // do a few quick searches
        assertTrue(search.findItem("missing jeans").isPresent());
        assertTrue(search.findItem("item jack").isPresent());

        assertEquals(10, search.findItems("item jack 20", 10).size());
        assertEquals(10, search.findItems("sh 50", 10).size());

        items.stream()
                .filter(item -> categories[0].equals(item.getCategory()))
                .forEach(search::removeItem);

        assertEquals(10, search.findItems("red shoes", 10).size());
        assertEquals(1, search.findItems("midd", 10).size());
        search.clear();
    }

    @Test
    public void hashWrapperEquals() {
        HashWrapper<String> w = new HashWrapper<>("cat");

        assertFalse(w.equals(null));
        assertFalse(w.equals("test"));
        assertTrue(w.equals(new HashWrapper<>("cat")));
        assertFalse(w.equals(new HashWrapper<>("dog")));
    }

    @Test
    public void hashWrapperHash() {
        String testString = "testString";

        assertTrue(new HashWrapper<>(testString).hashCode() == testString.hashCode());
    }

    @Test
    public void enums() {
        // Silly to do it like this, but catches last few untouched code paths
        assertNotNull(QuickSearch.CANDIDATE_ACCUMULATION_POLICY.valueOf("UNION"));
        assertNotNull(QuickSearch.CANDIDATE_ACCUMULATION_POLICY.valueOf("INTERSECTION"));

        for (QuickSearch.CANDIDATE_ACCUMULATION_POLICY policy : QuickSearch.CANDIDATE_ACCUMULATION_POLICY.values()) {
            assertNotNull(policy.toString());
        }

        assertNotNull(QuickSearch.UNMATCHED_POLICY.valueOf("EXACT"));
        assertNotNull(QuickSearch.UNMATCHED_POLICY.valueOf("BACKTRACKING"));

        for (QuickSearch.UNMATCHED_POLICY policy : QuickSearch.UNMATCHED_POLICY.values()) {
            assertNotNull(policy.toString());
        }
    }

    @Test
    public void customSorting() {
        List<Double> testSet = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            testSet.add((double) i);
        }

        testSet.add(1000.0);

        Collections.shuffle(testSet);

        List<Double> list = searchInstance.sortAndLimit(testSet, 10, Comparator.naturalOrder());
        assertEquals(0.0, list.get(0), 0);
        assertEquals(1.0, list.get(1), 0);
        assertEquals(2.0, list.get(2), 0);
        assertEquals(9.0, list.get(9), 0);
        assertEquals(10, list.size());
    }

    @Test
    public void customSortingReverse() {
        List<Double> testSet = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            testSet.add((double) i);
        }

        testSet.add(1000.0);

        Collections.shuffle(testSet);

        List<Double> list = searchInstance.sortAndLimit(testSet, 1, Comparator.reverseOrder());

        assertEquals(1000.0, list.get(0), 0);
        assertEquals(1, list.size());
    }

    /*
     * microbenchmarks and utilities
     */

    @Ignore
    @Test
    public void intersectionBench() throws Exception {
        searchInstance = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER,
                BACKTRACKING,
                INTERSECTION);

        long st = System.currentTimeMillis();
        for (int i = 0; i < 3000; i++) {
            String[] items = USA_STATES[i % USA_STATES.length];
            addItem(items[0] + "-" + i, String.format("%s %s %s %s", items[0], items[1], items[2], items[3]));
        }

        for (String[] items : USA_STATES) {
            addItem(items[0], String.format("%s %s %s %s", items[0], items[1], items[2], items[3]));
        }

        System.out.println("Loaded in " + (System.currentTimeMillis() - st) + "ms");

        int iterations = 5000;
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
//            assertTrue(searchInstance.findItem("a b c d e g h i l m n p r s u v y").isPresent());
            assertTrue(searchInstance.findItems("w a s", 10).size() > 0);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println(String.format("%f k/sec", (double) iterations / totalTime));
    }

    @Ignore
    @Test
    public void sortBench() {
        /*
         * Microbenchmarking caveats apply. Make sure the number of iterations is
         * large enough and JVM is given a chance to warm up and apply the
         * optimisations on the code exercised.
         */
        List<Double> testList = new LinkedList<>();

        int listSize = 100000;
        for (int i = 0; i < listSize; i++) {
            testList.add((double) i);
        }

        Collections.shuffle(testList);

        Map<Double, Double> testMap = new LinkedHashMap<>();
        testList.forEach(d -> testMap.put(d, d));

        int iterationsCount = 10;
        int topItems = 10;

        Comparator<Double> comparator = Comparator.reverseOrder();
        double topResultShouldBe = (double) listSize - 1;

        sortBench("Warmup: ", testMap.values(), iterationsCount, topItems, comparator, topResultShouldBe);
        sortBench("Warmer: ", testMap.values(), iterationsCount, topItems, comparator, topResultShouldBe);
        sortBench("Warm:   ", testMap.values(), iterationsCount, topItems, comparator, topResultShouldBe);
    }

    private void sortBench(String prefix, Collection<Double> testList, int iterationsCount, int topItems, Comparator<Double> comparator, double topResultShouldBe) {
        LongSupplier time = System::nanoTime;
        int timeDivisor = 1000;
        String unit = "us";

//        LongSupplier time = System::currentTimeMillis;
//        int timeDivisor = 1;
//        String unit = "ms";

        long totalTimeCropped = 0;
        long totalTimeCollections = 0;
        long totalTimeStreamed = 0;
        long totalTimeParallel = 0;
        long startTime;

        for (int i = 0; i < iterationsCount; i++) {
            startTime = time.getAsLong();
            List<Double> listCropped = searchInstance.sortAndLimit(testList, topItems, comparator);
            assertEquals(topResultShouldBe, listCropped.get(0), 0);
            totalTimeCropped += time.getAsLong() - startTime;

            List<Double> listRepresentation = new LinkedList<>(testList);
            Collections.shuffle(listRepresentation);
            startTime = time.getAsLong();
            Collections.sort(listRepresentation, comparator);
            assertEquals(topResultShouldBe, listRepresentation.get(0), 0);
            totalTimeCollections += time.getAsLong() - startTime;

            startTime = time.getAsLong();
            List<Double> listStreamed = testList.stream()
                    .sorted(comparator)
                    .limit(topItems)
                    .collect(Collectors.toList());
            assertEquals(topResultShouldBe, listStreamed.get(0), 0);
            totalTimeStreamed += time.getAsLong() - startTime;

            startTime = time.getAsLong();
            List<Double> listParallel = testList.parallelStream()
                    .sorted(comparator)
                    .limit(topItems)
                    .collect(Collectors.toList());
            assertEquals(topResultShouldBe, listParallel.get(0), 0);
            totalTimeParallel += time.getAsLong() - startTime;
        }

        System.out.println(String.format("%6$s %1$d%5$s cropped, %2$d%5$s collections, %3$d%5$s streamed, %4$d%5$s parallel on average",
                (totalTimeCropped / iterationsCount) / timeDivisor,
                (totalTimeCollections / iterationsCount) / timeDivisor,
                (totalTimeStreamed / iterationsCount) / timeDivisor,
                (totalTimeParallel / iterationsCount) / timeDivisor,
                unit,
                prefix
        ));
    }

    @Ignore
    @Test
    public void mtBench() throws Exception {
        for (String[] items : USA_STATES) {
            addItem(
                    items[0],
                    String.format("%s %s %s", items[1], items[2], items[3])
            );
        }

//        long startTime = System.currentTimeMillis();
//
//        for (int i = 0; i < 1000; i++) {
//            assertTrue("No results found?",
//                    searchInstance.findItems(USA_STATES[i % USA_STATES.length][1].substring(0, 3), 10).size() > 0);
//        }
//
//        assertTrue("Shouldn't be anywhere near this slow...", (System.currentTimeMillis() - startTime) < 1000);

        // Warmup
        mtIteration();

        // Warmed up
        mtIteration();
    }

    private void mtIteration() throws InterruptedException {
        int threads = 4;
        int iterationsPerThread = 1000000;
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicLong wrote = new AtomicLong(0L);

        // Writing thread
        new Thread(() -> {
            int i = 0;
            while (latch.getCount() > 0) {
                addItem("new item" + i, "few new keywords" + i++);
                wrote.incrementAndGet();

                //noinspection EmptyCatchBlock
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
        }).start();

        Long startTime = System.currentTimeMillis();

        // Reading threads
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                mtThreadIteration(iterationsPerThread);
                latch.countDown();
            });
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();
        }

        latch.await(60, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Aggregate throughput: " + ((iterationsPerThread / totalTime) * threads) + "k per second, " + wrote.get() + " writes");
    }

    private void mtThreadIteration(final int iterations) {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            assertTrue(searchInstance.findItems(USA_STATES[i % USA_STATES.length][1].substring(0, 3), 10).size() > 0);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println(String.format("Took %dms, %dk searches second.",
                totalTime,
                iterations / totalTime
        ));
    }

    @Ignore
    @Test
    public void opsBench() throws Exception {
        for (String[] items : USA_STATES) {
            addItem(items[0], String.format("%s %s %s", items[1], items[2], items[3]));
        }

        // Warmup
        mtOpsBench();

        // Warmed up
        mtOpsBench();
    }

    private void mtOpsBench() throws InterruptedException {
        int threads = 1;
        int iterationsPerThread = 100000;
        CountDownLatch latch = new CountDownLatch(threads);

        // ops threads
        Long startTime = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                mtOpsIteration(iterationsPerThread);
                latch.countDown();
            });
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();
        }

        latch.await(60, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Aggregate throughput: " + ((iterationsPerThread / totalTime) * threads) + "k ops per second");
    }

    private void mtOpsIteration(final int iterations) {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            addItem(USA_STATES[i % USA_STATES.length][1].substring(0, 3), USA_STATES[i % USA_STATES.length][0] + " " + USA_STATES[i % USA_STATES.length][2]);
            searchInstance.removeItem(USA_STATES[i % USA_STATES.length][1].substring(0, 3));
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println(String.format("Took %dms, %dk ops second.",
                totalTime,
                iterations / totalTime
        ));
    }

    @Test
    public void measureMemoryUse() {
        searchInstance = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER,
                BACKTRACKING,
                INTERSECTION);

        for (int i = 0; i < 100_000; i++) {
            String[] items = USA_STATES[i % USA_STATES.length];
            addItem(items[0] + "-" + i, String.format("%s %s %s %s", items[0], items[1], items[2], items[3]));
        }

//        searchInstance.goImmutable();

        MemoryMeter meter = new MemoryMeter().withGuessing(MemoryMeter.Guess.ALWAYS_UNSAFE);
        long measured = meter.measureDeep(searchInstance);
        System.out.println("Memory consumption measured at " + measured);

        final long JDK_COLLECTIONS_TARGET = 117_416_608;
        final long GUAVA_MULTIMAP_TARGET = 104_140_960;
        final long GUAVA_MULTIMAP_IMMUTABLE_TARGET = 54_848_672;

        assertTrue("Calculated size exceeds JDK Collections target", measured < (JDK_COLLECTIONS_TARGET * 1.1));
    }

    @Test
    public void measureQueryMicrobench() {
        searchInstance = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER,
                BACKTRACKING,
                INTERSECTION);

        for (int i = 0; i < 100_000; i++) {
            String[] items = USA_STATES[i % USA_STATES.length];
            addItem(items[0] + "-" + i, String.format("%s %s %s %s", items[0], items[1], items[2], items[3]));
        }

//        searchInstance.goImmutable();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 10_000; i++) {
            String searchString = USA_STATES[i % USA_STATES.length][0];
            assertFalse("No results found", searchInstance.findItems(searchString, 10).isEmpty());
        }

        final long JDK_COLLECTIONS_TARGET = 12100;
        final long GUAVA_MULTIMAP_TARGET = 19600;
        final long GUAVA_MULTIMAP_IMMUTABLE_TARGET = 13100;

        System.out.println("Time taken: " + (System.currentTimeMillis() - startTime));
    }



    /*
     * Tests boilerplate
     */

    private void addItem(String item, String keywords) {
        addItem(searchInstance, item, keywords);
    }

    private void addItem(QuickSearch<String> instance, String item, String keywords) {
        assertTrue("Failed to add item", instance.addItem(item, keywords));
    }

    private void checkStats(int items, int keywords, int fragments) {
        checkStats(searchInstance.getStats(), items, keywords, fragments);
    }

    private void checkStats(Stats stats, int items, int keywords, int fragments) {
        assertEquals(items, stats.getItems());
        assertEquals(keywords, stats.getKeywords());
        assertEquals(fragments, stats.getFragments());
    }
}
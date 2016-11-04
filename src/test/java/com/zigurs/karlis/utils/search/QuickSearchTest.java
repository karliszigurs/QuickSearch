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

import com.zigurs.karlis.utils.search.model.Item;
import com.zigurs.karlis.utils.search.model.Result;
import com.zigurs.karlis.utils.search.model.Stats;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.zigurs.karlis.utils.search.QuickSearch.ACCUMULATION_POLICY.INTERSECTION;
import static com.zigurs.karlis.utils.search.QuickSearch.ACCUMULATION_POLICY.UNION;
import static com.zigurs.karlis.utils.search.QuickSearch.UNMATCHED_POLICY.EXACT;
import static org.junit.Assert.*;

public class QuickSearchTest {

    public final static String[][] USA_STATES = {
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
    public void setUp() {
        searchInstance = QuickSearch.builder().build();
    }

    @After
    public void tearDown() {
        searchInstance = null;
    }

    @Test(expected = NullPointerException.class)
    public void missingExtractor() {
        assertNotNull(QuickSearch.builder()
                .withKeywordExtractor(null)
                .build());
    }

    @Test(expected = NullPointerException.class)
    public void missingNormaliser() {
        assertNotNull(QuickSearch.builder()
                .withKeywordNormalizer(null)
                .build());
    }

    @Test(expected = NullPointerException.class)
    public void missingScorer() {
        assertNotNull(QuickSearch.builder()
                .withKeywordMatchScorer(null)
                .build());
    }

    @Test(expected = NullPointerException.class)
    public void invalidMatchingPolicy() {
        assertNotNull(QuickSearch.builder()
                .withUnmatchedPolicy(null)
                .build());
    }

    @Test(expected = NullPointerException.class)
    public void invalidCandidatePolicy() {
        assertNotNull(QuickSearch.builder()
                .withAccumulationPolicy(null)
                .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractingFunction() {
        assertNotNull(QuickSearch.builder()
                .withKeywordExtractor(s -> null)
                .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractingFunction1() {
        assertNotNull(QuickSearch.builder()
                .withKeywordExtractor(s -> s.length() > 0 ? null : Collections.emptySet())
                .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractingFunction2() {
        assertNotNull(QuickSearch.builder()
                .withKeywordExtractor(s -> {
                    throw new NoSuchElementException("dummy");
                })
                .build());
    }

    @Test
    public void checkExtractingFunctionFilters() {
        searchInstance = QuickSearch.builder()
                .withKeywordExtractor(s -> new HashSet<>(Arrays.asList("", "one", "blue", "yellow", null)))
                .build();
        addItem("test", "onetwo three");
        searchInstance.findItem("one");
    }

    @Test
    public void testKeywordsFiltering() {
        assertNotNull(QuickSearch.builder()
                .withKeywordExtractor(s -> new HashSet<>(Arrays.asList(s.split(","))))
                .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizerFunction() {
        assertNotNull(QuickSearch.builder()
                .withKeywordNormalizer(s -> null)
                .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizerFunction1() {
        assertNotNull(QuickSearch.builder()
                .withKeywordNormalizer(s -> s.length() > 0 ? null : s)
                .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNormalizerFunction2() {
        assertNotNull(QuickSearch.builder()
                .withKeywordNormalizer(s -> {
                    throw new IndexOutOfBoundsException("dummy");
                })
                .build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testScorerFunction() {
        assertNotNull(QuickSearch.builder()
                .withKeywordMatchScorer((s1, s2) -> {
                    throw new IndexOutOfBoundsException("testing");
                })
                .build());
    }

    @Test
    public void buildWithCache() {
        assertNotNull(QuickSearch.builder()
                .withCache()
                .build());
    }

    @Test
    public void buildWithCache1() {
        assertNotNull(QuickSearch.builder()
                .withCacheLimit(Integer.MAX_VALUE)
                .build());
    }

    @Test
    public void buildWithCache2() {
        assertNotNull(QuickSearch.builder()
                .withCacheLimit(0)
                .build());
    }

    @Test
    public void buildWithCache3() {
        assertNotNull(QuickSearch.builder()
                .withCacheLimit(-1)
                .build());
    }

    @Test
    public void itemAdded() {
        addItem("item", "one two three");
        checkStats(1, 23);
        assertEquals(1, searchInstance.getStats().getItems());
    }

    @Test
    public void missingKeywords() {
        assertFalse("Add a new search item with no keywords?", searchInstance.addItem("item", ""));
    }

    @Test
    public void missingKeywords2() {
        assertFalse("Add a new search item with no keywords?", searchInstance.addItem("item", "    "));
    }

    @Test
    public void emptyItemAdded2() {
        addItem("", "one two three");
        assertEquals(1, searchInstance.getStats().getItems());
    }

    @Test
    public void nullItemAdded() {
        assertFalse("Failed to add a new search item.", searchInstance.addItem(null, "one two three"));
    }

    @Test
    public void nullItemAdded2() {
        assertFalse("Failed to add a new search item.", searchInstance.addItem("item", null));
    }

    @Test
    public void itemAddedOnce() {
        for (int i = 0; i < 10; i++) {
            String testString = "teststring5" + i;
            addItem(testString.substring(0, 5), testString.substring(0, 5));
        }

        checkStats(1, 13);
        assertEquals(1, searchInstance.getStats().getItems());
    }

    @Test
    public void itemRemoved() {
        addItem("toBeRemoved", "one two three");
        searchInstance.removeItem("toBeRemoved");
        assertEquals(0, searchInstance.getStats().getFragments());
    }

    @Test
    public void itemRemoved1() {
        addItem("test1", "onex two three");
        addItem("test2", "onexx two three");
        addItem("test3", "one two three");

        assertTrue("Unexpected result size", searchInstance.findItems("one", 10).size() == 3);

        for (int i = 1; i < 4; i++) {
            searchInstance.removeItem("test" + i);
        }

        assertTrue("Unexpected result size", searchInstance.findItems("one", 10).size() == 0);

        checkStats(0, 0);
    }

    @Test
    public void itemRemoved2() {
        addItem("toBeRemoved", "one two three");
        searchInstance.removeItem(null);
        assertEquals(23, searchInstance.getStats().getFragments());
    }

    @Test
    public void itemRemoved3() {
        searchInstance = QuickSearch.builder().withCache().build();

        addItem("toBeRemoved", "one two three");
        searchInstance.findItem("o");
        searchInstance.removeItem("toBeRemoved");
        assertEquals(0, searchInstance.getStats().getFragments());
    }


    @Test
    public void removeTwice() {
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

        assertEquals("Unexpected result size", 0, searchInstance.findItems("one", 10).size());
    }

    @Test
    public void itemFound() {
        addItem("test", "one two three");
        assertNotNull("Search item not found", searchInstance.findItem("one"));
    }

    @Test
    public void itemNotFound() {
        addItem("test", "one two three");
        assertFalse("Search item not found", searchInstance.findItem("").isPresent());
    }

    @Test
    public void itemNotFound8() {
        addItem("test", "one two three");
        assertFalse("Search item not found", searchInstance.findItem("   ").isPresent());
    }

    @Test
    public void itemNotFound1() {
        addItem("test", "one two three");
        assertTrue("Search item not found", searchInstance.findItems("", 1).isEmpty());
    }

    @Test
    public void itemNotFound2() {
        addItem("test", "one two three");
        assertFalse("Search item not found", searchInstance.findItemWithDetail("").isPresent());
    }

    @Test
    public void itemNotFound10() {
        addItem("test", "one two three");
        assertFalse("Search item not found", searchInstance.findItemWithDetail("    ").isPresent());
    }

    @Test
    public void itemNotFound9() {
        addItem("test", "one two three");
        assertTrue("Search item not found", searchInstance.findItemsWithDetail("    ", 1).getResponseItems().isEmpty());
    }

    @Test
    public void itemNotFound3() {
        addItem("test", "one two three");
        assertTrue("Search item not found", searchInstance.findItemsWithDetail("", 1).getResponseItems().isEmpty());
    }

    @Test
    public void itemNotFound4() {
        addItem("test", "one two three");
        assertTrue("Search item not found", searchInstance.findItemsWithDetail("", 0).getResponseItems().isEmpty());
    }

    @Test
    public void itemNotFound5() {
        addItem("test", "one two three");
        assertTrue("Search item not found", searchInstance.findItemsWithDetail(null, 1).getResponseItems().isEmpty());
    }

    @Test
    public void itemNotFound6() {
        addItem("test", "one two three");
        assertFalse("Search item not found", searchInstance.findItemWithDetail("London").isPresent());
    }

    @Test
    public void itemNotFound7() {
        addItem("test", "one two three");
        assertTrue("Search item not found", searchInstance.findItemsWithDetail("four five", 1).getResponseItems().isEmpty());
    }

    @Test
    public void nullItemNotFound() {
        addItem("test", "one two three");
        assertFalse("Search item found", searchInstance.findItem(null).isPresent());
    }

    @Test
    public void augumentedItemFound() {
        addItem("test", "one two three");

        Optional<Item<String>> result = searchInstance.findItemWithDetail("one");
        assertTrue(result.isPresent());
        assertEquals("test", result.get().getResult());
    }

    @Test
    public void augumentedItemFoundWithNull() {
        addItem("test", "one two three");
        assertFalse(searchInstance.findItemWithDetail(null).isPresent());
    }

    @Test
    public void findEmptyResult() {
        addItem("test", "one two three test");
        assertTrue(searchInstance.findItemsWithDetail("test", 0).getResponseItems().isEmpty());
    }

    @Test
    public void findCompletelyUnrelated() {
        addItem("test", "one two three");
        assertTrue(searchInstance.findItemsWithDetail("search engine", 0).getResponseItems().isEmpty());
        assertEquals("search engine", searchInstance.findItemsWithDetail("search engine", 0).getSearchString());
    }

    @Test
    public void itemsFound() {
        addItem("test1", "one two three");
        addItem("test2", "one two three");
        addItem("test3", "one two three");

        assertTrue("Unexpected size", searchInstance.findItems("one", 10).size() == 3);
    }

    @Test
    public void itemsFoundInOrder() {
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
    public void findAugumentedItems() {
        addItem("test1", "one two three");
        addItem("test2", "one two three");
        addItem("test3", "one two three");

        Result<String> res = searchInstance.findItemsWithDetail("one", 10);

        assertEquals("Unexpected size", 3, res.getResponseItems().size());
        assertTrue("Missing keywords", searchInstance.findItemsWithDetail("one", 10)
                .getResponseItems().get(0).getItemKeywords().size() > 0);
    }

    @Test
    public void findNoItems() {
        addItem("test1", "one two three");
        addItem("test2", "one two three");
        addItem("test3", "one two three");
        assertTrue("Unexpected results", searchInstance.findItems("       ", 10).isEmpty());
    }

    @Test
    public void findWithNullItems() {
        addItem("test1", "one two three");
        addItem("test2", "one two three");
        addItem("test3", "one two three");
        assertTrue("Unexpected results", searchInstance.findItems(null, 10).isEmpty());
    }

    @Test
    public void findNoAugumentedItems() {
        addItem("test1", "one two three");
        addItem("test2", "one two three");
        addItem("test3", "one two three");
        assertTrue("Unexpected results", searchInstance.findItemsWithDetail("       ", 10).getResponseItems().isEmpty());
    }

    @Test
    public void exerciseMultiStepMapping() {
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
    public void statsAreEmpty() {
        checkStats(0, 0);
        assertEquals(0, searchInstance.getStats().getItems());
    }

    @Test
    public void statsAreFull() {
        String exerciseString = "aquickbrownfoxjumpsoverthelazydog";

        for (int i = 2; i < exerciseString.length(); i++) {
            String kw = exerciseString.substring(0, i);
            addItem("test" + i, kw);
        }

        for (int i = 0; i < exerciseString.length() - 1; i++) {
            addItem("test" + i, exerciseString.substring(i, exerciseString.length()));
        }

        checkStats(33, 554);
        assertEquals(10, searchInstance.findItems("e ex exe exer exerc i is ise", 10).size());
    }

    @Test
    public void singleItem() {
        addItem("test", "abra");
        assertEquals(1, searchInstance.getStats().getItems());
    }

    @Test
    public void alternativeScorerFunction() {
        QuickSearch<String> alternativeConfig = QuickSearch.builder()
                .withKeywordMatchScorer((s1, s2) -> (double) (s1.length() * s1.length()))
                .build();

        String exerciseString = "aquickbrownfoxjumpsoverthelazydog";

        for (int i = 0; i < exerciseString.length(); i++) {
            alternativeConfig.addItem("test" + i, exerciseString.substring(0, i));
        }

        for (int i = 0; i < exerciseString.length(); i++) {
            alternativeConfig.addItem("test" + i, exerciseString.substring(i, exerciseString.length()));
        }

        checkStats(alternativeConfig.getStats(), 33, 554);
        assertEquals(10, alternativeConfig.findItems("e ex exe exer exerc i is ise", 10).size());
    }

    @Test
    public void expandKeywords() {
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

        checkStats(stats.getItems(), stats.getFragments());
        assertEquals(10, searchInstance.findItems("e ex exe exer exerc i is ise", 10).size());
    }

    @Test
    public void findItemsLimit() {
        addItem("test1", "one two three");
        addItem("test2", "one two three");
        addItem("test3", "one two three");

        assertTrue("Unexpected size", searchInstance.findItems("one", 3).size() == 3);
        assertTrue("Unexpected size", searchInstance.findItems("one", 2).size() == 2);
        assertTrue("Unexpected size", searchInstance.findItems("one", 1).size() == 1);
        assertTrue("Unexpected size", searchInstance.findItems("one", 0).size() == 0);
    }

    @Test
    public void itemsRanking() {
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
    public void directMatching() {
        addItem("test1", "keyword");
        addItem("test2", "keyboard");

        List<String> result = searchInstance.findItems("keyword", 10);

        assertTrue("Unexpected result size", result.size() == 1);
        assertEquals("test1", result.get(0));
    }

    @Test
    public void unionWorks() {
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
    public void unionWorks1() {
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
    public void largeUnionWorks() {
        for (int i = 0; i < 1000; i++)
            addItem("Item" + i, "one two three");

        Result<String> res = searchInstance.findItemsWithDetail("two three", 10);

        assertEquals("Unexpected result size", 10, res.getResponseItems().size());
        assertEquals("Unexpected score", 4.0, res.getResponseItems().get(0).getScore(), 0.0);
    }

    @Test
    public void intersectionWorks() {
        searchInstance = QuickSearch.builder()
                .withAccumulationPolicy(INTERSECTION)
                .withCache()
                .build();

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
    public void intersectionWorks1() {
        searchInstance = QuickSearch.builder()
                .withAccumulationPolicy(INTERSECTION)
                .build();

        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four");
        addItem("test7", "four cat");

        assertTrue("Unexpected results", searchInstance.findItems("cat three", 10).isEmpty());
        assertTrue("Unexpected results", searchInstance.findItems("cat three two", 10).isEmpty());
    }

    @Test
    public void intersectionWorks2() {
        searchInstance = QuickSearch.builder()
                .withAccumulationPolicy(INTERSECTION)
                .build();

        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four");

        assertTrue("Unexpected results", searchInstance.findItems("two four", 10).isEmpty());
    }

    @Test
    public void intersectionWorks3() {
        searchInstance = QuickSearch.builder()
                .withAccumulationPolicy(INTERSECTION)
                .build();

        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four");

        assertTrue("Unexpected results", searchInstance.findItems("five six", 10).isEmpty());
    }

    @Test
    public void intersectionWorks4() {
        searchInstance = QuickSearch.builder()
                .withAccumulationPolicy(INTERSECTION)
                .build();

        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four two");

        Result<String> res = searchInstance.findItemsWithDetail("two three", 10);

        assertEquals("Unexpected result size", 2, res.getResponseItems().size());
        assertEquals("Unexpected score", 4.0, res.getResponseItems().get(0).getScore(), 0.0);
        assertEquals("Unexpected score", 4.0, res.getResponseItems().get(1).getScore(), 0.0);
    }

    @Test
    public void checkCacheReports() {
        searchInstance = QuickSearch.builder()
                .withAccumulationPolicy(INTERSECTION)
                .withCache()
                .build();

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

        assertTrue(searchInstance.getCacheStats().isPresent());

        assertEquals(8, searchInstance.getCacheStats().get().getHits());
        assertEquals(0, searchInstance.getCacheStats().get().getEvictions());
        assertEquals(4, searchInstance.getCacheStats().get().getMisses());
        assertEquals(9, searchInstance.getCacheStats().get().getSize());
        assertEquals(0, searchInstance.getCacheStats().get().getUncacheable());
    }


    @Test
    public void checkCacheMissing() {
        searchInstance = QuickSearch.builder()
                .withAccumulationPolicy(UNION)
                .build();

        addItem("test1", "one two");

        assertEquals("Unexpected result size", 1, searchInstance.findItems("two three", 10).size());

        assertFalse(searchInstance.getCacheStats().isPresent());
    }

    @Test
    public void backtrackMatchingStops() {
        addItem("keyword", "keyword");
        addItem("keyboard", "keyboard");

        List<String> result = searchInstance.findItems("keywZ", 10);

        assertEquals("Unexpected result size", 1, result.size());
        assertEquals("keyword", result.get(0));
    }

    @Test
    public void backtrackMatchingContinues() {
        addItem("keyword", "keyword one");
        addItem("keyboard", "keyboard");

        List<String> result = searchInstance.findItems("keyZZ", 10);

        assertEquals("Unexpected result size", 2, result.size());
        assertEquals("keyword", result.get(0));
        assertEquals("keyboard", result.get(1));
    }

    @Test
    public void exactMatching() {
        searchInstance = QuickSearch.builder()
                .withAccumulationPolicy(UNION)
                .withUnmatchedPolicy(EXACT)
                .build();

        addItem("keyword", "keyword");
        addItem("keyboard", "keyboard");

        List<String> result = searchInstance.findItems("keywZ", 10);

        assertTrue("Unexpected result size", result.isEmpty());
    }

    @Test
    public void clear() {
        addItem("test1", "onex two three");
        addItem("test2", "onexx two three");
        addItem("test3", "one two three");

        searchInstance.clear();

        checkStats(0, 0);

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
        QuickSearch<StoreItem> search = QuickSearch.<StoreItem>builder().build();

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
        assertEquals(0, searchInstance.getStats().getItems());
    }

    @Test
    public void enums() {
        // Silly to do it like this, but catches last few untouched code paths
        assertNotNull(QuickSearch.ACCUMULATION_POLICY.valueOf("UNION"));
        assertNotNull(QuickSearch.ACCUMULATION_POLICY.valueOf("INTERSECTION"));

        for (QuickSearch.ACCUMULATION_POLICY policy : QuickSearch.ACCUMULATION_POLICY.values()) {
            assertNotNull(policy.toString());
        }

        assertNotNull(QuickSearch.UNMATCHED_POLICY.valueOf("EXACT"));
        assertNotNull(QuickSearch.UNMATCHED_POLICY.valueOf("BACKTRACKING"));

        for (QuickSearch.UNMATCHED_POLICY policy : QuickSearch.UNMATCHED_POLICY.values()) {
            assertNotNull(policy.toString());
        }
    }

    /*
     * Tests boilerplate
     */

    protected void addItem(String item, String keywords) {
        addItem(searchInstance, item, keywords);
    }

    protected void addItem(QuickSearch<String> instance, String item, String keywords) {
        assertTrue("Failed to add item", instance.addItem(item, keywords));
    }

    protected void checkStats(int items, int fragments) {
        checkStats(searchInstance.getStats(), items, fragments);
        assertNotNull(searchInstance.getStats());
    }

    protected void checkStats(Stats stats, int items, int fragments) {
        assertEquals(items, stats.getItems());
        assertEquals(fragments, stats.getFragments());
    }

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
}
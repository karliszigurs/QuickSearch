/*
 *                                     //
 * Copyright 2017 Karlis Zigurs (http://zigurs.com)
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zigurs.karlis.utils.search.model.QuickSearchStats;
import com.zigurs.karlis.utils.search.model.Result;
import com.zigurs.karlis.utils.search.model.ResultItem;

import static com.zigurs.karlis.utils.search.QuickSearch.MergePolicy.INTERSECTION;
import static com.zigurs.karlis.utils.search.QuickSearch.MergePolicy.UNION;
import static com.zigurs.karlis.utils.search.QuickSearch.UnmatchedPolicy.IGNORE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class QuickSearchTest {

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
                .withKeywordsExtractor(null)
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
                .withMergePolicy(null)
                .build());
    }

    @Test
    public void checkExtractingFunctionFilters() {
        searchInstance = QuickSearch.builder()
                .withKeywordsExtractor(s -> new HashSet<>(Arrays.asList("", "one", "blue", "yellow", null)))
                .build();
        addItem("test", "onetwo three");
        assertEquals("test", searchInstance.findItem("yellow").orElse(null));
    }

    @Test
    public void testKeywordsFiltering() {
        assertNotNull(QuickSearch.builder()
                .withKeywordsExtractor(s -> new HashSet<>(Arrays.asList(s.split(","))))
                .build());
    }

    @Test
    public void testKeywordsNormaliser() {
        assertNotNull(QuickSearch.builder()
                .withKeywordNormalizer(String::trim)
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
        searchInstance = QuickSearch.builder().build();

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
        assertTrue("Search item not found",
                searchInstance.findItemsWithDetail("    ", 1).getResponseResultItems().isEmpty());
    }

    @Test
    public void itemNotFound3() {
        addItem("test", "one two three");
        assertTrue("Search item not found",
                searchInstance.findItemsWithDetail("", 1).getResponseResultItems().isEmpty());
    }

    @Test
    public void itemNotFound4() {
        addItem("test", "one two three");
        assertTrue("Search item not found",
                searchInstance.findItemsWithDetail("", 0).getResponseResultItems().isEmpty());
    }

    @Test
    public void itemNotFound5() {
        addItem("test", "one two three");
        assertTrue("Search item not found",
                searchInstance.findItemsWithDetail(null, 1).getResponseResultItems().isEmpty());
    }

    @Test
    public void itemNotFound6() {
        addItem("test", "one two three");
        assertFalse("Search item not found", searchInstance.findItemWithDetail("London").isPresent());
    }

    @Test
    public void itemNotFound7() {
        addItem("test", "one two three");
        assertTrue("Search item not found",
                searchInstance.findItemsWithDetail("four five", 1).getResponseResultItems().isEmpty());
    }

    @Test
    public void nullItemNotFound() {
        addItem("test", "one two three");
        assertFalse("Search item found", searchInstance.findItem(null).isPresent());
    }

    @Test
    public void augumentedItemFound() {
        addItem("test", "one two three");

        Optional<ResultItem<String>> result = searchInstance.findItemWithDetail("one");
        assertTrue(result.isPresent());
        assertEquals("test", result.orElse(null).getResult());
    }

    @Test
    public void augumentedItemFoundWithNull() {
        addItem("test", "one two three");
        assertFalse(searchInstance.findItemWithDetail(null).isPresent());
    }

    @Test
    public void findEmptyResult() {
        addItem("test", "one two three test");
        assertTrue(searchInstance.findItemsWithDetail("test", 0).getResponseResultItems().isEmpty());
    }

    @Test
    public void findCompletelyUnrelated() {
        addItem("test", "one two three");
        assertTrue(searchInstance.findItemsWithDetail("search engine", 0).getResponseResultItems().isEmpty());
        assertEquals("search engine", searchInstance.findItemsWithDetail("search engine", 0).getSearchString());
        assertEquals(0, searchInstance.findItemsWithDetail("search engine", 0).getRequestedMaxItems());
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

        assertEquals(2.0, searchInstance.findItemWithDetail("two")
                .orElseThrow(NullPointerException::new).getScore(), 0);
        assertEquals(4.0, searchInstance.findItemWithDetail("two three")
                .orElseThrow(NullPointerException::new).getScore(), 0);
        assertEquals(4.5, searchInstance.findItemWithDetail("two three ecting")
                .orElseThrow(NullPointerException::new).getScore(), 0);
        assertEquals(5.5, searchInstance.findItemWithDetail("two three inters")
                .orElseThrow(NullPointerException::new).getScore(), 0);
    }

    @Test
    public void findAugumentedItems() {
        addItem("test1", "one two three");
        addItem("test2", "one two three");
        addItem("test3", "one two three");

        Result<String> res = searchInstance.findItemsWithDetail("one", 10);

        assertEquals("Unexpected size", 3, res.getResponseResultItems().size());
        assertTrue("Missing keywords", searchInstance.findItemsWithDetail("one", 10)
                .getResponseResultItems().get(0).getItemKeywords().size() > 0);
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
        assertTrue("Unexpected results",
                searchInstance.findItemsWithDetail("       ", 10).getResponseResultItems().isEmpty());
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

        QuickSearchStats stats = searchInstance.getStats();

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
        assertEquals("Unexpected result size", 6, result.size());

        List<String> result2 = searchInstance.findItems("three two", 10);
        assertEquals("Unexpected result size", 6, result2.size());
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

        assertEquals("Unexpected result size", 6, res.getResponseResultItems().size());
        assertEquals("Unexpected score", 4.0, res.getResponseResultItems().get(0).getScore(), 0.0);
    }

    @Test
    public void largeUnionWorks() {
        for (int i = 0; i < 1000; i++) {
            addItem("Item" + i, "one two three");
        }

        Result<String> res = searchInstance.findItemsWithDetail("two three", 10);

        assertEquals("Unexpected result size", 10, res.getResponseResultItems().size());
        assertEquals("Unexpected score", 4.0, res.getResponseResultItems().get(0).getScore(), 0.0);
    }

    @Test
    public void intersectionWorks() {
        searchInstance = QuickSearch.builder()
                .withMergePolicy(INTERSECTION)
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
                .withMergePolicy(INTERSECTION)
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
                .withMergePolicy(INTERSECTION)
                .build();

        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four");

        assertTrue("Unexpected results", searchInstance.findItems("two four", 10).isEmpty());
    }

    @Test
    public void intersectionWorks3() {
        searchInstance = QuickSearch.builder()
                .withMergePolicy(INTERSECTION)
                .build();

        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four");

        assertTrue("Unexpected results", searchInstance.findItems("five six", 10).isEmpty());
    }

    @Test
    public void intersectionWorks4() {
        searchInstance = QuickSearch.builder()
                .withMergePolicy(INTERSECTION)
                .build();

        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four two");

        Result<String> res = searchInstance.findItemsWithDetail("two three", 10);

        assertEquals("Unexpected result size", 2, res.getResponseResultItems().size());
        assertEquals("Unexpected score", 4.0, res.getResponseResultItems().get(0).getScore(), 0.0);
        assertEquals("Unexpected score", 4.0, res.getResponseResultItems().get(1).getScore(), 0.0);
    }

    @Test
    public void parallelIntersectionWorks() {
        verifyParallelAndInterning(true, true);
    }

    @Test
    public void serialIntersectionWorks() {
        verifyParallelAndInterning(false, true);
    }

    @Test
    public void interningWorks() {
        verifyParallelAndInterning(false, true);
    }

    @Test
    public void noInterningWorks() {
        verifyParallelAndInterning(false, false);
    }

    private void verifyParallelAndInterning(boolean parallelProcessing, boolean interning) {
        searchInstance = QuickSearch.builder()
                .withMergePolicy(INTERSECTION)
                .withParallelProcessing(parallelProcessing)
                .withKeywordsInterning(interning)
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
    public void parallelUnionWorks() {
        verifyUnion(true);
    }

    @Test
    public void serialUnionWorks() {
        verifyUnion(false);
    }

    private void verifyUnion(boolean parallelProcessing) {
        searchInstance = QuickSearch.builder()
                .withMergePolicy(UNION)
                .withParallelProcessing(parallelProcessing)
                .build();

        addItem("test1", "one two");
        addItem("test2", "two three");
        addItem("test3", "three four");
        addItem("test4", "three five");
        addItem("test5", "three six");
        addItem("test6", "three seven");
        addItem("test7", "zebra cat");

        assertEquals("Unexpected result size", 6, searchInstance.findItems("two three", 10).size());
        assertEquals("Unexpected result size", 2, searchInstance.findItems("two", 10).size());
        assertEquals("Unexpected result size", 6, searchInstance.findItems("three two", 10).size());
        assertEquals("Unexpected result size", 7, searchInstance.findItems("three cat two zebra", 10).size());
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
                .withMergePolicy(UNION)
                .withUnmatchedPolicy(IGNORE)
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
            items.add(new StoreItem(i, categories[i % 3],
                    String.format("Item%d %s", i, categories[i % 3])));
        }

        items.add(new StoreItem(1, "Fantasy",
                "tolkien fantasy hardbound middle earth lord of the rings"));

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
        assertNotNull(QuickSearch.MergePolicy.valueOf("UNION"));
        assertNotNull(QuickSearch.MergePolicy.valueOf("INTERSECTION"));

        for (QuickSearch.MergePolicy policy : QuickSearch.MergePolicy.values()) {
            assertNotNull(policy.toString());
        }

        assertNotNull(QuickSearch.UnmatchedPolicy.valueOf("IGNORE"));
        assertNotNull(QuickSearch.UnmatchedPolicy.valueOf("BACKTRACKING"));

        for (QuickSearch.UnmatchedPolicy policy : QuickSearch.UnmatchedPolicy.values()) {
            assertNotNull(policy.toString());
        }
    }

    @Test
    public void shouldSurviveMisbehavingFunctions() {
        AtomicInteger normalizerCounter = new AtomicInteger(-1);
        Function<String, String> misbehavingNormalizer = s -> {
            if (s == null || s.trim().isEmpty()) {
                throw new IllegalStateException("Shouldn't reach here");
            }

            switch (normalizerCounter.incrementAndGet()) {
                case 0:
                    return null;
                case 1:
                    return "";
                case 2:
                    return "\t  ";
                default:
                    return s;
            }
        };

        /* Return a fixed set (with problems) regardless of input. */
        Function<String, Set<String>> misbehavingExtractor = s ->
                new HashSet<>(Arrays.asList(null, "", "  \t", "yellow", "green", "red", "blue"));

        QuickSearch<String> instance = QuickSearch.builder()
                .withKeywordsExtractor(misbehavingExtractor)
                .withKeywordNormalizer(misbehavingNormalizer)
                .build();

        assertTrue(instance.addItem("testItem", "overridden by extractor"));
        assertTrue(instance.findItem("doesn't matter").isPresent());
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

    private void checkStats(int items, int fragments) {
        checkStats(searchInstance.getStats(), items, fragments);
        assertNotNull(searchInstance.getStats());
    }

    private void checkStats(QuickSearchStats stats, int items, int fragments) {
        assertEquals(items, stats.getItems());
        assertEquals(fragments, stats.getFragments());
    }

    private static final class StoreItem implements Comparable<StoreItem> {

        private final int itemIdentifier;
        private final String category;
        private final String description;

        private StoreItem(int itemIdentifier, String category, String description) {
            this.itemIdentifier = itemIdentifier;
            this.category = category;
            this.description = description;
        }

        private String getCategory() {
            return category;
        }

        private String getDescription() {
            return description;
        }

        @Override
        public int compareTo(StoreItem item) {
            return Integer.compare(itemIdentifier, item.itemIdentifier);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof StoreItem
                    && itemIdentifier == ((StoreItem) obj).itemIdentifier;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(itemIdentifier);
        }
    }
}

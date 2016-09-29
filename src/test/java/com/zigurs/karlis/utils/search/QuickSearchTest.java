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

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

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

    @org.junit.Before
    public void setUp() throws Exception {
        searchInstance = new QuickSearch<>();
    }

    @org.junit.After
    public void tearDown() throws Exception {
        searchInstance.clear();
        searchInstance = null;
    }

    @org.junit.Test
    public void addItem() throws Exception {
        assertTrue("Failed to add a new search item.",
                searchInstance.addItem("item", "one two three"));

        assertFalse("Item added with no keywords",
                searchInstance.addItem("empty item", ""));
    }

    @org.junit.Test
    public void addEmptyItem() throws Exception {
        assertFalse("Failed to add a new search item.",
                searchInstance.addItem("item", ""));
    }

    @org.junit.Test
    public void addEmptyItem2() throws Exception {
        assertFalse("Failed to add a new search item.",
                searchInstance.addItem("", "one two three"));
    }

    @org.junit.Test
    public void addNullItem() throws Exception {
        assertFalse("Failed to add a new search item.",
                searchInstance.addItem(null, "one two three"));
    }

    @org.junit.Test
    public void addNullItem2() throws Exception {
        assertFalse("Failed to add a new search item.",
                searchInstance.addItem("item", null));
    }

    @org.junit.Test
    public void removeItem() throws Exception {
        assertTrue("Failed to add search item",
                searchInstance.addItem("toBeRemoved", "one two three"));

        assertTrue("Failed to remove search item",
                searchInstance.removeItem("toBeRemoved"));
    }

    @org.junit.Test
    public void findItem() throws Exception {
        assertTrue("Failed to add search item",
                searchInstance.addItem("test", "one two three"));

        assertNotNull("Search item not found",
                searchInstance.findItem("one"));

    }

    @org.junit.Test
    public void findItems() throws Exception {
        assertTrue("Failed to add search item",
                searchInstance.addItem("test1", "one two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test2", "one two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test3", "one two three"));

        assertTrue("Unexpected size",
                searchInstance.findItems("one", 10).size() == 3
        );
    }

    @org.junit.Test
    public void findNoItems() throws Exception {
        assertTrue("Failed to add search item",
                searchInstance.addItem("test1", "one two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test2", "one two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test3", "one two three"));

        assertTrue("Unexpected size",
                searchInstance.findItems("       ", 10).size() == 0
        );
    }

    @org.junit.Test
    public void exerciseMultiStepMapping() throws Exception {
        String exerciseString = "aquickbrownfoxjumpsoverthelazydog";

        for (int i = 0; i < exerciseString.length(); i++) {
            searchInstance.addItem("test" + i, exerciseString.substring(0, i));
        }

        for (int i = 0; i < exerciseString.length(); i++) {
            searchInstance.addItem("test" + i, exerciseString.substring(i, exerciseString.length()));
        }

        assertEquals(10, searchInstance.findItems("e ex exe exer exerc i is ise", 10).size());
    }

    @org.junit.Test
    public void statsAreEmpty() throws Exception {
        assertEquals(searchInstance.getStats(), "0 items; 0 keywords; 0 fragments");
    }

    @org.junit.Test
    public void statsAreFull() throws Exception {
        String exerciseString = "aquickbrownfoxjumpsoverthelazydog";

        for (int i = 0; i < exerciseString.length(); i++) {
            searchInstance.addItem("test" + i, exerciseString.substring(0, i));
        }

        for (int i = 0; i < exerciseString.length(); i++) {
            searchInstance.addItem("test" + i, exerciseString.substring(i, exerciseString.length()));
        }

        assertEquals(10, searchInstance.findItems("e ex exe exer exerc i is ise", 10).size());

        assertEquals(searchInstance.getStats(), "33 items; 63 keywords; 554 fragments");
    }

    @org.junit.Test
    public void noErrorsInLengthMatcher() throws Exception {
        QuickSearch<String> alternativeConfig = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.LENGTH_MATCH_SCORER,
                QuickSearch.DEFAULT_MINIMUM_KEYWORD_LENGTH
        );

        String exerciseString = "aquickbrownfoxjumpsoverthelazydog";

        for (int i = 0; i < exerciseString.length(); i++) {
            alternativeConfig.addItem("test" + i, exerciseString.substring(0, i));
        }

        for (int i = 0; i < exerciseString.length(); i++) {
            alternativeConfig.addItem("test" + i, exerciseString.substring(i, exerciseString.length()));
        }

        assertEquals(10, alternativeConfig.findItems("e ex exe exer exerc i is ise", 10).size());

        assertEquals(alternativeConfig.getStats(), "33 items; 63 keywords; 554 fragments");
    }

    @org.junit.Test
    public void testExpandKeywords() throws Exception {
        String exerciseString = "aquickbrownfoxjumpsoverthelazydog";

        for (int i = 0; i < exerciseString.length(); i++) {
            searchInstance.addItem("test" + i, exerciseString.substring(0, i));
        }

        for (int i = 0; i < exerciseString.length(); i++) {
            searchInstance.addItem("test" + i, exerciseString.substring(i, exerciseString.length()));
        }

        assertEquals(10, searchInstance.findItems("e ex exe exer exerc i is ise", 10).size());

        String stats = searchInstance.getStats();

        //Repeat, but with different item names (forcing to expand the keywords
        for (int i = 0; i < exerciseString.length(); i++) {
            searchInstance.addItem("test" + (i % 3), exerciseString.substring(0, i));
        }

        for (int i = 0; i < exerciseString.length(); i++) {
            searchInstance.addItem("test" + ((i % 3) + 3), exerciseString.substring(i, exerciseString.length()));
        }

        assertEquals(10, searchInstance.findItems("e ex exe exer exerc i is ise", 10).size());

        assertEquals(stats, searchInstance.getStats());
    }


    @org.junit.Test
    public void findItemsLimit() throws Exception {
        assertTrue("Failed to add search item",
                searchInstance.addItem("test1", "one two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test2", "one two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test3", "one two three"));

        assertTrue("Unexpected size",
                searchInstance.findItems("one", 3).size() == 3);

        assertTrue("Unexpected size",
                searchInstance.findItems("one", 2).size() == 2);
    }

    @org.junit.Test
    public void itemsRanking() throws Exception {
        assertTrue("Failed to add search item",
                searchInstance.addItem("test1", "onex two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test2", "onexx two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test3", "one two three"));

        List<String> result = searchInstance.findItems("one", 10);

        assertTrue("Unexpected result size",
                result.size() == 3);

        assertEquals("test3", result.get(0));
        assertEquals("test1", result.get(1));
        assertEquals("test2", result.get(2));
    }

    @org.junit.Test
    public void testDirectMatching() throws Exception {
        assertTrue("Failed to add search item",
                searchInstance.addItem("test1", "keyword"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test2", "keyboard"));


        List<String> result = searchInstance.findItems("keyword", 10);

        assertTrue("Unexpected result size",
                result.size() == 1);

        assertEquals("test1", result.get(0));
    }


    @org.junit.Test
    public void testIntersectionWorks() throws Exception {
        assertTrue("Failed to add search item",
                searchInstance.addItem("test1", "one two"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test2", "two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test3", "twee onaa"));


        List<String> result = searchInstance.findItems("two ", 10);

        assertTrue("Unexpected result size",
                result.size() == 2);
    }

    @org.junit.Test
    public void testBacktrackMatchingStops() throws Exception {
        assertTrue("Failed to add search item",
                searchInstance.addItem("keyword", "keyword"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("keyboard", "keyboard"));


        List<String> result = searchInstance.findItems("keywZ", 10);

        assertTrue("Unexpected result size",
                result.size() == 1);

        assertEquals("keyword", result.get(0));
    }

    @org.junit.Test
    public void testBacktrackMatchingContinues() throws Exception {
        assertTrue("Failed to add search item",
                searchInstance.addItem("keyword", "keyword one"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("keyboard", "keyboard"));

        List<String> result = searchInstance.findItems("keyZZ", 10);

        assertTrue("Unexpected result size",
                result.size() == 2);

        assertEquals("keyword", result.get(0));
        assertEquals("keyboard", result.get(1));
    }

    @org.junit.Test
    public void testRemove() throws Exception {
        assertTrue("Failed to add search item",
                searchInstance.addItem("test1", "onex two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test2", "onexx two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test3", "one two three"));

        assertTrue("Unexpected result size",
                searchInstance.findItems("one", 10).size() == 3);

        for (int i = 1; i < 4; i++) {
            searchInstance.removeItem("test" + i);
        }

        assertTrue("Unexpected result size",
                searchInstance.findItems("one", 10).size() == 0);
    }

    @org.junit.Test
    public void testRemoveTwice() throws Exception {
        assertTrue("Failed to add search item",
                searchInstance.addItem("test1", "onex two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test2", "onexx two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test3", "one two three"));

        assertTrue("Unexpected result size",
                searchInstance.findItems("one", 10).size() == 3);

        for (int i = 1; i < 4; i++) {
            searchInstance.removeItem("test" + i);
        }

        for (int i = 1; i < 4; i++) {
            searchInstance.removeItem("test" + i);
        }

        assertTrue("Unexpected result size",
                searchInstance.findItems("one", 10).size() == 0);
    }

    @org.junit.Test
    public void clear() throws Exception {
        assertTrue("Failed to add search item",
                searchInstance.addItem("test1", "onex two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test2", "onexx two three"));

        assertTrue("Failed to add search item",
                searchInstance.addItem("test3", "one two three"));

        searchInstance.clear();

        assertNull("Unexpected result",
                searchInstance.findItem("one"));
    }

    @org.junit.Test
    public void quickLoadTest() throws Exception {
        for (String[] items : USA_STATES) {
            assertTrue(
                    "Failed to add item",
                    searchInstance.addItem(items[0], String.format("%s %s %s", items[1], items[2], items[3]))
            );
        }

        long startTime = System.currentTimeMillis();
        int iterationsCount = 1000;

        for (int i = 0; i < iterationsCount; i++) {
            assertTrue("No results found?",
                    searchInstance.findItems(USA_STATES[i % USA_STATES.length][1].substring(0, 3), 10).size() > 0);
        }

        long totalTime = System.currentTimeMillis() - startTime;

        assertTrue("Shouldn't be anywhere near this slow...",
                totalTime < 1000);


        // Note - do not use the crude perf sanity check above for any kind of benchmarking.
        // JVM is notorious for under-performing for the first few seconds after startup
        // until the warmup is finished, caches filled and runtime optimisations kick in.
    }

    @Test
    public void objectTypeExercise() {
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
        assertNull(search.findItem("missing jeans"));
        assertNotNull(search.findItem("item jack"));

        assertEquals(6, search.findItems("item jack 20", 10).size());
        assertEquals(6, search.findItems("sh 50", 10).size());

        items.stream()
                .filter(item -> categories[0].equals(item.getCategory()))
                .forEach(search::removeItem);

        assertEquals(10, search.findItems("red shoes", 10).size());
        assertNotNull(search.findItems("midd", 10));
        search.clear();
    }
}

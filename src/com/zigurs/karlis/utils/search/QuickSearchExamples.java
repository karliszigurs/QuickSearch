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

import java.util.LinkedList;
import java.util.List;

public class QuickSearchExamples {

    private final static String[][] USStatesData = {
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

    private static class StoreItem {

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

    public static void main(String[] args) {
        // basic functionality example
        useExample();

        // due to JVM warmup and optimisation delay we repeat the test a few times
        // after first couple of runs JVM should stabilise on a number
        // that will be representative on your hardware
        System.out.println("Starting search benchmarks...");
        searchBenchmark();
        searchBenchmark();
        searchBenchmark();
        searchBenchmark();
        searchBenchmark();
    }

    private static void useExample() {
        // create a few StoreItem instances
        List<StoreItem> items = new LinkedList<>();
        String[] categories = new String[]{"Shoes", "Jackets", "Skirts"};

        for (int i = 0; i < 1_000; i++) {
            items.add(
                    new StoreItem(
                            i,
                            "StoreItem",
                            categories[i % 3],
                            String.format("Item%d %s", i, categories[i % 3])
                    )
            );
        }

        // add a completely unrelated item
        items.add(
                new StoreItem(1,
                        "Lord of The Rings",
                        "Fantasy",
                        "tolkien fantasy hardbound middle earth lord of the rings"
                )
        );

        // create a quick search for StoreItems
        QuickSearch<StoreItem> search = new QuickSearch<>();

        // populate quick search data
        for (StoreItem item : items) {
            search.addItem(item, item.getDescription());
        }

        // do a few quick searches
        System.out.println("Found: " + search.findTopItem("item jack 20"));
        System.out.println("Found: " + search.findTopItem("missing jeans")); // no item found here

        // find top 10 hits for couple of queries
        System.out.println("Found: " + search.findItems("item jack 20", 10));
        System.out.println("Found: " + search.findItems("sh 50", 10));

        // remove all Shoes
        items.stream()
                .filter(item -> categories[0].equals(item.getCategory()))    // filter by shoes
                .forEach(search::removeItem);                   // remove filtered elements

        // search for the shoes again
        System.out.println("Found: " + search.findItems("red shoes", 10));   // No shoes found

        // search for the rogue element
        System.out.println("Found: " + search.findItems("midd", 10));

        // clear the store
        search.clear();
        // search can be repopulated again
    }

    private static void searchBenchmark() {
        // Create search
        QuickSearch<String> search = new QuickSearch<>();

        // populate search
        long startTime = System.currentTimeMillis();
        int itemsCount = 0;

        for (int i = 0; i < 10; i++) {
            for (String[] state : USStatesData) {
                search.addItem(
                        state[2] + ", " + state[1] + i,
                        state[0] + "," + state[1] + "," + state[2] + "," + state[3] + "," + i
                );
                itemsCount++;
            }
        }

        System.out.println(
                String.format(
                        "%d items added in %dms.",
                        itemsCount,
                        System.currentTimeMillis() - startTime)
        );

        // benchmark searches
        startTime = System.currentTimeMillis();

        int numberOfSearches = 1_000_000;
        long totalResultsCount = 0;

        for (int i = 0; i < numberOfSearches; i++) {
            List<String> foundStates = search.findItems(
                    USStatesData[i % USStatesData.length][0],
                    10
            );

            totalResultsCount += foundStates.size();
        }

        // print statistics
        long timeTaken = System.currentTimeMillis() - startTime;
        System.out.println(
                String.format(
                        "%,d searches in %,ds - %,dk searches second, %,.3fus per search. %,d results returned.",
                        numberOfSearches,
                        timeTaken,
                        numberOfSearches / timeTaken,
                        (double) 1000 / (numberOfSearches / timeTaken),
                        totalResultsCount
                )
        );
    }
}
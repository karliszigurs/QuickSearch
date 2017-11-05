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

import org.github.jamm.MemoryMeter;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QuickSearchMemoryUseTest {

    private static final String[][] USA_STATES = {
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

    @Test
    public void fewItemsCountCheck() {
        /*
         * As measured on:
         *   java version "1.8.0_111"
         *   Java(TM) SE Runtime Environment (build 1.8.0_111-b14)
         *   Java HotSpot(TM) 64-Bit Server VM (build 25.111-b14, mixed mode)
         *
         * Absolute memory profile may and will change depending on JVM version and options.
         */
        final long plainTarget = 901_992;
        final long internTarget = 579_728;
        final int fewItemsCount = 1_000;

        assertTrue("Expected memory ceiling exceeded (see test source)", plainTarget * 3 > measureMemoryUseImpl(fewItemsCount, false));
        assertTrue("Expected memory ceiling exceeded (see test source)", internTarget * 3 > measureMemoryUseImpl(fewItemsCount, true));
    }

    @Ignore
    @Test
    public void manyItemsCountCheck() {
        /*
         * As measured on:
         *   java version "1.8.0_111"
         *   Java(TM) SE Runtime Environment (build 1.8.0_111-b14)
         *   Java HotSpot(TM) 64-Bit Server VM (build 25.111-b14, mixed mode)
         *
         * Absolute memory profile may and will change depending on JVM version and options.
         */
        final long plainTarget = 52_685_816;
        final long internTarget = 19_463_872;
        final int manyItemsCount = 100_000;

        assertTrue("Expected memory ceiling exceeded (see test source)", plainTarget * 1.1 > measureMemoryUseImpl(manyItemsCount, false));
        assertTrue("Expected memory ceiling exceeded (see test source)", internTarget * 1.1 > measureMemoryUseImpl(manyItemsCount, true));
    }

    private long measureMemoryUseImpl(final int itemsCount, boolean intern) {
        QuickSearch<String> searchInstance;
        if (intern)
            searchInstance = QuickSearch.builder().withKeywordsInterning().build();
        else
            searchInstance = QuickSearch.builder().build();

        for (int i = 0; i < itemsCount; i++) {
            String[] items = USA_STATES[i % USA_STATES.length];
            searchInstance.addItem(items[0] + "-" + i, String.format("%s %s %s %s", items[0], items[1], items[2], items[3]));
        }

        // exercise it a bit
        for (char character = 'a'; character <= 'z'; character++) {
            for (char character2 = 'a'; character2 <= 'z'; character2++) {
                searchInstance.findItem(String.valueOf(character) + String.valueOf(character2));
                searchInstance.findItem(String.valueOf(character2));
                searchInstance.findItem(String.valueOf(character));
            }
        }

        assertEquals(10, searchInstance.findItems("a", 10).size());

        MemoryMeter meter = new MemoryMeter().withGuessing(MemoryMeter.Guess.ALWAYS_UNSAFE);
        return meter.measureDeep(searchInstance);
    }
}

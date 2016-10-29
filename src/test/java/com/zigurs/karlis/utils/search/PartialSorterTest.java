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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PartialSorterTest {

    @Test
    public void limits() {
        List<Double> testSet = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            testSet.add((double) i);
        }

        List<Double> list = PartialSorter.sortAndLimit(testSet, 1, Comparator.naturalOrder());
        assertEquals(1, list.size());
    }

    @Test
    public void limits1() {
        List<Double> testSet = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            testSet.add((double) i);
        }

        List<Double> list = PartialSorter.sortAndLimit(testSet, 0, Comparator.naturalOrder());
        assertEquals(0, list.size());
    }

    @Test
    public void limits2() {
        List<Double> testSet = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            testSet.add((double) i);
        }

        List<Double> list = PartialSorter.sortAndLimit(testSet, -1, Comparator.naturalOrder());
        assertEquals(0, list.size());
    }

    @Test
    public void customSorting() {
        List<Double> testSet = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            testSet.add((double) i);
        }

        testSet.add(1000.0);

        Collections.shuffle(testSet);

        List<Double> list = PartialSorter.sortAndLimit(testSet, 10, Comparator.naturalOrder());
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

        List<Double> list = PartialSorter.sortAndLimit(testSet, 1, Comparator.reverseOrder());

        assertEquals(1000.0, list.get(0), 0);
        assertEquals(1, list.size());
    }

    @Test
    public void dummyTest() {
        PartialSorter sorter = new PartialSorter();
        assertNotEquals(0, sorter.hashCode());
    }
}
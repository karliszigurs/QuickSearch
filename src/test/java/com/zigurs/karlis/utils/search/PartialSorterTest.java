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

import java.util.*;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

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
    public void sortingImplementationsBenchmark() {
        /*
         * Microbenchmarking caveats apply. Make sure the number of iterations is
         * large enough and JVM is given a chance to warm up and apply the
         * optimisations on the code exercised.
         */
        List<Double> testList = new LinkedList<>();

        int listSize = 10_000;
        for (int i = 0; i < listSize; i++) {
            testList.add((double) i);
        }

        Collections.shuffle(testList);

        Map<Double, Double> testMap = new LinkedHashMap<>();
        testList.forEach(d -> testMap.put(d, d));

        int iterationsCount = 100;
        int topItems = 10;

        Comparator<Double> comparator = Comparator.reverseOrder();
        double topResultShouldBe = (double) listSize - 1;

        sortBenchIteration("Warmup: ", testMap.values(), iterationsCount, topItems, comparator, topResultShouldBe);
        sortBenchIteration("Warmer: ", testMap.values(), iterationsCount, topItems, comparator, topResultShouldBe);
        sortBenchIteration("Warm:   ", testMap.values(), iterationsCount, topItems, comparator, topResultShouldBe);
    }

    private void sortBenchIteration(String prefix, Collection<Double> testList, int iterationsCount, int topItems, Comparator<Double> comparator, double topResultShouldBe) {
        LongSupplier time = System::nanoTime;
        int timeDivisor = 1000;
        String unit = "us";

        long totalTimePartial = 0;
        long totalTimeCollections = 0;
        long totalTimeStreamed = 0;
        long totalTimeParallel = 0;
        long startTime;

        for (int i = 0; i < iterationsCount; i++) {
            startTime = time.getAsLong();
            List<Double> listCropped = PartialSorter.sortAndLimit(testList, topItems, comparator);
            assertEquals(topResultShouldBe, listCropped.get(0), 0);
            totalTimePartial += time.getAsLong() - startTime;

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

        long ttPartial = (totalTimePartial / iterationsCount) / timeDivisor;
        long ttCollections = (totalTimeCollections / iterationsCount) / timeDivisor;
        long ttStreamed = (totalTimeStreamed / iterationsCount) / timeDivisor;
        long ttParallel = (totalTimeParallel / iterationsCount) / timeDivisor;

        System.out.println(String.format("%6$s %1$,d%5$s PartialSorter;\t%2$,d%5$s Collections::sort;\t%3$,d%5$s Stream::sorted;\t%4$,d%5$s ParallelStream::sorted;",
                ttPartial,
                ttCollections,
                ttStreamed,
                ttParallel,
                unit,
                prefix
        ));

        assertTrue(ttPartial < ttCollections);
        assertTrue(ttPartial < ttStreamed);
        assertTrue(ttPartial < ttParallel);
    }

    @Test
    public void dummyTest() {
        PartialSorter sorter = new PartialSorter();
        assertNotEquals(0, sorter.hashCode());
    }
}
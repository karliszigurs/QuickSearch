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

import org.github.jamm.MemoryMeter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.zigurs.karlis.utils.search.QuickSearch.CANDIDATE_ACCUMULATION_POLICY.INTERSECTION;
import static com.zigurs.karlis.utils.search.QuickSearch.UNMATCHED_POLICY.BACKTRACKING;
import static org.junit.Assert.assertTrue;

public class QuickSearchBenchmarksTest {

    private static boolean RUN_FULL_BENCHMARKS = true;

    private static final String[][] USA_STATES = QuickSearchTest.USA_STATES;

    private QuickSearch<String> searchInstance;

    @Before
    public void setUp() throws Exception {
        searchInstance = new QuickSearch<>();
    }

    @After
    public void tearDown() throws Exception {
        searchInstance = null;
    }

    @Test
    public void intersectionSearchBenchmark() throws Exception {
        searchInstance = new QuickSearch<>(
                QuickSearch.DEFAULT_KEYWORDS_EXTRACTOR,
                QuickSearch.DEFAULT_KEYWORD_NORMALIZER,
                QuickSearch.DEFAULT_MATCH_SCORER,
                BACKTRACKING,
                INTERSECTION);

        long st = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            String[] items = USA_STATES[i % USA_STATES.length];
            searchInstance.addItem(items[0] + "-" + i, String.format("%s %s %s %s", items[0], items[1], items[2], items[3]));
        }
        System.out.println("Loaded in " + (System.currentTimeMillis() - st) + "ms");

        final int iterations = RUN_FULL_BENCHMARKS ? 100_000 : 1000;

        searchIteration(iterations, "Warmup:", "a b c d e");
        searchIteration(iterations, "Simple:", "w");
        searchIteration(iterations, "Medium:", "wa sh");
        searchIteration(iterations, "Brutal:", "a b c d e g h i l m n p r s u v y");
        assertTrue(true);
    }

    private void searchIteration(int iterations, String prefix, String searchString) {
        long stSimple = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            assertTrue(searchInstance.findItems(searchString, 10).size() > 0);
        }
        long ttSimple = System.currentTimeMillis() - stSimple;
        System.out.println(String.format("%s\t%,f k/sec", prefix, (double) iterations / ttSimple));
    }

    @Test
    public void multiThreadBenchmark() throws Exception {
        for (String[] items : USA_STATES) {
            searchInstance.addItem(items[0], String.format("%s %s %s", items[1], items[2], items[3]));
        }

        multiThreadTestIteration("Warmup:");
        multiThreadTestIteration("Warm  :");
        assertTrue(true);
    }

    private void multiThreadTestIteration(final String label) throws InterruptedException {
        int threads = 4;
        int iterationsPerThread = RUN_FULL_BENCHMARKS ? 1_000_000 : 1_000;

        CountDownLatch latch = new CountDownLatch(threads);
        AtomicLong wrote = new AtomicLong(0L);

        // Writing thread
        Thread wt = new Thread(() -> {
            int i = 0;
            while (latch.getCount() > 0) {
                if (Thread.interrupted())
                    return;

                searchInstance.addItem("new item" + i, "few new keywords" + i++);
                wrote.incrementAndGet();

                //noinspection EmptyCatchBlock
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        wt.setPriority(Thread.MAX_PRIORITY);
        wt.start();

        Long startTime = System.currentTimeMillis();

        List<Thread> threadsList = new LinkedList<>();
        // Reading threads
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                multiThreadTestThread(iterationsPerThread, label);
                latch.countDown();
            });
            threadsList.add(t);
            t.start();
        }

        latch.await(60, TimeUnit.SECONDS);

        if (latch.getCount() > 0) {
            System.out.println("Timed out before completing all iterations, results are invalid");
            threadsList.forEach(Thread::interrupt);
        }

        long totalTime = System.currentTimeMillis() - startTime;

        System.out.println(String.format(
                "%s\t - Aggregate throughput: %,dk ops/sec, %,d writes.",
                label,
                ((iterationsPerThread / totalTime) * threads),
                wrote.get()
        ));
    }

    private void multiThreadTestThread(final int iterations, final String label) {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            assertTrue(searchInstance.findItems(USA_STATES[i % USA_STATES.length][1].substring(0, 3), 10).size() > 0);
            if (Thread.interrupted())
                return;
        }
        long totalTime = System.currentTimeMillis() - startTime;

        System.out.println(
                String.format("%s\tTook %dms, %dk ops/sec.",
                        label,
                        totalTime,
                        iterations / totalTime
                )
        );
    }

    @Test
    public void operationsBenchmark() throws Exception {
        for (String[] items : USA_STATES) {
            searchInstance.addItem(items[0], String.format("%s %s %s", items[1], items[2], items[3]));
        }
        operationsBenchIteration("Warmup:");
        operationsBenchIteration("Warmed:");
        assertTrue(true);
    }

    private void operationsBenchIteration(final String label) throws InterruptedException {
        int threads = 1;
        int iterationsPerThread = RUN_FULL_BENCHMARKS ? 1_000_000 : 1000;
        CountDownLatch latch = new CountDownLatch(threads);

        // ops threads
        Long startTime = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                operationsBenchThread(iterationsPerThread, label);
                latch.countDown();
            });
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();
        }

        latch.await(60, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println(
                String.format(
                        "%s - Aggregate throughput: %,dk ops/sec",
                        label,
                        ((iterationsPerThread / totalTime) * threads)
                )
        );
    }

    private void operationsBenchThread(final int iterations, final String label) {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            searchInstance.addItem(USA_STATES[i % USA_STATES.length][1].substring(0, 3), USA_STATES[i % USA_STATES.length][0] + " " + USA_STATES[i % USA_STATES.length][2]);
            searchInstance.removeItem(USA_STATES[i % USA_STATES.length][1].substring(0, 3));
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println(String.format("%s\tTook %dms, %dk ops/sec",
                label,
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

        final int itemsCount = RUN_FULL_BENCHMARKS ? 100_000 : 1000;

        for (int i = 0; i < itemsCount; i++) {
            String[] items = USA_STATES[i % USA_STATES.length];
            searchInstance.addItem(items[0] + "-" + i, String.format("%s %s %s %s", items[0], items[1], items[2], items[3]));
        }

        // Force population of caches
        for (char character = 'a'; character <= 'z'; character++) {
            for (char character2 = 'a'; character2 <= 'z'; character2++) {
                searchInstance.findItem(String.valueOf(character) + String.valueOf(character2));
            }
        }

        MemoryMeter meter = new MemoryMeter().withGuessing(MemoryMeter.Guess.ALWAYS_UNSAFE);

        long measured = meter.measureDeep(searchInstance);

        System.out.println("Memory consumption is " + measured);

        /*
         * Measured on Java 1.8.0_102 for 100_000 items set above
         */
//        final long JDK_COLLECTIONS_TARGET = 117_416_608; // 100k items
//        final long GUAVA_MULTIMAP_TARGET = 104_140_960; // 100k items
//        final long CUSTOM_TREE_TARGET = 54_255_008; // 100k items
//        final long CUSTOM_TREE_TARGET = 54_255_008; // 100k items
//        final long CUSTOM_TREE_TARGET_INTERN = 21_848_000; // 100k items + keyword intern.
//        final long SMALL_TEST_TARGET = 587_865; // itemsCount = 1000;

//        assertTrue("Calculated size exceeds target", measured < ((RUN_FULL_BENCHMARKS ? CUSTOM_TREE_TARGET_INTERN : SMALL_TEST_TARGET) * 1.1));
    }
}

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
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class QuickSearchBenchmarksTest {

    private static final boolean RUN_FULL_BENCHMARKS = true;

    private static final String[][] USA_STATES = QuickSearchTest.USA_STATES;

    @Rule
    public TestWatcher testWatcher = new TestWatcher() {
        @Override
        protected void starting(final Description description) {
            String methodName = description.getMethodName();
            String className = description.getClassName();
            className = className.substring(className.lastIndexOf('.') + 1);
            System.out.println(String.format("Starting %s (%s)", methodName, className));
        }

        @Override
        protected void finished(Description description) {
            String methodName = description.getMethodName();
            String className = description.getClassName();
            className = className.substring(className.lastIndexOf('.') + 1);
            System.out.println(String.format("Finished %s (%s)", methodName, className));
        }
    };

    /*
     * Benchmark cache impact
     */

    @Test
    public void benchmarkWithUnlimitedCache() throws Exception {
        QuickSearch<String> searchInstance = QuickSearch.builder()
                .withCacheLimit(-1)
                .build();

        if (RUN_FULL_BENCHMARKS) {
            multiKeywordBenchmarkRun(searchInstance, 10.0, 75.0, 75.0, 3.0);
            assertEquals(
                    "{ \"hits\": 2499980, \"misses\": 20, \"uncacheable\": 0, \"evictions\": 0, \"size\": 3246, \"keysCached\": 20, \"maxSize\": 35791394, \"disabled\": false, \"keyLimit\": 10 }",
                    searchInstance.getCacheStats()
            );
        } else {
            multiKeywordBenchmarkRun(searchInstance, 1.0, 10.0, 10.0, 1.0);
            assertEquals(
                    "{ \"hits\": 24980, \"misses\": 20, \"uncacheable\": 0, \"evictions\": 0, \"size\": 3246, \"keysCached\": 20, \"maxSize\": 35791394, \"disabled\": false, \"keyLimit\": 10 }",
                    searchInstance.getCacheStats()
            );
        }
    }

    @Test
    public void benchmarkWithDefaultCache() throws Exception {
        QuickSearch<String> searchInstance = QuickSearch.builder()
                .withCache()
                .build();

        if (RUN_FULL_BENCHMARKS) {
            multiKeywordBenchmarkRun(searchInstance, 10.0, 75.0, 75.0, 3.0);
            assertEquals(
                    "{ \"hits\": 2499980, \"misses\": 20, \"uncacheable\": 0, \"evictions\": 0, \"size\": 3246, \"keysCached\": 20, \"maxSize\": 1747626, \"disabled\": false, \"keyLimit\": 10 }",
                    searchInstance.getCacheStats()
            );
        } else {
            multiKeywordBenchmarkRun(searchInstance, 1.0, 10.0, 10.0, 1.0);
            assertEquals(
                    "{ \"hits\": 24980, \"misses\": 20, \"uncacheable\": 0, \"evictions\": 0, \"size\": 3246, \"keysCached\": 20, \"maxSize\": 1747626, \"disabled\": false, \"keyLimit\": 10 }",
                    searchInstance.getCacheStats()
            );
        }
    }

    @Test
    public void benchmarkWithSillyCache() throws Exception {
        QuickSearch<String> searchInstance = QuickSearch.builder()
                .withCacheLimit(1024)
                .build();

        if (RUN_FULL_BENCHMARKS) {
            multiKeywordBenchmarkRun(searchInstance, 1.0, 30.0, 30.0, 0.1);
            assertEquals(
                    "{ \"hits\": 0, \"misses\": 10, \"uncacheable\": 2499990, \"evictions\": 10, \"size\": 0, \"keysCached\": 0, \"maxSize\": 17, \"disabled\": true, \"keyLimit\": 0 }",
                    searchInstance.getCacheStats()
            );
        } else {
            multiKeywordBenchmarkRun(searchInstance, 0.1, 5.0, 5.0, 0.1);
            assertEquals(
                    "{ \"hits\": 0, \"misses\": 10, \"uncacheable\": 24990, \"evictions\": 10, \"size\": 0, \"keysCached\": 0, \"maxSize\": 17, \"disabled\": true, \"keyLimit\": 0 }",
                    searchInstance.getCacheStats()
            );
        }
    }

    @Test
    public void benchmarkWithoutCache() throws Exception {
        QuickSearch<String> searchInstance = QuickSearch.builder()
                .build();

        if (RUN_FULL_BENCHMARKS)
            multiKeywordBenchmarkRun(searchInstance, 1.0, 30.0, 30.0, 0.1);
        else
            multiKeywordBenchmarkRun(searchInstance, 0.1, 5.0, 5.0, 0.1);

        assertEquals("", searchInstance.getCacheStats());
    }

    private void multiKeywordBenchmarkRun(final QuickSearch<String> searchInstance,
                                          final double targetWarm,
                                          final double targetSimple,
                                          final double targetMedium,
                                          final double targetBrutal) {
        final int iterations = RUN_FULL_BENCHMARKS ? 100_000 : 1_000;

        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            String[] items = USA_STATES[i % USA_STATES.length];
            searchInstance.addItem(items[0] + "-" + i, String.format("%s %s %s %s", items[0], items[1], items[2], items[3]));
        }
        assertTrue("Data loading taken unexpectedly long", (System.currentTimeMillis() - startTime) < 1000);

        final double tWarm = searchIteration(searchInstance, iterations, "Warmup:", "a b c d e");
        final double tSimple = searchIteration(searchInstance, iterations, "Simple:", "montgomery");
        final double tMedium = searchIteration(searchInstance, iterations, "Medium:", "wa sh");
        final double tBrutal = searchIteration(searchInstance, iterations, "Brutal:", "a b c d e g h i l m n p r s u v y");

        assertTrue(tWarm > targetWarm);
        assertTrue(tSimple > targetSimple);
        assertTrue(tMedium > targetMedium);
        assertTrue(tBrutal > targetBrutal);
    }

    private double searchIteration(QuickSearch<String> searchInstance, int iterations, String prefix, String searchString) {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            assertFalse(searchInstance.findItems(searchString, 10).isEmpty());
        }

        double throughput = (double) iterations / (System.currentTimeMillis() - startTime);

        System.out.println(String.format("%s\t%,.2fk ops / sec", prefix, throughput));

        return throughput;
    }

    /*
     * Benchmark add/remove operations
     */

    @Test
    public void benchmarkOperations() throws Exception {
        QuickSearch<String> searchInstance = QuickSearch.builder().build();

        for (String[] items : USA_STATES) {
            searchInstance.addItem(items[0], String.format("%s %s %s", items[1], items[2], items[3]));
        }

        final double tWarmup = operationsBenchIteration("Warmup:", searchInstance);
        final double tWarmed = operationsBenchIteration("Warmed:", searchInstance);
        assertTrue(tWarmup > 1.0);
        assertTrue(tWarmed > 1.0);
    }

    private double operationsBenchIteration(final String label, QuickSearch<String> searchInstance) throws InterruptedException {
        int threads = 4;
        int iterationsPerThread = RUN_FULL_BENCHMARKS ? 1_000_000 : 1_000;

        final long totalTime = executeMultipleThreads(threads,
                () -> operationsBenchThread(searchInstance, iterationsPerThread, label),
                60);

        final double throughput = ((double) iterationsPerThread / totalTime) * threads;

        System.out.println(
                String.format(
                        "%s\tAggregate throughput: %,.2fk ops / sec",
                        label,
                        throughput
                )
        );

        return throughput;
    }

    private double operationsBenchThread(final QuickSearch<String> searchInstance,
                                         final int iterations,
                                         final String label) {
        final long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            searchInstance.addItem(USA_STATES[i % USA_STATES.length][1].substring(0, 3), USA_STATES[i % USA_STATES.length][0] + " " + USA_STATES[i % USA_STATES.length][2]);
            searchInstance.removeItem(USA_STATES[i % USA_STATES.length][1].substring(0, 3));
        }

        final long totalTime = System.currentTimeMillis() - startTime;
        final double throughput = (double) iterations / totalTime;

        System.out.println(String.format("%s\t\tThread took %dms, %,.2fk ops / sec",
                label,
                totalTime,
                throughput
        ));

        return throughput;
    }

    /*
     * Measure read performance
     */

    @Test
    public void benchmarkSimpleReads() throws Exception {
        QuickSearch<String> searchInstance = QuickSearch.builder().withCache().build();

        for (String[] items : USA_STATES) {
            searchInstance.addItem(items[0], String.format("%s %s %s", items[1], items[2], items[3]));
        }

        assertTrue(benchmarkReadsMT(searchInstance, "Warmup:", 4) > 5.0);
        assertTrue(benchmarkReadsMT(searchInstance, "Warm-1:", 1) > 10.0);
        assertTrue(benchmarkReadsMT(searchInstance, "Warm-2:", 2) > 10.0);
        assertTrue(benchmarkReadsMT(searchInstance, "Warm-4:", 4) > 10.0);
        assertTrue(benchmarkReadsMT(searchInstance, "Warm-8:", 8) > 10.0);
    }

    @Test
    public void exerciseScheduler() throws Exception {
        /*
         * Tests the scheduler and cpu caches more than anything else... Still, fun one.
         */
        QuickSearch<String> searchInstance = QuickSearch.builder().build();

        for (String[] items : USA_STATES) {
            searchInstance.addItem(items[0], String.format("%s %s %s", items[1], items[2], items[3]));
        }

        assertTrue(benchmarkReadsMT(searchInstance, "War-16:", 16) > 10.0);
        assertTrue(benchmarkReadsMT(searchInstance, "Wa-128:", 128) > 10.0);
        assertTrue(benchmarkReadsMT(searchInstance, "Wa-512:", 512) > 10.0);
    }

    private double benchmarkReadsMT(final QuickSearch<String> searchInstance,
                                    final String label,
                                    final int threads) throws InterruptedException {
        final int iterationsPerThread = RUN_FULL_BENCHMARKS ? 10_000_000 / threads : 10_000;

        final long totalTime = executeMultipleThreads(threads, () -> {
            try {
                benchmarkReadsMTThread(searchInstance, iterationsPerThread, label);
            } catch (InterruptedException e) {
                return;
            }
        }, 120);

        double aggregateThroughput = (((double) iterationsPerThread / totalTime) * threads);

        System.out.println(String.format(
                "%s\tAggregate throughput (%d threads): %,.2fk ops / sec",
                label,
                threads,
                aggregateThroughput
        ));

        return aggregateThroughput;
    }

    private double benchmarkReadsMTThread(final QuickSearch<String> searchInstance,
                                          final int iterations,
                                          final String label) throws InterruptedException {
        final long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            if (Thread.interrupted())
                throw new InterruptedException();

            assertTrue(searchInstance.findItems(USA_STATES[i % USA_STATES.length][1], 10).size() > 0);
        }

        final long totalTime = System.currentTimeMillis() - startTime;
        final double throughput = (double) iterations / totalTime;

        System.out.println(
                String.format("%s\t\tThread took %dms, %,.2fk ops / sec",
                        label,
                        totalTime,
                        throughput
                )
        );

        return throughput;
    }

    @Test
    public void measureMemoryUse() {

        /*
         * Measured on Java 1.8.0_102 for 100_000 items set above
         */
//        final long JDK_COLLECTIONS_TARGET = 117_416_608; // 100k items, no cache
//        final long GUAVA_MULTIMAP_TARGET = 104_140_960; // 100k items, no cache
//        final long CUSTOM_TREE_TARGET = 54_255_008; // 100k items, no cache
//        final long CUSTOM_TREE_TARGET = 54_255_008; // 100k items, no cache
        final long CUSTOM_TREE_TARGET_INTERN = 19_448_040; // 100k items, no cache + keyword intern.
        final long CUSTOM_SMALL_TREE_TARGET_INTERN = 563_848; // itemsCount = 1000;

        final long measured = measureMemoryUseImpl(false, 0);
        assertTrue("Instance size exceeds target", measured < ((RUN_FULL_BENCHMARKS ? CUSTOM_TREE_TARGET_INTERN : CUSTOM_SMALL_TREE_TARGET_INTERN) * 1.1));
    }

    @Test
    public void measureMemoryUseWithCache() {
        final int capMB = 80;
        final long measured = measureMemoryUseImpl(true, capMB * 1024 * 1024);
        assertTrue("Instance size exceeds target", measured < (((RUN_FULL_BENCHMARKS ? capMB : 0) + 20) * 1024 * 1024) * 1.1);
    }

    @Test
    public void measureMemoryUseWithCacheDisabledItself() {
        final long measured = measureMemoryUseImpl(true, 20 * 1024 * 1024);
        assertTrue("Instance size exceeds target", measured < ((20 * 1024 * 1024) * 1.1));
    }

    @Test
    public void measureMemoryUseWithUnlimitedCache() {
        final long measured = measureMemoryUseImpl(true, Integer.MAX_VALUE);
        /* Currently as-measured, to detect regressions. */
        assertTrue("Instance size exceeds target", measured < 150 * 1024 * 1024);
    }

    public long measureMemoryUseImpl(boolean useCache, int cacheSize) {
        QuickSearch<String> searchInstance;
        if (useCache)
            searchInstance = QuickSearch.builder().withCacheLimit(cacheSize).build();
        else
            searchInstance = QuickSearch.builder().build();

        final int itemsCount = RUN_FULL_BENCHMARKS ? 100_000 : 1000;

        final long startTime = System.currentTimeMillis();

        for (int i = 0; i < itemsCount; i++) {
            String[] items = USA_STATES[i % USA_STATES.length];
            searchInstance.addItem(items[0] + "-" + i, String.format("%s %s %s %s", items[0], items[1], items[2], items[3]));
        }

        // Force population of caches, if active
        for (char character = 'a'; character <= 'z'; character++) {
            for (char character2 = 'a'; character2 <= 'z'; character2++) {
                searchInstance.findItem(String.valueOf(character) + String.valueOf(character2));
                searchInstance.findItem(String.valueOf(character2));
                searchInstance.findItem(String.valueOf(character));
            }
        }

        // Force population of caches, if active
        for (char character = 'a'; character <= 'z'; character++) {
            searchInstance.findItem(String.valueOf(character));
        }

        final long endTime = System.currentTimeMillis() - startTime;

        System.out.println(String.format("Inserting and querying took %,dms", endTime));


        MemoryMeter meter = new MemoryMeter().withGuessing(MemoryMeter.Guess.ALWAYS_UNSAFE);
        final long measured = meter.measureDeep(searchInstance);
        System.out.println(String.format("Measured instance size: %,d bytes", measured));
        System.out.println(searchInstance.getCacheStats());
        return measured;
    }

    /*
     * Common
     */

    private long executeMultipleThreads(final int threadsCount,
                                        @NotNull final Runnable runnable,
                                        final int timeoutInSeconds) throws InterruptedException {
        Objects.requireNonNull(runnable);

        if (threadsCount < 1 || timeoutInSeconds < 1)
            throw new IllegalArgumentException("Check your arguments");

        final long startTime = System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(threadsCount);

        List<Thread> threads = new LinkedList<>();

        for (int i = 0; i < threadsCount; i++) {
            Thread thread = new Thread(() -> {
                runnable.run();
                latch.countDown();
            });
            threads.add(thread);
            thread.start();
        }

        latch.await(timeoutInSeconds, TimeUnit.SECONDS);

        /* try to kill wandering threads */
        if (latch.getCount() > 0) {
            for (Thread thread : threads) {
                thread.interrupt();
            }
            throw new InterruptedException("Threads did not finish properly");
        }

        return System.currentTimeMillis() - startTime;
    }
}

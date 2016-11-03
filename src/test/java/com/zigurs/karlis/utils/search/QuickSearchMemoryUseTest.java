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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import static org.junit.Assert.assertTrue;

public class QuickSearchMemoryUseTest {

    private static final boolean MEASURE_LARGE_COLLECTIONS = false;

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

    @Test
    public void measureMemoryUse() {
        /*
         * Measured on Java 1.8.0_102
         */

//        final long JDK_COLLECTIONS_TARGET = 117_416_608; // 100k items, no cache
//        final long GUAVA_MULTIMAP_TARGET = 104_140_960; // 100k items, no cache
//        final long CUSTOM_TREE_TARGET = 54_255_008; // 100k items, no cache
//        final long CUSTOM_TREE_TARGET = 54_255_008; // 100k items, no cache
        final long CUSTOM_TREE_TARGET_INTERN = 19_448_040; // 100k items, no cache + keyword intern.
        final long CUSTOM_SMALL_TREE_TARGET_INTERN = 579_776; // itemsCount = 1000;

        final long measured = measureMemoryUseImpl(false, 0);
        assertTrue("Instance size exceeds target", measured < ((MEASURE_LARGE_COLLECTIONS ? CUSTOM_TREE_TARGET_INTERN : CUSTOM_SMALL_TREE_TARGET_INTERN) * 1.1));
    }

    @Test
    public void measureMemoryUseWithCache() {
        final int capMB = 80;
        final long measured = measureMemoryUseImpl(true, capMB * 1024 * 1024);
        assertTrue("Instance size exceeds target", measured < (((MEASURE_LARGE_COLLECTIONS ? capMB : 0) + 20) * 1024 * 1024) * 1.1);
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
        assertTrue("Instance size exceeds target", measured < 200 * 1024 * 1024);
    }

    public long measureMemoryUseImpl(boolean useCache, int cacheSize) {
        QuickSearch<String> searchInstance;
        if (useCache)
            searchInstance = QuickSearch.builder().withCacheLimit(cacheSize).build();
        else
            searchInstance = QuickSearch.builder().build();

        final int itemsCount = MEASURE_LARGE_COLLECTIONS ? 100_000 : 1_000;

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

        // Finish by populating worst case scenario
        for (char character = 'a'; character <= 'z'; character++) {
            searchInstance.findItem(String.valueOf(character));
        }

        final long endTime = System.currentTimeMillis() - startTime;

        System.out.println(String.format("Inserting and warming caches took %,dms", endTime));


        MemoryMeter meter = new MemoryMeter().withGuessing(MemoryMeter.Guess.ALWAYS_UNSAFE);
        final long measured = meter.measureDeep(searchInstance);
        System.out.println(String.format("Measured instance size: %,d bytes", measured));
        return measured;
    }
}

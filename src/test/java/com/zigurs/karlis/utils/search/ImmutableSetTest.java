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

import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ImmutableSetTest {

    @Test
    public void empty() {
        Set<String> set = ImmutableSet.emptySet();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void shouldCreateSingletonSet() {
        String cat = "cat";
        Set<String> set = ImmutableSet.singletonSet(cat);
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton(cat), set);
    }

    @Test
    public void shouldAddToSingletonSet() {
        String cat = "cat";
        String dog = "dog";

        ImmutableSet<String> single = ImmutableSet.singletonSet(cat);
        Set<String> set = ImmutableSet.add(single, dog);
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList(cat, dog)), set);
    }

    @Test
    public void shouldNotAddRepeatedly() {
        String cat = "cat";
        String dog = "dog";

        ImmutableSet<String> single = ImmutableSet.singletonSet(cat);
        ImmutableSet<String> set = ImmutableSet.add(single, dog);
        set = ImmutableSet.add(set, dog);
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList(cat, dog)), set);
    }

    @Test
    public void shouldAddAndRemoveCorrectly() {
        String cat = "cat";
        String dog = "dog";

        ImmutableSet<String> single = ImmutableSet.singletonSet(cat);
        ImmutableSet<String> both = ImmutableSet.add(single, dog);
        Set<String> set = ImmutableSet.remove(both, dog);
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton(cat), set);
    }

    @Test
    public void shouldRemoveSingleElementAndReturnEmpty() {
        ImmutableSet<String> single = ImmutableSet.singletonSet("cat");
        Set<String> set = ImmutableSet.remove(single, "cat");
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void shouldNotRemoveNonExistingElement() {
        ImmutableSet<String> single = ImmutableSet.singletonSet("cat");
        Set<String> set = ImmutableSet.remove(single, "dog");
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton("cat"), set);
    }

    @Test
    public void shouldSurviveRemovingMatchingHashCodeButNotEquals() {
        assertEquals("FB".hashCode(), "Ea".hashCode());

        ImmutableSet<String> single = ImmutableSet.fromCollection(Arrays.asList("FB", "one"));
        Set<String> set = ImmutableSet.remove(single, "Ea");
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("FB", "one")), set);
    }

    @Test
    public void shouldSurviveRemovingNonExistentEntry() {
        ImmutableSet<String> single = ImmutableSet.emptySet();
        Set<String> set = ImmutableSet.remove(single, "Ea");
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void shouldSkipAllNulls() {
        ImmutableSet<String> multiple = ImmutableSet.fromCollection(Arrays.asList(null, null, null));
        assertTrue(multiple.isEmpty());
    }

    @Test
    public void shouldSkipNullsInSource() {
        ImmutableSet<String> multiple = ImmutableSet.fromCollection(Arrays.asList(null, null, null, "one"));
        assertEquals(1, multiple.size());
        assertEquals("one", multiple.iterator().next());
    }

    @Test
    public void shouldReturnEmptyArrayForEmptySplit() {
        ImmutableSet<String> empty = ImmutableSet.emptySet();
        assertEquals(0, empty.split().length);
    }

    @Test
    public void shouldReturnSingleEntryForSplitSingleton() {
        ImmutableSet<String> single = ImmutableSet.singletonSet("one");
        assertEquals(1, single.split().length);
        assertEquals("one", (single.split()[0].iterator().next()));
    }

    @Test
    public void shouldSplitTwoElementsRepeatedly() {
        ImmutableSet<String> two = ImmutableSet.fromCollection(Arrays.asList("one", "two"));
        assertEquals(2, two.split().length);
        assertEquals("one", two.split()[0].iterator().next());
        assertEquals(1, two.split()[0].size());
        assertEquals("two", two.split()[1].iterator().next());
        assertEquals(1, two.split()[1].size());
    }

    @Test
    public void shouldSplitThreeElements() {
        ImmutableSet<String> two = ImmutableSet.fromCollection(Arrays.asList("one", "two", "three"));
        assertEquals(2, two.split().length);
        assertEquals("one", (two.split()[0].iterator().next()));
        assertEquals(1, (two.split()[0].size()));
        assertEquals("two", (two.split()[1].iterator().next()));
        assertEquals(2, (two.split()[1].size()));
    }

    @Test
    public void shouldSplitLotsOfElements() {
        List<String> source = new ArrayList<>();
        for (int i = 0; i < 1009; i++)
            source.add("test" + i);

        ImmutableSet<String> set = ImmutableSet.fromCollection(source);
        ImmutableSet<String>[] split = set.split();

        assertEquals(2, split.length);

        assertEquals(504, split[0].size());
        assertEquals(505, split[1].size());

        ImmutableSet<String> mergedAgain = ImmutableSet.fromCollections(split[0], split[1]);

        assertEquals(1009, mergedAgain.size());
    }

    @Test
    public void shouldCreateFromSingleCollection() {
        Set<String> set = ImmutableSet.fromCollection(Arrays.asList("cat", "dog"));
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("cat", "dog")), set);
    }

    @Test
    public void shouldCreateEmptyFromOneEmptyCollection() {
        Set<String> set = ImmutableSet.fromCollection(Collections.emptyList());
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertEquals(Collections.emptySet(), set);
    }

    @Test
    public void shouldCreateUnionFromSuppliedCollections() {
        Set<String> set = ImmutableSet.fromCollections(Arrays.asList("cat", "dog"), Arrays.asList("red", "blue"));
        assertFalse(set.isEmpty());
        assertEquals(4, set.size());
        assertEquals(new HashSet<>(Arrays.asList("red", "cat", "dog", "blue")), set);
    }

    @Test
    public void shouldCreateSetIfOneCollectionIsEmptyAgain() {
        Set<String> set = ImmutableSet.fromCollections(Collections.emptySet(), Arrays.asList("red", "blue"));
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("red", "blue")), set);
    }

    @Test
    public void shouldCreateSetIfOneCollectionIsEmpty() {
        Set<String> set = ImmutableSet.fromCollections(Arrays.asList("red", "blue"), Collections.emptySet());
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("red", "blue")), set);
    }

    @Test
    public void shouldCreateEmptySetFromEmptyCollections() {
        Set<String> set = ImmutableSet.fromCollections(Collections.emptySet(), Collections.emptySet());
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertEquals(Collections.emptySet(), set);
    }

    @Test
    public void shouldFindContainsItem() {
        ImmutableSet<String> set = ImmutableSet.emptySet();
        for (int i = 0; i < 1000; i++) {
            set = ImmutableSet.add(set, "Item" + i);
        }

        assertFalse(set.isEmpty());
        assertEquals(1000, set.size());
        assertTrue(set.contains("Item999"));
    }

    @Test
    public void equalHashcodesShouldNotMatch() {
        /* following two have identical hashcodes */
        assertEquals("FB".hashCode(), "Ea".hashCode());
        Set<String> set = ImmutableSet.singletonSet("FB");
        assertFalse(set.contains("Ea"));
    }

    @Test
    public void noSurpriseNullElements() {
        ImmutableSet<String> set = ImmutableSet.emptySet();

        for (int i = 0; i < 1000; i++) {
            set = ImmutableSet.add(set, "Item" + i);
        }

        set.forEach(Assert::assertNotNull);
        assertFalse(set.isEmpty());
    }

    @Test(expected = NoSuchElementException.class)
    public void finiteIteratorShouldTerminate() {
        ImmutableSet<String> set = ImmutableSet.emptySet();
        for (int i = 0; i < 10; i++) {
            set = ImmutableSet.add(set, "Item" + i);
        }

        Iterator<String> iterator = set.iterator();

        //noinspection InfiniteLoopStatement
        while (true)
            iterator.next();
    }

    @Test
    public void shouldPassOverAllElementsInForEach() {
        ImmutableSet<String> set = ImmutableSet.emptySet();
        for (int i = 0; i < 1000; i++) {
            set = ImmutableSet.add(set, "Item" + i);
        }

        AtomicInteger integer = new AtomicInteger();
        set.forEach((item) -> integer.incrementAndGet());

        assertEquals(1000, integer.get());
    }

    @Test
    public void shouldNotEqualDifferentSingletonSets() {
        Set<String> setOne = ImmutableSet.singletonSet("one");
        Set<String> setTwo = ImmutableSet.singletonSet("two");
        assertFalse(setOne.equals(setTwo));
    }

    @Test
    public void shouldEqualSingleTonSetsAndSetHashcode() {
        Set<String> setOne = ImmutableSet.singletonSet("one");
        Set<String> setTwo = ImmutableSet.singletonSet("one");
        assertTrue(setOne.equals(setTwo));
        assertEquals(setOne.hashCode(), setTwo.hashCode());
        assertFalse(setOne.hashCode() == 0);
    }

    @Test
    public void shouldEqualDifferentlyCreatedSets() {
        Set<String> setOne = ImmutableSet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = ImmutableSet.fromCollection(Arrays.asList("two", "one"));
        assertTrue(setOne.equals(setTwo));
        assertEquals(setOne.hashCode(), setTwo.hashCode());
    }

    @Test
    public void shouldNotEqualOverlappingSets() {
        Set<String> setOne = ImmutableSet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = ImmutableSet.fromCollection(Arrays.asList("two", "three"));
        assertFalse(setOne.equals(setTwo));
    }

    @Test
    public void shouldNotEqualSingletonSet() {
        Set<String> setOne = ImmutableSet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = Collections.singleton("three");
        assertFalse(setOne.equals(setTwo));
        assertFalse(setTwo.equals(setOne));
    }

    @Test
    public void shouldNotEqualDifferentSets() {
        Set<String> setOne = ImmutableSet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = ImmutableSet.singletonSet("three");
        assertFalse(setOne.equals(setTwo));
    }

    @Test
    public void shouldCompareToCollectionsSet() {
        Set<String> setOne = ImmutableSet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = new HashSet<>(Arrays.asList("one", "two"));
        assertTrue(setOne.equals(setTwo));
        assertEquals(setOne.hashCode(), setTwo.hashCode());
    }

    @Test
    public void shouldNotCompareToList() {
        Set<String> set = ImmutableSet.fromCollection(Arrays.asList("one", "two"));
        Collection<String> list = Arrays.asList("one", "two");
        assertFalse(set.equals(list));
        assertNotEquals(set.hashCode(), list.hashCode());
    }

    @Test
    public void emptySetShouldReportEmpty() {
        Set<String> set = ImmutableSet.emptySet();
        assertTrue(set.isEmpty());
    }

    @Test
    public void emptySizeShouldBeZero() {
        Set<String> set = ImmutableSet.emptySet();
        assertEquals(0, set.size());
    }

    @Test(expected = NoSuchElementException.class)
    public void emptyIteratorShouldThrowException() {
        Set<String> set = ImmutableSet.emptySet();
        Iterator<String> iterator = set.iterator();
        assertFalse(iterator.hasNext());
        assertNull(iterator.next());
    }

    @Test
    public void emptyHashCodeShouldMatchCollectionsEmptyHashCode() {
        Set<String> set = ImmutableSet.emptySet();
        assertEquals(0, set.hashCode());
        assertEquals(new HashSet<>().hashCode(), set.hashCode());
    }

    @Test
    public void emptyShouldMatchCollectionsEmpty() {
        Set<String> set = ImmutableSet.emptySet();
        assertTrue(set.equals(ImmutableSet.emptySet()));
        assertTrue(set.equals(Collections.emptySet()));
    }

    @Test
    public void shouldContain() {
        ImmutableSet<String> multiple = ImmutableSet.fromCollection(Arrays.asList("one", "two", "three"));
        //noinspection RedundantStringConstructorCall
        assertTrue(multiple.contains(new String("two")));
    }

    @Test
    public void shouldNotContain() {
        ImmutableSet<String> multiple = ImmutableSet.fromCollection(Arrays.asList("one", "two", "three"));
        assertFalse(multiple.contains("moon"));
        assertFalse(multiple.contains(null));
    }
}
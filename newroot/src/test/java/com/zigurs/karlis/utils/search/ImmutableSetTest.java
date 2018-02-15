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

import com.zigurs.karlis.utils.collections.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
        Set<String> set = single.createInstanceByAdding(dog);
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList(cat, dog)), set);
    }

    @Test
    public void shouldNotAddRepeatedly() {
        String cat = "cat";
        String dog = "dog";

        ImmutableSet<String> single = ImmutableSet.singletonSet(cat);
        ImmutableSet<String> set = single.createInstanceByAdding(dog);
        set = set.createInstanceByAdding(dog);
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        //noinspection AssertEqualsBetweenInconvertibleTypes
        assertEquals(new HashSet<>(Arrays.asList(cat, dog)), set);
    }

    @Test
    public void shouldAddAndRemoveCorrectly() {
        String cat = "cat";
        String dog = "dog";

        ImmutableSet<String> single = ImmutableSet.singletonSet(cat);
        ImmutableSet<String> both = single.createInstanceByAdding(dog);
        Set<String> set = both.createInstanceByRemoving(dog);
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton(cat), set);
    }

    @Test
    public void shouldRemoveSingleElementAndReturnEmpty() {
        ImmutableSet<String> single = ImmutableSet.singletonSet("cat");
        Set<String> set = single.createInstanceByRemoving("cat");
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void shouldNotRemoveNonExistingElement() {
        ImmutableSet<String> single = ImmutableSet.singletonSet("cat");
        Set<String> set = single.createInstanceByRemoving("dog");
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton("cat"), set);
    }

    @Test
    public void shouldSurviveRemovingMatchingHashCodeButNotEquals() {
        assertEquals("FB".hashCode(), "Ea".hashCode());

        ImmutableSet<String> single = ImmutableSet.fromCollection(Arrays.asList("FB", "one"));
        Set<String> set = single.createInstanceByRemoving("Ea");
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("FB", "one")), set);
    }

    @Test
    public void shouldSurviveRemovingNonExistentEntry() {
        ImmutableSet<String> single = ImmutableSet.emptySet();
        Set<String> set = single.createInstanceByRemoving("Ea");
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
            set = set.createInstanceByAdding("Item" + i);
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
            set = set.createInstanceByAdding("Item" + i);
        }

        assertFalse(set.isEmpty());
        set.forEach(Assert::assertNotNull);
    }

    @Test(expected = NoSuchElementException.class)
    public void finiteIteratorShouldTerminate() {
        ImmutableSet<String> set = ImmutableSet.emptySet();

        for (int i = 0; i < 10; i++) {
            set = set.createInstanceByAdding("Item" + i);
        }

        Iterator<String> iterator = set.iterator();

        for (int i = 0; i < 11; i++)
            assertNotNull(iterator.next());
    }

    @Test
    public void shouldPassOverAllElementsInForEach() {
        ImmutableSet<String> set = ImmutableSet.emptySet();
        for (int i = 0; i < 1000; i++) {
            set = set.createInstanceByAdding("Item" + i);
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

    @Test
    public void shouldStreamAndCollectLotsOfItems() {
        Set<String> sourceSet = new HashSet<>();
        for (int i = 0; i < 1000; i++)
            sourceSet.add(String.format("XXitem-%d", i));

        ImmutableSet<String> immutableSet = ImmutableSet.fromCollection(sourceSet);

        Set<String> resultSet = immutableSet.parallelStream()
                .map(s -> s.substring(2))
                .collect(Collectors.toSet());

        assertEquals("mismatching size", 1000, resultSet.size());
        assertTrue("item missing", resultSet.contains("item-500"));
        assertFalse("unexpected item", resultSet.contains("XXitem-500"));
    }
}

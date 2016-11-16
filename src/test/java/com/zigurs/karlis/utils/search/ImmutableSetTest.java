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
    public void fromSingle() {
        String cat = "cat";
        Set<String> set = ImmutableSet.singletonSet(cat);
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton(cat), set);
    }

    @Test
    public void addAndCreate() {
        String cat = "cat";
        String dog = "dog";

        ImmutableSet<String> single = ImmutableSet.singletonSet(cat);
        Set<String> set = ImmutableSet.add(single, dog);
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList(cat, dog)), set);
    }

    @Test
    public void addAndCreate1() {
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
    public void removeAndCreate() {
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
    public void removeAndCreate1() {
        ImmutableSet<String> single = ImmutableSet.singletonSet("cat");
        Set<String> set = ImmutableSet.remove(single, "cat");
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void removeAndCreate2() {
        ImmutableSet<String> single = ImmutableSet.singletonSet("cat");
        Set<String> set = ImmutableSet.remove(single, "dog");
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton("cat"), set);
    }

    @Test
    public void removeAndCreate4() {
        assertEquals("FB".hashCode(), "Ea".hashCode());

        ImmutableSet<String> single = ImmutableSet.fromCollection(Arrays.asList("FB", "one"));
        Set<String> set = ImmutableSet.remove(single, "Ea");
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("FB", "one")), set);
    }

    @Test
    public void removeAndCreate5() {
        ImmutableSet<String> single = ImmutableSet.emptySet();
        Set<String> set = ImmutableSet.remove(single, "Ea");
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void getFirst() {
        ImmutableSet<String> one = ImmutableSet.singletonSet("one");
        assertEquals("one", one.getSingleElement());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getFirstFromEmpty() {
        ImmutableSet<String> empty = ImmutableSet.emptySet();
        assertEquals("one", empty.getSingleElement());
    }

    @Test
    public void getFirstFromMultiple() {
        ImmutableSet<String> multiple = ImmutableSet.fromCollection(Arrays.asList("one", "two", "three"));
        assertEquals("one", multiple.getSingleElement());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getFirstFromNulls() {
        ImmutableSet<String> multiple = ImmutableSet.fromCollection(Arrays.asList(null, null, null));
        assertEquals("one", multiple.getSingleElement());
    }

    @Test
    public void getFirstFromNullsAndItems() {
        ImmutableSet<String> multiple = ImmutableSet.fromCollection(Arrays.asList(null, null, null, "one"));
        assertEquals("one", multiple.getSingleElement());
    }

    @Test
    public void getSplitEmpty() {
        ImmutableSet<String> empty = ImmutableSet.emptySet();
        assertEquals(0, empty.split().length);
    }

    @Test
    public void getSplitSingle() {
        ImmutableSet<String> single = ImmutableSet.singletonSet("one");
        assertEquals(1, single.split().length);
        assertEquals("one", (single.split()[0].getSingleElement()));
    }

    @Test
    public void getSplitTwo() {
        ImmutableSet<String> two = ImmutableSet.fromCollection(Arrays.asList("one", "two"));
        assertEquals(2, two.split().length);
        assertEquals("one", two.split()[0].getSingleElement());
        assertEquals(1, two.split()[0].size());
        assertEquals("two", two.split()[1].getSingleElement());
        assertEquals(1, two.split()[1].size());
    }

    @Test
    public void getSplitThree() {
        ImmutableSet<String> two = ImmutableSet.fromCollection(Arrays.asList("one", "two", "three"));
        assertEquals(2, two.split().length);
        assertEquals("one", (two.split()[0].getSingleElement()));
        assertEquals(1, (two.split()[0].size()));
        assertEquals("two", (two.split()[1].getSingleElement()));
        assertEquals(2, (two.split()[1].size()));
    }

    @Test
    public void fromCollection() {
        Set<String> set = ImmutableSet.fromCollection(Arrays.asList("cat", "dog"));
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("cat", "dog")), set);
    }

    @Test
    public void fromCollection1() {
        Set<String> set = ImmutableSet.fromCollection(Collections.emptySet());
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertEquals(Collections.emptySet(), set);
    }

    @Test
    public void fromCollections() {
        Set<String> set = ImmutableSet.fromCollections(Arrays.asList("cat", "dog"), Arrays.asList("red", "blue"));
        assertFalse(set.isEmpty());
        assertEquals(4, set.size());
        assertEquals(new HashSet<>(Arrays.asList("red", "cat", "dog", "blue")), set);
    }

    @Test
    public void fromCollections1() {
        Set<String> set = ImmutableSet.fromCollections(Collections.emptySet(), Arrays.asList("red", "blue"));
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("red", "blue")), set);
    }

    @Test
    public void fromCollections2() {
        Set<String> set = ImmutableSet.fromCollections(Arrays.asList("red", "blue"), Collections.emptySet());
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("red", "blue")), set);
    }

    @Test
    public void fromCollections3() {
        Set<String> set = ImmutableSet.fromCollections(Collections.emptySet(), Collections.emptySet());
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertEquals(Collections.emptySet(), set);
    }

    @Test
    public void contains() {
        ImmutableSet<String> set = ImmutableSet.emptySet();
        for (int i = 0; i < 1000; i++) {
            set = ImmutableSet.add(set, "Item" + i);
        }

        assertFalse(set.isEmpty());
        assertEquals(1000, set.size());
        assertTrue(set.contains("Item999"));
    }

    @Test
    public void contains1() {
        assertEquals("FB".hashCode(), "Ea".hashCode());
        Set<String> set = ImmutableSet.singletonSet("FB");
        assertFalse(set.contains("Ea"));
    }

    @Test
    public void iterator() {
        ImmutableSet<String> set = ImmutableSet.emptySet();
        for (int i = 0; i < 1000; i++) {
            set = ImmutableSet.add(set, "Item" + i);
        }

        set.forEach(Assert::assertNotNull);
        assertFalse(set.isEmpty());
    }

    @Test(expected = NoSuchElementException.class)
    public void iterator1() {
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
    public void forEach() {
        ImmutableSet<String> set = ImmutableSet.emptySet();
        for (int i = 0; i < 1000; i++) {
            set = ImmutableSet.add(set, "Item" + i);
        }

        AtomicInteger integer = new AtomicInteger();
        set.forEach((item) -> integer.incrementAndGet());

        assertEquals(1000, integer.get());
    }

    @Test
    public void equalsTest() {
        Set<String> setOne = ImmutableSet.singletonSet("one");
        Set<String> setTwo = ImmutableSet.singletonSet("two");
        assertFalse(setOne.equals(setTwo));
    }

    @Test
    public void equalsTest1() {
        Set<String> setOne = ImmutableSet.singletonSet("one");
        Set<String> setTwo = ImmutableSet.singletonSet("one");
        assertTrue(setOne.equals(setTwo));
        assertFalse(setOne.hashCode() == 0);
        assertEquals(setOne.hashCode(), setTwo.hashCode());
    }

    @Test
    public void equalsTest2() {
        Set<String> setOne = ImmutableSet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = ImmutableSet.fromCollection(Arrays.asList("two", "one"));
        assertTrue(setOne.equals(setTwo));
        assertEquals(setOne.hashCode(), setTwo.hashCode());
    }

    @Test
    public void equalsTest3() {
        Set<String> setOne = ImmutableSet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = ImmutableSet.fromCollection(Arrays.asList("two", "three"));
        assertFalse(setOne.equals(setTwo));
    }

    @Test
    public void equalsTest4() {
        Set<String> setOne = ImmutableSet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = Collections.singleton("three");
        assertFalse(setOne.equals(setTwo));
    }

    @Test
    public void equalsTest5() {
        Set<String> setOne = ImmutableSet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = ImmutableSet.singletonSet("three");
        assertFalse(setOne.equals(setTwo));
    }

    @Test
    public void equalsTest6() {
        Set<String> setOne = ImmutableSet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = new HashSet<>(Arrays.asList("one", "two"));
        assertTrue(setOne.equals(setTwo));
        assertEquals(setOne.hashCode(), setTwo.hashCode());
    }

    @Test
    public void equalsTest7() {
        Set<String> setOne = ImmutableSet.fromCollection(Arrays.asList("one", "two"));
        Collection<String> setTwo = Arrays.asList("one", "two");
        assertFalse(setOne.equals(setTwo));
        assertNotEquals(setOne.hashCode(), setTwo.hashCode());
    }

    @Test
    public void emptySetTest() {
        Set<String> set = ImmutableSet.emptySet();
        assertTrue(set.isEmpty());
    }

    @Test
    public void emptySetTest1() {
        Set<String> set = ImmutableSet.emptySet();
        assertEquals(0, set.size());
    }

    @Test
    public void emptySetTest3() {
        Set<String> set = ImmutableSet.emptySet();
        assertFalse(set.contains("cat"));
    }

    @Test(expected = NoSuchElementException.class)
    public void emptySetTest4() {
        Set<String> set = ImmutableSet.emptySet();
        Iterator<String> iterator = set.iterator();
        assertFalse(iterator.hasNext());
        assertNull(iterator.next());
    }

    @Test
    public void emptySetTest5() {
        Set<String> set = ImmutableSet.emptySet();
        assertEquals(0, set.hashCode());
    }

    @Test
    public void emptySetTest6() {
        Set<String> set = ImmutableSet.emptySet();
        assertTrue(set.equals(ImmutableSet.emptySet()));
        assertTrue(set.equals(Collections.emptySet()));
    }
}
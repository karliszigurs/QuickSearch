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
        Set<String> set = ImmutableSet.empty();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void fromSingle() {
        String cat = "cat";
        Set<String> set = ImmutableSet.fromSingle(cat);
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton(cat), set);
    }

    @Test
    public void addAndCreate() {
        String cat = "cat";
        String dog = "dog";

        Set<String> single = ImmutableSet.fromSingle(cat);
        Set<String> set = ImmutableSet.addAndCreate(single, dog);
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList(cat, dog)), set);
    }

    @Test
    public void addAndCreate1() {
        String cat = "cat";
        String dog = "dog";

        Set<String> single = ImmutableSet.fromSingle(cat);
        Set<String> set = ImmutableSet.addAndCreate(single, dog);
        set = ImmutableSet.addAndCreate(set, dog);
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList(cat, dog)), set);
    }

    @Test
    public void addAndCreate2() {
        Collection<String> single = Collections.singleton("cat");
        Set<String> set = ImmutableSet.addAndCreate(single, "dog");
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("cat", "dog")), set);
    }

    @Test
    public void removeAndCreate() {
        String cat = "cat";
        String dog = "dog";

        Set<String> single = ImmutableSet.fromSingle(cat);
        Set<String> both = ImmutableSet.addAndCreate(single, dog);
        Set<String> set = ImmutableSet.removeAndCreate(both, dog);
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton(cat), set);
    }

    @Test
    public void removeAndCreate1() {
        Set<String> single = ImmutableSet.fromSingle("cat");
        Set<String> set = ImmutableSet.removeAndCreate(single, "cat");
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void removeAndCreate2() {
        Set<String> single = ImmutableSet.fromSingle("cat");
        Set<String> set = ImmutableSet.removeAndCreate(single, "dog");
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton("cat"), set);
    }

    @Test
    public void removeAndCreate3() {
        Collection<String> single = Collections.singleton("cat");
        Set<String> set = ImmutableSet.removeAndCreate(single, "dog");
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton("cat"), set);
    }

    @Test
    public void removeAndCreate4() {
        assertEquals("FB".hashCode(), "Ea".hashCode());

        Collection<String> single = ImmutableSet.fromCollection(Arrays.asList("FB", "one"));
        Set<String> set = ImmutableSet.removeAndCreate(single, "Ea");
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("FB", "one")), set);
    }

    @Test
    public void removeAndCreate5() {
        Collection<String> single = ImmutableSet.empty();
        Set<String> set = ImmutableSet.removeAndCreate(single, "Ea");
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
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
        Set<String> set = ImmutableSet.empty();
        for (int i = 0; i < 1000; i++) {
            set = ImmutableSet.addAndCreate(set, "Item" + i);
        }

        assertFalse(set.isEmpty());
        assertEquals(1000, set.size());
        assertTrue(set.contains("Item999"));
    }

    @Test
    public void contains1() {
        assertEquals("FB".hashCode(), "Ea".hashCode());
        Set<String> set = ImmutableSet.fromSingle("FB");
        assertFalse(set.contains("Ea"));
    }

    @Test
    public void iterator() {
        Set<String> set = ImmutableSet.empty();
        for (int i = 0; i < 1000; i++) {
            set = ImmutableSet.addAndCreate(set, "Item" + i);
        }

        set.forEach(Assert::assertNotNull);
    }

    @Test(expected = NoSuchElementException.class)
    public void iterator1() {
        Set<String> set = ImmutableSet.empty();
        for (int i = 0; i < 10; i++) {
            set = ImmutableSet.addAndCreate(set, "Item" + i);
        }

        Iterator<String> iterator = set.iterator();

        //noinspection InfiniteLoopStatement
        while (true)
            iterator.next();
    }

    @Test
    public void forEach() {
        Set<String> set = ImmutableSet.empty();
        for (int i = 0; i < 1000; i++) {
            set = ImmutableSet.addAndCreate(set, "Item" + i);
        }

        AtomicInteger integer = new AtomicInteger();
        set.forEach((item) -> integer.incrementAndGet());

        assertEquals(1000, integer.get());
    }

    @Test
    public void safeCopy() {
        ImmutableSet<String> set = ImmutableSet.empty();
        for (int i = 0; i < 1000; i++) {
            set = ImmutableSet.addAndCreate(set, "Item" + i);
        }

        Set<String> copy = set.safeCopy();

        assertFalse(copy.isEmpty());
        assertEquals(1000, copy.size());
    }

    @Test
    public void equalsTest() {
        Set<String> setOne = ImmutableSet.fromSingle("one");
        Set<String> setTwo = ImmutableSet.fromSingle("two");
        assertFalse(setOne.equals(setTwo));
    }

    @Test
    public void equalsTest1() {
        Set<String> setOne = ImmutableSet.fromSingle("one");
        Set<String> setTwo = ImmutableSet.fromSingle("one");
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
        Set<String> setTwo = ImmutableSet.fromSingle("three");
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
        Set<String> set = ImmutableSet.empty();
        assertTrue(set.isEmpty());
    }

    @Test
    public void emptySetTest1() {
        Set<String> set = ImmutableSet.empty();
        assertEquals(0, set.size());
    }

    @Test
    public void emptySetTest2() {
        ImmutableSet<String> set = ImmutableSet.empty();
        assertFalse(set.contains("cat"));
        assertFalse(set.safeCopy().contains("cat"));
    }

    @Test
    public void emptySetTest3() {
        Set<String> set = ImmutableSet.empty();
        assertFalse(set.contains("cat"));
    }

    @Test(expected = NoSuchElementException.class)
    public void emptySetTest4() {
        Set<String> set = ImmutableSet.empty();
        Iterator<String> iterator = set.iterator();
        assertFalse(iterator.hasNext());
        assertNull(iterator.next());
    }

    @Test
    public void emptySetTest5() {
        Set<String> set = ImmutableSet.empty();
        assertEquals(0, set.hashCode());
    }

    @Test
    public void emptySetTest6() {
        Set<String> set = ImmutableSet.empty();
        assertTrue(set.equals(ImmutableSet.empty()));
        assertTrue(set.equals(Collections.emptySet()));
    }
}
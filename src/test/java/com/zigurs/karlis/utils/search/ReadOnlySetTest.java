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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ReadOnlySetTest {

    @Test
    public void empty() throws Exception {
        Set<String> set = ReadOnlySet.empty();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void fromSingle() throws Exception {
        String cat = "cat";
        Set<String> set = ReadOnlySet.fromSingle(cat);
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton(cat), set);
    }

    @Test
    public void addAndCreate() throws Exception {
        String cat = "cat";
        String dog = "dog";

        Set<String> single = ReadOnlySet.fromSingle(cat);
        Set<String> set = ReadOnlySet.addAndCreate(single, dog);
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList(cat, dog)), set);
    }

    @Test
    public void addAndCreate1() throws Exception {
        String cat = "cat";
        String dog = "dog";

        Set<String> single = ReadOnlySet.fromSingle(cat);
        Set<String> set = ReadOnlySet.addAndCreate(single, dog);
        set = ReadOnlySet.addAndCreate(set, dog);
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList(cat, dog)), set);
    }

    @Test
    public void addAndCreate2() throws Exception {
        Collection<String> single = Arrays.asList("cat");
        Set<String> set = ReadOnlySet.addAndCreate(single, "dog");
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("cat", "dog")), set);
    }

    @Test
    public void removeAndCreate() throws Exception {
        String cat = "cat";
        String dog = "dog";

        Set<String> single = ReadOnlySet.fromSingle(cat);
        Set<String> both = ReadOnlySet.addAndCreate(single, dog);
        Set<String> set = ReadOnlySet.removeAndCreate(both, dog);
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton(cat), set);
    }

    @Test
    public void removeAndCreate1() throws Exception {
        Set<String> single = ReadOnlySet.fromSingle("cat");
        Set<String> set = ReadOnlySet.removeAndCreate(single, "cat");
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void removeAndCreate2() throws Exception {
        Set<String> single = ReadOnlySet.fromSingle("cat");
        Set<String> set = ReadOnlySet.removeAndCreate(single, "dog");
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton("cat"), set);
    }

    @Test
    public void removeAndCreate3() throws Exception {
        Collection<String> single = Arrays.asList("cat");
        Set<String> set = ReadOnlySet.removeAndCreate(single, "dog");
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertEquals(Collections.singleton("cat"), set);
    }

    @Test
    public void removeAndCreate4() throws Exception {
        assertEquals("FB".hashCode(), "Ea".hashCode());

        Collection<String> single = ReadOnlySet.fromCollection(Arrays.asList("FB", "one"));
        Set<String> set = ReadOnlySet.removeAndCreate(single, "Ea");
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("FB", "one")), set);
    }

    @Test
    public void fromCollection() throws Exception {
        Set<String> set = ReadOnlySet.fromCollection(Arrays.asList("cat", "dog"));
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("cat", "dog")), set);
    }

    @Test
    public void fromCollection1() throws Exception {
        Set<String> set = ReadOnlySet.fromCollection(Collections.emptySet());
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertEquals(Collections.emptySet(), set);
    }

    @Test
    public void fromCollections() throws Exception {
        Set<String> set = ReadOnlySet.fromCollections(Arrays.asList("cat", "dog"), Arrays.asList("red", "blue"));
        assertFalse(set.isEmpty());
        assertEquals(4, set.size());
        assertEquals(new HashSet<>(Arrays.asList("red", "cat", "dog", "blue")), set);
    }

    @Test
    public void fromCollections1() throws Exception {
        Set<String> set = ReadOnlySet.fromCollections(Collections.emptySet(), Arrays.asList("red", "blue"));
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("red", "blue")), set);
    }

    @Test
    public void fromCollections2() throws Exception {
        Set<String> set = ReadOnlySet.fromCollections(Arrays.asList("red", "blue"), Collections.emptySet());
        assertFalse(set.isEmpty());
        assertEquals(2, set.size());
        assertEquals(new HashSet<>(Arrays.asList("red", "blue")), set);
    }

    @Test
    public void fromCollections3() throws Exception {
        Set<String> set = ReadOnlySet.fromCollections(Collections.emptySet(), Collections.emptySet());
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertEquals(Collections.emptySet(), set);
    }

    @Test
    public void contains() throws Exception {
        Set<String> set = ReadOnlySet.empty();
        for (int i = 0; i < 1000; i++) {
            set = ReadOnlySet.addAndCreate(set, "Item" + i);
        }

        assertFalse(set.isEmpty());
        assertEquals(1000, set.size());
        assertTrue(set.contains("Item999"));
    }

    @Test
    public void contains1() throws Exception {
        assertEquals("FB".hashCode(), "Ea".hashCode());
        Set<String> set = ReadOnlySet.fromSingle("FB");
        assertFalse(set.contains("Ea"));
    }

    @Test
    public void iterator() throws Exception {
        Set<String> set = ReadOnlySet.empty();
        for (int i = 0; i < 1000; i++) {
            set = ReadOnlySet.addAndCreate(set, "Item" + i);
        }

        for (String item : set) {
            assertNotNull(item);
        }
    }

    @Test(expected=NoSuchElementException.class)
    public void iterator1() throws Exception {
        Set<String> set = ReadOnlySet.empty();
        for (int i = 0; i < 10; i++) {
            set = ReadOnlySet.addAndCreate(set, "Item" + i);
        }

        Iterator<String> iterator = set.iterator();

        while(true) {
            iterator.next();
        }
    }

    @Test
    public void forEach() throws Exception {
        Set<String> set = ReadOnlySet.empty();
        for (int i = 0; i < 1000; i++) {
            set = ReadOnlySet.addAndCreate(set, "Item" + i);
        }

        AtomicInteger integer = new AtomicInteger();
        set.forEach((item) -> integer.incrementAndGet());

        assertEquals(1000, integer.get());
    }

    @Test
    public void safeCopy() throws Exception {
        ReadOnlySet<String> set = ReadOnlySet.empty();
        for (int i = 0; i < 1000; i++) {
            set = ReadOnlySet.addAndCreate(set, "Item" + i);
        }

        Set<String> copy = set.safeCopy();

        assertFalse(copy.isEmpty());
        assertEquals(1000, copy.size());
    }

    @Test
    public void equalsTest() {
        Set<String> setOne = ReadOnlySet.fromSingle("one");
        Set<String> setTwo = ReadOnlySet.fromSingle("two");
        assertFalse(setOne.equals(setTwo));
    }

    @Test
    public void equalsTest1() {
        Set<String> setOne = ReadOnlySet.fromSingle("one");
        Set<String> setTwo = ReadOnlySet.fromSingle("one");
        assertTrue(setOne.equals(setTwo));
        assertFalse(setOne.hashCode() == 0);
        assertEquals(setOne.hashCode(), setTwo.hashCode());
    }

    @Test
    public void equalsTest2() {
        Set<String> setOne = ReadOnlySet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = ReadOnlySet.fromCollection(Arrays.asList("two", "one"));
        assertTrue(setOne.equals(setTwo));
        assertEquals(setOne.hashCode(), setTwo.hashCode());
    }

    @Test
    public void equalsTest3() {
        Set<String> setOne = ReadOnlySet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = ReadOnlySet.fromCollection(Arrays.asList("two", "three"));
        assertFalse(setOne.equals(setTwo));
    }

    @Test
    public void equalsTest4() {
        Set<String> setOne = ReadOnlySet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = Collections.singleton("three");
        assertFalse(setOne.equals(setTwo));
    }

    @Test
    public void equalsTest5() {
        Set<String> setOne = ReadOnlySet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = ReadOnlySet.fromSingle("three");
        assertFalse(setOne.equals(setTwo));
    }

    @Test
    public void equalsTest6() {
        Set<String> setOne = ReadOnlySet.fromCollection(Arrays.asList("one", "two"));
        Set<String> setTwo = new HashSet<>(Arrays.asList("one", "two"));
        assertTrue(setOne.equals(setTwo));
        assertEquals(setOne.hashCode(), setTwo.hashCode());
    }

    @Test
    public void emptySetTest() {
        Set<String> set = ReadOnlySet.empty();
        assertTrue(set.isEmpty());
    }

    @Test
    public void emptySetTest1() {
        Set<String> set = ReadOnlySet.empty();
        assertEquals(0, set.size());
    }

    @Test
    public void emptySetTest2() {
        ReadOnlySet<String> set = ReadOnlySet.empty();
        assertFalse(set.contains("cat"));
        assertFalse(set.safeCopy().contains("cat"));
    }

    @Test
    public void emptySetTest3() {
        Set<String> set = ReadOnlySet.empty();
        assertFalse(set.contains("cat"));
    }

    @Test(expected = NoSuchElementException.class)
    public void emptySetTest4() {
        Set<String> set = ReadOnlySet.empty();
        Iterator<String> iterator = set.iterator();
        assertFalse(iterator.hasNext());
        assertNull(iterator.next());
    }

    @Test
    public void emptySetTest5() {
        Set<String> set = ReadOnlySet.empty();
        assertEquals(0, set.hashCode());
    }

    @Test
    public void emptySetTest6() {
        Set<String> set = ReadOnlySet.empty();
        assertTrue(set.equals(ReadOnlySet.empty()));
        assertTrue(set.equals(Collections.emptySet()));
    }


}
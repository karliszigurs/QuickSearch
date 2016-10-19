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

}
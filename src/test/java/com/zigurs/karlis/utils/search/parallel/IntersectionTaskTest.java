package com.zigurs.karlis.utils.search.parallel;

import com.zigurs.karlis.utils.search.ImmutableSet;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.Assert.assertTrue;

public class IntersectionTaskTest {

    @Test
    public void allMapsAreEmpty() {
        Function<String, Map<String, Double>> supplierFunction = string -> new HashMap<>();
        ImmutableSet<String> set = ImmutableSet.fromCollection(Arrays.asList("one", "two", "three", "four"));

        IntersectionTask<String> task = new IntersectionTask<>(set, supplierFunction);
        task.fork();
        assertTrue(task.join().isEmpty());
    }

    @Test
    public void secondMapIsEmpty() {
        Function<String, Map<String, Double>> supplierFunction = new Function<String, Map<String, Double>>() {
            int cntr = 0;

            @Override
            public Map<String, Double> apply(String s) {
                if (cntr < 2) {
                    cntr++;

                    HashMap<String, Double> map = new HashMap<>();
                    map.put("one", 1.0);
                    return map;
                } else {
                    return Collections.emptyMap();
                }
            }
        };

        ImmutableSet<String> set = ImmutableSet.fromCollection(Arrays.asList("one", "two", "three"));

        IntersectionTask<String> task = new IntersectionTask<>(set, supplierFunction);
        task.fork();
        assertTrue(task.join().isEmpty());
    }
}
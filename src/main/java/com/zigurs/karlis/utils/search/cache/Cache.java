package com.zigurs.karlis.utils.search.cache;

import com.zigurs.karlis.utils.search.GraphNode;

import java.util.Map;
import java.util.function.Function;

public interface Cache<T> {

    Map<T, Double> getFromCacheOrSupplier(GraphNode<T> rootNode, Function<GraphNode<T>, Map<T, Double>> supplier);

    void clear();

}

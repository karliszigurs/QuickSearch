package com.zigurs.karlis.utils.search.cache;

import com.zigurs.karlis.utils.search.GraphNode;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface Cache<T> extends BiFunction<GraphNode<T>, Function<GraphNode<T>, Map<T, Double>>, Map<T, Double>>, Callable {

    @Override
    Object call() throws Exception;

    @Override
    Map<T, Double> apply(GraphNode<T> tGraphNode, Function<GraphNode<T>, Map<T, Double>> graphNodeMapFunction);
}

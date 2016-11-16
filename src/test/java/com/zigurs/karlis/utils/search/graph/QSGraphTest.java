package com.zigurs.karlis.utils.search.graph;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class QSGraphTest {

    @Test
    public void keywordsFromMissingElement() {
        QSGraph<String> graph = new QSGraph<>();
        assertTrue(graph.getItemKeywords("missing").isEmpty());
    }

}
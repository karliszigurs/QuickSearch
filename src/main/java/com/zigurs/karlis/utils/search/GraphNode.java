package com.zigurs.karlis.utils.search;

import java.util.Collections;
import java.util.Set;

public class GraphNode<T, V> {

    private final T identity;
    private Set<V> items;
    private Set<GraphNode<T, V>> parents;

    public GraphNode(T nodeIdentity) {
        this.identity = nodeIdentity;
        this.items = Collections.emptySet();
        this.parents = Collections.emptySet();
    }

    public T getIdentity() {
        return identity;
    }

    public Set<V> getItems() {
        return items;
    }

    public void addItem(V item) {
        if (items.isEmpty()) {
            items = ReadOnlySet.create(item);
            return;
        }

        items = ReadOnlySet.addAndCreate(items, item);
    }

    public void removeItem(V item) {
        items = ReadOnlySet.removeAndCreate(items, item);
    }

    public Set<GraphNode<T, V>> getParents() {
        return parents;
    }

    public void addParent(GraphNode<T, V> parent) {
        if (parents.isEmpty()) {
            parents = ReadOnlySet.create(parent);
            return;
        }

        parents = ReadOnlySet.addAndCreate(parents, parent);
    }

    public void removeParent(GraphNode<T, V> parent) {
        // TODO investigate - assert parents.contains(parent);
        parents = ReadOnlySet.removeAndCreate(parents, parent);
    }
}

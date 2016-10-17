package com.zigurs.karlis.utils.search;

import java.util.Set;

public class GraphNode<V> {

    private final String key;
    private Set<V> items;
    private Set<GraphNode<V>> parents;

    public GraphNode(String nodeIdentity) {
        this.key = nodeIdentity;
        this.items = null;
        this.parents = ReadOnlySet.empty();
    }

    public String getKey() {
        return key;
    }

    public Set<V> getItems() {
        return items;
    }

    public void addItem(V item) {
        if (items == null || items.isEmpty()) {
            items = ReadOnlySet.fromSingle(item);
            return;
        }

        items = ReadOnlySet.addAndCreate(items, item);
    }

    public void removeItem(V item) {
        items = ReadOnlySet.removeAndCreate(items, item);
    }

    public Set<GraphNode<V>> getParents() {
        return parents;
    }

    public void addParent(GraphNode<V> parent) {
        if (parents.isEmpty()) {
            parents = ReadOnlySet.fromSingle(parent);
            return;
        }

        parents = ReadOnlySet.addAndCreate(parents, parent);
    }

    public void removeParent(GraphNode<V> parent) {
        // TODO investigate - assert parents.contains(parent);
        parents = ReadOnlySet.removeAndCreate(parents, parent);
    }
}

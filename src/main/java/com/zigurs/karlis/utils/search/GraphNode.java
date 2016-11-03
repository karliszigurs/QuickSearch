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

import java.util.Objects;
import java.util.Set;

/*
 * Graph node that may contain a set of links to parent nodes
 * and a set of concrete items associated with this node.
 *
 * The underlying idea is to have a hierarchical graph (ok, multi-root tree)
 * where arbitrary nodes can have items associated with them. Each particular node
 * serves as an entry point to traverse the graph upwards of it and
 * operate on associated items.
 */
public final class GraphNode<V> {

    private final String fragment;
    private Set<V> items;
    private Set<GraphNode<V>> parents;

    private int itemsSizeHint;
    private int nodesSizeHint;

    /**
     * Create a node with immutable identity string.
     *
     * @param fragment any string you like
     */
    public GraphNode(final String fragment) {
        Objects.requireNonNull(fragment);
        this.fragment = fragment;
        this.items = ImmutableSet.empty();
        this.parents = ImmutableSet.empty();
    }

    /**
     * Retrieve identifier.
     *
     * @return selected identifier
     */
    public String getFragment() {
        return fragment;
    }

    /**
     * Retrieve set containing node items. The set will likely be read only
     * and you _must_ use add and remove methods to add and remove items.
     *
     * @return Immutable, possibly empty, set of associated items.
     */
    public Set<V> getItems() {
        return items;
    }

    /**
     * Register an item with this node.
     *
     * @param item item to add.
     */
    public void addItem(final V item) {
        if (items.isEmpty())
            items = ImmutableSet.fromSingle(item);
        else
            items = ImmutableSet.addAndCreate(items, item);
    }

    /**
     * Remove an item from this node if it is present.
     *
     * @param item item to remove.
     */
    public void removeItem(final V item) {
        items = ImmutableSet.removeAndCreate(items, item);
    }

    /**
     * Retrieve set containing known node parents. The set will likely
     * be read only and you _must_ use add and remove methods to ... add
     * and remove parents.
     *
     * @return Immutable, possibly empty, set of known parent nodes.
     */
    public Set<GraphNode<V>> getParents() {
        return parents;
    }

    /**
     * Add a parent node if not already known.
     *
     * @param parent parent to add.
     */
    public void addParent(final GraphNode<V> parent) {
        if (parents.isEmpty())
            parents = ImmutableSet.fromSingle(parent);
        else
            parents = ImmutableSet.addAndCreate(parents, parent);
    }

    /**
     * Remove a parent node if known.
     *
     * @param parent parent to remove.
     */
    public void removeParent(final GraphNode<V> parent) {
        parents = ImmutableSet.removeAndCreate(parents, parent);
    }

    /**
     * Internal helper to aid in pre-setting the
     * size of collections for results traversal.
     *
     * @return previously set hint
     */
    int getItemsSizeHint() {
        return itemsSizeHint;
    }

    /**
     * Internal helper to aid in pre-setting the
     * size of collections for results traversal.
     *
     * @param sizeHint hint
     */
    void setItemsSizeHint(int sizeHint) {
        this.itemsSizeHint = sizeHint;
    }

    /**
     * Internal helper to aid in pre-setting the
     * size of collections for results traversal.
     *
     * @return previously set hint
     */
    int getNodesSizeHint() {
        return nodesSizeHint;
    }

    /**
     * Internal helper to aid in pre-setting the
     * size of collections for results traversal.
     *
     * @param nodesSizeHint hint
     */
    void setNodesSizeHint(int nodesSizeHint) {
        this.nodesSizeHint = nodesSizeHint;
    }
}

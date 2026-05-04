// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.sorting;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

/**
 * Graph construction and structural utilities used by {@link SimplifiedDependencyAwareSorter}.
 *
 * <p>Contains adjacency-list construction, acyclicity validation, reverse-adjacency building,
 * and base-order/rank computation.</p>
 */
@UtilityClass
// Annotation-string repetitions ("PMD.UseVarargs") are an unavoidable annotation artifact.
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DependencyGraphUtils {

    // ------------------------------------------------------------------ //
    // Graph construction                                                  //
    // ------------------------------------------------------------------ //

    /**
     * Builds the super-node dependency graph from raw dependency edges.
     *
     * <p>Returns {@code null} when there are no dependency edges, allowing callers to skip
     * adjacency traversal entirely.  Adjacency lists are allocated lazily — only for
     * super-nodes that actually have outgoing edges.  Edge deduplication uses
     * {@link IntArrayList#contains(int)} (linear scan on small adjacency lists)
     * instead of {@code HashSet<Long>}, avoiding Long boxing.</p>
     *
     * @param itemToIndex         item-to-index map built from the input items
     * @param itemToSuperNode     item-index → super-node-index mapping
     * @param firstSingletonIndex boundary between group and singleton super-nodes
     * @param nodeCount           total number of super-nodes
     * @param dependencies        provider → dependent ordering edges
     * @param inDegree            in-degree array (mutated — incremented for each new edge)
     * @param <TNode>     the item type
     * @return adjacency lists indexed by super-node, or {@code null} when there are no edges
     */
    // Array parameter is intentional: varargs would add allocation overhead in this performance path.
    @SuppressWarnings({"PMD.UseVarargs", "PMD.ReturnEmptyCollectionRatherThanNull"})
    @Nullable
    static <TNode> IntList[] buildDependencyGraph(
            @NonNull Map<TNode, Integer> itemToIndex,
            @NonNull int[] itemToSuperNode,
            int firstSingletonIndex,
            int nodeCount,
            @NonNull Dependencies<TNode> dependencies,
            @NonNull int[] inDegree) {
        List<Dependencies.Dependency<TNode>> edges = dependencies.getEdges();
        if (edges.isEmpty()) {
            return null;
        }

        IntList[] adjacencyLists = new IntList[nodeCount];

        for (Dependencies.Dependency<TNode> edge : edges) {
            SortingUtils.ResolvedEdge<TNode> resolved = SortingUtils.resolveDependencyEdge(edge, itemToIndex);

            int providerSuperNode = itemToSuperNode[resolved.getProviderIndex()];
            int dependentSuperNode = itemToSuperNode[resolved.getDependentIndex()];

            validateNotGroupedMember(providerSuperNode, firstSingletonIndex, resolved.getProvider(), "provider");
            validateNotGroupedMember(dependentSuperNode, firstSingletonIndex, resolved.getDependent(), "dependent");

            addEdgeIfAbsent(adjacencyLists, providerSuperNode, dependentSuperNode, inDegree);
        }

        return adjacencyLists;
    }

    /**
     * Validates that a dependency endpoint does not belong to a group super-node.
     *
     * @param superNodeIndex      the endpoint's super-node index
     * @param firstSingletonIndex the boundary between group and singleton super-nodes
     * @param member              the endpoint item (for error messages)
     * @param role                either "provider" or "dependent" (for error messages)
     */
    private static void validateNotGroupedMember(
            int superNodeIndex, int firstSingletonIndex, Object member, String role) {
        if (superNodeIndex < firstSingletonIndex) {
            throw new SortingException("Grouped member \"" + member + "\" cannot be a dependency " + role);
        }
    }

    /**
     * Adds a directed edge from {@code fromNode} to {@code toNode} if it doesn't already exist.
     * Lazily allocates the adjacency list for {@code fromNode}.
     */
    // Array parameter is intentional: varargs would add allocation overhead in this performance path.
    @SuppressWarnings("PMD.UseVarargs")
    private static void addEdgeIfAbsent(IntList[] adjacencyLists, int fromNode, int toNode, int[] inDegree) {
        IntList list = adjacencyLists[fromNode];
        if (list == null) {
            list = new IntArrayList(4);
            adjacencyLists[fromNode] = list;
            list.add(toNode);
            inDegree[toNode]++;
        } else if (!list.contains(toNode)) {
            list.add(toNode);
            inDegree[toNode]++;
        }
    }

    // ------------------------------------------------------------------ //
    // Acyclicity validation                                               //
    // ------------------------------------------------------------------ //

    /**
     * Validates that the dependency graph is acyclic using Kahn's algorithm.
     *
     * @param nodeCount      total number of super-nodes
     * @param inDegree       in-degree array (not mutated — a copy is used internally)
     * @param adjacencyLists provider → dependent adjacency lists (may contain {@code null} entries)
     * @throws SortingException if a dependency cycle is detected
     */
    // Array parameter is intentional: varargs would add allocation overhead in this performance path.
    @SuppressWarnings("PMD.UseVarargs")
    static void validateAcyclic(int nodeCount, @NonNull int[] inDegree, @NonNull IntList[] adjacencyLists) {
        int[] degreesCopy = Arrays.copyOf(inDegree, nodeCount);
        int readyCount = 0;
        int[] readyStack = new int[nodeCount];
        int stackTop = 0;

        for (int i = 0; i < nodeCount; i++) {
            if (degreesCopy[i] == 0) {
                readyStack[stackTop] = i;
                stackTop++;
            }
        }

        while (stackTop > 0) {
            stackTop--;
            int current = readyStack[stackTop];
            readyCount++;
            IntList neighbors = adjacencyLists[current];
            if (neighbors != null) {
                for (int i = 0; i < neighbors.size(); i++) {
                    int neighbor = neighbors.getInt(i);
                    degreesCopy[neighbor]--;
                    if (degreesCopy[neighbor] == 0) {
                        readyStack[stackTop] = neighbor;
                        stackTop++;
                    }
                }
            }
        }

        if (readyCount != nodeCount) {
            throw new SortingException("Dependency cycle detected among members");
        }
    }

    // ------------------------------------------------------------------ //
    // Base order and rank                                                 //
    // ------------------------------------------------------------------ //

    /**
     * Sorts super-node indices {@code [0..nodeCount)} by the comparator to produce base order.
     *
     * @param nodeCount      total number of super-nodes
     * @param nodeComparator comparator for super-node indices based on their keys
     * @return an array of super-node indices in base order
     */
    @NonNull
    static int[] computeBaseOrder(int nodeCount, @NonNull IntComparator nodeComparator) {
        int[] order = new int[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            order[i] = i;
        }
        IntArrays.mergeSort(order, 0, nodeCount, nodeComparator);
        return order;
    }

    /**
     * Computes the inverse permutation of {@code baseOrder}: the rank of each super-node
     * in the base order.
     *
     * @param baseOrder the base-order array (super-node indices sorted by comparator)
     * @param nodeCount total number of super-nodes
     * @return an array where {@code result[node]} is the rank of {@code node} in the base order
     */
    @NonNull
    static int[] computeBaseRank(@NonNull int[] baseOrder, int nodeCount) {
        int[] baseRank = new int[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            baseRank[baseOrder[i]] = i;
        }
        return baseRank;
    }

    // ------------------------------------------------------------------ //
    // Reverse adjacency                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Builds reverse adjacency lists (dependent → list of providers).
     *
     * @param nodeCount      total number of super-nodes
     * @param adjacencyLists forward adjacency lists (provider → dependents)
     * @return reverse adjacency lists indexed by super-node
     */
    // Array parameter is intentional: varargs would add allocation overhead in this performance path.
    @SuppressWarnings("PMD.UseVarargs")
    @NonNull
    static IntList[] buildReverseAdjacency(int nodeCount, @NonNull IntList[] adjacencyLists) {
        IntList[] reverse = new IntList[nodeCount];
        for (int from = 0; from < nodeCount; from++) {
            IntList adj = adjacencyLists[from];
            if (adj != null) {
                for (int i = 0; i < adj.size(); i++) {
                    int to = adj.getInt(i);
                    if (reverse[to] == null) {
                        reverse[to] = new IntArrayList(4);
                    }
                    reverse[to].add(from);
                }
            }
        }
        return reverse;
    }
}

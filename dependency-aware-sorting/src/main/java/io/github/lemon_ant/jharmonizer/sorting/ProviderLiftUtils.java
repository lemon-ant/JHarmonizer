package io.github.lemon_ant.jharmonizer.sorting;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Provider-lift repair traversal utilities used by {@link SimplifiedDependencyAwareSorter}.
 *
 * <p>Contains the left-to-right scan that emits super-nodes in dependency-valid order,
 * transitive provider closure computation, and subset topological sorting.</p>
 *
 * <p>All internal data structures use fastutil primitive-int collections to avoid
 * {@link Integer} boxing in the hot path.  Expensive per-call allocations
 * ({@code boolean[]} for DFS visited tracking, {@code int[]} for sub-graph in-degrees)
 * are allocated once in the entry point and reused across iterations.</p>
 */
@UtilityClass
class ProviderLiftUtils {

    /** Minimum subset size that requires topological sorting (trivial subsets are returned as-is). */
    private static final int TOPOLOGICAL_SORT_THRESHOLD = 2;

    // ------------------------------------------------------------------ //
    // Provider-lift scan                                                  //
    // ------------------------------------------------------------------ //

    /**
     * Scans base order left to right, emitting each super-node either directly
     * (when all its providers are already emitted) or after lifting its unemitted
     * transitive provider closure.
     *
     * <p>Reusable scratch structures are allocated once and passed to helper methods
     * to avoid per-blocked-node allocations.</p>
     *
     * @param baseOrder      super-node indices in comparator-sorted order
     * @param reverseAdj     reverse adjacency lists (dependent → providers)
     * @param adjacencyLists forward adjacency lists (provider → dependents)
     * @param baseRank       base-order rank for each super-node (tie-breaker)
     * @param nodeCount      total number of super-nodes
     * @return the final ordering of super-node indices
     */
    @NonNull
    static int[] scanAndEmitOrder(
            @NonNull int[] baseOrder,
            @NonNull IntList[] reverseAdj,
            @NonNull IntList[] adjacencyLists,
            @NonNull int[] baseRank,
            int nodeCount) {
        boolean[] emitted = new boolean[nodeCount];
        int[] result = new int[nodeCount];
        int writePos = 0;

        // Reusable structures — allocated once, reused across all provider-lift iterations.
        // visitGeneration + generation counter replace per-closure boolean[] allocation.
        int[] visitGeneration = new int[nodeCount];
        int generation = 0;
        int[] dfsStack = new int[nodeCount];
        IntList closureBuffer = new IntArrayList();
        // subInDegree self-cleans after each Kahn's run (all entries return to 0).
        int[] subInDegree = new int[nodeCount];

        for (int i = 0; i < nodeCount; i++) {
            int node = baseOrder[i];
            if (emitted[node]) {
                continue;
            }

            if (allProvidersEmitted(node, reverseAdj, emitted)) {
                result[writePos] = node;
                writePos++;
                emitted[node] = true;
            } else {
                generation++;
                computeTransitiveProviderClosure(
                        node, reverseAdj, emitted, visitGeneration, generation, dfsStack, closureBuffer);
                writePos = emitProviderBlock(
                        closureBuffer,
                        adjacencyLists,
                        reverseAdj,
                        emitted,
                        baseRank,
                        subInDegree,
                        node,
                        result,
                        writePos);
            }
        }

        return result;
    }

    /**
     * Lifts the unemitted transitive provider closure of a blocked node as a contiguous block,
     * topologically sorts them, emits them, then emits the blocked node itself.
     *
     * @param providers      the unemitted transitive providers (populated by
     *                       {@link #computeTransitiveProviderClosure})
     * @param adjacencyLists forward adjacency lists (provider → dependents)
     * @param reverseAdj     reverse adjacency lists (dependent → providers)
     * @param emitted        boolean array tracking which nodes have been emitted (mutated)
     * @param baseRank       base-order rank for each super-node (tie-breaker)
     * @param subInDegree    reusable in-degree scratch array (self-cleaning after Kahn's)
     * @param blockedNode    the blocked super-node whose providers were lifted
     * @param result         the output array for the final ordering (mutated)
     * @param initialWritePos current write position in the result array
     * @return the updated write position after emitting the provider block and the blocked node
     */
    private static int emitProviderBlock(
            IntList providers,
            IntList[] adjacencyLists,
            IntList[] reverseAdj,
            boolean[] emitted,
            int[] baseRank,
            int[] subInDegree,
            int blockedNode,
            int[] result,
            int initialWritePos) {
        int writePos = initialWritePos;
        IntList sorted = topologicallySortSubset(providers, adjacencyLists, reverseAdj, baseRank, subInDegree);

        for (int i = 0; i < sorted.size(); i++) {
            int p = sorted.getInt(i);
            if (!emitted[p]) {
                result[writePos] = p;
                writePos++;
                emitted[p] = true;
            }
        }
        result[writePos] = blockedNode;
        writePos++;
        emitted[blockedNode] = true;
        return writePos;
    }

    // ------------------------------------------------------------------ //
    // Provider emission check                                             //
    // ------------------------------------------------------------------ //

    /**
     * Returns {@code true} when every provider of {@code node} has already been emitted.
     */
    @SuppressWarnings("PMD.UseVarargs")
    private static boolean allProvidersEmitted(int node, IntList[] reverseAdj, boolean[] emitted) {
        IntList providers = reverseAdj[node];
        if (providers == null) {
            return true;
        }
        for (int i = 0; i < providers.size(); i++) {
            if (!emitted[providers.getInt(i)]) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ //
    // Transitive provider closure                                         //
    // ------------------------------------------------------------------ //

    /**
     * Computes the minimal set of unemitted transitive providers required to unblock
     * {@code node}.  Uses iterative DFS on the reverse adjacency graph.
     *
     * <p>Uses a generation counter ({@code visitGeneration}/{@code generation}) instead of
     * allocating a fresh {@code boolean[]} per call, and a flat {@code int[]} manual stack
     * instead of {@code ArrayDeque<Integer>} to avoid boxing.</p>
     *
     * @param node            the blocked super-node
     * @param reverseAdj      reverse adjacency lists (dependent → providers)
     * @param emitted         which nodes have been emitted
     * @param visitGeneration per-node generation stamps (reused across calls)
     * @param generation      current generation counter value
     * @param dfsStack        reusable manual DFS stack
     * @param closure         output buffer (cleared and populated with the closure members)
     */
    private static void computeTransitiveProviderClosure(
            int node,
            IntList[] reverseAdj,
            boolean[] emitted,
            int[] visitGeneration,
            int generation,
            int[] dfsStack,
            IntList closure) {
        closure.clear();
        int stackTop = seedDfsStack(reverseAdj[node], emitted, visitGeneration, generation, dfsStack, 0);

        while (stackTop > 0) {
            stackTop--;
            int current = dfsStack[stackTop];
            closure.add(current);
            stackTop = seedDfsStack(reverseAdj[current], emitted, visitGeneration, generation, dfsStack, stackTop);
        }
    }

    /**
     * Pushes all unemitted, unvisited providers onto the manual DFS stack.
     *
     * @return the updated stack top position
     */
    private static int seedDfsStack(
            IntList providers, boolean[] emitted, int[] visitGeneration, int generation, int[] dfsStack, int stackTop) {
        if (providers == null) {
            return stackTop;
        }
        int top = stackTop;
        for (int i = 0; i < providers.size(); i++) {
            int p = providers.getInt(i);
            if (!emitted[p] && visitGeneration[p] != generation) {
                visitGeneration[p] = generation;
                dfsStack[top] = p;
                top++;
            }
        }
        return top;
    }

    // ------------------------------------------------------------------ //
    // Subset topological sort                                             //
    // ------------------------------------------------------------------ //

    /**
     * Topologically sorts a subset of nodes using only edges within the subset.
     * Base-rank is used as a deterministic tie-breaker.
     *
     * <p>Uses {@link IntOpenHashSet} for O(1) unboxed membership tests and
     * {@link IntHeapPriorityQueue} for unboxed priority-queue operations.</p>
     */
    @NonNull
    @SuppressWarnings("PMD.UseVarargs")
    private static IntList topologicallySortSubset(
            IntList nodes, IntList[] adjacencyLists, IntList[] reverseAdj, int[] baseRank, int[] subInDegree) {
        if (nodes.size() < TOPOLOGICAL_SORT_THRESHOLD) {
            return nodes;
        }

        IntSet nodeSet = new IntOpenHashSet(nodes.size() * 2);
        for (int i = 0; i < nodes.size(); i++) {
            nodeSet.add(nodes.getInt(i));
        }

        computeSubGraphInDegrees(nodes, nodeSet, reverseAdj, subInDegree);
        IntComparator rankComparator = (a, b) -> Integer.compare(baseRank[a], baseRank[b]);
        IntPriorityQueue ready = collectZeroInDegreeNodes(nodes, subInDegree, rankComparator);
        return runKahnOnSubset(ready, subInDegree, adjacencyLists, nodeSet, nodes.size());
    }

    /**
     * Computes in-degrees for each node in the subset, counting only edges
     * whose source is also in the subset.  Writes directly into the reusable
     * {@code subInDegree} array (entries self-clean after Kahn's run).
     */
    @SuppressWarnings("PMD.UseVarargs")
    private static void computeSubGraphInDegrees(
            IntList nodes, IntSet nodeSet, IntList[] reverseAdj, int[] subInDegree) {
        for (int i = 0; i < nodes.size(); i++) {
            int n = nodes.getInt(i);
            IntList providers = reverseAdj[n];
            if (providers != null) {
                int count = 0;
                for (int j = 0; j < providers.size(); j++) {
                    if (nodeSet.contains(providers.getInt(j))) {
                        count++;
                    }
                }
                subInDegree[n] = count;
            }
        }
    }

    /**
     * Collects all zero-in-degree nodes from the subset into a priority queue
     * ordered by base rank.
     */
    @NonNull
    private static IntPriorityQueue collectZeroInDegreeNodes(
            IntList nodes, int[] subInDegree, IntComparator rankComparator) {
        IntHeapPriorityQueue ready = new IntHeapPriorityQueue(nodes.size(), rankComparator);
        for (int i = 0; i < nodes.size(); i++) {
            int n = nodes.getInt(i);
            if (subInDegree[n] == 0) {
                ready.enqueue(n);
            }
        }
        return ready;
    }

    /**
     * Runs Kahn's algorithm on the subset, consuming the ready queue and producing
     * a topologically sorted list.
     */
    @NonNull
    private static IntList runKahnOnSubset(
            IntPriorityQueue ready, int[] subInDegree, IntList[] adjacencyLists, IntSet nodeSet, int expectedSize) {
        IntList sorted = new IntArrayList(expectedSize);
        while (!ready.isEmpty()) {
            int current = ready.dequeueInt();
            sorted.add(current);
            IntList dependents = adjacencyLists[current];
            if (dependents != null) {
                for (int i = 0; i < dependents.size(); i++) {
                    int dep = dependents.getInt(i);
                    if (!nodeSet.contains(dep)) {
                        continue;
                    }
                    subInDegree[dep]--;
                    if (subInDegree[dep] == 0) {
                        ready.enqueue(dep);
                    }
                }
            }
        }
        return sorted;
    }
}

package io.github.lemon_ant.jharmonizer.sorting;

import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Provider-lift repair traversal utilities used by {@link SimplifiedDependencyAwareSorter}.
 *
 * <p>Contains the left-to-right scan that emits super-nodes in dependency-valid order,
 * transitive provider closure computation, and subset topological sorting.</p>
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
                writePos = emitProviderBlock(
                        node, reverseAdj, adjacencyLists, emitted, baseRank, nodeCount, result, writePos);
            }
        }

        return result;
    }

    /**
     * Lifts the unemitted transitive provider closure of a blocked node as a contiguous block,
     * topologically sorts them, emits them, then emits the blocked node itself.
     *
     * @param node           the blocked super-node whose providers need lifting
     * @param reverseAdj     reverse adjacency lists (dependent → providers)
     * @param adjacencyLists forward adjacency lists (provider → dependents)
     * @param emitted        boolean array tracking which nodes have been emitted (mutated)
     * @param baseRank       base-order rank for each super-node (tie-breaker)
     * @param nodeCount      total number of super-nodes
     * @param result         the output array for the final ordering (mutated)
     * @param initialWritePos current write position in the result array
     * @return the updated write position after emitting the provider block and the blocked node
     */
    private static int emitProviderBlock(
            int node,
            IntList[] reverseAdj,
            IntList[] adjacencyLists,
            boolean[] emitted,
            int[] baseRank,
            int nodeCount,
            int[] result,
            int initialWritePos) {
        int writePos = initialWritePos;
        List<Integer> providers = computeTransitiveProviderClosure(node, reverseAdj, emitted, nodeCount);
        List<Integer> sorted = topologicallySortSubset(providers, adjacencyLists, reverseAdj, baseRank);

        for (int provider : sorted) {
            if (!emitted[provider]) {
                result[writePos] = provider;
                writePos++;
                emitted[provider] = true;
            }
        }
        result[writePos] = node;
        writePos++;
        emitted[node] = true;
        return writePos;
    }

    // ------------------------------------------------------------------ //
    // Provider emission check                                             //
    // ------------------------------------------------------------------ //

    /**
     * Returns {@code true} when every provider of {@code node} has already been emitted.
     */
    // Array parameter is intentional: varargs would add allocation overhead in this performance path.
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
     */
    @NonNull
    private static List<Integer> computeTransitiveProviderClosure(
            int node, IntList[] reverseAdj, boolean[] emitted, int nodeCount) {
        List<Integer> closure = new ArrayList<>();
        boolean[] visited = new boolean[nodeCount];
        Deque<Integer> stack = new ArrayDeque<>();

        seedStack(reverseAdj[node], emitted, visited, stack);
        while (!stack.isEmpty()) {
            int current = stack.pop();
            closure.add(current);
            seedStack(reverseAdj[current], emitted, visited, stack);
        }
        return closure;
    }

    /**
     * Pushes all unemitted, unvisited providers onto the DFS stack.
     */
    private static void seedStack(IntList providers, boolean[] emitted, boolean[] visited, Deque<Integer> stack) {
        if (providers == null) {
            return;
        }
        for (int i = 0; i < providers.size(); i++) {
            int provider = providers.getInt(i);
            if (!emitted[provider] && !visited[provider]) {
                visited[provider] = true;
                stack.push(provider);
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Subset topological sort                                             //
    // ------------------------------------------------------------------ //

    /**
     * Topologically sorts a subset of nodes using only edges within the subset.
     * Base-rank is used as a deterministic tie-breaker.
     */
    // Array parameter is intentional: varargs would add allocation overhead in this performance path.
    @NonNull
    @SuppressWarnings("PMD.UseVarargs")
    private static List<Integer> topologicallySortSubset(
            List<Integer> nodes, IntList[] adjacencyLists, IntList[] reverseAdj, int[] baseRank) {
        if (nodes.size() < TOPOLOGICAL_SORT_THRESHOLD) {
            return nodes;
        }

        Set<Integer> nodeSet = new HashSet<>(nodes.size() * 2);
        for (int node : nodes) {
            nodeSet.add(node);
        }

        int[] subInDegree = computeSubGraphInDegrees(nodes, nodeSet, reverseAdj, baseRank.length);
        Queue<Integer> ready = collectZeroInDegreeNodes(nodes, subInDegree, baseRank);
        return runKahnOnSubset(ready, subInDegree, adjacencyLists, nodeSet, nodes.size());
    }

    /**
     * Computes in-degrees for each node in the subset, counting only edges
     * whose source is also in the subset.
     */
    @NonNull
    private static int[] computeSubGraphInDegrees(
            List<Integer> nodes, Set<Integer> nodeSet, IntList[] reverseAdj, int arrayLength) {
        int[] subInDegree = new int[arrayLength];
        for (int node : nodes) {
            IntList providers = reverseAdj[node];
            if (providers != null) {
                for (int i = 0; i < providers.size(); i++) {
                    if (nodeSet.contains(providers.getInt(i))) {
                        subInDegree[node]++;
                    }
                }
            }
        }
        return subInDegree;
    }

    /**
     * Collects all zero-in-degree nodes from the subset into a priority queue
     * ordered by base rank.
     */
    // Array parameter is intentional: varargs would add allocation overhead in this performance path.
    @SuppressWarnings("PMD.UseVarargs")
    @NonNull
    private static Queue<Integer> collectZeroInDegreeNodes(List<Integer> nodes, int[] subInDegree, int[] baseRank) {
        Queue<Integer> ready = new PriorityQueue<>(Comparator.comparingInt(node -> baseRank[node]));
        for (int node : nodes) {
            if (subInDegree[node] == 0) {
                ready.add(node);
            }
        }
        return ready;
    }

    /**
     * Runs Kahn's algorithm on the subset, consuming the ready queue and producing
     * a topologically sorted list.
     */
    @NonNull
    private static List<Integer> runKahnOnSubset(
            Queue<Integer> ready, int[] subInDegree, IntList[] adjacencyLists, Set<Integer> nodeSet, int expectedSize) {
        List<Integer> sorted = new ArrayList<>(expectedSize);
        while (!ready.isEmpty()) {
            int current = ready.poll();
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
                        ready.add(dep);
                    }
                }
            }
        }
        return sorted;
    }
}

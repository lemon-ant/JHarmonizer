package io.github.lemon_ant.jharmonizer.sorting;

import edu.umd.cs.findbugs.annotations.Nullable;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * A simplified, high-performance dependency-aware sorter designed for inputs where
 * <em>groups and dependencies are mutually exclusive</em>.
 *
 * <h2>Simplified Preconditions (validated at runtime)</h2>
 * <ul>
 *   <li>An item that belongs to a group <b>must not</b> appear in any dependency
 *       (neither as provider nor as dependent).</li>
 *   <li>Groups do not overlap or nest — each item belongs to at most one group.</li>
 * </ul>
 *
 * <p>These invariants allow several optimizations:</p>
 * <ul>
 *   <li><b>Flat-array super-node storage</b> — a single {@code int[n]} holds all item indices
 *       grouped by super-node, with offset/length pairs for O(1) random access.  This eliminates
 *       per-singleton {@code int[]{i}} allocations and {@code List<Integer>} boxing entirely.</li>
 *   <li><b>Adjacency-based edge dedup</b> — duplicate super-node edges are detected via
 *       linear scan on small lists instead of
 *       {@code HashSet<Long>}, avoiding {@code Long} boxing entirely.</li>
 *   <li><b>Insertion sort</b> for intra-group ordering instead of {@code List.sort()}.</li>
 *   <li><b>No intra-group dependency checks</b> — impossible by precondition, so the
 *       comparator is never invoked during graph construction.</li>
 *   <li><b>Fast path</b> — when there are no constraints at all, items are sorted directly
 *       by the comparator with a single {@code List.sort()}, bypassing all super-node
 *       machinery.</li>
 * </ul>
 *
 * <h2>Ordering policy — Provider-lift repair</h2>
 * <p>When dependency edges exist, the engine computes the <b>base order</b> (sorted by the
 * supplied comparator) and repairs it using a single deterministic strategy:
 * <b>provider-lift</b>.  The base order is scanned left to right; when a blocked dependent
 * is encountered (its required providers have not yet been emitted), the minimal transitive
 * provider closure is lifted as a contiguous block directly before the blocked element.
 * Lifted providers are topologically sorted among themselves with base-rank tie-breaking
 * to ensure determinism and dependency validity.</p>
 *
 * <p>Time complexity: <em>O(n log n + E)</em> · Space: <em>O(n + E)</em>.</p>
 */
@UtilityClass
public class SimplifiedDependencyAwareSorter {

    /**
     * Sorts {@code items} according to the supplied constraints, comparator, and identity
     * function.
     *
     * <p>The comparator governs:
     * <ul>
     *   <li>the ordering of items <em>within</em> each group block, and</li>
     *   <li>the base order used as the starting point for provider-lift repair.</li>
     * </ul>
     *
     * <p>Items are identified by their {@code equals/hashCode} contract for duplicate
     * detection, dependency/group resolution, and error messages.
     * Two items must not be equal.
     *
     * @param <TNode>        the item type
     * @param items             items to sort (input order is irrelevant)
     * @param groups          group definitions; use {@link Groups#empty()} if none
     * @param dependencies      provider → dependent ordering edges; use
     *                          {@link Dependencies#empty()} if none
     * @param comparator        determines intra-group and base ordering
     * @return a new, unmodifiable list of the same items in the computed order
     * @throws SortingException if the input is invalid or violates simplified preconditions
     */
    @NonNull
    public static <TNode> List<TNode> sort(
            @NonNull Collection<TNode> items,
            @NonNull Groups<TNode> groups,
            @NonNull Dependencies<TNode> dependencies,
            @NonNull Comparator<TNode> comparator) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<TNode> itemList = new ArrayList<>(items);

        Map<TNode, Integer> itemToIndex = SortingUtils.buildItemIndex(itemList);

        // Fast path: no constraints at all — just sort by comparator, skip all super-node machinery
        if (groups.getGroups().isEmpty() && dependencies.getEdges().isEmpty()) {
            itemList.sort(comparator);
            return Collections.unmodifiableList(itemList);
        }

        SuperNodeUtils.SuperNodes<TNode> superNodes =
                SuperNodeUtils.buildSuperNodes(itemList, itemToIndex, groups, comparator);

        int[] inDegree = new int[superNodes.getCount()];
        IntList[] adjacencyLists = DependencyGraphUtils.buildDependencyGraph(
                itemToIndex,
                superNodes.getItemToSuperNode(),
                superNodes.getFirstSingletonIndex(),
                superNodes.getCount(),
                dependencies,
                inDegree);

        IntComparator nodeKeyComparator = (leftIndex, rightIndex) -> comparator.compare(
                superNodes.getNodeKeys()[leftIndex], superNodes.getNodeKeys()[rightIndex]);

        int[] finalOrder = providerLiftRepair(superNodes.getCount(), inDegree, adjacencyLists, nodeKeyComparator);

        List<TNode> result = SuperNodeUtils.expandOrder(finalOrder, superNodes, itemList);
        return Collections.unmodifiableList(result);
    }

    /**
     * Produces the final super-node ordering by provider-lift repair over the base order.
     *
     * @param nodeCount      total number of super-nodes
     * @param inDegree       in-degree array (consumed by cycle check)
     * @param adjacencyLists provider → dependent adjacency lists (may contain {@code null} entries)
     * @param nodeComparator comparator for super-node indices based on their keys
     * @return an ordered array of super-node indices
     * @throws SortingException if a dependency cycle is detected
     */
    @NonNull
    private static int[] providerLiftRepair(
            int nodeCount, int[] inDegree, @Nullable IntList[] adjacencyLists, IntComparator nodeComparator) {
        // No dependency edges — just return base order
        if (adjacencyLists == null) {
            return DependencyGraphUtils.computeBaseOrder(nodeCount, nodeComparator);
        }

        // Validate acyclicity via Kahn's algorithm
        DependencyGraphUtils.validateAcyclic(nodeCount, inDegree, adjacencyLists);

        // Compute base order and its inverse (rank)
        int[] baseOrder = DependencyGraphUtils.computeBaseOrder(nodeCount, nodeComparator);
        int[] baseRank = DependencyGraphUtils.computeBaseRank(baseOrder, nodeCount);

        // Build reverse adjacency (dependent → list of providers)
        IntList[] reverseAdj = DependencyGraphUtils.buildReverseAdjacency(nodeCount, adjacencyLists);

        return ProviderLiftUtils.scanAndEmitOrder(baseOrder, reverseAdj, adjacencyLists, baseRank, nodeCount);
    }
}

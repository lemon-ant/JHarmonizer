package io.github.lemon_ant.jharmonizer.sorting;

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
 *       grouped by super-node, with offset/length pairs for O(1) random access.</li>
 *   <li><b>Boxing-free {@link DependencyGraphUtils.IntHeap}</b> — replaces
 *       {@code PriorityQueue<Integer>}, eliminating all {@code Integer} boxing/unboxing
 *       in the topological selection hot path.</li>
 *   <li><b>Free / constrained split</b> — super-nodes without dependency edges are pre-sorted
 *       with merge sort; only dependency-involved nodes go through the heap.</li>
 *   <li><b>{@link DependencyGraphUtils.IntBag} adjacency dedup</b> — duplicate super-node
 *       edges are detected via linear scan on small lists instead of {@code HashSet}.</li>
 *   <li><b>Insertion sort</b> for intra-group ordering (typically ≤ 4 elements).</li>
 *   <li><b>Fast path</b> — when no groups and no dependencies exist, a single
 *       {@code List.sort()} bypasses all super-node machinery.</li>
 * </ul>
 *
 * <p>Time complexity: <em>O(n log n + E)</em> · Space: <em>O(n + E)</em>.</p>
 */
@UtilityClass
public class SimplifiedDependencyAwareSorter {

    /**
     * Sorts {@code items} according to the supplied constraints, comparator, and identity
     * function.
     *
     * <p>The algorithm proceeds through these stages:</p>
     * <ol>
     *   <li><b>Item index</b> — maps each item to its position in the list.</li>
     *   <li><b>Super-node construction</b> — groups items by explicit groups, plus singletons.</li>
     *   <li><b>Fast-path check</b> — if no groups and no dependencies exist, sorts directly.</li>
     *   <li><b>Intra-super-node sort</b> — orders items within each group (insertion sort).</li>
     *   <li><b>Super-node dependency graph</b> — builds edges between super-nodes with dedup.</li>
     *   <li><b>Free / constrained split</b> — partitions super-nodes by edge participation.</li>
     *   <li><b>Free-node merge sort</b> — pre-sorts free super-nodes by key.</li>
     *   <li><b>Constrained-node topological sort</b> — Kahn's algorithm via boxing-free heap.</li>
     *   <li><b>Merge and expand</b> — merges the two sorted streams, expanding super-nodes.</li>
     * </ol>
     *
     * <p>Items are identified by their {@code equals/hashCode} contract for duplicate
     * detection, dependency/group resolution, and error messages.
     * Two items must not be equal.
     *
     * @param <TSortableItem>        the item type
     * @param items             items to sort (input order is irrelevant)
     * @param groups            group definitions; use {@link Groups#empty()} if none
     * @param dependencies      provider → dependent ordering edges; use
     *                          {@link Dependencies#empty()} if none
     * @param comparator        determines intra-group and base ordering
     * @return a new, unmodifiable list of the same items in the computed order
     * @throws SortingException if the input is invalid or violates simplified preconditions
     */
    @NonNull
    public static <TSortableItem> List<TSortableItem> sort(
            @NonNull Collection<TSortableItem> items,
            @NonNull Groups<TSortableItem> groups,
            @NonNull Dependencies<TSortableItem> dependencies,
            @NonNull Comparator<TSortableItem> comparator) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<TSortableItem> itemList = new ArrayList<>(items);

        // Step 1: Build index mapping from item → list position.
        Map<TSortableItem, Integer> itemToIndex = SortingUtils.buildItemIndex(itemList);

        // Fast path: no constraints at all — just sort by comparator, skip all super-node machinery.
        if (groups.getGroups().isEmpty() && dependencies.getEdges().isEmpty()) {
            itemList.sort(comparator);
            return Collections.unmodifiableList(itemList);
        }

        // Step 2: Build super-nodes from explicit groups + singletons.
        //         Intra-group items are sorted by insertion sort during construction.
        SuperNodeUtils.SuperNodes<TSortableItem> superNodes =
                SuperNodeUtils.buildSuperNodes(itemList, itemToIndex, groups, comparator);
        int nodeCount = superNodes.getCount();

        // Step 3: Build the super-node dependency graph.
        DependencyGraphUtils.SuperNodeGraph graph = DependencyGraphUtils.buildDependencyGraph(
                itemToIndex,
                superNodes.getItemToSuperNode(),
                superNodes.getFirstSingletonIndex(),
                nodeCount,
                dependencies);

        // No dependency edges — sort all super-nodes by key and expand.
        if (graph == null) {
            int[] sortedOrder = computeIdentityOrder(nodeCount);
            DependencyGraphUtils.mergeSortByKey(sortedOrder, 0, nodeCount, superNodes.getNodeKeys(), comparator);
            List<TSortableItem> result = SuperNodeUtils.expandOrder(sortedOrder, superNodes, itemList);
            return Collections.unmodifiableList(result);
        }

        // Step 4: Split super-nodes into free (no edges) and constrained partitions.
        DependencyGraphUtils.FreeConstrainedPartition partition =
                DependencyGraphUtils.partitionFreeAndConstrained(graph, nodeCount);

        // Step 5: Pre-sort free super-nodes by key (merge sort).
        DependencyGraphUtils.mergeSortByKey(
                partition.freeSuperNodes, 0, partition.freeCount, superNodes.getNodeKeys(), comparator);

        // Step 6: Topologically sort constrained super-nodes (Kahn's algorithm via IntHeap).
        int[] constrainedOrder = DependencyGraphUtils.topologicallySortConstrained(
                partition, graph, nodeCount, superNodes.getNodeKeys(), comparator);

        // Step 7: Merge the two sorted streams and expand each super-node into its items.
        List<TSortableItem> result = DependencyGraphUtils.mergeAndExpand(
                partition, constrainedOrder, superNodes.getNodeKeys(), comparator, superNodes, itemList);
        return Collections.unmodifiableList(result);
    }

    /**
     * Creates an identity permutation array {@code [0, 1, 2, ..., n-1]}.
     *
     * @param nodeCount the number of elements
     * @return an identity permutation array
     */
    @NonNull
    private static int[] computeIdentityOrder(int nodeCount) {
        int[] order = new int[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            order[i] = i;
        }
        return order;
    }
}

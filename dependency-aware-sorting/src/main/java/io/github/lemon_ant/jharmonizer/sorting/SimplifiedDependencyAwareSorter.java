package io.github.lemon_ant.jharmonizer.sorting;

import java.util.*;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * A simplified, high-performance variant of {@link DependencyAwareSorter} designed for inputs where
 * <em>groups and dependencies are mutually exclusive</em>.
 *
 * <h2>Simplified Preconditions (validated at runtime)</h2>
 * <ul>
 *   <li>An item that belongs to a group <b>must not</b> appear in any dependency
 *       (neither as provider nor as dependent).</li>
 *   <li>Groups do not overlap or nest — each item belongs to at most one group.</li>
 * </ul>
 *
 * <p>These invariants allow several optimizations over the general algorithm:</p>
 * <ul>
 *   <li><b>Flat-array super-node storage</b> — a single {@code int[n]} holds all item indices
 *       grouped by super-node, with offset/length pairs for O(1) random access.  This eliminates
 *       per-singleton {@code int[]{i}} allocations and {@code List<Integer>} boxing entirely.</li>
 *   <li><b>Boxing-free min-heap</b> — a custom {@code IntHeap} replaces
 *       {@code PriorityQueue<Integer>}, eliminating all {@code Integer} boxing/unboxing in
 *       Kahn's topological sort (the dominant cost for large inputs).</li>
 *   <li><b>Free-node / constrained-node split</b> — super-nodes with no dependency edges
 *       (groups + independent singletons) are pre-sorted with merge sort, while only
 *       dependency-involved nodes go through the heap.  The two sorted streams are merged
 *       during expansion, dramatically reducing heap operations for typical inputs.</li>
 *   <li><b>Adjacency-based edge dedup</b> — duplicate super-node edges are detected via
 *       {@code IntBag.contains()} (linear scan on small lists) instead of
 *       {@code HashSet<Long>}, avoiding {@code Long} boxing entirely.</li>
 *   <li><b>Insertion sort</b> for intra-group ordering instead of {@code List.sort()}.</li>
 *   <li><b>No intra-group dependency checks</b> — impossible by precondition, so the
 *       comparator is never invoked during graph construction.</li>
 *   <li><b>Fast path</b> — when there are no constraints at all, items are sorted directly
 *       by the comparator with a single {@code List.sort()}, bypassing all super-node
 *       machinery.</li>
 * </ul>
 *
 * <p>The public API is identical to {@link DependencyAwareSorter}: same models, same return type,
 * same exception semantics for violations.</p>
 *
 * <p>Time complexity: <em>O(n log n + E)</em> · Space: <em>O(n + E)</em>.</p>
 */
@UtilityClass
// CouplingBetweenObjects: the algorithm inherently requires many parameter types.
// GodClass / TooManyMethods: already decomposed into SimplifiedSortingUtils; the
// remaining methods are tightly coupled algorithm steps that cannot be meaningfully
// separated further.
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.GodClass", "PMD.TooManyMethods"})
public class SimplifiedDependencyAwareSorter {

    // ------------------------------------------------------------------ //
    // Public API — Generic                                                //
    // ------------------------------------------------------------------ //

    /**
     * Sorts {@code items} according to the supplied constraints, comparator, and identity
     * function.
     *
     * <p>The comparator governs:
     * <ul>
     *   <li>the ordering of items <em>within</em> each group block, and</li>
     *   <li>the tie-breaking order among super-nodes at each step of the topological sort.</li>
     * </ul>
     *
     * <p>Items are identified by their {@code equals/hashCode} contract for duplicate
     * detection, dependency/group resolution, and error messages.
     * Two items must not be equal.
     *
     * @param <TSortableItem>        the item type
     * @param items             items to sort (input order is irrelevant)
     * @param groups          group definitions; use {@link Groups#empty()} if none
     * @param dependencies      provider → dependent ordering edges; use
     *                          {@link Dependencies#empty()} if none
     * @param comparator        determines intra-group and tie-break ordering
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

        Map<TSortableItem, Integer> itemToIndex = CommonSortingUtils.buildItemIndex(itemList);

        // Fast path: no constraints at all — just sort by comparator, skip all super-node machinery
        if (groups.getGroups().isEmpty() && dependencies.getEdges().isEmpty()) {
            itemList.sort(comparator);
            return Collections.unmodifiableList(itemList);
        }

        SimplifiedSortingUtils.SuperNodes<TSortableItem> superNodes =
                buildSuperNodes(itemList, itemToIndex, groups, comparator);

        int[] inDegree = new int[superNodes.getCount()];
        CommonSortingUtils.IntBag[] adjacencyLists =
                buildDependencyGraph(itemToIndex, superNodes, dependencies, inDegree);

        List<TSortableItem> result =
                topologicalSortAndExpand(superNodes, inDegree, adjacencyLists, itemList, comparator);
        return Collections.unmodifiableList(result);
    }

    // ------------------------------------------------------------------ //
    // Step 1 – Build super-nodes from groups + singletons                 //
    // ------------------------------------------------------------------ //

    /**
     * Builds the complete super-node layout: assigns every item to a super-node,
     * populates the flat storage arrays, and computes per-super-node keys.
     *
     * <p>Group super-nodes are created first (one per non-empty group), followed by
     * singleton super-nodes for items not assigned to any group.</p>
     */
    @NonNull
    @SuppressWarnings("unchecked")
    private static <TSortableItem> SimplifiedSortingUtils.SuperNodes<TSortableItem> buildSuperNodes(
            List<TSortableItem> items,
            Map<TSortableItem, Integer> itemToIndex,
            Groups<TSortableItem> groups,
            Comparator<TSortableItem> comparator) {
        int itemCount = items.size();
        int[] itemToSuperNode = new int[itemCount];
        Arrays.fill(itemToSuperNode, CommonSortingUtils.UNASSIGNED);

        int[] memberIndices = new int[itemCount];
        int[] nodeOffset = new int[itemCount];
        int[] nodeLength = new int[itemCount];
        // Safe unchecked cast: the array is only used internally; all reads go through
        // TSortableItem-typed fields/variables, and the array is never exposed outside this class.
        // This is the standard pattern used by java.util.ArrayList and java.util.HashMap.
        TSortableItem[] nodeKeys = (TSortableItem[]) new Object[itemCount];

        int superNodeCount = 0;
        int dataPosition = 0;

        // Phase 1: group super-nodes
        for (Group<TSortableItem> group : groups.getGroups()) {
            List<TSortableItem> groupItems = group.getItems();
            if (groupItems.isEmpty()) {
                continue;
            }

            nodeOffset[superNodeCount] = dataPosition;
            nodeLength[superNodeCount] = groupItems.size();
            resolveGroupMembers(groupItems, itemToIndex, itemToSuperNode, superNodeCount, memberIndices, dataPosition);

            SimplifiedSortingUtils.insertionSortRange(
                    memberIndices, dataPosition, groupItems.size(), items, comparator);
            nodeKeys[superNodeCount] = items.get(memberIndices[dataPosition]);
            dataPosition += groupItems.size();
            superNodeCount++;
        }

        int firstSingletonIndex = superNodeCount;

        // Phase 2: singleton super-nodes (zero per-singleton allocation!)
        for (int i = 0; i < itemCount; i++) {
            if (itemToSuperNode[i] == CommonSortingUtils.UNASSIGNED) {
                itemToSuperNode[i] = superNodeCount;
                nodeOffset[superNodeCount] = dataPosition;
                nodeLength[superNodeCount] = 1;
                memberIndices[dataPosition] = i;
                dataPosition++;
                nodeKeys[superNodeCount] = items.get(i);
                superNodeCount++;
            }
        }

        return new SimplifiedSortingUtils.SuperNodes<>(
                itemToSuperNode, memberIndices, nodeOffset, nodeLength, nodeKeys, superNodeCount, firstSingletonIndex);
    }

    /**
     * Resolves group item identities to indices, validates uniqueness, and populates
     * {@code itemToSuperNode} and {@code memberIndices} arrays.
     */
    private static <TSortableItem> void resolveGroupMembers(
            List<TSortableItem> groupItems,
            Map<TSortableItem, Integer> itemToIndex,
            int[] itemToSuperNode,
            int superNodeIndex,
            int[] memberIndices,
            int dataPosition) {
        for (int j = 0; j < groupItems.size(); j++) {
            TSortableItem item = groupItems.get(j);
            int itemIndex = CommonSortingUtils.resolveGroupMemberIndex(itemToIndex, item);
            CommonSortingUtils.validateNotAlreadyGrouped(itemToSuperNode[itemIndex], item);
            itemToSuperNode[itemIndex] = superNodeIndex;
            memberIndices[dataPosition + j] = itemIndex;
        }
    }

    // ------------------------------------------------------------------ //
    // Step 2 – Build super-node dependency graph                          //
    // ------------------------------------------------------------------ //

    /**
     * Builds the dependency graph on super-nodes.  Returns {@code null} when there are no
     * dependency edges (allows the caller to skip adjacency traversal entirely).
     * {@link CommonSortingUtils.IntBag} entries are allocated lazily — only for super-nodes that
     * actually have outgoing edges.  Edge deduplication uses
     * {@link CommonSortingUtils.IntBag#contains(int)} (linear scan on small adjacency lists)
     * instead of {@code HashSet<Long>}, avoiding Long boxing.
     */
    // Null return is intentional: avoids allocating an empty IntBag[] when there are no edges,
    // and callers already null-check adjacencyLists in the hot path.
    @SuppressWarnings({"PMD.ReturnEmptyCollectionRatherThanNull", "PMD.UseVarargs"})
    private static <TSortableItem> CommonSortingUtils.IntBag[] buildDependencyGraph(
            Map<TSortableItem, Integer> itemToIndex,
            SimplifiedSortingUtils.SuperNodes<TSortableItem> superNodes,
            Dependencies<TSortableItem> dependencies,
            int[] inDegree) {
        List<Dependencies.Dependency<TSortableItem>> edges = dependencies.getEdges();
        if (edges.isEmpty()) {
            return null;
        }

        int[] itemToSuperNode = superNodes.getItemToSuperNode();
        int firstSingletonIndex = superNodes.getFirstSingletonIndex();
        CommonSortingUtils.IntBag[] adjacencyLists = new CommonSortingUtils.IntBag[superNodes.getCount()];

        for (Dependencies.Dependency<TSortableItem> edge : edges) {
            CommonSortingUtils.ResolvedEdge<TSortableItem> resolved =
                    CommonSortingUtils.resolveDependencyEdge(edge, itemToIndex);

            int providerSuperNode = itemToSuperNode[resolved.getProviderIndex()];
            int dependentSuperNode = itemToSuperNode[resolved.getDependentIndex()];

            // Simplified constraint: grouped items cannot participate in dependencies
            validateNotGroupedMember(providerSuperNode, firstSingletonIndex, resolved.getProvider(), "provider");
            validateNotGroupedMember(dependentSuperNode, firstSingletonIndex, resolved.getDependent(), "dependent");

            // Deduplicate using IntBag linear scan (avoids HashSet<Long> boxing)
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
     * Lazily allocates the adjacency bag for {@code fromNode}.
     */
    @SuppressWarnings("PMD.UseVarargs")
    private static void addEdgeIfAbsent(
            CommonSortingUtils.IntBag[] adjacencyLists, int fromNode, int toNode, int[] inDegree) {
        CommonSortingUtils.IntBag bag = adjacencyLists[fromNode];
        if (bag == null) {
            bag = new CommonSortingUtils.IntBag();
            adjacencyLists[fromNode] = bag;
            bag.add(toNode);
            inDegree[toNode]++;
        } else if (!bag.contains(toNode)) {
            bag.add(toNode);
            inDegree[toNode]++;
        }
    }

    // ------------------------------------------------------------------ //
    // Step 3 – Kahn's topological sort + expansion                       //
    // ------------------------------------------------------------------ //

    /**
     * Performs Kahn's topological sort with a free-node / constrained-node split.
     *
     * <p><b>Free</b> super-nodes (in-degree = 0 and no outgoing edges) are pre-sorted with
     * a boxing-free merge sort.  <b>Constrained</b> super-nodes go through the
     * {@link SimplifiedSortingUtils.IntHeap} (also boxing-free).  The two sorted streams
     * are merged during expansion.</p>
     */
    private static <TSortableItem> List<TSortableItem> topologicalSortAndExpand(
            SimplifiedSortingUtils.SuperNodes<TSortableItem> superNodes,
            int[] inDegree,
            CommonSortingUtils.IntBag[] adjacencyLists,
            List<TSortableItem> items,
            Comparator<TSortableItem> comparator) {
        int superNodeCount = superNodes.getCount();
        TSortableItem[] nodeKeys = superNodes.getNodeKeys();

        int[] freeNodes = classifySuperNodes(superNodeCount, inDegree, adjacencyLists);
        int freeCount = freeNodes.length;
        int constrainedCount = superNodeCount - freeCount;

        SimplifiedSortingUtils.IntHeap<TSortableItem> constrainedQueue =
                new SimplifiedSortingUtils.IntHeap<>(Math.max(constrainedCount, 1), nodeKeys, comparator);
        for (int i = 0; i < superNodeCount; i++) {
            boolean zeroInDegree = inDegree[i] == 0;
            boolean noOutEdges = adjacencyLists == null || adjacencyLists[i] == null;
            if (zeroInDegree && !noOutEdges) {
                constrainedQueue.add(i);
            }
        }

        // Sort free nodes by key using boxing-free merge sort
        SimplifiedSortingUtils.sortIndicesByKey(freeNodes, freeCount, nodeKeys, comparator);

        List<TSortableItem> result = new ArrayList<>(items.size());
        int visitedCount = mergeFreeAndConstrainedStreams(
                freeNodes,
                freeCount,
                constrainedQueue,
                adjacencyLists,
                inDegree,
                superNodes,
                items,
                comparator,
                result);

        if (visitedCount != constrainedCount) {
            throw new SortingException("Dependency cycle detected among members");
        }
        return result;
    }

    /**
     * Classifies super-nodes into free (no dependency edges at all) vs. constrained, and
     * returns an array containing the indices of all free super-nodes.
     *
     * @param superNodeCount  total number of super-nodes
     * @param inDegree        per-super-node in-degree
     * @param adjacencyLists  per-super-node outgoing edge bags (may be {@code null})
     * @return an {@code int[]} of free super-node indices
     */
    @NonNull
    @SuppressWarnings("PMD.UseVarargs")
    private static int[] classifySuperNodes(
            int superNodeCount, int[] inDegree, CommonSortingUtils.IntBag[] adjacencyLists) {
        int freeCount = 0;
        for (int i = 0; i < superNodeCount; i++) {
            if (inDegree[i] == 0 && (adjacencyLists == null || adjacencyLists[i] == null)) {
                freeCount++;
            }
        }

        int[] freeNodes = new int[freeCount];
        int freeWriteIndex = 0;
        for (int i = 0; i < superNodeCount; i++) {
            boolean zeroInDegree = inDegree[i] == 0;
            boolean noOutEdges = adjacencyLists == null || adjacencyLists[i] == null;
            if (zeroInDegree && noOutEdges) {
                freeNodes[freeWriteIndex] = i;
                freeWriteIndex++;
            }
        }
        return freeNodes;
    }

    /**
     * Merges the pre-sorted free stream and the heap-driven constrained stream into the result
     * list, expanding super-nodes along the way.
     *
     * @return the number of constrained nodes visited (used for cycle detection)
     */
    private static <TSortableItem> int mergeFreeAndConstrainedStreams(
            int[] freeNodes,
            int freeCount,
            SimplifiedSortingUtils.IntHeap<TSortableItem> constrainedQueue,
            CommonSortingUtils.IntBag[] adjacencyLists,
            int[] inDegree,
            SimplifiedSortingUtils.SuperNodes<TSortableItem> superNodes,
            List<TSortableItem> items,
            Comparator<TSortableItem> comparator,
            List<TSortableItem> result) {
        TSortableItem[] nodeKeys = superNodes.getNodeKeys();
        int visitedCount = 0;
        int freeReadIndex = 0;

        // Phase 1: merge free stream with constrained stream
        while (freeReadIndex < freeCount && !constrainedQueue.isEmpty()) {
            int currentNode;
            if (comparator.compare(nodeKeys[freeNodes[freeReadIndex]], nodeKeys[constrainedQueue.peek()]) <= 0) {
                currentNode = freeNodes[freeReadIndex];
                freeReadIndex++;
            } else {
                currentNode = constrainedQueue.poll();
                visitedCount++;
                advanceNeighbors(adjacencyLists, currentNode, inDegree, constrainedQueue);
            }
            expandNode(currentNode, superNodes, items, result);
        }

        // Phase 2: drain remaining free nodes (no heap interaction)
        while (freeReadIndex < freeCount) {
            expandNode(freeNodes[freeReadIndex], superNodes, items, result);
            freeReadIndex++;
        }

        // Phase 3: drain remaining constrained nodes (Kahn's)
        while (!constrainedQueue.isEmpty()) {
            int currentNode = constrainedQueue.poll();
            visitedCount++;
            expandNode(currentNode, superNodes, items, result);
            advanceNeighbors(adjacencyLists, currentNode, inDegree, constrainedQueue);
        }

        return visitedCount;
    }

    /** Appends all member items of a super-node to the result list. */
    private static <TSortableItem> void expandNode(
            int nodeIndex,
            SimplifiedSortingUtils.SuperNodes<TSortableItem> superNodes,
            List<TSortableItem> items,
            List<TSortableItem> result) {
        int start = superNodes.getNodeOffset()[nodeIndex];
        int memberCount = superNodes.getNodeLength()[nodeIndex];
        for (int i = start; i < start + memberCount; i++) {
            result.add(items.get(superNodes.getMemberIndices()[i]));
        }
    }

    /** Decrements in-degrees of neighbors and adds newly freed nodes to the heap. */
    private static <TSortableItem> void advanceNeighbors(
            CommonSortingUtils.IntBag[] adjacencyLists,
            int nodeIndex,
            int[] inDegree,
            SimplifiedSortingUtils.IntHeap<TSortableItem> constrainedQueue) {
        if (adjacencyLists == null) return;
        CommonSortingUtils.IntBag neighbors = adjacencyLists[nodeIndex];
        if (neighbors == null) return;
        for (int i = 0; i < neighbors.size; i++) {
            int neighborNode = neighbors.data[i];
            inDegree[neighborNode]--;
            if (inDegree[neighborNode] == 0) {
                constrainedQueue.add(neighborNode);
            }
        }
    }
}

package io.github.lemon_ant.jharmonizer.sorting;

import java.util.*;
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
@SuppressWarnings({
    "PMD.GodClass",
    "PMD.TooManyMethods",
    "PMD.CouplingBetweenObjects",
    "PMD.CyclomaticComplexity",
    "PMD.UseVarargs",
    "PMD.AssignmentInOperand"
})
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
    public static <TSortableItem> List<TSortableItem> sort(
            Collection<TSortableItem> items,
            Groups<TSortableItem> groups,
            Dependencies<TSortableItem> dependencies,
            Comparator<TSortableItem> comparator) {
        if (items.isEmpty()) {
            return List.of();
        }
        List<TSortableItem> itemList = new ArrayList<>(items);

        Map<TSortableItem, Integer> itemToIndex = SortingUtils.buildItemIndex(itemList);

        // Fast path: no constraints at all — just sort by comparator, skip all super-node machinery
        if (groups.getGroups().isEmpty() && dependencies.getEdges().isEmpty()) {
            itemList.sort(comparator);
            return Collections.unmodifiableList(itemList);
        }

        SuperNodes<TSortableItem> superNodes = buildSuperNodes(itemList, itemToIndex, groups, comparator);

        int[] inDegree = new int[superNodes.count];
        SortingUtils.IntBag[] adjacencyLists = buildDependencyGraph(itemToIndex, superNodes, dependencies, inDegree);

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
    @SuppressWarnings("unchecked")
    private static <TSortableItem> SuperNodes<TSortableItem> buildSuperNodes(
            List<TSortableItem> items,
            Map<TSortableItem, Integer> itemToIndex,
            Groups<TSortableItem> groups,
            Comparator<TSortableItem> comparator) {
        int itemCount = items.size();
        int[] itemToSuperNode = new int[itemCount];
        Arrays.fill(itemToSuperNode, SortingUtils.UNASSIGNED);

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

            insertionSortRange(memberIndices, dataPosition, groupItems.size(), items, comparator);
            nodeKeys[superNodeCount] = items.get(memberIndices[dataPosition]);
            dataPosition += groupItems.size();
            superNodeCount++;
        }

        int firstSingletonIndex = superNodeCount;

        // Phase 2: singleton super-nodes (zero per-singleton allocation!)
        for (int i = 0; i < itemCount; i++) {
            if (itemToSuperNode[i] == SortingUtils.UNASSIGNED) {
                itemToSuperNode[i] = superNodeCount;
                nodeOffset[superNodeCount] = dataPosition;
                nodeLength[superNodeCount] = 1;
                memberIndices[dataPosition++] = i;
                nodeKeys[superNodeCount] = items.get(i);
                superNodeCount++;
            }
        }

        return new SuperNodes<>(
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
            int itemIndex = SortingUtils.resolveGroupMemberIndex(itemToIndex, item);
            SortingUtils.validateNotAlreadyGrouped(itemToSuperNode[itemIndex], item);
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
     * {@link SortingUtils.IntBag} entries are allocated lazily — only for super-nodes that
     * actually have outgoing edges.  Edge deduplication uses
     * {@link SortingUtils.IntBag#contains(int)} (linear scan on small adjacency lists)
     * instead of {@code HashSet<Long>}, avoiding Long boxing.
     */
    // Null return is intentional: avoids allocating an empty IntBag[] when there are no edges,
    // and callers already null-check adjacencyLists in the hot path.
    @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
    private static <TSortableItem> SortingUtils.IntBag[] buildDependencyGraph(
            Map<TSortableItem, Integer> itemToIndex,
            SuperNodes<TSortableItem> superNodes,
            Dependencies<TSortableItem> dependencies,
            int[] inDegree) {
        List<Dependencies.Dependency<TSortableItem>> edges = dependencies.getEdges();
        if (edges.isEmpty()) {
            return null;
        }

        int[] itemToSuperNode = superNodes.itemToSuperNode;
        int firstSingletonIndex = superNodes.firstSingletonIndex;
        SortingUtils.IntBag[] adjacencyLists = new SortingUtils.IntBag[superNodes.count];

        for (Dependencies.Dependency<TSortableItem> edge : edges) {
            SortingUtils.ResolvedEdge<TSortableItem> resolved = SortingUtils.resolveDependencyEdge(edge, itemToIndex);

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
    private static void addEdgeIfAbsent(
            SortingUtils.IntBag[] adjacencyLists, int fromNode, int toNode, int[] inDegree) {
        SortingUtils.IntBag bag = adjacencyLists[fromNode];
        if (bag == null) {
            bag = new SortingUtils.IntBag();
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
     * a boxing-free merge sort.  <b>Constrained</b> super-nodes go through the {@link IntHeap}
     * (also boxing-free).  The two sorted streams are merged during expansion.</p>
     */
    @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity"})
    private static <TSortableItem> List<TSortableItem> topologicalSortAndExpand(
            SuperNodes<TSortableItem> superNodes,
            int[] inDegree,
            SortingUtils.IntBag[] adjacencyLists,
            List<TSortableItem> items,
            Comparator<TSortableItem> comparator) {
        int superNodeCount = superNodes.count;
        TSortableItem[] nodeKeys = superNodes.nodeKeys;

        // Classify super-nodes: free (no deps at all) vs. constrained
        int freeCount = 0;
        for (int i = 0; i < superNodeCount; i++) {
            if (inDegree[i] == 0 && (adjacencyLists == null || adjacencyLists[i] == null)) {
                freeCount++;
            }
        }

        int[] freeNodes = new int[freeCount];
        int constrainedCount = superNodeCount - freeCount;
        IntHeap<TSortableItem> constrainedQueue = new IntHeap<>(Math.max(constrainedCount, 1), nodeKeys, comparator);

        int freeWriteIndex = 0;
        for (int i = 0; i < superNodeCount; i++) {
            boolean zeroInDegree = inDegree[i] == 0;
            boolean noOutEdges = adjacencyLists == null || adjacencyLists[i] == null;
            if (zeroInDegree && noOutEdges) {
                freeNodes[freeWriteIndex++] = i;
            } else if (zeroInDegree) {
                constrainedQueue.add(i);
            }
        }

        // Sort free nodes by key using boxing-free merge sort
        sortIndicesByKey(freeNodes, freeCount, nodeKeys, comparator);

        List<TSortableItem> result = new ArrayList<>(items.size());
        int visitedCount = 0;
        int freeReadIndex = 0;

        // Phase 1: merge free stream with constrained stream
        while (freeReadIndex < freeCount && !constrainedQueue.isEmpty()) {
            int currentNode;
            if (comparator.compare(nodeKeys[freeNodes[freeReadIndex]], nodeKeys[constrainedQueue.peek()]) <= 0) {
                currentNode = freeNodes[freeReadIndex++];
            } else {
                currentNode = constrainedQueue.poll();
                visitedCount++;
                advanceNeighbors(adjacencyLists, currentNode, inDegree, constrainedQueue);
            }
            expandNode(currentNode, superNodes, items, result);
        }

        // Phase 2: drain remaining free nodes (no heap interaction)
        while (freeReadIndex < freeCount) {
            expandNode(freeNodes[freeReadIndex++], superNodes, items, result);
        }

        // Phase 3: drain remaining constrained nodes (Kahn's)
        while (!constrainedQueue.isEmpty()) {
            int currentNode = constrainedQueue.poll();
            visitedCount++;
            expandNode(currentNode, superNodes, items, result);
            advanceNeighbors(adjacencyLists, currentNode, inDegree, constrainedQueue);
        }

        if (visitedCount != constrainedCount) {
            throw new SortingException("Dependency cycle detected among members");
        }
        return result;
    }

    /** Appends all member items of a super-node to the result list. */
    private static <TSortableItem> void expandNode(
            int nodeIndex,
            SuperNodes<TSortableItem> superNodes,
            List<TSortableItem> items,
            List<TSortableItem> result) {
        int start = superNodes.nodeOffset[nodeIndex];
        int memberCount = superNodes.nodeLength[nodeIndex];
        for (int i = start; i < start + memberCount; i++) {
            result.add(items.get(superNodes.memberIndices[i]));
        }
    }

    /** Decrements in-degrees of neighbors and adds newly freed nodes to the heap. */
    private static <TSortableItem> void advanceNeighbors(
            SortingUtils.IntBag[] adjacencyLists,
            int nodeIndex,
            int[] inDegree,
            IntHeap<TSortableItem> constrainedQueue) {
        if (adjacencyLists == null) return;
        SortingUtils.IntBag neighbors = adjacencyLists[nodeIndex];
        if (neighbors == null) return;
        for (int i = 0; i < neighbors.size; i++) {
            int neighborNode = neighbors.data[i];
            if (--inDegree[neighborNode] == 0) {
                constrainedQueue.add(neighborNode);
            }
        }
    }

    // ------------------------------------------------------------------ //
    // Sorting utilities                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Insertion sort for a small range within {@code data[offset .. offset+length-1]}.
     * Used for intra-group ordering where groups are typically small.
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    private static <TSortableItem> void insertionSortRange(
            int[] data, int offset, int length, List<TSortableItem> items, Comparator<TSortableItem> comparator) {
        for (int i = 1; i < length; i++) {
            int insertedIndex = data[offset + i];
            TSortableItem insertedItem = items.get(insertedIndex);
            int j = i - 1;
            while (j >= 0 && comparator.compare(items.get(data[offset + j]), insertedItem) > 0) {
                data[offset + j + 1] = data[offset + j];
                j--;
            }
            data[offset + j + 1] = insertedIndex;
        }
    }

    /**
     * Sorts {@code array[0..length)} by comparing {@code keys[array[i]]} using the comparator.
     * Uses merge sort with insertion-sort base case.  No {@code Integer} boxing.
     */
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private static <TSortableItem> void sortIndicesByKey(
            int[] array, int length, TSortableItem[] keys, Comparator<TSortableItem> comparator) {
        if (length <= 1) return;
        int[] workspace = new int[length];
        mergeSortByKey(array, workspace, 0, length, keys, comparator);
    }

    @SuppressWarnings({
        "PMD.AvoidArrayLoops",
        "PMD.AvoidLiteralsInIfCondition",
        "PMD.OneDeclarationPerLine",
        "PMD.CognitiveComplexity"
    })
    private static <TSortableItem> void mergeSortByKey(
            int[] array, int[] workspace, int lo, int hi, TSortableItem[] keys, Comparator<TSortableItem> comparator) {
        int length = hi - lo;
        if (length <= 16) {
            // Insertion sort for small ranges
            for (int i = lo + 1; i < hi; i++) {
                int insertedIndex = array[i];
                TSortableItem insertedKey = keys[insertedIndex];
                int j = i - 1;
                while (j >= lo && comparator.compare(keys[array[j]], insertedKey) > 0) {
                    array[j + 1] = array[j];
                    j--;
                }
                array[j + 1] = insertedIndex;
            }
            return;
        }

        int mid = (lo + hi) >>> 1;
        mergeSortByKey(array, workspace, lo, mid, keys, comparator);
        mergeSortByKey(array, workspace, mid, hi, keys, comparator);

        // Merge array[lo..mid) and array[mid..hi) — only left half needs a workspace copy
        // (right half stays in array and is consumed in place)
        System.arraycopy(array, lo, workspace, lo, mid - lo);
        int left = lo, right = mid, writePos = lo;
        while (left < mid && right < hi) {
            if (comparator.compare(keys[workspace[left]], keys[array[right]]) <= 0) {
                array[writePos++] = workspace[left++];
            } else {
                array[writePos++] = array[right++];
            }
        }
        while (left < mid) {
            array[writePos++] = workspace[left++];
        }
    }

    // ------------------------------------------------------------------ //
    // Inner data structures                                               //
    // ------------------------------------------------------------------ //

    /**
     * Immutable holder for the super-node layout computed by {@link #buildSuperNodes}.
     *
     * <p>All item indices are stored in a single flat {@code memberIndices} array, with
     * per-super-node offset/length pairs for O(1) access.  This avoids thousands of small
     * {@code int[]} allocations for singleton super-nodes.</p>
     *
     * @param <TSortableItem> the item type (used for the keys array)
     */
    private static final class SuperNodes<TSortableItem> {
        /** Item index → super-node index. */
        final int[] itemToSuperNode;
        /** Flat item-index storage, grouped by super-node. */
        final int[] memberIndices;
        /** Per-super-node start position in {@code memberIndices}. */
        final int[] nodeOffset;
        /** Per-super-node item count. */
        final int[] nodeLength;
        /** Per-super-node comparator-minimum item (tie-break key). */
        final TSortableItem[] nodeKeys;
        /** Total number of super-nodes. */
        final int count;
        /** Index of the first singleton (non-group) super-node. */
        final int firstSingletonIndex;

        @SuppressWarnings("PMD.ArrayIsStoredDirectly")
        SuperNodes(
                int[] itemToSuperNode,
                int[] memberIndices,
                int[] nodeOffset,
                int[] nodeLength,
                TSortableItem[] nodeKeys,
                int count,
                int firstSingletonIndex) {
            this.itemToSuperNode = itemToSuperNode;
            this.memberIndices = memberIndices;
            this.nodeOffset = nodeOffset;
            this.nodeLength = nodeLength;
            this.nodeKeys = nodeKeys;
            this.count = count;
            this.firstSingletonIndex = firstSingletonIndex;
        }
    }

    /**
     * Boxing-free min-heap that orders super-node indices by their keys.
     * Replaces {@code PriorityQueue<Integer>} to eliminate all {@code Integer}
     * boxing/unboxing in Kahn's topological sort.
     *
     * @param <TSortableItem> the item type (used for key comparison)
     */
    private static final class IntHeap<TSortableItem> {
        private final int[] heap;
        private int size;
        private final TSortableItem[] keys;
        private final Comparator<TSortableItem> comparator;

        private IntHeap(int capacity, TSortableItem[] keys, Comparator<TSortableItem> comparator) {
            this.heap = new int[capacity];
            this.keys = keys;
            this.comparator = comparator;
        }

        private void add(int value) {
            heap[size] = value;
            siftUp(size++);
        }

        private int poll() {
            int min = heap[0];
            heap[0] = heap[--size];
            if (size > 0) siftDown(0);
            return min;
        }

        private int peek() {
            return heap[0];
        }

        private boolean isEmpty() {
            return size == 0;
        }

        @SuppressWarnings("PMD.AvoidReassigningParameters")
        private void siftUp(int index) {
            int value = heap[index];
            TSortableItem valueKey = keys[value];
            while (index > 0) {
                int parent = (index - 1) >>> 1;
                if (comparator.compare(keys[heap[parent]], valueKey) <= 0) break;
                heap[index] = heap[parent];
                index = parent;
            }
            heap[index] = value;
        }

        @SuppressWarnings("PMD.AvoidReassigningParameters")
        private void siftDown(int index) {
            int value = heap[index];
            TSortableItem valueKey = keys[value];
            int half = size >>> 1;
            while (index < half) {
                int child = (index << 1) + 1;
                int right = child + 1;
                if (right < size && comparator.compare(keys[heap[right]], keys[heap[child]]) < 0) {
                    child = right;
                }
                if (comparator.compare(valueKey, keys[heap[child]]) <= 0) break;
                heap[index] = heap[child];
                index = child;
            }
            heap[index] = value;
        }
    }
}

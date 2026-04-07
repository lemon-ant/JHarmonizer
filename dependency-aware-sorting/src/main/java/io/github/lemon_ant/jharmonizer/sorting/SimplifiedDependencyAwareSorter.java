package io.github.lemon_ant.jharmonizer.sorting;

import java.util.ArrayList;
import java.util.Arrays;
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
 *   <li><b>Boxing-free {@link IntHeap}</b> — replaces {@code PriorityQueue}, eliminating all
 *       {@code Integer} boxing/unboxing in the topological selection hot path.</li>
 *   <li><b>Free / constrained split</b> — super-nodes without dependency edges are pre-sorted
 *       with merge sort; only dependency-involved nodes go through the heap.</li>
 *   <li><b>{@link IntBag} adjacency dedup</b> — duplicate super-node edges are detected via
 *       linear scan on small lists instead of {@code HashSet}.</li>
 *   <li><b>Insertion sort</b> for intra-group ordering instead of {@code List.sort()}.</li>
 *   <li><b>Fast path</b> — when there are no constraints at all, items are sorted directly
 *       by the comparator with a single {@code List.sort()}, bypassing all super-node
 *       machinery.</li>
 * </ul>
 *
 * <p>Time complexity: <em>O(n log n + E)</em> · Space: <em>O(n + E)</em>.</p>
 */
@UtilityClass
@SuppressWarnings({
    "PMD.GodClass",
    "PMD.TooManyMethods",
    "PMD.CouplingBetweenObjects",
    "PMD.UseVarargs",
    "PMD.AssignmentInOperand"
})
public class SimplifiedDependencyAwareSorter {

    private static final int ONE = 1;

    // ------------------------------------------------------------------ //
    //  Public entry point                                                 //
    // ------------------------------------------------------------------ //

    /**
     * Sorts {@code items} according to the supplied constraints, comparator, and identity
     * function.
     *
     * <p>The comparator governs:
     * <ul>
     *   <li>the ordering of items <em>within</em> each group block, and</li>
     *   <li>the base order used to determine super-node representative keys.</li>
     * </ul>
     *
     * <p>Items are identified by their {@code equals/hashCode} contract for duplicate
     * detection, dependency/group resolution, and error messages.
     * Two items must not be equal.
     *
     * @param <TSortableItem>   the item type
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

        Map<TSortableItem, Integer> itemToIndex = SortingUtils.buildItemIndex(itemList);

        // Fast path: no constraints at all — just sort by comparator, skip all super-node machinery
        if (groups.getGroups().isEmpty() && dependencies.getEdges().isEmpty()) {
            itemList.sort(comparator);
            return Collections.unmodifiableList(itemList);
        }

        SuperNodeUtils.SuperNodes<TSortableItem> superNodes =
                SuperNodeUtils.buildSuperNodes(itemList, itemToIndex, groups, comparator);

        // Build the super-node dependency graph
        SuperNodeGraph graph = buildSuperNodeDependencyGraph(
                itemToIndex,
                superNodes.getItemToSuperNode(),
                superNodes.getFirstSingletonIndex(),
                superNodes.getCount(),
                dependencies);

        // If no dependency edges exist, sort super-nodes by key and expand
        if (graph == null) {
            int[] sortedOrder = sortSuperNodesByKey(superNodes, comparator);
            List<TSortableItem> result = SuperNodeUtils.expandOrder(sortedOrder, superNodes, itemList);
            return Collections.unmodifiableList(result);
        }

        // Split super-nodes into free (no edges) and constrained partitions
        FreeConstrainedPartition partition =
                partitionFreeAndConstrained(graph.inDegree, graph.hasOutgoing, superNodes.getCount());

        // Pre-sort free super-nodes by representative key (merge sort)
        mergeSortByKey(partition.freeSuperNodes, 0, partition.freeCount, superNodes, comparator);

        // Topologically sort constrained super-nodes (Kahn's algorithm via IntHeap)
        int[] constrainedOrder = topologicallySortConstrained(partition, graph, superNodes, comparator);

        // Merge the two sorted streams and expand each super-node into its members
        List<TSortableItem> result = mergeAndExpand(partition, constrainedOrder, superNodes, comparator, itemList);
        return Collections.unmodifiableList(result);
    }

    // ------------------------------------------------------------------ //
    //  Super-node dependency graph construction                           //
    // ------------------------------------------------------------------ //

    /**
     * Builds the directed dependency graph between super-nodes from raw dependency edges.
     *
     * @return the super-node graph, or {@code null} when there are no edges
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    @edu.umd.cs.findbugs.annotations.Nullable
    private static <TSortableItem> SuperNodeGraph buildSuperNodeDependencyGraph(
            Map<TSortableItem, Integer> itemToIndex,
            int[] itemToSuperNode,
            int firstSingletonIndex,
            int superNodeCount,
            Dependencies<TSortableItem> dependencies) {
        List<Dependencies.Dependency<TSortableItem>> edges = dependencies.getEdges();
        if (edges.isEmpty()) {
            return null;
        }

        IntBag[] adjacencyLists = new IntBag[superNodeCount];
        int[] inDegree = new int[superNodeCount];
        boolean[] hasOutgoing = new boolean[superNodeCount];

        for (Dependencies.Dependency<TSortableItem> edge : edges) {
            SortingUtils.ResolvedEdge<TSortableItem> resolved = SortingUtils.resolveDependencyEdge(edge, itemToIndex);

            int providerSuperNode = itemToSuperNode[resolved.getProviderIndex()];
            int dependentSuperNode = itemToSuperNode[resolved.getDependentIndex()];

            validateNotGroupedMember(providerSuperNode, firstSingletonIndex, resolved.getProvider(), "provider");
            validateNotGroupedMember(dependentSuperNode, firstSingletonIndex, resolved.getDependent(), "dependent");

            hasOutgoing[providerSuperNode] = true;
            IntBag bag = adjacencyLists[providerSuperNode];
            if (bag == null) {
                bag = new IntBag();
                adjacencyLists[providerSuperNode] = bag;
            }
            if (!bag.contains(dependentSuperNode)) {
                bag.add(dependentSuperNode);
                inDegree[dependentSuperNode]++;
            }
        }

        return new SuperNodeGraph(adjacencyLists, inDegree, hasOutgoing);
    }

    /**
     * Validates that a dependency endpoint does not belong to a group super-node.
     */
    private static void validateNotGroupedMember(
            int superNodeIndex, int firstSingletonIndex, Object member, String role) {
        if (superNodeIndex < firstSingletonIndex) {
            throw new SortingException("Grouped member \"" + member + "\" cannot be a dependency " + role);
        }
    }

    // ------------------------------------------------------------------ //
    //  Sort super-nodes by key (no dependencies)                          //
    // ------------------------------------------------------------------ //

    /**
     * Sorts all super-node indices by their representative key using the comparator.
     * Used when there are no dependency edges.
     */
    @NonNull
    private static <TSortableItem> int[] sortSuperNodesByKey(
            SuperNodeUtils.SuperNodes<TSortableItem> superNodes, Comparator<TSortableItem> comparator) {
        int nodeCount = superNodes.getCount();
        int[] order = new int[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            order[i] = i;
        }
        TSortableItem[] keys = superNodes.getNodeKeys();
        mergeSortArray(order, 0, nodeCount, keys, comparator);
        return order;
    }

    // ------------------------------------------------------------------ //
    //  Free / constrained partition                                       //
    // ------------------------------------------------------------------ //

    /**
     * Partitions super-nodes into <em>free</em> (no incoming or outgoing edges) and
     * <em>constrained</em> (participates in at least one dependency).
     */
    @NonNull
    private static FreeConstrainedPartition partitionFreeAndConstrained(
            int[] inDegree, boolean[] hasOutgoing, int superNodeCount) {
        int freeCount = 0;
        int constrainedCount = 0;
        boolean[] isFree = new boolean[superNodeCount];

        for (int superNodeIdx = 0; superNodeIdx < superNodeCount; superNodeIdx++) {
            if (inDegree[superNodeIdx] == 0 && !hasOutgoing[superNodeIdx]) {
                isFree[superNodeIdx] = true;
                freeCount++;
            } else {
                constrainedCount++;
            }
        }

        int[] freeSuperNodes = new int[freeCount];
        int freeIdx = 0;
        for (int superNodeIdx = 0; superNodeIdx < superNodeCount; superNodeIdx++) {
            if (isFree[superNodeIdx]) {
                freeSuperNodes[freeIdx++] = superNodeIdx;
            }
        }

        return new FreeConstrainedPartition(freeSuperNodes, freeCount, constrainedCount, isFree);
    }

    // ------------------------------------------------------------------ //
    //  Constrained topological sort                                       //
    // ------------------------------------------------------------------ //

    /**
     * Topologically sorts constrained super-nodes using Kahn's algorithm backed by a boxing-free
     * {@link IntHeap}. At each step the heap selects the eligible super-node with the smallest
     * representative key, ensuring deterministic output.
     *
     * @throws SortingException if a cycle prevents scheduling all constrained super-nodes
     */
    @NonNull
    @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
    private static <TSortableItem> int[] topologicallySortConstrained(
            FreeConstrainedPartition partition,
            SuperNodeGraph graph,
            SuperNodeUtils.SuperNodes<TSortableItem> superNodes,
            Comparator<TSortableItem> comparator) {
        int superNodeCount = superNodes.getCount();

        // Seed the heap with constrained super-nodes that already have in-degree 0.
        IntHeap eligibleHeap = new IntHeap(partition.constrainedCount, superNodes.getNodeKeys(), comparator);
        for (int superNodeIdx = 0; superNodeIdx < superNodeCount; superNodeIdx++) {
            if (!partition.isFree[superNodeIdx] && graph.inDegree[superNodeIdx] == 0) {
                eligibleHeap.add(superNodeIdx);
            }
        }

        // Process the heap: extract min, decrement dependents' in-degrees, seed newly eligible.
        int[] constrainedOrder = new int[partition.constrainedCount];
        int constrainedIdx = 0;
        while (!eligibleHeap.isEmpty()) {
            int superNodeIdx = eligibleHeap.removeMin();
            constrainedOrder[constrainedIdx++] = superNodeIdx;

            IntBag dependents = graph.adjacencyLists[superNodeIdx];
            if (dependents != null) {
                for (int bagIdx = 0; bagIdx < dependents.size(); bagIdx++) {
                    int dependentSuperNode = dependents.get(bagIdx);
                    if (--graph.inDegree[dependentSuperNode] == 0) {
                        eligibleHeap.add(dependentSuperNode);
                    }
                }
            }
        }

        if (constrainedIdx != partition.constrainedCount) {
            throw new SortingException("Dependency cycle detected among members");
        }

        return constrainedOrder;
    }

    // ------------------------------------------------------------------ //
    //  Merge and expand                                                   //
    // ------------------------------------------------------------------ //

    /**
     * Merges the pre-sorted free and topologically-sorted constrained super-node streams,
     * expanding each super-node into its individual members as it is emitted.
     */
    @NonNull
    private static <TSortableItem> List<TSortableItem> mergeAndExpand(
            FreeConstrainedPartition partition,
            int[] constrainedOrder,
            SuperNodeUtils.SuperNodes<TSortableItem> superNodes,
            Comparator<TSortableItem> comparator,
            List<TSortableItem> items) {
        int memberCount = items.size();
        List<TSortableItem> result = new ArrayList<>(memberCount);
        int freeIdx = 0;
        int constrainedIdx = 0;
        TSortableItem[] nodeKeys = superNodes.getNodeKeys();

        // Two-pointer merge: pick the super-node with the smaller representative key.
        while (freeIdx < partition.freeCount && constrainedIdx < partition.constrainedCount) {
            if (comparator.compare(
                            nodeKeys[partition.freeSuperNodes[freeIdx]], nodeKeys[constrainedOrder[constrainedIdx]])
                    <= 0) {
                expandSuperNode(partition.freeSuperNodes[freeIdx++], superNodes, items, result);
            } else {
                expandSuperNode(constrainedOrder[constrainedIdx++], superNodes, items, result);
            }
        }

        // Drain remaining free super-nodes.
        while (freeIdx < partition.freeCount) {
            expandSuperNode(partition.freeSuperNodes[freeIdx++], superNodes, items, result);
        }

        // Drain remaining constrained super-nodes.
        while (constrainedIdx < partition.constrainedCount) {
            expandSuperNode(constrainedOrder[constrainedIdx++], superNodes, items, result);
        }

        return result;
    }

    /**
     * Appends all members of the specified super-node to the result list.
     */
    private static <TSortableItem> void expandSuperNode(
            int superNodeIdx,
            SuperNodeUtils.SuperNodes<TSortableItem> superNodes,
            List<TSortableItem> items,
            List<TSortableItem> result) {
        int offset = superNodes.getNodeOffset()[superNodeIdx];
        int length = superNodes.getNodeLength()[superNodeIdx];
        for (int j = 0; j < length; j++) {
            result.add(items.get(superNodes.getMemberIndices()[offset + j]));
        }
    }

    // ------------------------------------------------------------------ //
    //  Merge sort for super-node indices by key                           //
    // ------------------------------------------------------------------ //

    /**
     * Merge-sorts super-node indices by their representative key.
     */
    private static <TSortableItem> void mergeSortByKey(
            int[] indices,
            int from,
            int to,
            SuperNodeUtils.SuperNodes<TSortableItem> superNodes,
            Comparator<TSortableItem> comparator) {
        mergeSortArray(indices, from, to, superNodes.getNodeKeys(), comparator);
    }

    /**
     * Merge-sorts indices by looking up keys in the provided array.
     */
    private static <TSortableItem> void mergeSortArray(
            int[] indices, int from, int to, TSortableItem[] keys, Comparator<TSortableItem> comparator) {
        int length = to - from;
        if (length <= ONE) {
            return;
        }
        int[] buffer = new int[length];
        mergeSortHelper(indices, buffer, from, to, keys, comparator);
    }

    private static <TSortableItem> void mergeSortHelper(
            int[] source, int[] buffer, int from, int to, TSortableItem[] keys, Comparator<TSortableItem> comparator) {
        if (to - from <= ONE) {
            return;
        }
        int mid = from + ((to - from) >>> 1);
        mergeSortHelper(source, buffer, from, mid, keys, comparator);
        mergeSortHelper(source, buffer, mid, to, keys, comparator);

        int leftIdx = from;
        int rightIdx = mid;
        int bufferIdx = 0;
        while (leftIdx < mid && rightIdx < to) {
            if (comparator.compare(keys[source[leftIdx]], keys[source[rightIdx]]) <= 0) {
                buffer[bufferIdx] = source[leftIdx];
                bufferIdx++;
                leftIdx++;
            } else {
                buffer[bufferIdx] = source[rightIdx];
                bufferIdx++;
                rightIdx++;
            }
        }
        System.arraycopy(source, leftIdx, buffer, bufferIdx, mid - leftIdx);
        bufferIdx += mid - leftIdx;
        System.arraycopy(source, rightIdx, buffer, bufferIdx, to - rightIdx);
        System.arraycopy(buffer, 0, source, from, to - from);
    }

    // ------------------------------------------------------------------ //
    //  Data carriers for intermediate algorithm state                     //
    // ------------------------------------------------------------------ //

    /** Directed dependency graph between super-nodes. */
    private static final class SuperNodeGraph {

        final IntBag[] adjacencyLists;
        final int[] inDegree;
        final boolean[] hasOutgoing;

        private SuperNodeGraph(IntBag[] adjacencyLists, int[] inDegree, boolean[] hasOutgoing) {
            this.adjacencyLists = adjacencyLists;
            this.inDegree = inDegree;
            this.hasOutgoing = hasOutgoing;
        }
    }

    /** Result of splitting super-nodes into free and constrained partitions. */
    private static final class FreeConstrainedPartition {

        final int[] freeSuperNodes;
        final int freeCount;
        final int constrainedCount;
        final boolean[] isFree;

        private FreeConstrainedPartition(int[] freeSuperNodes, int freeCount, int constrainedCount, boolean[] isFree) {
            this.freeSuperNodes = freeSuperNodes;
            this.freeCount = freeCount;
            this.constrainedCount = constrainedCount;
            this.isFree = isFree;
        }
    }

    // ------------------------------------------------------------------ //
    //  Boxing-free min-heap                                               //
    // ------------------------------------------------------------------ //

    /** Boxing-free min-heap for super-node indices, ordered by representative key. */
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    private static final class IntHeap {

        private final int[] data;
        private final Object[] keys;
        private final Comparator comparator;
        private int size;

        @SuppressWarnings("unchecked")
        IntHeap(int capacity, Object[] keys, Comparator<?> comparator) {
            this.data = new int[capacity];
            this.keys = keys;
            this.comparator = comparator;
        }

        void add(int value) {
            data[size] = value;
            siftUp(size);
            size++;
        }

        int removeMin() {
            int min = data[0];
            size--;
            data[0] = data[size];
            if (size > 0) {
                siftDown(0);
            }
            return min;
        }

        boolean isEmpty() {
            return size == 0;
        }

        @SuppressWarnings("unchecked")
        private void siftUp(int startIndex) {
            int currentIndex = startIndex;
            int value = data[currentIndex];
            while (currentIndex > 0) {
                int parent = (currentIndex - 1) >>> 1;
                if (comparator.compare(keys[value], keys[data[parent]]) >= 0) {
                    break;
                }
                data[currentIndex] = data[parent];
                currentIndex = parent;
            }
            data[currentIndex] = value;
        }

        @SuppressWarnings("unchecked")
        private void siftDown(int startIndex) {
            int currentIndex = startIndex;
            int value = data[currentIndex];
            int half = size >>> 1;
            while (currentIndex < half) {
                int child = (currentIndex << 1) + 1;
                int right = child + 1;
                if (right < size && comparator.compare(keys[data[right]], keys[data[child]]) < 0) {
                    child = right;
                }
                if (comparator.compare(keys[value], keys[data[child]]) <= 0) {
                    break;
                }
                data[currentIndex] = data[child];
                currentIndex = child;
            }
            data[currentIndex] = value;
        }
    }

    // ------------------------------------------------------------------ //
    //  Growable int list with linear-scan dedup                           //
    // ------------------------------------------------------------------ //

    /** Growable int list with linear-scan contains, used for super-node adjacency dedup. */
    private static final class IntBag {

        private int[] data;
        private int count;

        IntBag() {
            this.data = new int[4];
        }

        boolean contains(int value) {
            for (int i = 0; i < count; i++) {
                if (data[i] == value) {
                    return true;
                }
            }
            return false;
        }

        void add(int value) {
            if (count == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[count] = value;
            count++;
        }

        int size() {
            return count;
        }

        int get(int index) {
            return data[index];
        }
    }
}

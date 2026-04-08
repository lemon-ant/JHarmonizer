package io.github.lemon_ant.jharmonizer.sorting;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Graph construction, partitioning, and merge utilities used by {@link SimplifiedDependencyAwareSorter}.
 *
 * <p>Contains adjacency-list construction (with {@link IntBag}-based dedup),
 * free/constrained partitioning, topological sort via boxing-free {@link IntHeap},
 * merge sort for free super-nodes, and two-pointer merge-and-expand.</p>
 */
@UtilityClass
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.CyclomaticComplexity", "PMD.AssignmentInOperand"})
class DependencyGraphUtils {

    private static final int ONE = 1;

    // ------------------------------------------------------------------ //
    // Graph construction                                                  //
    // ------------------------------------------------------------------ //

    /**
     * Builds the super-node dependency graph from raw dependency edges.
     *
     * <p>Returns {@code null} when there are no dependency edges, allowing callers to skip
     * graph traversal entirely. Edge deduplication uses {@link IntBag#contains(int)}
     * (linear scan on small adjacency lists) instead of {@code HashSet<Long>},
     * avoiding {@code Long} boxing.</p>
     *
     * @param itemToIndex         item-to-index map built from the input items
     * @param itemToSuperNode     item-index → super-node-index mapping
     * @param firstSingletonIndex boundary between group and singleton super-nodes
     * @param nodeCount           total number of super-nodes
     * @param dependencies        provider → dependent ordering edges
     * @param <TSortableItem>     the item type
     * @return the super-node graph, or {@code null} when there are no edges
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    @Nullable
    static <TSortableItem> SuperNodeGraph buildDependencyGraph(
            @NonNull Map<TSortableItem, Integer> itemToIndex,
            @NonNull int[] itemToSuperNode,
            int firstSingletonIndex,
            int nodeCount,
            @NonNull Dependencies<TSortableItem> dependencies) {
        List<Dependencies.Dependency<TSortableItem>> edges = dependencies.getEdges();
        if (edges.isEmpty()) {
            return null;
        }

        IntBag[] snDependents = new IntBag[nodeCount];
        int[] snInDegree = new int[nodeCount];
        boolean[] snHasOutgoing = new boolean[nodeCount];

        for (Dependencies.Dependency<TSortableItem> edge : edges) {
            SortingUtils.ResolvedEdge<TSortableItem> resolved = SortingUtils.resolveDependencyEdge(edge, itemToIndex);

            int providerSuperNode = itemToSuperNode[resolved.getProviderIndex()];
            int dependentSuperNode = itemToSuperNode[resolved.getDependentIndex()];

            validateNotGroupedMember(providerSuperNode, firstSingletonIndex, resolved.getProvider(), "provider");
            validateNotGroupedMember(dependentSuperNode, firstSingletonIndex, resolved.getDependent(), "dependent");

            snHasOutgoing[providerSuperNode] = true;
            IntBag bag = snDependents[providerSuperNode];
            if (bag == null) {
                bag = new IntBag();
                snDependents[providerSuperNode] = bag;
            }
            // Linear-scan dedup on small adjacency lists avoids HashSet boxing.
            if (!bag.contains(dependentSuperNode)) {
                bag.add(dependentSuperNode);
                snInDegree[dependentSuperNode]++;
            }
        }

        return new SuperNodeGraph(snDependents, snInDegree, snHasOutgoing);
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

    // ------------------------------------------------------------------ //
    // Free / constrained partition                                        //
    // ------------------------------------------------------------------ //

    /**
     * Partitions super-nodes into <em>free</em> (no incoming or outgoing edges) and
     * <em>constrained</em> (participates in at least one dependency).
     *
     * @param graph     the super-node dependency graph
     * @param nodeCount total number of super-nodes
     * @return the partition containing free and constrained super-node arrays
     */
    @NonNull
    static FreeConstrainedPartition partitionFreeAndConstrained(@NonNull SuperNodeGraph graph, int nodeCount) {
        int freeCount = 0;
        int constrainedCount = 0;
        boolean[] isFree = new boolean[nodeCount];

        for (int superNodeIdx = 0; superNodeIdx < nodeCount; superNodeIdx++) {
            if (graph.snInDegree[superNodeIdx] == 0 && !graph.snHasOutgoing[superNodeIdx]) {
                isFree[superNodeIdx] = true;
                freeCount++;
            } else {
                constrainedCount++;
            }
        }

        int[] freeSuperNodes = new int[freeCount];
        int freeIdx = 0;
        for (int superNodeIdx = 0; superNodeIdx < nodeCount; superNodeIdx++) {
            if (isFree[superNodeIdx]) {
                freeSuperNodes[freeIdx++] = superNodeIdx;
            }
        }

        return new FreeConstrainedPartition(freeSuperNodes, freeCount, constrainedCount, isFree);
    }

    // ------------------------------------------------------------------ //
    // Topological sort (Kahn's via IntHeap)                               //
    // ------------------------------------------------------------------ //

    /**
     * Topologically sorts constrained super-nodes using Kahn's algorithm backed by a boxing-free
     * {@link IntHeap}. At each step the heap selects the eligible super-node with the smallest
     * key, ensuring deterministic output.
     *
     * @param partition      free/constrained partition
     * @param graph          super-node dependency graph
     * @param nodeCount      total number of super-nodes
     * @param nodeKeys       keys indexed by super-node ID
     * @param keyComparator  key comparator
     * @param <TSortableItem> the item type
     * @return the constrained super-nodes in topological order
     * @throws SortingException if a cycle prevents scheduling all constrained super-nodes
     */
    @NonNull
    @SuppressWarnings("PMD.CognitiveComplexity")
    static <TSortableItem> int[] topologicallySortConstrained(
            @NonNull FreeConstrainedPartition partition,
            @NonNull SuperNodeGraph graph,
            int nodeCount,
            @NonNull TSortableItem[] nodeKeys,
            @NonNull Comparator<TSortableItem> keyComparator) {

        // Seed the heap with constrained super-nodes that already have in-degree 0.
        IntHeap<TSortableItem> eligibleHeap = new IntHeap<>(partition.constrainedCount, nodeKeys, keyComparator);
        for (int superNodeIdx = 0; superNodeIdx < nodeCount; superNodeIdx++) {
            if (!partition.isFree[superNodeIdx] && graph.snInDegree[superNodeIdx] == 0) {
                eligibleHeap.add(superNodeIdx);
            }
        }

        // Process the heap: extract min, decrement dependents' in-degrees, seed newly eligible.
        int[] constrainedOrder = new int[partition.constrainedCount];
        int constrainedIdx = 0;
        while (!eligibleHeap.isEmpty()) {
            int superNodeIdx = eligibleHeap.removeMin();
            constrainedOrder[constrainedIdx++] = superNodeIdx;

            IntBag dependents = graph.snDependents[superNodeIdx];
            if (dependents != null) {
                for (int bagIdx = 0; bagIdx < dependents.size(); bagIdx++) {
                    int dependentSuperNode = dependents.get(bagIdx);
                    if (--graph.snInDegree[dependentSuperNode] == 0) {
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
    // Merge sort for free super-nodes                                     //
    // ------------------------------------------------------------------ //

    /**
     * Merge-sorts free super-node indices by their key.
     *
     * @param indices       the array of free super-node indices to sort in-place
     * @param from          start index (inclusive)
     * @param to            end index (exclusive)
     * @param nodeKeys      keys indexed by super-node ID
     * @param keyComparator key comparator
     * @param <TSortableItem> the item type
     */
    static <TSortableItem> void mergeSortByKey(
            @NonNull int[] indices,
            int from,
            int to,
            @NonNull TSortableItem[] nodeKeys,
            @NonNull Comparator<TSortableItem> keyComparator) {
        int length = to - from;
        if (length <= ONE) {
            return;
        }
        int[] buffer = new int[length];
        mergeSortHelper(indices, buffer, from, to, nodeKeys, keyComparator);
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
    // Two-pointer merge and expand                                        //
    // ------------------------------------------------------------------ //

    /**
     * Merges the pre-sorted free and topologically-sorted constrained super-node streams,
     * expanding each super-node into its individual items as it is emitted.
     *
     * @param partition         free/constrained partition
     * @param constrainedOrder  constrained super-nodes in topological order
     * @param nodeKeys          keys indexed by super-node ID
     * @param keyComparator     key comparator
     * @param superNodes        the super-node layout
     * @param items             the original item list
     * @param <TSortableItem>   the item type
     * @return the final sorted item list
     */
    @NonNull
    static <TSortableItem> List<TSortableItem> mergeAndExpand(
            @NonNull FreeConstrainedPartition partition,
            @NonNull int[] constrainedOrder,
            @NonNull TSortableItem[] nodeKeys,
            @NonNull Comparator<TSortableItem> keyComparator,
            @NonNull SuperNodeUtils.SuperNodes<TSortableItem> superNodes,
            @NonNull List<TSortableItem> items) {
        int itemCount = items.size();
        List<TSortableItem> result = new ArrayList<>(itemCount);
        int freeIdx = 0;
        int constrainedIdx = 0;

        // Two-pointer merge: pick the super-node with the smaller key.
        while (freeIdx < partition.freeCount && constrainedIdx < partition.constrainedCount) {
            if (keyComparator.compare(
                            nodeKeys[partition.freeSuperNodes[freeIdx]], nodeKeys[constrainedOrder[constrainedIdx]])
                    <= 0) {
                SuperNodeUtils.expandSuperNode(partition.freeSuperNodes[freeIdx++], superNodes, items, result);
            } else {
                SuperNodeUtils.expandSuperNode(constrainedOrder[constrainedIdx++], superNodes, items, result);
            }
        }

        // Drain remaining free super-nodes.
        while (freeIdx < partition.freeCount) {
            SuperNodeUtils.expandSuperNode(partition.freeSuperNodes[freeIdx++], superNodes, items, result);
        }

        // Drain remaining constrained super-nodes.
        while (constrainedIdx < partition.constrainedCount) {
            SuperNodeUtils.expandSuperNode(constrainedOrder[constrainedIdx++], superNodes, items, result);
        }

        return result;
    }

    // ------------------------------------------------------------------ //
    // Data carriers for intermediate algorithm state                      //
    // ------------------------------------------------------------------ //

    /** Directed dependency graph between super-nodes. */
    @SuppressWarnings({"PMD.UseVarargs", "PMD.ArrayIsStoredDirectly"})
    static final class SuperNodeGraph {

        final IntBag[] snDependents;
        final int[] snInDegree;
        final boolean[] snHasOutgoing;

        SuperNodeGraph(IntBag[] snDependents, int[] snInDegree, boolean[] snHasOutgoing) {
            this.snDependents = snDependents;
            this.snInDegree = snInDegree;
            this.snHasOutgoing = snHasOutgoing;
        }
    }

    /** Result of splitting super-nodes into free and constrained partitions. */
    @SuppressWarnings({"PMD.UseVarargs", "PMD.ArrayIsStoredDirectly"})
    static final class FreeConstrainedPartition {

        final int[] freeSuperNodes;
        final int freeCount;
        final int constrainedCount;
        final boolean[] isFree;

        FreeConstrainedPartition(int[] freeSuperNodes, int freeCount, int constrainedCount, boolean[] isFree) {
            this.freeSuperNodes = freeSuperNodes;
            this.freeCount = freeCount;
            this.constrainedCount = constrainedCount;
            this.isFree = isFree;
        }
    }

    // ------------------------------------------------------------------ //
    // Boxing-free min-heap                                                //
    // ------------------------------------------------------------------ //

    /** Boxing-free min-heap for super-node indices, ordered by key. */
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    private static final class IntHeap<TSortableItem> {

        private final int[] data;
        private final TSortableItem[] keys; // intentional: keys array is owned by the caller
        private final Comparator<TSortableItem> comparator;
        private int size;

        IntHeap(int capacity, TSortableItem[] keys, Comparator<TSortableItem> comparator) {
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
    // Growable int list with linear-scan dedup                            //
    // ------------------------------------------------------------------ //

    /** Growable int list with linear-scan contains, used for super-node adjacency dedup. */
    static final class IntBag {

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

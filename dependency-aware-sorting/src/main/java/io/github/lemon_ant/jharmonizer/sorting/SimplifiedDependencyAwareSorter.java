package io.github.lemon_ant.jharmonizer.sorting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * A high-performance dependency-aware sorter that operates on arbitrary item types,
 * respecting group constraints and dependency ordering.
 *
 * <p>The algorithm collapses group members into <em>super-nodes</em>,
 * builds a dependency graph between super-nodes, splits them into free (no dependency edges) and
 * constrained partitions, sorts each partition independently, and finally merges the two sorted
 * streams while expanding each super-node back into its individual members.</p>
 *
 * <h2>Optimizations</h2>
 * <ul>
 *   <li><b>Flat-array super-node storage</b> — a single {@code int[n]} holds all member indices
 *       grouped by super-node, with offset/length pairs for O(1) random access.</li>
 *   <li><b>Boxing-free {@link IntHeap}</b> — replaces {@code PriorityQueue}, eliminating all
 *       {@code Integer} boxing/unboxing in the topological selection hot path.</li>
 *   <li><b>Free / constrained split</b> — super-nodes without dependency edges are pre-sorted
 *       with merge sort; only dependency-involved nodes go through the heap.</li>
 *   <li><b>{@link IntBag} adjacency dedup</b> — duplicate super-node edges are detected via
 *       linear scan on small lists instead of {@code HashSet}.</li>
 *   <li><b>Insertion sort</b> for intra-super-node ordering (typically ≤ 4 elements),
 *       with fallback to mini topological sort when internal dependency edges violate
 *       comparator ordering.</li>
 *   <li><b>Fast path</b> — when no groups and no dependencies exist, a single
 *       {@code List.sort} bypasses all super-node machinery.</li>
 * </ul>
 *
 * <p>Time complexity: <em>O(n log n + E)</em> · Space: <em>O(n + E)</em>.</p>
 */
@UtilityClass
@SuppressWarnings({
    "PMD.GodClass",
    "PMD.TooManyMethods",
    "PMD.CyclomaticComplexity",
    "PMD.UseVarargs",
    "PMD.AssignmentInOperand"
})
public class SimplifiedDependencyAwareSorter {

    private static final int ONE = 1;

    // ------------------------------------------------------------------ //
    //  Public entry point                                                 //
    // ------------------------------------------------------------------ //

    /**
     * Sorts items respecting dependency constraints and group bundling.
     *
     * <p>Dependency edges between members of the same group are used for intra-group
     * topological ordering; edges between different groups (or ungrouped singletons) drive
     * inter-super-node ordering.</p>
     *
     * @param items        items to sort
     * @param groups       group definitions; use {@link Groups#empty()} if none
     * @param dependencies provider → dependent ordering edges
     * @param comparator   determines base ordering and intra-group ordering
     * @param <TSortableItem> the item type
     * @return a new, unmodifiable list of the same items in the computed order
     * @throws SortingException if the input is invalid or contains a dependency cycle
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
        if (itemList.size() <= ONE) {
            return List.copyOf(itemList);
        }

        Map<TSortableItem, Integer> itemToIndex = SortingUtils.buildItemIndex(itemList);

        // Fast path — no constraints: sort directly.
        if (groups.getGroups().isEmpty() && dependencies.getEdges().isEmpty()) {
            itemList.sort(comparator);
            return Collections.unmodifiableList(itemList);
        }

        // Step 1: Group members into super-nodes by explicit group definitions.
        SuperNodeLayout layout = buildSuperNodeLayout(itemList, itemToIndex, groups);

        // Step 2: Build per-member dependency adjacency for intra-super-node use.
        MemberDependencyIndex memberDeps =
                buildMemberDependencyIndex(dependencies, itemToIndex, layout.memberToSuperNode, itemList.size());

        // Step 3: Fast path — if every item is its own super-node and has no dependencies.
        if (isUnconstrainedSingletons(layout, memberDeps, itemList.size())) {
            itemList.sort(comparator);
            return Collections.unmodifiableList(itemList);
        }

        // Step 4: Sort members within each super-node (insertion sort + fallback topo-sort).
        sortIntraSuperNodeMembers(layout, itemList, comparator, memberDeps);

        // Step 5: Compute representative keys for inter-super-node comparison.
        @SuppressWarnings("unchecked")
        TSortableItem[] snRepresentativeKey = (TSortableItem[]) new Object[layout.superNodeCount];
        collectRepresentativeKeys(layout, itemList, snRepresentativeKey);

        // Step 6: Build the super-node dependency graph.
        SuperNodeGraph graph = buildSuperNodeDependencyGraph(
                memberDeps, layout.memberToSuperNode, layout.superNodeCount, itemList.size());

        // Step 7: Split super-nodes into free (no edges) and constrained partitions.
        FreeConstrainedPartition partition =
                partitionFreeAndConstrained(graph.snInDegree, graph.snHasOutgoing, layout.superNodeCount);

        // Step 8: Pre-sort free super-nodes by representative key (merge sort).
        mergeSortByRepresentativeKey(partition.freeSuperNodes, 0, partition.freeCount, snRepresentativeKey, comparator);

        // Step 9: Topologically sort constrained super-nodes (Kahn's algorithm via IntHeap).
        int[] constrainedOrder =
                topologicallySortConstrained(partition, graph, layout.superNodeCount, snRepresentativeKey, comparator);

        // Step 10: Merge the two sorted streams and expand each super-node into its members.
        List<TSortableItem> result =
                mergeAndExpand(partition, constrainedOrder, snRepresentativeKey, comparator, layout, itemList);

        return Collections.unmodifiableList(result);
    }

    // ------------------------------------------------------------------ //
    //  Step 1: Super-node construction from Groups                        //
    // ------------------------------------------------------------------ //

    /**
     * Groups members into super-nodes based on explicit group definitions and builds
     * the flat-array storage for efficient random access.
     */
    @NonNull
    @SuppressWarnings("PMD.CognitiveComplexity")
    private static <TSortableItem> SuperNodeLayout buildSuperNodeLayout(
            List<TSortableItem> items, Map<TSortableItem, Integer> itemToIndex, Groups<TSortableItem> groups) {
        int itemCount = items.size();
        int[] memberToSuperNode = new int[itemCount];
        Arrays.fill(memberToSuperNode, SortingUtils.UNASSIGNED);
        int superNodeCount = 0;

        for (Group<TSortableItem> group : groups.getGroups()) {
            List<TSortableItem> groupItems = group.getItems();
            if (groupItems.isEmpty()) {
                continue;
            }
            for (TSortableItem member : groupItems) {
                int idx = SortingUtils.resolveGroupMemberIndex(itemToIndex, member);
                SortingUtils.validateNotAlreadyGrouped(memberToSuperNode[idx], member);
                memberToSuperNode[idx] = superNodeCount;
            }
            superNodeCount++;
        }

        // Assign singleton super-nodes for ungrouped items.
        for (int i = 0; i < itemCount; i++) {
            if (memberToSuperNode[i] == SortingUtils.UNASSIGNED) {
                memberToSuperNode[i] = superNodeCount++;
            }
        }

        // Build flat-array storage: single int[n] with offset/length pairs per super-node.
        int[] snLength = new int[superNodeCount];
        for (int memberIdx = 0; memberIdx < itemCount; memberIdx++) {
            snLength[memberToSuperNode[memberIdx]]++;
        }

        int[] snOffset = new int[superNodeCount];
        for (int superNodeIdx = 1; superNodeIdx < superNodeCount; superNodeIdx++) {
            snOffset[superNodeIdx] = snOffset[superNodeIdx - 1] + snLength[superNodeIdx - 1];
        }

        int[] snMembers = new int[itemCount];
        int[] fillCounters = new int[superNodeCount];
        for (int memberIdx = 0; memberIdx < itemCount; memberIdx++) {
            int superNodeIdx = memberToSuperNode[memberIdx];
            snMembers[snOffset[superNodeIdx] + fillCounters[superNodeIdx]++] = memberIdx;
        }

        return new SuperNodeLayout(memberToSuperNode, superNodeCount, snOffset, snLength, snMembers);
    }

    // ------------------------------------------------------------------ //
    //  Step 2: Per-member dependency index                                //
    // ------------------------------------------------------------------ //

    /**
     * Builds a per-member dependency adjacency index from the supplied dependency edges.
     * Each member maps to a bag of dependent member indices.
     */
    @NonNull
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private static <TSortableItem> MemberDependencyIndex buildMemberDependencyIndex(
            Dependencies<TSortableItem> dependencies,
            Map<TSortableItem, Integer> itemToIndex,
            int[] memberToSuperNode,
            int itemCount) {
        IntBag[] memberDependents = new IntBag[itemCount];
        boolean hasDependencies = false;

        for (Dependencies.Dependency<TSortableItem> edge : dependencies.getEdges()) {
            SortingUtils.ResolvedEdge<TSortableItem> resolved = SortingUtils.resolveDependencyEdge(edge, itemToIndex);

            int providerIdx = resolved.getProviderIndex();
            int dependentIdx = resolved.getDependentIndex();

            IntBag bag = memberDependents[providerIdx];
            if (bag == null) {
                bag = new IntBag();
                memberDependents[providerIdx] = bag;
            }
            if (!bag.contains(dependentIdx)) {
                bag.add(dependentIdx);
                hasDependencies = true;
            }
        }

        return new MemberDependencyIndex(memberDependents, hasDependencies);
    }

    // ------------------------------------------------------------------ //
    //  Step 3: Fast-path check                                            //
    // ------------------------------------------------------------------ //

    /**
     * Returns {@code true} when every member is its own super-node and none has dependencies.
     */
    private static boolean isUnconstrainedSingletons(
            SuperNodeLayout layout, MemberDependencyIndex memberDeps, int itemCount) {
        return layout.superNodeCount == itemCount && !memberDeps.hasDependencies;
    }

    // ------------------------------------------------------------------ //
    //  Step 4: Intra-super-node sort                                      //
    // ------------------------------------------------------------------ //

    /**
     * Sorts members within each multi-member super-node.
     *
     * <p>Uses insertion sort as the primary strategy (optimal for ≤ 4 elements). Falls back to a
     * mini topological sort when internal dependency edges cause the comparator-only ordering to
     * violate dependency constraints.</p>
     */
    private static <TSortableItem> void sortIntraSuperNodeMembers(
            SuperNodeLayout layout,
            List<TSortableItem> items,
            Comparator<TSortableItem> comparator,
            MemberDependencyIndex memberDeps) {
        for (int superNodeIdx = 0; superNodeIdx < layout.superNodeCount; superNodeIdx++) {
            if (layout.snLength[superNodeIdx] > ONE) {
                sortSingleSuperNodeMembers(
                        layout.snMembers,
                        layout.snOffset[superNodeIdx],
                        layout.snLength[superNodeIdx],
                        items,
                        comparator,
                        memberDeps,
                        layout.memberToSuperNode);
            }
        }
    }

    /**
     * Sorts a single super-node's member indices. Applies insertion sort first, then checks for
     * internal dependency violations and falls back to topological sort if needed.
     */
    private static <TSortableItem> void sortSingleSuperNodeMembers(
            int[] snMembers,
            int offset,
            int length,
            List<TSortableItem> items,
            Comparator<TSortableItem> comparator,
            MemberDependencyIndex memberDeps,
            int[] memberToSuperNode) {
        insertionSortMemberIndices(snMembers, offset, length, items, comparator);

        if (hasInternalDependencyViolation(snMembers, offset, length, memberDeps, memberToSuperNode)) {
            topoSortMemberIndices(snMembers, offset, length, items, comparator, memberDeps, memberToSuperNode);
        }
    }

    // ------------------------------------------------------------------ //
    //  Step 5: Representative keys                                        //
    // ------------------------------------------------------------------ //

    /**
     * Collects the representative item (first member after intra-sort) of each super-node.
     */
    private static <TSortableItem> void collectRepresentativeKeys(
            SuperNodeLayout layout, List<TSortableItem> items, TSortableItem[] snRepresentativeKey) {
        for (int sn = 0; sn < layout.superNodeCount; sn++) {
            snRepresentativeKey[sn] = items.get(layout.snMembers[layout.snOffset[sn]]);
        }
    }

    // ------------------------------------------------------------------ //
    //  Step 6: Super-node dependency graph                                //
    // ------------------------------------------------------------------ //

    /**
     * Builds the directed dependency graph between super-nodes from the per-member
     * dependency index. Intra-super-node edges are skipped.
     */
    @NonNull
    @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.AvoidInstantiatingObjectsInLoops"})
    private static SuperNodeGraph buildSuperNodeDependencyGraph(
            MemberDependencyIndex memberDeps, int[] memberToSuperNode, int superNodeCount, int itemCount) {
        IntBag[] snDependents = new IntBag[superNodeCount];
        int[] snInDegree = new int[superNodeCount];
        boolean[] snHasOutgoing = new boolean[superNodeCount];

        for (int memberIdx = 0; memberIdx < itemCount; memberIdx++) {
            IntBag dependents = memberDeps.memberDependents[memberIdx];
            if (dependents == null) {
                continue;
            }

            int providerSuperNode = memberToSuperNode[memberIdx];
            for (int bagIdx = 0; bagIdx < dependents.size(); bagIdx++) {
                int dependentMemberIdx = dependents.get(bagIdx);
                int dependentSuperNode = memberToSuperNode[dependentMemberIdx];

                if (dependentSuperNode == providerSuperNode) {
                    // Intra-super-node edge — handled by intra-super-node topo sort.
                    continue;
                }

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
        }

        return new SuperNodeGraph(snDependents, snInDegree, snHasOutgoing);
    }

    // ------------------------------------------------------------------ //
    //  Step 7: Free / constrained partition                               //
    // ------------------------------------------------------------------ //

    /**
     * Partitions super-nodes into <em>free</em> (no incoming or outgoing edges) and
     * <em>constrained</em> (participates in at least one dependency).
     */
    @NonNull
    private static FreeConstrainedPartition partitionFreeAndConstrained(
            int[] snInDegree, boolean[] snHasOutgoing, int superNodeCount) {
        int freeCount = 0;
        int constrainedCount = 0;
        boolean[] isFree = new boolean[superNodeCount];

        for (int superNodeIdx = 0; superNodeIdx < superNodeCount; superNodeIdx++) {
            if (snInDegree[superNodeIdx] == 0 && !snHasOutgoing[superNodeIdx]) {
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
    //  Step 8: Merge sort for free super-nodes                            //
    // ------------------------------------------------------------------ //

    /**
     * Merge-sorts free super-node indices by their representative key.
     */
    private static <TSortableItem> void mergeSortByRepresentativeKey(
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
    //  Step 9: Constrained topological sort                               //
    // ------------------------------------------------------------------ //

    /**
     * Topologically sorts constrained super-nodes using Kahn's algorithm backed by a boxing-free
     * {@link IntHeap}. At each step the heap selects the eligible super-node with the smallest
     * representative key, ensuring deterministic output.
     */
    @NonNull
    @SuppressWarnings("PMD.CognitiveComplexity")
    private static <TSortableItem> int[] topologicallySortConstrained(
            FreeConstrainedPartition partition,
            SuperNodeGraph graph,
            int superNodeCount,
            TSortableItem[] snRepresentativeKey,
            Comparator<TSortableItem> comparator) {
        // Seed the heap with constrained super-nodes that already have in-degree 0.
        IntHeap<TSortableItem> eligibleHeap =
                new IntHeap<>(partition.constrainedCount, snRepresentativeKey, comparator);
        for (int superNodeIdx = 0; superNodeIdx < superNodeCount; superNodeIdx++) {
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
    //  Step 10: Merge and expand                                          //
    // ------------------------------------------------------------------ //

    /**
     * Merges the pre-sorted free and topologically-sorted constrained super-node streams,
     * expanding each super-node into its individual members as it is emitted.
     */
    @NonNull
    private static <TSortableItem> List<TSortableItem> mergeAndExpand(
            FreeConstrainedPartition partition,
            int[] constrainedOrder,
            TSortableItem[] snRepresentativeKey,
            Comparator<TSortableItem> comparator,
            SuperNodeLayout layout,
            List<TSortableItem> items) {
        List<TSortableItem> result = new ArrayList<>(items.size());
        int freeIdx = 0;
        int constrainedIdx = 0;

        // Two-pointer merge: pick the super-node with the smaller representative key.
        while (freeIdx < partition.freeCount && constrainedIdx < partition.constrainedCount) {
            if (comparator.compare(
                            snRepresentativeKey[partition.freeSuperNodes[freeIdx]],
                            snRepresentativeKey[constrainedOrder[constrainedIdx]])
                    <= 0) {
                expandSuperNode(partition.freeSuperNodes[freeIdx++], layout, items, result);
            } else {
                expandSuperNode(constrainedOrder[constrainedIdx++], layout, items, result);
            }
        }

        // Drain remaining free super-nodes.
        while (freeIdx < partition.freeCount) {
            expandSuperNode(partition.freeSuperNodes[freeIdx++], layout, items, result);
        }

        // Drain remaining constrained super-nodes.
        while (constrainedIdx < partition.constrainedCount) {
            expandSuperNode(constrainedOrder[constrainedIdx++], layout, items, result);
        }

        return result;
    }

    /**
     * Appends all members of the specified super-node to the result list.
     */
    private static <TSortableItem> void expandSuperNode(
            int superNodeIdx, SuperNodeLayout layout, List<TSortableItem> items, List<TSortableItem> result) {
        int offset = layout.snOffset[superNodeIdx];
        int length = layout.snLength[superNodeIdx];
        for (int j = 0; j < length; j++) {
            result.add(items.get(layout.snMembers[offset + j]));
        }
    }

    // ------------------------------------------------------------------ //
    //  Intra-super-node sorting helpers                                   //
    // ------------------------------------------------------------------ //

    /**
     * Insertion sort on a slice of the member-index array. Optimal for super-nodes with ≤ 4 members.
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    private static <TSortableItem> void insertionSortMemberIndices(
            int[] arr, int offset, int length, List<TSortableItem> items, Comparator<TSortableItem> comparator) {
        for (int i = 1; i < length; i++) {
            int keyIdx = arr[offset + i];
            TSortableItem keyItem = items.get(keyIdx);
            int j = i - 1;
            while (j >= 0 && comparator.compare(items.get(arr[offset + j]), keyItem) > 0) {
                arr[offset + j + 1] = arr[offset + j];
                j--;
            }
            arr[offset + j + 1] = keyIdx;
        }
    }

    /**
     * Checks whether the insertion-sorted order violates any internal dependency constraint.
     * A violation occurs when a provider appears after its dependent within the same super-node.
     */
    private static boolean hasInternalDependencyViolation(
            int[] snMembers, int offset, int length, MemberDependencyIndex memberDeps, int[] memberToSuperNode) {
        int superNodeId = memberToSuperNode[snMembers[offset]];

        for (int i = 0; i < length; i++) {
            int memberIdx = snMembers[offset + i];
            IntBag dependents = memberDeps.memberDependents[memberIdx];
            if (dependents == null) {
                continue;
            }
            for (int bagIdx = 0; bagIdx < dependents.size(); bagIdx++) {
                int depIdx = dependents.get(bagIdx);
                if (memberToSuperNode[depIdx] != superNodeId) {
                    continue;
                }
                // Provider at position i must come before dependent. If dependent appears at
                // position j < i, the insertion-sort ordering violates the dependency.
                for (int j = 0; j < i; j++) {
                    if (snMembers[offset + j] == depIdx) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Fallback mini topological sort for a single super-node's member indices.
     *
     * <p>This handles the case where a dependent member sorts before its
     * provider by the configured comparator, causing both to share a group and creating
     * super-nodes with internal dependency edges that insertion sort alone cannot resolve.</p>
     */
    @SuppressWarnings({
        "PMD.UseConcurrentHashMap",
        "PMD.CognitiveComplexity",
        "PMD.CyclomaticComplexity",
        "PMD.NPathComplexity"
    })
    private static <TSortableItem> void topoSortMemberIndices(
            int[] snMembers,
            int offset,
            int length,
            List<TSortableItem> items,
            Comparator<TSortableItem> comparator,
            MemberDependencyIndex memberDeps,
            int[] memberToSuperNode) {
        int superNodeId = memberToSuperNode[snMembers[offset]];

        // Map global member indices to local [0..length) positions within this super-node.
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<Integer, Integer> globalToLocal = new HashMap<>(length * 2);
        for (int localIdx = 0; localIdx < length; localIdx++) {
            globalToLocal.put(snMembers[offset + localIdx], localIdx);
        }

        // Compute local in-degrees from internal dependency edges.
        int[] localInDegree = new int[length];
        for (int localIdx = 0; localIdx < length; localIdx++) {
            int memberIdx = snMembers[offset + localIdx];
            IntBag dependents = memberDeps.memberDependents[memberIdx];
            if (dependents == null) {
                continue;
            }
            for (int bagIdx = 0; bagIdx < dependents.size(); bagIdx++) {
                int depGlobalIdx = dependents.get(bagIdx);
                if (memberToSuperNode[depGlobalIdx] != superNodeId) {
                    continue;
                }
                Integer depLocalIdx = globalToLocal.get(depGlobalIdx);
                if (depLocalIdx != null) {
                    localInDegree[depLocalIdx]++;
                }
            }
        }

        // Greedy topological sort: at each step pick the eligible member with the smallest key.
        int[] result = new int[length];
        boolean[] processed = new boolean[length];

        for (int step = 0; step < length; step++) {
            int bestLocal = -1;
            for (int localIdx = 0; localIdx < length; localIdx++) {
                if (processed[localIdx] || localInDegree[localIdx] != 0) {
                    continue;
                }
                if (bestLocal == -1
                        || comparator.compare(
                                        items.get(snMembers[offset + localIdx]),
                                        items.get(snMembers[offset + bestLocal]))
                                < 0) {
                    bestLocal = localIdx;
                }
            }

            if (bestLocal == -1) {
                throw new SortingException("Circular dependency detected within group");
            }

            result[step] = snMembers[offset + bestLocal];
            processed[bestLocal] = true;

            // Decrement in-degrees for dependents of the just-processed member.
            int memberIdx = snMembers[offset + bestLocal];
            IntBag dependents = memberDeps.memberDependents[memberIdx];
            if (dependents != null) {
                for (int bagIdx = 0; bagIdx < dependents.size(); bagIdx++) {
                    int depGlobalIdx = dependents.get(bagIdx);
                    if (memberToSuperNode[depGlobalIdx] != superNodeId) {
                        continue;
                    }
                    Integer depLocalIdx = globalToLocal.get(depGlobalIdx);
                    if (depLocalIdx != null) {
                        localInDegree[depLocalIdx]--;
                    }
                }
            }
        }

        System.arraycopy(result, 0, snMembers, offset, length);
    }

    // ------------------------------------------------------------------ //
    //  Data carriers for intermediate algorithm state                     //
    // ------------------------------------------------------------------ //

    /** Flat-array layout of super-node membership data. */
    private static final class SuperNodeLayout {

        final int[] memberToSuperNode;
        final int superNodeCount;
        final int[] snOffset;
        final int[] snLength;
        final int[] snMembers;

        private SuperNodeLayout(
                int[] memberToSuperNode, int superNodeCount, int[] snOffset, int[] snLength, int[] snMembers) {
            this.memberToSuperNode = memberToSuperNode;
            this.superNodeCount = superNodeCount;
            this.snOffset = snOffset;
            this.snLength = snLength;
            this.snMembers = snMembers;
        }
    }

    /** Per-member dependency adjacency used for both intra and inter super-node processing. */
    private static final class MemberDependencyIndex {

        final IntBag[] memberDependents;
        final boolean hasDependencies;

        private MemberDependencyIndex(IntBag[] memberDependents, boolean hasDependencies) {
            this.memberDependents = memberDependents;
            this.hasDependencies = hasDependencies;
        }
    }

    /** Directed dependency graph between super-nodes. */
    private static final class SuperNodeGraph {

        final IntBag[] snDependents;
        final int[] snInDegree;
        final boolean[] snHasOutgoing;

        private SuperNodeGraph(IntBag[] snDependents, int[] snInDegree, boolean[] snHasOutgoing) {
            this.snDependents = snDependents;
            this.snInDegree = snInDegree;
            this.snHasOutgoing = snHasOutgoing;
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
    private static final class IntHeap<TKey> {

        private final int[] data;
        private final TKey[] keys;
        private final Comparator<TKey> comparator;
        private int size;

        IntHeap(int capacity, TKey[] keys, Comparator<TKey> comparator) {
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
    //  Growable int list with linear-scan dedup                           //
    // ------------------------------------------------------------------ //

    /** Growable int list with linear-scan contains, used for adjacency dedup. */
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

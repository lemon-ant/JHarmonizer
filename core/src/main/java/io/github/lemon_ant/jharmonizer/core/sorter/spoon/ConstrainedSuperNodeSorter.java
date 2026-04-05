package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

/**
 * A low-level, high-performance constrained sorting engine that operates on {@link SortableTypeMember} arrays.
 *
 * <p>The algorithm collapses members sharing the same representative into <em>super-nodes</em>,
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
 *   <li><b>Insertion sort</b> for intra-super-node ordering (typically ≤ 4 elements).</li>
 *   <li><b>Fast path</b> — when no super-nodes and no dependencies exist, a single
 *       {@code Arrays.sort} bypasses all super-node machinery.</li>
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
class ConstrainedSuperNodeSorter {

    private static final int ONE = 1;

    // ------------------------------------------------------------------ //
    //  Public entry point                                                 //
    // ------------------------------------------------------------------ //

    /**
     * Sorts the given sortable type members respecting dependency constraints and representative grouping.
     *
     * <p>The algorithm proceeds through these stages:</p>
     * <ol>
     *   <li><b>Index mapping</b> — maps each {@link CtTypeMember} to its position in the member array.</li>
     *   <li><b>Super-node construction</b> — groups members sharing the same representative instance.</li>
     *   <li><b>Fast-path check</b> — if no clusters and no dependencies exist, sorts directly.</li>
     *   <li><b>Flat-array layout</b> — allocates and fills the flat member-index array.</li>
     *   <li><b>Intra-super-node sort</b> — orders members within each super-node.</li>
     *   <li><b>Super-node dependency graph</b> — builds edges between super-nodes with dedup.</li>
     *   <li><b>Free / constrained split</b> — partitions super-nodes by edge participation.</li>
     *   <li><b>Free-node merge sort</b> — pre-sorts free super-nodes by representative key.</li>
     *   <li><b>Constrained-node topological sort</b> — Kahn's algorithm via boxing-free heap.</li>
     *   <li><b>Merge and expand</b> — merges the two sorted streams, expanding super-nodes.</li>
     * </ol>
     *
     * @param sortableTypeMembers the members to sort
     * @param orderingKeyComparator comparator for ordering keys
     * @return the sorted list of members
     */
    @NonNull
    static List<@NonNull SortableTypeMember> sort(
            @NonNull List<SortableTypeMember> sortableTypeMembers,
            @NonNull Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {
        int memberCount = sortableTypeMembers.size();
        if (memberCount <= ONE) {
            return sortableTypeMembers;
        }

        SortableTypeMember[] members = sortableTypeMembers.toArray(SortableTypeMember[]::new);

        // Step 1: Build index mapping from CtTypeMember → array position.
        Map<CtTypeMember, Integer> typeMemberToIndex = buildTypeMemberToIndexMap(members);

        // Step 2: Group members into super-nodes by shared representative instance.
        SuperNodeLayout layout = buildSuperNodeLayout(members);

        // Step 3: Fast path — if every member is its own super-node and has no dependencies,
        //         a simple Arrays.sort is sufficient.
        if (isUnconstrainedSingletons(members, layout)) {
            Arrays.sort(members, comparingByOrderingKey(orderingKeyComparator));
            return List.of(members);
        }

        // Step 4: Sort members within each super-node (insertion sort + fallback topo-sort).
        sortIntraSuperNodeMembers(layout, members, orderingKeyComparator, typeMemberToIndex);

        // Step 5: Compute representative ordering keys for inter-super-node comparison.
        SortableTypeMember.OrderingKey[] snRepresentativeKey =
                collectRepresentativeKeys(layout.representativeToSuperNode);

        // Step 6: Build the super-node dependency graph.
        SuperNodeGraph graph = buildSuperNodeDependencyGraph(
                members, typeMemberToIndex, layout.memberToSuperNode, layout.superNodeCount);

        // Step 7: Split super-nodes into free (no edges) and constrained partitions.
        FreeConstrainedPartition partition =
                partitionFreeAndConstrained(graph.snInDegree, graph.snHasOutgoing, layout.superNodeCount);

        // Step 8: Pre-sort free super-nodes by representative key (merge sort).
        mergeSortByRepresentativeKey(
                partition.freeSuperNodes, 0, partition.freeCount, snRepresentativeKey, orderingKeyComparator);

        // Step 9: Topologically sort constrained super-nodes (Kahn's algorithm via IntHeap).
        int[] constrainedOrder = topologicallySortConstrained(
                partition, graph, layout.superNodeCount, snRepresentativeKey, orderingKeyComparator);

        // Step 10: Merge the two sorted streams and expand each super-node into its members.
        return mergeAndExpand(partition, constrainedOrder, snRepresentativeKey, orderingKeyComparator, layout, members);
    }

    // ------------------------------------------------------------------ //
    //  Step 1: Index mapping                                              //
    // ------------------------------------------------------------------ //

    /**
     * Builds a mapping from each {@link CtTypeMember} to its index in the member array.
     *
     * @param members the sorted member array
     * @return mapping from CtTypeMember to array index
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    @NonNull
    private static Map<CtTypeMember, Integer> buildTypeMemberToIndexMap(SortableTypeMember[] members) {
        Map<CtTypeMember, Integer> typeMemberToIndex = new HashMap<>(members.length * 2);
        for (int memberIdx = 0; memberIdx < members.length; memberIdx++) {
            typeMemberToIndex.put(members[memberIdx].getTypeMember(), memberIdx);
        }
        return typeMemberToIndex;
    }

    // ------------------------------------------------------------------ //
    //  Step 2: Super-node construction                                    //
    // ------------------------------------------------------------------ //

    /**
     * Groups members into super-nodes based on shared representative instances and builds
     * the flat-array storage for efficient random access.
     *
     * @param members the member array
     * @return the super-node layout containing offset/length/member arrays
     */
    @NonNull
    private static SuperNodeLayout buildSuperNodeLayout(SortableTypeMember[] members) {
        int memberCount = members.length;

        // IdentityHashMap is required: grouping uses representative object identity (instance sharing),
        // not value equality.
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<SortableTypeMember, Integer> representativeToSuperNode = new IdentityHashMap<>();
        int[] memberToSuperNode = new int[memberCount];
        int superNodeCount = 0;

        for (int memberIdx = 0; memberIdx < memberCount; memberIdx++) {
            SortableTypeMember representative = members[memberIdx].getRepresentativeTypeMember();
            Integer superNodeId = representativeToSuperNode.get(representative);
            if (superNodeId == null) {
                superNodeId = superNodeCount++;
                representativeToSuperNode.put(representative, superNodeId);
            }
            memberToSuperNode[memberIdx] = superNodeId;
        }

        // Build flat-array storage: single int[n] with offset/length pairs per super-node.
        int[] snLength = new int[superNodeCount];
        for (int memberIdx = 0; memberIdx < memberCount; memberIdx++) {
            snLength[memberToSuperNode[memberIdx]]++;
        }

        int[] snOffset = new int[superNodeCount];
        for (int superNodeIdx = 1; superNodeIdx < superNodeCount; superNodeIdx++) {
            snOffset[superNodeIdx] = snOffset[superNodeIdx - 1] + snLength[superNodeIdx - 1];
        }

        int[] snMembers = new int[memberCount];
        int[] fillCounters = new int[superNodeCount];
        for (int memberIdx = 0; memberIdx < memberCount; memberIdx++) {
            int superNodeIdx = memberToSuperNode[memberIdx];
            snMembers[snOffset[superNodeIdx] + fillCounters[superNodeIdx]++] = memberIdx;
        }

        return new SuperNodeLayout(
                representativeToSuperNode, memberToSuperNode, superNodeCount, snOffset, snLength, snMembers);
    }

    // ------------------------------------------------------------------ //
    //  Step 3: Fast-path check                                            //
    // ------------------------------------------------------------------ //

    /**
     * Returns {@code true} when every member is its own super-node and none has dependencies,
     * meaning a simple {@code Arrays.sort} suffices.
     *
     * @param members the member array
     * @param layout super-node layout
     * @return whether the input is unconstrained singletons
     */
    private static boolean isUnconstrainedSingletons(SortableTypeMember[] members, SuperNodeLayout layout) {
        if (layout.superNodeCount != members.length) {
            return false;
        }
        for (SortableTypeMember member : members) {
            if (!member.getOrderingDependentsInGroup().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ //
    //  Step 4: Intra-super-node sort                                      //
    // ------------------------------------------------------------------ //

    /**
     * Sorts members within each multi-member super-node.
     *
     * <p>Uses insertion sort as the primary strategy (optimal for ≤ 4 elements). Falls back to a
     * mini topological sort when internal dependency edges cause the comparator-only ordering to
     * violate dependency constraints — this can happen in JHarmonizer when a dependent member sorts
     * before its provider by the configured comparator, causing both to share the same representative.</p>
     *
     * @param layout super-node layout
     * @param members the member array
     * @param comparator ordering key comparator
     * @param typeMemberToIndex index mapping
     */
    private static void sortIntraSuperNodeMembers(
            SuperNodeLayout layout,
            SortableTypeMember[] members,
            Comparator<SortableTypeMember.OrderingKey> comparator,
            Map<CtTypeMember, Integer> typeMemberToIndex) {
        for (int superNodeIdx = 0; superNodeIdx < layout.superNodeCount; superNodeIdx++) {
            if (layout.snLength[superNodeIdx] > ONE) {
                sortSingleSuperNodeMembers(
                        layout.snMembers,
                        layout.snOffset[superNodeIdx],
                        layout.snLength[superNodeIdx],
                        members,
                        comparator,
                        typeMemberToIndex,
                        layout.memberToSuperNode);
            }
        }
    }

    /**
     * Sorts a single super-node's member indices. Applies insertion sort first, then checks for
     * internal dependency violations and falls back to topological sort if needed.
     */
    private static void sortSingleSuperNodeMembers(
            int[] snMembers,
            int offset,
            int length,
            SortableTypeMember[] members,
            Comparator<SortableTypeMember.OrderingKey> comparator,
            Map<CtTypeMember, Integer> typeMemberToIndex,
            int[] memberToSuperNode) {
        insertionSortMemberIndices(snMembers, offset, length, members, comparator);

        if (hasInternalDependencyViolation(snMembers, offset, length, members, typeMemberToIndex, memberToSuperNode)) {
            topoSortMemberIndices(snMembers, offset, length, members, comparator, typeMemberToIndex, memberToSuperNode);
        }
    }

    // ------------------------------------------------------------------ //
    //  Step 5: Representative keys                                        //
    // ------------------------------------------------------------------ //

    /**
     * Collects the ordering key of each super-node's representative into an array
     * indexed by super-node ID, enabling O(1) key lookups during inter-super-node comparison.
     *
     * @param representativeToSuperNode mapping from representative to super-node ID
     * @return array of ordering keys indexed by super-node ID
     */
    @NonNull
    private static SortableTypeMember.OrderingKey[] collectRepresentativeKeys(
            Map<SortableTypeMember, Integer> representativeToSuperNode) {
        SortableTypeMember.OrderingKey[] snRepresentativeKey =
                new SortableTypeMember.OrderingKey[representativeToSuperNode.size()];
        for (Map.Entry<SortableTypeMember, Integer> entry : representativeToSuperNode.entrySet()) {
            snRepresentativeKey[entry.getValue()] = entry.getKey().getOrderingKey();
        }
        return snRepresentativeKey;
    }

    // ------------------------------------------------------------------ //
    //  Step 6: Super-node dependency graph                                //
    // ------------------------------------------------------------------ //

    /**
     * Builds the directed dependency graph between super-nodes.
     *
     * <p>Iterates over each member's dependency set. For each provider → dependent pair that maps
     * to different super-nodes, registers the edge (with adjacency-based dedup via {@link IntBag})
     * and increments the dependent super-node's in-degree.</p>
     *
     * @param members the member array
     * @param typeMemberToIndex index mapping
     * @param memberToSuperNode per-member super-node assignment
     * @param superNodeCount total number of super-nodes
     * @return the super-node graph (adjacency lists + in-degrees + outgoing flags)
     */
    @NonNull
    @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.AvoidInstantiatingObjectsInLoops"})
    private static SuperNodeGraph buildSuperNodeDependencyGraph(
            SortableTypeMember[] members,
            Map<CtTypeMember, Integer> typeMemberToIndex,
            int[] memberToSuperNode,
            int superNodeCount) {
        IntBag[] snDependents = new IntBag[superNodeCount];
        int[] snInDegree = new int[superNodeCount];
        boolean[] snHasOutgoing = new boolean[superNodeCount];

        for (int memberIdx = 0; memberIdx < members.length; memberIdx++) {
            Set<CtTypeMember> dependentsInGroup = members[memberIdx].getOrderingDependentsInGroup();
            if (dependentsInGroup.isEmpty()) {
                continue;
            }

            int providerSuperNode = memberToSuperNode[memberIdx];
            for (CtTypeMember dependentCtMember : dependentsInGroup) {
                Integer dependentMemberIdx = typeMemberToIndex.get(dependentCtMember);
                if (dependentMemberIdx == null) {
                    continue;
                }

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
     *
     * @param snInDegree per-super-node in-degree
     * @param snHasOutgoing per-super-node outgoing-edge flag
     * @param superNodeCount total number of super-nodes
     * @return the partition containing free and constrained super-node arrays
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
    //  Step 9: Constrained topological sort                               //
    // ------------------------------------------------------------------ //

    /**
     * Topologically sorts constrained super-nodes using Kahn's algorithm backed by a boxing-free
     * {@link IntHeap}. At each step the heap selects the eligible super-node with the smallest
     * representative ordering key, ensuring deterministic output.
     *
     * @param partition free/constrained partition
     * @param graph super-node dependency graph
     * @param superNodeCount total number of super-nodes
     * @param snRepresentativeKey representative keys indexed by super-node ID
     * @param orderingKeyComparator ordering key comparator
     * @return the constrained super-nodes in topological order
     * @throws IllegalStateException if a cycle prevents scheduling all constrained super-nodes
     */
    @NonNull
    @SuppressWarnings("PMD.CognitiveComplexity")
    private static int[] topologicallySortConstrained(
            FreeConstrainedPartition partition,
            SuperNodeGraph graph,
            int superNodeCount,
            SortableTypeMember.OrderingKey[] snRepresentativeKey,
            Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {

        // Seed the heap with constrained super-nodes that already have in-degree 0.
        IntHeap eligibleHeap = new IntHeap(partition.constrainedCount, snRepresentativeKey, orderingKeyComparator);
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
            throw new IllegalStateException("Cycle detected in super-node dependency graph; "
                    + "not all constrained super-nodes could be scheduled.");
        }

        return constrainedOrder;
    }

    // ------------------------------------------------------------------ //
    //  Step 10: Merge and expand                                          //
    // ------------------------------------------------------------------ //

    /**
     * Merges the pre-sorted free and topologically-sorted constrained super-node streams,
     * expanding each super-node into its individual members as it is emitted.
     *
     * @param partition free/constrained partition
     * @param constrainedOrder constrained super-nodes in topological order
     * @param snRepresentativeKey representative keys indexed by super-node ID
     * @param orderingKeyComparator ordering key comparator
     * @param layout super-node layout
     * @param members the member array
     * @return the final sorted member list
     */
    @NonNull
    private static List<SortableTypeMember> mergeAndExpand(
            FreeConstrainedPartition partition,
            int[] constrainedOrder,
            SortableTypeMember.OrderingKey[] snRepresentativeKey,
            Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator,
            SuperNodeLayout layout,
            SortableTypeMember[] members) {
        int memberCount = members.length;
        List<SortableTypeMember> result = new ArrayList<>(memberCount);
        int freeIdx = 0;
        int constrainedIdx = 0;

        // Two-pointer merge: pick the super-node with the smaller representative key.
        while (freeIdx < partition.freeCount && constrainedIdx < partition.constrainedCount) {
            if (orderingKeyComparator.compare(
                            snRepresentativeKey[partition.freeSuperNodes[freeIdx]],
                            snRepresentativeKey[constrainedOrder[constrainedIdx]])
                    <= 0) {
                expandSuperNode(partition.freeSuperNodes[freeIdx++], layout, members, result);
            } else {
                expandSuperNode(constrainedOrder[constrainedIdx++], layout, members, result);
            }
        }

        // Drain remaining free super-nodes.
        while (freeIdx < partition.freeCount) {
            expandSuperNode(partition.freeSuperNodes[freeIdx++], layout, members, result);
        }

        // Drain remaining constrained super-nodes.
        while (constrainedIdx < partition.constrainedCount) {
            expandSuperNode(constrainedOrder[constrainedIdx++], layout, members, result);
        }

        return result;
    }

    /**
     * Appends all members of the specified super-node to the result list.
     */
    private static void expandSuperNode(
            int superNodeIdx, SuperNodeLayout layout, SortableTypeMember[] members, List<SortableTypeMember> result) {
        int offset = layout.snOffset[superNodeIdx];
        int length = layout.snLength[superNodeIdx];
        for (int j = 0; j < length; j++) {
            result.add(members[layout.snMembers[offset + j]]);
        }
    }

    // ------------------------------------------------------------------ //
    //  Comparator helper                                                  //
    // ------------------------------------------------------------------ //

    @NonNull
    private static Comparator<SortableTypeMember> comparingByOrderingKey(
            Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {
        return (left, right) -> orderingKeyComparator.compare(left.getOrderingKey(), right.getOrderingKey());
    }

    // ------------------------------------------------------------------ //
    //  Intra-super-node sorting helpers                                   //
    // ------------------------------------------------------------------ //

    /**
     * Insertion sort on a slice of the member-index array. Optimal for super-nodes with ≤ 4 members.
     */
    @SuppressWarnings("PMD.AvoidArrayLoops")
    private static void insertionSortMemberIndices(
            int[] arr,
            int offset,
            int length,
            SortableTypeMember[] members,
            Comparator<SortableTypeMember.OrderingKey> comparator) {
        for (int i = 1; i < length; i++) {
            int keyIdx = arr[offset + i];
            SortableTypeMember.OrderingKey keyOrdering = members[keyIdx].getOrderingKey();
            int j = i - 1;
            while (j >= 0 && comparator.compare(members[arr[offset + j]].getOrderingKey(), keyOrdering) > 0) {
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
            int[] snMembers,
            int offset,
            int length,
            SortableTypeMember[] members,
            Map<CtTypeMember, Integer> typeMemberToIndex,
            int[] memberToSuperNode) {
        int superNodeId = memberToSuperNode[snMembers[offset]];

        for (int i = 0; i < length; i++) {
            int memberIdx = snMembers[offset + i];
            for (CtTypeMember dependent : members[memberIdx].getOrderingDependentsInGroup()) {
                Integer depIdx = typeMemberToIndex.get(dependent);
                if (depIdx == null || memberToSuperNode[depIdx] != superNodeId) {
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
     * <p>This handles the JHarmonizer-specific case where a dependent member sorts before its
     * provider by the configured comparator, causing both to share a representative and creating
     * super-nodes with internal dependency edges that insertion sort alone cannot resolve.</p>
     */
    @SuppressWarnings({
        "PMD.UseConcurrentHashMap",
        "PMD.CognitiveComplexity",
        "PMD.CyclomaticComplexity",
        "PMD.NPathComplexity"
    })
    private static void topoSortMemberIndices(
            int[] snMembers,
            int offset,
            int length,
            SortableTypeMember[] members,
            Comparator<SortableTypeMember.OrderingKey> comparator,
            Map<CtTypeMember, Integer> typeMemberToIndex,
            int[] memberToSuperNode) {
        int superNodeId = memberToSuperNode[snMembers[offset]];

        // Map global member indices to local [0..length) positions within this super-node.
        Map<Integer, Integer> globalToLocal = new HashMap<>(length * 2);
        for (int localIdx = 0; localIdx < length; localIdx++) {
            globalToLocal.put(snMembers[offset + localIdx], localIdx);
        }

        // Compute local in-degrees from internal dependency edges.
        int[] localInDegree = new int[length];
        for (int localIdx = 0; localIdx < length; localIdx++) {
            int memberIdx = snMembers[offset + localIdx];
            for (CtTypeMember dependent : members[memberIdx].getOrderingDependentsInGroup()) {
                Integer depGlobalIdx = typeMemberToIndex.get(dependent);
                if (depGlobalIdx == null || memberToSuperNode[depGlobalIdx] != superNodeId) {
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
                                        members[snMembers[offset + localIdx]].getOrderingKey(),
                                        members[snMembers[offset + bestLocal]].getOrderingKey())
                                < 0) {
                    bestLocal = localIdx;
                }
            }

            if (bestLocal == -1) {
                throw new IllegalStateException("Circular dependency detected within representative group");
            }

            result[step] = snMembers[offset + bestLocal];
            processed[bestLocal] = true;

            // Decrement in-degrees for dependents of the just-processed member.
            int memberIdx = snMembers[offset + bestLocal];
            for (CtTypeMember dependent : members[memberIdx].getOrderingDependentsInGroup()) {
                Integer depGlobalIdx = typeMemberToIndex.get(dependent);
                if (depGlobalIdx == null || memberToSuperNode[depGlobalIdx] != superNodeId) {
                    continue;
                }
                Integer depLocalIdx = globalToLocal.get(depGlobalIdx);
                if (depLocalIdx != null) {
                    localInDegree[depLocalIdx]--;
                }
            }
        }

        System.arraycopy(result, 0, snMembers, offset, length);
    }

    // ------------------------------------------------------------------ //
    //  Merge sort for free super-nodes                                    //
    // ------------------------------------------------------------------ //

    /**
     * Merge-sorts free super-node indices by their representative ordering key.
     */
    private static void mergeSortByRepresentativeKey(
            int[] indices,
            int from,
            int to,
            SortableTypeMember.OrderingKey[] keys,
            Comparator<SortableTypeMember.OrderingKey> comparator) {
        int length = to - from;
        if (length <= ONE) {
            return;
        }
        int[] buffer = new int[length];
        mergeSortHelper(indices, buffer, from, to, keys, comparator);
    }

    private static void mergeSortHelper(
            int[] source,
            int[] buffer,
            int from,
            int to,
            SortableTypeMember.OrderingKey[] keys,
            Comparator<SortableTypeMember.OrderingKey> comparator) {
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
    //  Error message                                                      //
    // ------------------------------------------------------------------ //

    /**
     * Composes a diagnostic message listing unschedulable super-node members (those with non-zero in-degree
     * after topological sort has stalled).
     *
     * @param snInDegree per-super-node in-degree array (post-topo-sort)
     * @param isFree per-super-node free flag
     * @param layout super-node layout
     * @param members the member array
     * @return the diagnostic message
     */
    @NonNull
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static String composeUnschedulableSuperNodesMessage(
            int[] snInDegree, boolean[] isFree, SuperNodeLayout layout, SortableTypeMember[] members) {
        List<String> unresolvedMemberNames = new ArrayList<>();
        for (int superNodeIdx = 0; superNodeIdx < snInDegree.length; superNodeIdx++) {
            if (!isFree[superNodeIdx] && snInDegree[superNodeIdx] > 0) {
                int offset = layout.snOffset[superNodeIdx];
                int length = layout.snLength[superNodeIdx];
                for (int j = 0; j < length; j++) {
                    unresolvedMemberNames.add(
                            SpoonTypeMemberUtils.deriveAlphaKey(members[layout.snMembers[offset + j]].getTypeMember()));
                }
            }
        }

        String unresolvedMembers = unresolvedMemberNames.stream().sorted().collect(Collectors.joining(", "));

        return "Detected declaration dependencies that cannot be scheduled deterministically within the member group. "
                + "The pairwise comparator is intentionally not used for this choice because partial-order constraints "
                + "can make such a comparator non-transitive. Check for circular dependencies or unexpected dependency "
                + "relationships between these members: " + unresolvedMembers;
    }

    // ------------------------------------------------------------------ //
    //  Data carriers for intermediate algorithm state                     //
    // ------------------------------------------------------------------ //

    /** Flat-array layout of super-node membership data. */
    private static final class SuperNodeLayout {

        final Map<SortableTypeMember, Integer> representativeToSuperNode;
        final int[] memberToSuperNode;
        final int superNodeCount;
        final int[] snOffset;
        final int[] snLength;
        final int[] snMembers;

        private SuperNodeLayout(
                Map<SortableTypeMember, Integer> representativeToSuperNode,
                int[] memberToSuperNode,
                int superNodeCount,
                int[] snOffset,
                int[] snLength,
                int[] snMembers) {
            this.representativeToSuperNode = representativeToSuperNode;
            this.memberToSuperNode = memberToSuperNode;
            this.superNodeCount = superNodeCount;
            this.snOffset = snOffset;
            this.snLength = snLength;
            this.snMembers = snMembers;
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

    /** Boxing-free min-heap for super-node indices, ordered by representative ordering key. */
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    private static final class IntHeap {

        private final int[] data;
        private final SortableTypeMember.OrderingKey[] keys; // intentional: keys array is owned by the caller
        private final Comparator<SortableTypeMember.OrderingKey> comparator;
        private int size;

        IntHeap(
                int capacity,
                SortableTypeMember.OrderingKey[] keys,
                Comparator<SortableTypeMember.OrderingKey> comparator) {
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

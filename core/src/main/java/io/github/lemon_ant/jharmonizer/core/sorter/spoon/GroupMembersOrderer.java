package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.ComparatorUtils.buildTypeMemberBaseComparator;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyEdgeKind;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Orders type members inside each {@link MemberGroupBlock} according to the group's configured ordering rules,
 * respecting declaration dependencies and optional accessor-pair bundling.
 */
@UtilityClass
@SuppressWarnings({
    "PMD.GodClass",
    "PMD.CouplingBetweenObjects",
    "PMD.TooManyMethods",
    "PMD.CyclomaticComplexity",
    "PMD.UseVarargs"
})
class GroupMembersOrderer {

    private static final Set<MemberDependencyEdgeKind> DECLARATION_DEPENDENCY_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

    private static final Set<MemberDependencyEdgeKind> ACCESSOR_BUNDLE_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE);
    private static final int ONE = 1;

    /**
     * Performs the order members inside groups.
     * @param unorderedMemberGroupBlocks the unordered member group blocks
     * @param memberDependencyGraph the member dependency graph
     * @return the resulting list
     */
    @NonNull
    static List<@NonNull MemberGroupBlock> orderMembersInsideGroups(
            @NonNull List<@NonNull MemberGroupBlock> unorderedMemberGroupBlocks,
            @NonNull MemberDependencyGraph memberDependencyGraph) {
        return unorderedMemberGroupBlocks.stream()
                .map(memberGroupBlock -> orderMembersInsideGroup(memberGroupBlock, memberDependencyGraph))
                .toList();
    }

    @NonNull
    private static MemberGroupBlock orderMembersInsideGroup(
            MemberGroupBlock memberGroupBlock, MemberDependencyGraph memberDependencyGraph) {

        CompiledMemberGroup compiledMemberGroup = memberGroupBlock.getCompiledMemberGroup();
        List<CtTypeMember> groupMembers = memberGroupBlock.getTypeMembers();

        List<CtTypeMember> orderedMembers =
                orderMembersInsideGroup(compiledMemberGroup, groupMembers, memberDependencyGraph);

        return new MemberGroupBlock(compiledMemberGroup, orderedMembers);
    }

    @NonNull
    private static List<@NonNull CtTypeMember> orderMembersInsideGroup(
            CompiledMemberGroup compiledMemberGroup,
            List<CtTypeMember> groupMembers,
            MemberDependencyGraph memberDependencyGraph) {
        if (groupMembers.size() <= ONE) {
            return groupMembers;
        }

        List<OrderingRule> orderingRules = compiledMemberGroup.getOrderingRules();

        Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator =
                ComparatorUtils.buildOrderingComparator(orderingRules);
        List<SortableTypeMember> sortableTypeMembers = convertTypeMembers2SortableTypeMembers(
                compiledMemberGroup, groupMembers, memberDependencyGraph, orderingKeyComparator);
        return orderSortableTypeMembers(sortableTypeMembers, orderingKeyComparator).stream()
                .map(SortableTypeMember::getTypeMember)
                .toList();
    }

    /**
     * Converts the type members2 sortable type members.
     * @param compiledMemberGroup the compiled member group
     * @param groupMembers the group members
     * @param memberDependencyGraph the member dependency graph
     * @param orderingKeyComparator the ordering key comparator
     * @return the converted type members2 sortable type members
     */
    @NonNull
    static List<@NonNull SortableTypeMember> convertTypeMembers2SortableTypeMembers(
            @NonNull CompiledMemberGroup compiledMemberGroup,
            @NonNull List<@NonNull CtTypeMember> groupMembers,
            @NonNull MemberDependencyGraph memberDependencyGraph,
            @NonNull Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {
        boolean keepAccessorsTogether = compiledMemberGroup.isKeepAccessorsTogether();
        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMembers);

        Function<CtTypeMember, SortableTypeMember.OrderingKey> orderingKeyProvider =
                SortableTypeMember.OrderingKey.getOrderingKeyProvider();
        Comparator<CtTypeMember> typeMemberBaseComparator =
                buildTypeMemberBaseComparator(orderingKeyProvider, orderingKeyComparator);
        Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember = keepAccessorsTogether
                ? buildAccessorBundleMembersByMember(groupMemberSet, memberDependencyGraph, typeMemberBaseComparator)
                : Map.of();

        class SortableTypeMemberFactory {

            @SuppressWarnings("PMD.UseConcurrentHashMap")
            private final Map<CtTypeMember, SortableTypeMember> sortableTypeMemberByMember = new HashMap<>();

            private final Set<CtTypeMember> resolvingMembers = new HashSet<>();

            @NonNull
            private SortableTypeMember getOrCreate(CtTypeMember typeMember) {
                SortableTypeMember cachedSortableTypeMember = sortableTypeMemberByMember.get(typeMember);
                if (cachedSortableTypeMember != null) {
                    return cachedSortableTypeMember;
                }
                if (!resolvingMembers.add(typeMember)) {
                    throw new IllegalStateException(
                            "Detected a cycle while resolving representative sortable members for "
                                    + SpoonTypeMemberUtils.deriveAlphaKey(typeMember));
                }

                try {
                    Set<CtTypeMember> declarationDependentsInGroup = findDeclarationDependentsInGroup(
                            typeMember,
                            groupMemberSet,
                            memberDependencyGraph,
                            keepAccessorsTogether,
                            accessorBundleMembersByMember);

                    CtTypeMember representativeTypeMember = findRepresentativeTypeMember(
                            typeMember,
                            declarationDependentsInGroup,
                            keepAccessorsTogether,
                            accessorBundleMembersByMember,
                            typeMemberBaseComparator);

                    SortableTypeMember representativeSortableTypeMember =
                            Objects.equals(representativeTypeMember, typeMember)
                                    ? null
                                    : getOrCreate(representativeTypeMember);

                    SortableTypeMember sortableTypeMember = new SortableTypeMember(
                            typeMember,
                            representativeSortableTypeMember,
                            declarationDependentsInGroup,
                            orderingKeyProvider);
                    sortableTypeMemberByMember.put(typeMember, sortableTypeMember);
                    return sortableTypeMember;
                } finally {
                    resolvingMembers.remove(typeMember);
                }
            }
        }

        SortableTypeMemberFactory sortableTypeMemberFactory = new SortableTypeMemberFactory();
        return groupMembers.stream().map(sortableTypeMemberFactory::getOrCreate).toList();
    }

    @NonNull
    private static Set<@NonNull CtTypeMember> findDeclarationDependentsInGroup(
            CtTypeMember typeMember,
            Set<CtTypeMember> groupMembers,
            MemberDependencyGraph memberDependencyGraph,
            boolean keepAccessorsTogether,
            Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember) {
        Set<CtTypeMember> declarationDependentsInGroup =
                memberDependencyGraph.findTransitiveDependents(typeMember, DECLARATION_DEPENDENCY_ONLY).stream()
                        .filter(groupMembers::contains)
                        .collect(Collectors.toUnmodifiableSet());

        if (keepAccessorsTogether) {
            declarationDependentsInGroup =
                    expandDependentsWithAccessorBundles(declarationDependentsInGroup, accessorBundleMembersByMember);
        }
        return declarationDependentsInGroup;
    }

    @NonNull
    private static CtTypeMember findRepresentativeTypeMember(
            CtTypeMember typeMember,
            Set<CtTypeMember> declarationDependentsInGroup,
            boolean keepAccessorsTogether,
            Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember,
            Comparator<CtTypeMember> typeMemberBaseComparator) {
        if (!declarationDependentsInGroup.isEmpty()) {
            return Stream.concat(Stream.of(typeMember), declarationDependentsInGroup.stream())
                    .min(typeMemberBaseComparator)
                    .orElseThrow();
        }
        if (keepAccessorsTogether) {
            return findAccessorBundleRepresentativeTypeMember(typeMember, accessorBundleMembersByMember);
        }
        return typeMember;
    }

    @NonNull
    private static Set<@NonNull CtTypeMember> expandDependentsWithAccessorBundles(
            Set<CtTypeMember> declarationDependentsInGroup,
            Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember) {
        return declarationDependentsInGroup.stream()
                .flatMap(dependentMember -> Optional.ofNullable(accessorBundleMembersByMember.get(dependentMember))
                        .map(List::stream)
                        .orElseGet(() -> Stream.of(dependentMember)))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Sorts sortable type members using a super-node–based constrained sorting algorithm.
     *
     * <p>Key optimizations over a plain {@code PriorityQueue}-based Kahn's sort:
     * <ul>
     *   <li><b>Flat-array super-node storage</b> — a single {@code int[n]} holds all member indices
     *       grouped by super-node, with offset/length pairs for O(1) random access.</li>
     *   <li><b>Boxing-free {@link IntHeap}</b> — replaces {@code PriorityQueue<SortableTypeMember>},
     *       eliminating all object boxing/unboxing in topological selection.</li>
     *   <li><b>Free-node / constrained-node split</b> — super-nodes with no dependency edges
     *       are pre-sorted with merge sort; only dependency-involved nodes go through the heap.
     *       The two sorted streams are merged during expansion.</li>
     *   <li><b>Adjacency-based edge dedup</b> — duplicate super-node edges are detected via
     *       {@link IntBag#contains} (linear scan on small lists) instead of {@code HashSet}.</li>
     *   <li><b>Insertion sort</b> for intra-cluster ordering (typically ≤ 4 elements).</li>
     *   <li><b>Fast path</b> — when there are no constraints at all, members are sorted directly
     *       by the comparator with {@code Arrays.sort}, bypassing all super-node machinery.</li>
     * </ul>
     *
     * @param sortableTypeMembers the members to sort
     * @param orderingKeyComparator the ordering key comparator
     * @return the sorted list of members
     */
    @NonNull
    @SuppressWarnings({
        "PMD.NPathComplexity",
        "PMD.NcssCount",
        "PMD.CognitiveComplexity",
        "PMD.CyclomaticComplexity",
        "PMD.AssignmentInOperand",
        "PMD.AvoidInstantiatingObjectsInLoops"
    })
    private static List<@NonNull SortableTypeMember> orderSortableTypeMembers(
            List<SortableTypeMember> sortableTypeMembers,
            Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {
        int memberCount = sortableTypeMembers.size();
        if (memberCount <= ONE) {
            return sortableTypeMembers;
        }

        SortableTypeMember[] members = sortableTypeMembers.toArray(SortableTypeMember[]::new);

        // --- Index mapping: CtTypeMember → member array index ---
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, Integer> ctMemberToIndex = new HashMap<>(memberCount * 2);
        for (int memberIdx = 0; memberIdx < memberCount; memberIdx++) {
            ctMemberToIndex.put(members[memberIdx].getTypeMember(), memberIdx);
        }

        // --- Build super-nodes by grouping members with the same representative instance ---
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        // IdentityHashMap is required: grouping uses representative object identity (instance sharing),
        // not value equality.
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

        // --- Fast path: no clusters and no dependencies → simple Arrays.sort ---
        boolean hasAnyDependencies = false;
        for (int memberIdx = 0; memberIdx < memberCount; memberIdx++) {
            if (!members[memberIdx].getOrderingDependentsInGroup().isEmpty()) {
                hasAnyDependencies = true;
                break;
            }
        }

        if (!hasAnyDependencies && superNodeCount == memberCount) {
            Arrays.sort(members, comparingByOrderingKey(orderingKeyComparator));
            return List.of(members);
        }

        // --- Flat-array super-node storage: single int[n] with offset/length pairs ---
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

        // --- Sort intra-super-node members (insertion sort for small clusters) ---
        for (int superNodeIdx = 0; superNodeIdx < superNodeCount; superNodeIdx++) {
            if (snLength[superNodeIdx] > ONE) {
                sortSuperNodeMembers(
                        snMembers,
                        snOffset[superNodeIdx],
                        snLength[superNodeIdx],
                        members,
                        orderingKeyComparator,
                        ctMemberToIndex,
                        memberToSuperNode);
            }
        }

        // --- Representative ordering keys for super-node comparisons ---
        SortableTypeMember.OrderingKey[] snRepresentativeKey = new SortableTypeMember.OrderingKey[superNodeCount];
        for (Map.Entry<SortableTypeMember, Integer> entry : representativeToSuperNode.entrySet()) {
            snRepresentativeKey[entry.getValue()] = entry.getKey().getOrderingKey();
        }

        // --- Build super-node dependency graph with adjacency-based edge dedup ---
        IntBag[] snDependents = new IntBag[superNodeCount];
        int[] snInDegree = new int[superNodeCount];
        boolean[] snHasOutgoing = new boolean[superNodeCount];

        for (int memberIdx = 0; memberIdx < memberCount; memberIdx++) {
            Set<CtTypeMember> dependentsInGroup = members[memberIdx].getOrderingDependentsInGroup();
            if (dependentsInGroup.isEmpty()) {
                continue;
            }

            int providerSuperNode = memberToSuperNode[memberIdx];
            for (CtTypeMember dependentCtMember : dependentsInGroup) {
                Integer dependentMemberIdx = ctMemberToIndex.get(dependentCtMember);
                if (dependentMemberIdx == null) {
                    continue;
                }

                int dependentSuperNode = memberToSuperNode[dependentMemberIdx];
                if (dependentSuperNode == providerSuperNode) {
                    continue;
                }

                snHasOutgoing[providerSuperNode] = true;
                IntBag bag = snDependents[providerSuperNode];
                if (bag == null) {
                    bag = new IntBag();
                    snDependents[providerSuperNode] = bag;
                }
                if (!bag.contains(dependentSuperNode)) {
                    bag.add(dependentSuperNode);
                    snInDegree[dependentSuperNode]++;
                }
            }
        }

        // --- Split into free (no dependency edges) and constrained super-nodes ---
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

        // --- Pre-sort free super-nodes with merge sort ---
        int[] freeSuperNodes = new int[freeCount];
        int freeIdx = 0;
        for (int superNodeIdx = 0; superNodeIdx < superNodeCount; superNodeIdx++) {
            if (isFree[superNodeIdx]) {
                freeSuperNodes[freeIdx++] = superNodeIdx;
            }
        }
        mergeSortByRepresentativeKey(freeSuperNodes, 0, freeCount, snRepresentativeKey, orderingKeyComparator);

        // --- Topological sort constrained super-nodes using boxing-free IntHeap ---
        IntHeap eligibleHeap = new IntHeap(constrainedCount, snRepresentativeKey, orderingKeyComparator);
        for (int superNodeIdx = 0; superNodeIdx < superNodeCount; superNodeIdx++) {
            if (!isFree[superNodeIdx] && snInDegree[superNodeIdx] == 0) {
                eligibleHeap.add(superNodeIdx);
            }
        }

        int[] constrainedOrder = new int[constrainedCount];
        int constrainedIdx = 0;
        while (!eligibleHeap.isEmpty()) {
            int superNodeIdx = eligibleHeap.removeMin();
            constrainedOrder[constrainedIdx++] = superNodeIdx;

            IntBag dependents = snDependents[superNodeIdx];
            if (dependents != null) {
                for (int bagIdx = 0; bagIdx < dependents.size(); bagIdx++) {
                    int dependentSuperNode = dependents.get(bagIdx);
                    if (--snInDegree[dependentSuperNode] == 0) {
                        eligibleHeap.add(dependentSuperNode);
                    }
                }
            }
        }

        if (constrainedIdx != constrainedCount) {
            throw new IllegalStateException(
                    composeUnschedulableSuperNodesMessage(snInDegree, isFree, snOffset, snLength, snMembers, members));
        }

        // --- Merge free and constrained streams, expanding super-nodes ---
        List<SortableTypeMember> result = new ArrayList<>(memberCount);
        freeIdx = 0;
        constrainedIdx = 0;

        while (freeIdx < freeCount && constrainedIdx < constrainedCount) {
            if (orderingKeyComparator.compare(
                            snRepresentativeKey[freeSuperNodes[freeIdx]],
                            snRepresentativeKey[constrainedOrder[constrainedIdx]])
                    <= 0) {
                expandSuperNode(freeSuperNodes[freeIdx++], snOffset, snLength, snMembers, members, result);
            } else {
                expandSuperNode(constrainedOrder[constrainedIdx++], snOffset, snLength, snMembers, members, result);
            }
        }
        while (freeIdx < freeCount) {
            expandSuperNode(freeSuperNodes[freeIdx++], snOffset, snLength, snMembers, members, result);
        }
        while (constrainedIdx < constrainedCount) {
            expandSuperNode(constrainedOrder[constrainedIdx++], snOffset, snLength, snMembers, members, result);
        }

        return result;
    }

    @NonNull
    private static Comparator<SortableTypeMember> comparingByOrderingKey(
            Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {
        return (left, right) -> orderingKeyComparator.compare(left.getOrderingKey(), right.getOrderingKey());
    }

    private static void expandSuperNode(
            int superNodeIdx,
            int[] snOffset,
            int[] snLength,
            int[] snMembers,
            SortableTypeMember[] members,
            List<SortableTypeMember> result) {
        int offset = snOffset[superNodeIdx];
        int length = snLength[superNodeIdx];
        for (int j = 0; j < length; j++) {
            result.add(members[snMembers[offset + j]]);
        }
    }

    /**
     * Sorts member indices within a single super-node. Uses insertion sort as the primary strategy
     * (optimal for the typically small cluster sizes). Falls back to a mini topological sort when the
     * super-node contains internal declaration dependencies that the comparator-only ordering would
     * violate — this can occur in JHarmonizer when a dependent member sorts before its provider by
     * the configured comparator, causing both to share the same representative instance.
     */
    private static void sortSuperNodeMembers(
            int[] snMembers,
            int offset,
            int length,
            SortableTypeMember[] members,
            Comparator<SortableTypeMember.OrderingKey> comparator,
            Map<CtTypeMember, Integer> ctMemberToIndex,
            int[] memberToSuperNode) {
        insertionSortMemberIndices(snMembers, offset, length, members, comparator);

        if (hasInternalDependencyViolation(snMembers, offset, length, members, ctMemberToIndex, memberToSuperNode)) {
            topoSortMemberIndices(snMembers, offset, length, members, comparator, ctMemberToIndex, memberToSuperNode);
        }
    }

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

    private static boolean hasInternalDependencyViolation(
            int[] snMembers,
            int offset,
            int length,
            SortableTypeMember[] members,
            Map<CtTypeMember, Integer> ctMemberToIndex,
            int[] memberToSuperNode) {
        int superNodeId = memberToSuperNode[snMembers[offset]];

        for (int i = 0; i < length; i++) {
            int memberIdx = snMembers[offset + i];
            for (CtTypeMember dependent : members[memberIdx].getOrderingDependentsInGroup()) {
                Integer depIdx = ctMemberToIndex.get(dependent);
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
            Map<CtTypeMember, Integer> ctMemberToIndex,
            int[] memberToSuperNode) {
        int superNodeId = memberToSuperNode[snMembers[offset]];

        Map<Integer, Integer> globalToLocal = new HashMap<>(length * 2);
        for (int localIdx = 0; localIdx < length; localIdx++) {
            globalToLocal.put(snMembers[offset + localIdx], localIdx);
        }

        int[] localInDegree = new int[length];
        for (int localIdx = 0; localIdx < length; localIdx++) {
            int memberIdx = snMembers[offset + localIdx];
            for (CtTypeMember dependent : members[memberIdx].getOrderingDependentsInGroup()) {
                Integer depGlobalIdx = ctMemberToIndex.get(dependent);
                if (depGlobalIdx == null || memberToSuperNode[depGlobalIdx] != superNodeId) {
                    continue;
                }
                Integer depLocalIdx = globalToLocal.get(depGlobalIdx);
                if (depLocalIdx != null) {
                    localInDegree[depLocalIdx]++;
                }
            }
        }

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

            int memberIdx = snMembers[offset + bestLocal];
            for (CtTypeMember dependent : members[memberIdx].getOrderingDependentsInGroup()) {
                Integer depGlobalIdx = ctMemberToIndex.get(dependent);
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

    @NonNull
    private static String composeUnschedulableSuperNodesMessage(
            int[] snInDegree,
            boolean[] isFree,
            int[] snOffset,
            int[] snLength,
            int[] snMembers,
            SortableTypeMember[] members) {
        List<String> unresolvedMemberNames = new ArrayList<>();
        for (int superNodeIdx = 0; superNodeIdx < snInDegree.length; superNodeIdx++) {
            if (!isFree[superNodeIdx] && snInDegree[superNodeIdx] > 0) {
                int offset = snOffset[superNodeIdx];
                int length = snLength[superNodeIdx];
                for (int j = 0; j < length; j++) {
                    unresolvedMemberNames.add(
                            SpoonTypeMemberUtils.deriveAlphaKey(members[snMembers[offset + j]].getTypeMember()));
                }
            }
        }

        String unresolvedMembers = unresolvedMemberNames.stream().sorted().collect(Collectors.joining(", "));

        return "Detected declaration dependencies that cannot be scheduled deterministically within the member group. "
                + "The pairwise comparator is intentionally not used for this choice because partial-order constraints "
                + "can make such a comparator non-transitive. Check for circular dependencies or unexpected dependency "
                + "relationships between these members: " + unresolvedMembers;
    }

    @NonNull
    private static Map<CtTypeMember, List<CtTypeMember>> buildAccessorBundleMembersByMember(
            Set<CtTypeMember> groupMembers,
            MemberDependencyGraph memberDependencyGraph,
            Comparator<CtTypeMember> typeMemberBaseComparator) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember = new HashMap<>();
        Set<CtTypeMember> alreadyIndexedMembers = new HashSet<>();

        for (CtTypeMember groupMember : groupMembers) {
            if (alreadyIndexedMembers.contains(groupMember)) {
                continue;
            }

            List<CtTypeMember> sortedBundleMembersInGroup = Stream.concat(
                            Stream.of(groupMember),
                            memberDependencyGraph.findDirectDependents(groupMember, ACCESSOR_BUNDLE_ONLY).stream())
                    .filter(groupMembers::contains)
                    .sorted(typeMemberBaseComparator)
                    .toList();

            alreadyIndexedMembers.addAll(sortedBundleMembersInGroup);

            // Store only real bundles (size > 1). Singletons are not accessor bundles semantically.
            if (sortedBundleMembersInGroup.size() <= ONE) {
                continue;
            }

            sortedBundleMembersInGroup.forEach(
                    bundleMember -> accessorBundleMembersByMember.put(bundleMember, sortedBundleMembersInGroup));
        }

        return Collections.unmodifiableMap(accessorBundleMembersByMember);
    }

    @NonNull
    private static CtTypeMember findAccessorBundleRepresentativeTypeMember(
            CtTypeMember typeMember, Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember) {
        List<CtTypeMember> sortedBundleMembersInGroup = accessorBundleMembersByMember.get(typeMember);
        return sortedBundleMembersInGroup == null ? typeMember : sortedBundleMembersInGroup.getFirst();
    }

    /** Boxing-free min-heap for super-node indices, ordered by representative ordering key. */
    private static final class IntHeap {

        private final int[] data;

        @SuppressWarnings("PMD.ArrayIsStoredDirectly") // intentional: keys array is owned by the caller
        private final SortableTypeMember.OrderingKey[] keys;

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

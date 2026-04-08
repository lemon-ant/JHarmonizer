package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.ComparatorUtils.buildTypeMemberBaseComparator;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyEdgeKind;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.sorting.Dependencies;
import io.github.lemon_ant.jharmonizer.sorting.Group;
import io.github.lemon_ant.jharmonizer.sorting.Groups;
import io.github.lemon_ant.jharmonizer.sorting.SimplifiedDependencyAwareSorter;
import java.util.ArrayList;
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
 *
 * <p>This class builds {@link SortableTypeMember} wrappers, resolves representative relationships and
 * accessor bundles, then delegates the actual constrained sorting to {@link SimplifiedDependencyAwareSorter}.</p>
 */
@UtilityClass
@SuppressWarnings("PMD.CouplingBetweenObjects")
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
     * Sorts sortable type members respecting dependency constraints and representative grouping.
     *
     * <p>Builds {@link Groups} from representative relationships and {@link Dependencies} from
     * each member's ordering dependents, then delegates to {@link SimplifiedDependencyAwareSorter}.</p>
     *
     * @param sortableTypeMembers the members to sort
     * @param orderingKeyComparator the ordering key comparator
     * @return the sorted list of members
     */
    @NonNull
    @SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.AvoidInstantiatingObjectsInLoops"})
    private static List<@NonNull SortableTypeMember> orderSortableTypeMembers(
            List<SortableTypeMember> sortableTypeMembers,
            Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {

        // Build CtTypeMember → SortableTypeMember lookup for dependency resolution.
        Map<CtTypeMember, SortableTypeMember> ctToSortable = new HashMap<>(sortableTypeMembers.size() * 2);
        for (SortableTypeMember stm : sortableTypeMembers) {
            ctToSortable.put(stm.getTypeMember(), stm);
        }

        // Build Groups from representative identity relationships.
        // IdentityHashMap is required: grouping uses representative object identity (instance sharing).
        Map<SortableTypeMember, List<SortableTypeMember>> groupsByRep = new IdentityHashMap<>();
        for (SortableTypeMember stm : sortableTypeMembers) {
            groupsByRep
                    .computeIfAbsent(stm.getRepresentativeTypeMember(), k -> new ArrayList<>())
                    .add(stm);
        }
        List<Group<SortableTypeMember>> groupList = groupsByRep.values().stream()
                .filter(g -> g.size() > ONE)
                .map(g -> new Group<>(List.copyOf(g)))
                .toList();
        Groups<SortableTypeMember> groups = groupList.isEmpty() ? Groups.empty() : new Groups<>(List.copyOf(groupList));

        // Build Dependencies from each member's ordering dependents.
        List<Dependencies.Dependency<SortableTypeMember>> edges = new ArrayList<>();
        for (SortableTypeMember stm : sortableTypeMembers) {
            for (CtTypeMember dep : stm.getOrderingDependentsInGroup()) {
                SortableTypeMember depStm = ctToSortable.get(dep);
                if (depStm != null) {
                    edges.add(new Dependencies.Dependency<>(stm, depStm));
                }
            }
        }
        Dependencies<SortableTypeMember> dependencies =
                edges.isEmpty() ? Dependencies.empty() : new Dependencies<>(List.copyOf(edges));

        // Build comparator on SortableTypeMember via OrderingKey.
        Comparator<SortableTypeMember> comparator =
                Comparator.comparing(SortableTypeMember::getOrderingKey, orderingKeyComparator);

        return SimplifiedDependencyAwareSorter.sort(sortableTypeMembers, groups, dependencies, comparator);
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
}

package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.ComparatorUtils.buildTypeMemberBaseComparator;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyEdgeKind;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
        Comparator<SortableTypeMember> groupComparator = ComparatorUtils.buildGroupComparator(orderingKeyComparator);

        return sortableTypeMembers.stream()
                .sorted(groupComparator)
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
                        .collect(Collectors.collectingAndThen(
                                Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet));

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
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet));
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

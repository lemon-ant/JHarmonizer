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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

@UtilityClass
class GroupMembersOrderer {

    private static final Set<MemberDependencyEdgeKind> DECLARATION_DEPENDENCY_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

    private static final Set<MemberDependencyEdgeKind> ACCESSOR_BUNDLE_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE);
    private static final int ONE = 1;

    @NonNull
    static List<@NonNull MemberGroupBlock> orderMembersInsideGroups(
            @NonNull List<@NonNull MemberGroupBlock> unorderedMemberGroupBlocks,
            @NonNull MemberDependencyGraph memberDependencyGraph) {
        return unorderedMemberGroupBlocks.stream()
                .map(memberGroupBlock -> orderMembersInsideGroup(memberGroupBlock, memberDependencyGraph))
                .toList();
    }

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
            List<@NonNull CtTypeMember> groupMembers,
            MemberDependencyGraph memberDependencyGraph) {
        if (groupMembers.size() <= ONE) {
            return List.copyOf(groupMembers);
        }

        boolean keepAccessorsTogether = compiledMemberGroup.isKeepAccessorsTogether();
        List<OrderingRule> orderingRules = compiledMemberGroup.getOrderingRules();
        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMembers);

        Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator =
                ComparatorUtils.buildOrderingComparator(orderingRules);

        Function<CtTypeMember, SortableTypeMember.OrderingKey> orderingKeyProvider =
                SortableTypeMember.getOrderingKeyProvider();

        Comparator<CtTypeMember> typeMemberBaseComparator =
                buildTypeMemberBaseComparator(orderingKeyProvider, orderingKeyComparator);

        Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember = keepAccessorsTogether
                ? buildAccessorBundleMembersByMember(groupMemberSet, memberDependencyGraph, typeMemberBaseComparator)
                : Map.of();

        Function<CtTypeMember, SortableTypeMember> sortableTypeMemberResolver = buildSortableTypeMemberResolver(
                groupMemberSet,
                memberDependencyGraph,
                keepAccessorsTogether,
                accessorBundleMembersByMember,
                orderingKeyProvider,
                typeMemberBaseComparator);

        List<SortableTypeMember> sortableTypeMembers =
                groupMembers.stream().map(sortableTypeMemberResolver).toList();

        Comparator<SortableTypeMember> groupComparator = ComparatorUtils.buildGroupComparator(typeMemberBaseComparator);

        return sortableTypeMembers.stream()
                .sorted(groupComparator)
                .map(SortableTypeMember::getTypeMember)
                .toList();
    }

    @NonNull
    private static Function<CtTypeMember, SortableTypeMember> buildSortableTypeMemberResolver(
            Set<CtTypeMember> groupMembers,
            MemberDependencyGraph memberDependencyGraph,
            boolean keepAccessorsTogether,
            Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember,
            Function<CtTypeMember, SortableTypeMember.OrderingKey> orderingKeyProvider,
            Comparator<CtTypeMember> typeMemberBaseComparator) {
        // Resolver cache is confined to one orderMembersInsideGroup invocation and accessed sequentially.
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, SortableTypeMember> sortableTypeMembersByMember = new HashMap<>();
        return typeMember -> resolveSortableTypeMember(
                typeMember,
                groupMembers,
                memberDependencyGraph,
                keepAccessorsTogether,
                accessorBundleMembersByMember,
                orderingKeyProvider,
                typeMemberBaseComparator,
                sortableTypeMembersByMember);
    }

    @NonNull
    private static SortableTypeMember resolveSortableTypeMember(
            CtTypeMember typeMember,
            Set<CtTypeMember> groupMembers,
            MemberDependencyGraph memberDependencyGraph,
            boolean keepAccessorsTogether,
            Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember,
            Function<CtTypeMember, SortableTypeMember.OrderingKey> orderingKeyProvider,
            Comparator<CtTypeMember> typeMemberBaseComparator,
            Map<CtTypeMember, SortableTypeMember> sortableTypeMembersByMember) {
        SortableTypeMember cachedSortableTypeMember = sortableTypeMembersByMember.get(typeMember);
        if (cachedSortableTypeMember != null) {
            return cachedSortableTypeMember;
        }

        SortableTypeMember resolvedSortableTypeMember = convertTypeMember2SortableTypeMember(
                typeMember,
                groupMembers,
                memberDependencyGraph,
                keepAccessorsTogether,
                accessorBundleMembersByMember,
                orderingKeyProvider,
                typeMemberBaseComparator,
                sortableTypeMembersByMember);
        sortableTypeMembersByMember.put(typeMember, resolvedSortableTypeMember);
        return resolvedSortableTypeMember;
    }

    @NonNull
    private static SortableTypeMember convertTypeMember2SortableTypeMember(
            CtTypeMember typeMember,
            Set<CtTypeMember> groupMembers,
            MemberDependencyGraph memberDependencyGraph,
            boolean keepAccessorsTogether,
            Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember,
            Function<CtTypeMember, SortableTypeMember.OrderingKey> orderingKeyProvider,
            Comparator<CtTypeMember> typeMemberBaseComparator,
            Map<CtTypeMember, SortableTypeMember> sortableTypeMembersByMember) {

        Set<CtTypeMember> declarationDependentsInGroup =
                memberDependencyGraph.findTransitiveDependents(typeMember, DECLARATION_DEPENDENCY_ONLY).stream()
                        .filter(groupMembers::contains)
                        .collect(Collectors.toUnmodifiableSet());

        if (keepAccessorsTogether) {
            declarationDependentsInGroup =
                    expandDependentsWithAccessorBundles(declarationDependentsInGroup, accessorBundleMembersByMember);
        }

        CtTypeMember representativeTypeMember = resolveRepresentativeTypeMember(
                typeMember,
                declarationDependentsInGroup,
                keepAccessorsTogether,
                accessorBundleMembersByMember,
                typeMemberBaseComparator);

        @SuppressWarnings("PMD.CompareObjectsWithEquals")
        boolean isSelfRepresentative = representativeTypeMember == typeMember;
        if (isSelfRepresentative) {
            return new SortableTypeMember(typeMember, declarationDependentsInGroup, orderingKeyProvider);
        }

        SortableTypeMember representativeSortableTypeMember = resolveSortableTypeMember(
                representativeTypeMember,
                groupMembers,
                memberDependencyGraph,
                keepAccessorsTogether,
                accessorBundleMembersByMember,
                orderingKeyProvider,
                typeMemberBaseComparator,
                sortableTypeMembersByMember);

        return new SortableTypeMember(
                typeMember, representativeSortableTypeMember, declarationDependentsInGroup, orderingKeyProvider);
    }

    @NonNull
    private static CtTypeMember resolveRepresentativeTypeMember(
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
            return resolveAccessorBundleRepresentative(typeMember, accessorBundleMembersByMember);
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

    @NonNull
    private static Map<CtTypeMember, List<CtTypeMember>> buildAccessorBundleMembersByMember(
            Set<@NonNull CtTypeMember> groupMembers,
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
    private static CtTypeMember resolveAccessorBundleRepresentative(
            CtTypeMember typeMember, Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember) {
        List<CtTypeMember> sortedBundleMembersInGroup = accessorBundleMembersByMember.get(typeMember);
        return sortedBundleMembersInGroup == null ? typeMember : sortedBundleMembersInGroup.getFirst();
    }
}

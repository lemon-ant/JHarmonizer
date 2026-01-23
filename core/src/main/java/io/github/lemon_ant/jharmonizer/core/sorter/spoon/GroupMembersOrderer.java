package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.ComparatorUtils.buildTypeMemberComparator;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.SortKey;
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
            @NonNull CompiledMemberGroup compiledMemberGroup,
            @NonNull List<@NonNull CtTypeMember> groupMembers,
            @NonNull MemberDependencyGraph memberDependencyGraph) {

        if (groupMembers.size() <= ONE) {
            return List.copyOf(groupMembers);
        }

        boolean keepAccessorsTogether = compiledMemberGroup.isKeepAccessorsTogether();
        List<SortKey> sortKeys = compiledMemberGroup.getSortKeys();
        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMembers);

        Comparator<SortableTypeMember.SortKeyValues> sortKeyValuesComparator =
                ComparatorUtils.buildSortKeyValuesComparator(sortKeys);

        Function<CtTypeMember, SortableTypeMember.SortKeyValues> sortKeyValuesProvider =
                SortableTypeMember.getSortKeyValuesProvider();

        Comparator<CtTypeMember> typeMemberBaseComparator =
                buildTypeMemberComparator(sortKeyValuesProvider, sortKeyValuesComparator);

        Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember = keepAccessorsTogether
                ? buildAccessorBundleMembersByMember(groupMemberSet, memberDependencyGraph, typeMemberBaseComparator)
                : Map.of();

        List<SortableTypeMember> sortableTypeMembers = groupMembers.stream()
                .map(typeMember -> convertTypeMember2SortableTypeMember(
                        typeMember,
                        groupMemberSet,
                        memberDependencyGraph,
                        keepAccessorsTogether,
                        accessorBundleMembersByMember,
                        sortKeyValuesProvider,
                        typeMemberBaseComparator))
                .toList();

        Comparator<SortableTypeMember> sortableBaseComparator =
                Comparator.comparing(SortableTypeMember::getSortKeyValues, sortKeyValuesComparator);

        Comparator<SortableTypeMember> groupComparator =
                ComparatorUtils.buildGroupComparator(sortableBaseComparator, typeMemberBaseComparator);

        return sortableTypeMembers.stream()
                .sorted(groupComparator)
                .map(SortableTypeMember::getTypeMember)
                .toList();
    }

    @NonNull
    private static SortableTypeMember convertTypeMember2SortableTypeMember(
            @NonNull CtTypeMember typeMember,
            @NonNull Set<CtTypeMember> groupMembers,
            @NonNull MemberDependencyGraph memberDependencyGraph,
            boolean keepAccessorsTogether,
            @NonNull Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember,
            @NonNull Function<CtTypeMember, SortableTypeMember.SortKeyValues> sortKeyValuesProvider,
            @NonNull Comparator<CtTypeMember> typeMemberBaseComparator) {

        Set<CtTypeMember> declarationDependentsInGroup =
                memberDependencyGraph.findTransitiveDependents(typeMember, DECLARATION_DEPENDENCY_ONLY).stream()
                        .filter(groupMembers::contains)
                        .collect(Collectors.toUnmodifiableSet());

        if (keepAccessorsTogether) {
            declarationDependentsInGroup =
                    expandDependentsWithAccessorBundles(declarationDependentsInGroup, accessorBundleMembersByMember);
        }

        CtTypeMember representativeTypeMember;
        if (!declarationDependentsInGroup.isEmpty()) {
            representativeTypeMember = Stream.concat(Stream.of(typeMember), declarationDependentsInGroup.stream())
                    .min(typeMemberBaseComparator)
                    .orElseThrow();
        } else if (keepAccessorsTogether) {
            representativeTypeMember = resolveAccessorBundleRepresentative(typeMember, accessorBundleMembersByMember);
        } else {
            representativeTypeMember = typeMember;
        }

        return new SortableTypeMember(
                typeMember, representativeTypeMember, declarationDependentsInGroup, sortKeyValuesProvider);
    }

    @NonNull
    private static Set<@NonNull CtTypeMember> expandDependentsWithAccessorBundles(
            @NonNull Set<CtTypeMember> declarationDependentsInGroup,
            @NonNull Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember) {
        return declarationDependentsInGroup.stream()
                .flatMap(dependentMember -> Optional.ofNullable(accessorBundleMembersByMember.get(dependentMember))
                        .map(List::stream)
                        .orElseGet(() -> Stream.of(dependentMember)))
                .collect(Collectors.toUnmodifiableSet());
    }

    @NonNull
    private static Map<CtTypeMember, List<CtTypeMember>> buildAccessorBundleMembersByMember(
            @NonNull Set<@NonNull CtTypeMember> groupMembers,
            @NonNull MemberDependencyGraph memberDependencyGraph,
            @NonNull Comparator<CtTypeMember> typeMemberBaseComparator) {

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
            @NonNull CtTypeMember typeMember,
            @NonNull Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember) {

        List<CtTypeMember> sortedBundleMembersInGroup = accessorBundleMembersByMember.get(typeMember);
        return sortedBundleMembersInGroup == null ? typeMember : sortedBundleMembersInGroup.getFirst();
    }
}

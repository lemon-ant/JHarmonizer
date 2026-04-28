// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SortableTypeMember.OrderingKey.AccessorClusterOrderingKey;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Orders type members inside each {@link MemberGroupBlock} according to the group's configured ordering rules,
 * respecting declaration dependencies and optional accessor super-clustering.
 *
 * <p>The accessor super-cluster is mapped to {@link Groups} and declaration dependency edges from the
 * {@link MemberDependencyGraph} are mapped to {@link Dependencies}; the actual constrained sort is
 * then delegated to {@link SimplifiedDependencyAwareSorter}.</p>
 */
@UtilityClass
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.TooManyMethods"})
class GroupMembersOrderer {

    private static final Set<MemberDependencyEdgeKind> DECLARATION_DEPENDENCY_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

    private static final int ONE = 1;

    /**
     * Orders the members inside each group block according to the group's ordering rules.
     *
     * @param unorderedMemberGroupBlocks the unordered member group blocks
     * @param memberDependencyGraph the member dependency graph
     * @return the resulting list with members ordered inside each block
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
        Comparator<SortableTypeMember.OrderingKey> memberOrderingKeyComparator =
                ComparatorUtils.buildOrderingComparator(orderingRules);

        boolean keepAccessorsTogether = compiledMemberGroup.isKeepAccessorsTogether();
        List<SortableTypeMember> sortableTypeMembers =
                createSortableTypeMembers(groupMembers, keepAccessorsTogether, memberOrderingKeyComparator);
        Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator = keepAccessorsTogether
                ? ComparatorUtils.buildAccessorClusterOrderingComparator(orderingRules)
                : memberOrderingKeyComparator;

        Map<CtTypeMember, SortableTypeMember> typeMemberToSortable = buildTypeMemberToSortableMap(sortableTypeMembers);
        Comparator<SortableTypeMember> comparator =
                Comparator.comparing(SortableTypeMember::getOrderingKey, orderingKeyComparator);
        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMembers);
        Groups<SortableTypeMember> groups = compiledMemberGroup.isKeepAccessorsTogether()
                ? buildAccessorSuperClusterGroup(sortableTypeMembers)
                : Groups.empty();

        // Collect accessor super-cluster members so declaration-dependency edges involving them are skipped.
        // SimplifiedDependencyAwareSorter requires groups and dependencies to be mutually exclusive.
        Set<CtTypeMember> bundledMembers = groups.getGroups().stream()
                .flatMap(group -> group.getItems().stream())
                .map(SortableTypeMember::getTypeMember)
                .collect(Collectors.toUnmodifiableSet());

        Dependencies<SortableTypeMember> dependencies = buildDeclarationDependencies(
                groupMemberSet, bundledMembers, memberDependencyGraph, typeMemberToSortable);

        return SimplifiedDependencyAwareSorter.sort(sortableTypeMembers, groups, dependencies, comparator).stream()
                .map(SortableTypeMember::getTypeMember)
                .toList();
    }

    @NonNull
    private static List<SortableTypeMember> createSortableTypeMembers(
            List<CtTypeMember> groupMembers,
            boolean keepAccessorsTogether,
            Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {
        List<SortableTypeMember> sortableTypeMembers = groupMembers.stream()
                .map(member -> new SortableTypeMember(member, SortableTypeMember.OrderingKey.derive(member)))
                .toList();
        if (!keepAccessorsTogether) {
            return sortableTypeMembers;
        }

        Map<SortableTypeMember, String> sortableTypeMember2PropertyName =
                buildSortableTypeMember2PropertyNameMap(sortableTypeMembers);
        Map<String, SortableTypeMember.OrderingKey> propertyName2Representative =
                resolveAccessorClusterRepresentatives(sortableTypeMember2PropertyName, orderingKeyComparator);
        if (propertyName2Representative.isEmpty()) {
            return sortableTypeMembers;
        }
        SortableTypeMember.OrderingKey accessorSuperClusterRepresentative =
                resolveAccessorSuperClusterRepresentative(sortableTypeMember2PropertyName, orderingKeyComparator);
        return sortableTypeMembers.stream()
                .map(sortableTypeMember -> resolveAccessorClusterRepresentative(
                        sortableTypeMember,
                        sortableTypeMember2PropertyName,
                        propertyName2Representative,
                        accessorSuperClusterRepresentative))
                .toList();
    }

    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<SortableTypeMember, String> buildSortableTypeMember2PropertyNameMap(
            List<SortableTypeMember> sortableTypeMembers) {
        Map<SortableTypeMember, String> sortableTypeMember2PropertyName = new HashMap<>();
        sortableTypeMembers.forEach(sortableTypeMember -> SortableTypeMember.findAccessorPropertyName(
                        sortableTypeMember.getTypeMember())
                .ifPresent(propertyName -> sortableTypeMember2PropertyName.put(sortableTypeMember, propertyName)));
        return Collections.unmodifiableMap(sortableTypeMember2PropertyName);
    }

    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<String, SortableTypeMember.OrderingKey> resolveAccessorClusterRepresentatives(
            Map<SortableTypeMember, String> sortableTypeMember2PropertyName,
            Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {
        Map<String, List<SortableTypeMember.OrderingKey>> propertyName2OrderingKeys = new HashMap<>();
        sortableTypeMember2PropertyName.forEach((sortableTypeMember, propertyName) -> propertyName2OrderingKeys
                .computeIfAbsent(propertyName, key -> new ArrayList<>())
                .add(sortableTypeMember.getOrderingKey()));

        Map<String, SortableTypeMember.OrderingKey> propertyName2Representative =
                new HashMap<>(propertyName2OrderingKeys.size() * 2);
        propertyName2OrderingKeys.forEach((propertyName, orderingKeys) -> propertyName2Representative.put(
                propertyName,
                orderingKeys.stream()
                        .min(orderingKeyComparator)
                        .orElseThrow(() -> new IllegalStateException(
                                "Accessor property cluster should contain at least one member: " + propertyName))));
        return Collections.unmodifiableMap(propertyName2Representative);
    }

    @NonNull
    private static SortableTypeMember.OrderingKey resolveAccessorSuperClusterRepresentative(
            Map<SortableTypeMember, String> sortableTypeMember2PropertyName,
            Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator) {
        return sortableTypeMember2PropertyName.keySet().stream()
                .map(SortableTypeMember::getOrderingKey)
                .min(orderingKeyComparator)
                .orElseThrow(
                        () -> new IllegalStateException("Accessor super-cluster should contain at least one member"));
    }

    @NonNull
    private static SortableTypeMember resolveAccessorClusterRepresentative(
            SortableTypeMember sortableTypeMember,
            Map<SortableTypeMember, String> sortableTypeMember2PropertyName,
            Map<String, SortableTypeMember.OrderingKey> propertyName2Representative,
            SortableTypeMember.OrderingKey accessorSuperClusterRepresentative) {
        SortableTypeMember.OrderingKey orderingKey = sortableTypeMember.getOrderingKey();
        String propertyName = sortableTypeMember2PropertyName.get(sortableTypeMember);
        if (propertyName == null) {
            return sortableTypeMember;
        }
        SortableTypeMember.OrderingKey representativeOrderingKey = propertyName2Representative.get(propertyName);
        return new SortableTypeMember(
                sortableTypeMember.getTypeMember(),
                orderingKey.resolveWithAccessorClusterRepresentative(
                        representativeOrderingKey, accessorSuperClusterRepresentative, propertyName));
    }

    @NonNull
    private static Groups<SortableTypeMember> buildAccessorSuperClusterGroup(
            List<SortableTypeMember> sortableTypeMembers) {
        List<SortableTypeMember> accessorMembers = sortableTypeMembers.stream()
                .filter(sortableTypeMember -> sortableTypeMember.getOrderingKey() instanceof AccessorClusterOrderingKey)
                .toList();
        return accessorMembers.isEmpty() ? Groups.empty() : new Groups<>(List.of(new Group<>(accessorMembers)));
    }

    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<CtTypeMember, SortableTypeMember> buildTypeMemberToSortableMap(
            List<SortableTypeMember> sortableTypeMembers) {
        // Capacity * 2 ensures no resize at the default 0.75 load factor (threshold = capacity * 0.75).
        Map<CtTypeMember, SortableTypeMember> map = new HashMap<>(sortableTypeMembers.size() * 2);
        sortableTypeMembers.forEach(sortable -> map.put(sortable.getTypeMember(), sortable));
        return Collections.unmodifiableMap(map);
    }

    @NonNull
    private static Dependencies<SortableTypeMember> buildDeclarationDependencies(
            Set<CtTypeMember> groupMemberSet,
            Set<CtTypeMember> bundledMembers,
            MemberDependencyGraph memberDependencyGraph,
            Map<CtTypeMember, SortableTypeMember> typeMemberToSortable) {
        List<Dependencies.Dependency<SortableTypeMember>> edges = new ArrayList<>();

        for (CtTypeMember provider : groupMemberSet) {
            if (bundledMembers.contains(provider)) {
                continue;
            }
            for (CtTypeMember dependent :
                    memberDependencyGraph.findDirectDependents(provider, DECLARATION_DEPENDENCY_ONLY)) {
                if (!groupMemberSet.contains(dependent) || bundledMembers.contains(dependent)) {
                    continue;
                }
                edges.add(new Dependencies.Dependency<>(
                        typeMemberToSortable.get(provider), typeMemberToSortable.get(dependent)));
            }
        }

        return edges.isEmpty() ? Dependencies.empty() : new Dependencies<>(List.copyOf(edges));
    }
}

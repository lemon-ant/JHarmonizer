// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyEdgeKind;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.SpoonJavaBeansAccessorUtils;
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
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Orders type members inside each {@link MemberGroupBlock} according to the group's configured ordering rules,
 * respecting declaration dependencies and optional accessor-pair bundling.
 *
 * <p>Accessor-pair bundles are mapped to {@link Groups} and declaration dependency edges from the
 * {@link MemberDependencyGraph} are mapped to {@link Dependencies}; the actual constrained sort is
 * then delegated to {@link SimplifiedDependencyAwareSorter}.</p>
 */
@UtilityClass
class GroupMembersOrderer {

    private static final Set<MemberDependencyEdgeKind> DECLARATION_DEPENDENCY_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

    private static final int ONE = 1;

    /**
     * Shared accessor super-cluster threshold; kept in sync with representative-key derivation by
     * centralizing the value in {@link OrderingKeyFactory}.
     */
    private static final int MIN_ACCESSORS_FOR_SUPER_CLUSTER =
            OrderingKeyFactory.MIN_ACCESSORS_FOR_SUPER_CLUSTER;

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
        boolean keepAccessorsTogether = compiledMemberGroup.isKeepAccessorsTogether();
        List<SortableTypeMember> sortableTypeMembers =
                OrderingKeyFactory.createSortableMembers(groupMembers, keepAccessorsTogether, orderingRules);

        Map<CtTypeMember, SortableTypeMember> typeMemberToSortable = buildTypeMemberToSortableMap(sortableTypeMembers);
        Comparator<SortableTypeMember> comparator = ComparatorUtils.buildSortableTypeMemberComparator(orderingRules);
        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMembers);
        Groups<SortableTypeMember> accessorSuperCluster =
                keepAccessorsTogether ? buildAccessorSuperCluster(sortableTypeMembers) : Groups.empty();

        // Collect accessor-bundle members so declaration-dependency edges involving them are skipped.
        // SimplifiedDependencyAwareSorter requires groups and dependencies to be mutually exclusive.
        Set<CtTypeMember> bundledMembers = accessorSuperCluster.getGroups().stream()
                .flatMap(group -> group.getItems().stream())
                .map(SortableTypeMember::getTypeMember)
                .collect(Collectors.toUnmodifiableSet());

        Dependencies<SortableTypeMember> dependencies = buildDeclarationDependencies(
                groupMemberSet, bundledMembers, memberDependencyGraph, typeMemberToSortable);

        return SimplifiedDependencyAwareSorter.sort(sortableTypeMembers, accessorSuperCluster, dependencies, comparator)
                .stream()
                .map(SortableTypeMember::getTypeMember)
                .toList();
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

    /**
     * Bundles every recognized JavaBeans accessor of the group into one indivisible accessor
     * super-cluster {@link Group}. This guarantees that the accessor super-cluster is treated as
     * a single super-node by {@link SimplifiedDependencyAwareSorter}, so non-accessor methods can
     * never be interleaved between two property clusters of the accessor super-cluster — they are
     * placed either entirely above or entirely below the accessor super-cluster, as decided by the
     * cluster-aware comparator on the super-cluster's representative member.
     *
     * <p>Per-property cluster ordering (and the choice of cluster representative inside the
     * accessor super-cluster) is purely a comparator concern; see
     * {@link OrderingKeyFactory#createSortableMembers(List, boolean, List)} and
     * {@link ComparatorUtils#buildSortableTypeMemberComparator(List)}.
     *
     * @param sortableTypeMembers all sortable members of the group
     * @return a single-{@link Group} {@link Groups} bundling every accessor; {@link Groups#empty()}
     *     when fewer than two accessors are present (a single accessor needs no bundling because
     *     the cross-cluster comparator path is never triggered without a second accessor cluster)
     */
    @NonNull
    private static Groups<SortableTypeMember> buildAccessorSuperCluster(List<SortableTypeMember> sortableTypeMembers) {
        List<SortableTypeMember> accessors = sortableTypeMembers.stream()
                .filter(sortable -> isAccessor(sortable.getTypeMember()))
                .toList();
        if (accessors.size() < MIN_ACCESSORS_FOR_SUPER_CLUSTER) {
            return Groups.empty();
        }
        return new Groups<>(List.of(new Group<>(accessors)));
    }

    private static boolean isAccessor(CtTypeMember typeMember) {
        return typeMember instanceof CtMethod<?> method
                && SpoonJavaBeansAccessorUtils.findAccessorPropertyName(method).isPresent();
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

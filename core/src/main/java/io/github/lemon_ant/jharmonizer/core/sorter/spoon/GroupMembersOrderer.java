// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.SortableTypeMember.OrderingKey;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;
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
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.TooManyMethods"})
class GroupMembersOrderer {

    private static final Set<MemberDependencyEdgeKind> DECLARATION_DEPENDENCY_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

    private static final Set<MemberDependencyEdgeKind> ACCESSOR_BUNDLE_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE);

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
        Comparator<SortableTypeMember.OrderingKey> orderingKeyComparator =
                ComparatorUtils.buildOrderingComparator(orderingRules);

        boolean keepAccessorsTogether = compiledMemberGroup.isKeepAccessorsTogether();
        List<SortableTypeMember> sortableTypeMembers = createSortableTypeMembers(groupMembers, keepAccessorsTogether);

        Map<CtTypeMember, SortableTypeMember> typeMemberToSortable = buildTypeMemberToSortableMap(sortableTypeMembers);
        Comparator<SortableTypeMember> comparator =
                Comparator.comparing(SortableTypeMember::getOrderingKey, orderingKeyComparator);
        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMembers);
        Groups<SortableTypeMember> groups = compiledMemberGroup.isKeepAccessorsTogether()
                ? buildAccessorBundleGroups(groupMemberSet, memberDependencyGraph, typeMemberToSortable)
                : Groups.empty();

        // Collect accessor-bundle members so declaration-dependency edges involving them are skipped.
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
            List<CtTypeMember> groupMembers, boolean keepAccessorsTogether) {
        List<SortableTypeMember> sortableTypeMembers = groupMembers.stream()
                .map(member -> new SortableTypeMember(
                        member, SortableTypeMember.OrderingKey.derive(member, keepAccessorsTogether)))
                .toList();
        if (!keepAccessorsTogether) {
            return sortableTypeMembers;
        }

        Map<String, String> propertyName2AlphaClusterKey = resolveAccessorAlphaClusterKeys(sortableTypeMembers);
        if (propertyName2AlphaClusterKey.isEmpty()) {
            return sortableTypeMembers;
        }
        return sortableTypeMembers.stream()
                .map(sortableTypeMember ->
                        resolveAccessorSuperCluster(sortableTypeMember, propertyName2AlphaClusterKey))
                .toList();
    }

    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<String, String> resolveAccessorAlphaClusterKeys(List<SortableTypeMember> sortableTypeMembers) {
        List<AccessorPropertyCluster> propertyClusters = collectAccessorPropertyClusters(sortableTypeMembers);
        if (propertyClusters.isEmpty()) {
            return Map.of();
        }
        List<AccessorSuperCluster> superClusters = collectAccessorSuperClusters(propertyClusters);
        // One map entry is still needed for each accessor property even when several properties merge
        // into the same super-cluster.
        Map<String, String> propertyName2AlphaClusterKey = new HashMap<>(propertyClusters.size() * 2);
        superClusters.forEach(superCluster -> superCluster
                .getPropertyNames()
                .forEach(
                        propertyName -> propertyName2AlphaClusterKey.put(propertyName, superCluster.getMinAlphaKey())));
        return Collections.unmodifiableMap(propertyName2AlphaClusterKey);
    }

    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static List<AccessorPropertyCluster> collectAccessorPropertyClusters(
            List<SortableTypeMember> sortableTypeMembers) {
        Map<String, List<OrderingKey>> propertyName2OrderingKeys = new TreeMap<>();
        sortableTypeMembers.stream()
                .map(SortableTypeMember::getOrderingKey)
                .filter(orderingKey -> orderingKey.getClusterPropertyName() != null)
                .forEach(orderingKey -> propertyName2OrderingKeys
                        .computeIfAbsent(orderingKey.getClusterPropertyName(), propertyName -> new ArrayList<>())
                        .add(orderingKey));

        return propertyName2OrderingKeys.entrySet().stream()
                .map(entry -> new AccessorPropertyCluster(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(OrderingKey::getAlphaKey)
                                .min(Comparator.naturalOrder())
                                .orElseThrow(),
                        entry.getValue().stream()
                                .map(OrderingKey::getAlphaKey)
                                .max(Comparator.naturalOrder())
                                .orElseThrow()))
                .toList();
    }

    @NonNull
    private static List<AccessorSuperCluster> collectAccessorSuperClusters(
            List<AccessorPropertyCluster> propertyClusters) {
        List<AccessorSuperCluster> superClusters = new ArrayList<>();
        propertyClusters.stream()
                .map(AccessorSuperCluster::fromPropertyCluster)
                .forEach(superCluster -> addAccessorSuperCluster(superClusters, superCluster));
        return List.copyOf(superClusters);
    }

    private static void addAccessorSuperCluster(
            List<AccessorSuperCluster> superClusters, AccessorSuperCluster superCluster) {
        AccessorSuperCluster mergedSuperCluster = superCluster;
        while (!superClusters.isEmpty()) {
            AccessorSuperCluster previousSuperCluster = superClusters.getLast();
            if (previousSuperCluster.getMaxAlphaKey().compareTo(mergedSuperCluster.getMinAlphaKey()) <= 0) {
                break;
            }
            superClusters.removeLast();
            mergedSuperCluster = previousSuperCluster.merge(mergedSuperCluster);
        }
        superClusters.add(mergedSuperCluster);
    }

    @NonNull
    private static SortableTypeMember resolveAccessorSuperCluster(
            SortableTypeMember sortableTypeMember, Map<String, String> propertyName2AlphaClusterKey) {
        SortableTypeMember.OrderingKey orderingKey = sortableTypeMember.getOrderingKey();
        String propertyName = orderingKey.getClusterPropertyName();
        if (propertyName == null) {
            return sortableTypeMember;
        }
        String alphaClusterKey = propertyName2AlphaClusterKey.get(propertyName);
        return new SortableTypeMember(
                sortableTypeMember.getTypeMember(), orderingKey.resolveWithAlphaClusterKey(alphaClusterKey));
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
     * Builds the accessor-bundle {@link Groups} by walking the accessor-bundle edges of the
     * dependency graph. Cluster property names are already embedded in the {@link OrderingKey} of
     * each {@link SortableTypeMember} (computed by
     * {@link SortableTypeMember.OrderingKey#derive(CtTypeMember, boolean)}
     * with {@code keepAccessorsTogether=true}), so this method only constructs the grouping
     * structure.
     *
     * @param groupMemberSet set view of {@code groupMembers} for fast membership checks
     * @param memberDependencyGraph the dependency graph
     * @param typeMemberToSortable the fully initialized sortable map
     * @return the {@link Groups} built from the bundle traversal; empty when no bundles were found
     */
    @NonNull
    private static Groups<SortableTypeMember> buildAccessorBundleGroups(
            Set<CtTypeMember> groupMemberSet,
            MemberDependencyGraph memberDependencyGraph,
            Map<CtTypeMember, SortableTypeMember> typeMemberToSortable) {
        Set<CtTypeMember> alreadyGrouped = new HashSet<>();
        List<Group<SortableTypeMember>> bundles = new ArrayList<>();

        for (CtTypeMember member : groupMemberSet) {
            if (alreadyGrouped.contains(member)) {
                continue;
            }
            Set<CtTypeMember> bundleDependents =
                    memberDependencyGraph.findDirectDependents(member, ACCESSOR_BUNDLE_ONLY).stream()
                            .filter(groupMemberSet::contains)
                            .collect(Collectors.toUnmodifiableSet());
            if (bundleDependents.isEmpty()) {
                continue;
            }
            alreadyGrouped.add(member);
            alreadyGrouped.addAll(bundleDependents);
            List<SortableTypeMember> bundleMembers = Stream.concat(Stream.of(member), bundleDependents.stream())
                    .map(typeMemberToSortable::get)
                    .toList();
            bundles.add(new Group<>(bundleMembers));
        }

        return bundles.isEmpty() ? Groups.empty() : new Groups<>(List.copyOf(bundles));
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

    @Value
    private static class AccessorPropertyCluster {
        @NonNull
        String propertyName;

        @NonNull
        String minAlphaKey;

        @NonNull
        String maxAlphaKey;
    }

    @Value
    private static class AccessorSuperCluster {
        @NonNull
        List<@NonNull String> propertyNames;

        @NonNull
        String minAlphaKey;

        @NonNull
        String maxAlphaKey;

        @NonNull
        private static AccessorSuperCluster fromPropertyCluster(AccessorPropertyCluster propertyCluster) {
            return new AccessorSuperCluster(
                    List.of(propertyCluster.getPropertyName()),
                    propertyCluster.getMinAlphaKey(),
                    propertyCluster.getMaxAlphaKey());
        }

        @NonNull
        private AccessorSuperCluster merge(AccessorSuperCluster nextSuperCluster) {
            List<String> mergedPropertyNames =
                    new ArrayList<>(propertyNames.size() + nextSuperCluster.propertyNames.size());
            mergedPropertyNames.addAll(propertyNames);
            mergedPropertyNames.addAll(nextSuperCluster.propertyNames);
            return new AccessorSuperCluster(
                    mergedPropertyNames,
                    min(minAlphaKey, nextSuperCluster.minAlphaKey),
                    max(maxAlphaKey, nextSuperCluster.maxAlphaKey));
        }

        @NonNull
        private static String min(String left, String right) {
            return left.compareTo(right) <= 0 ? left : right;
        }

        @NonNull
        private static String max(String left, String right) {
            return left.compareTo(right) >= 0 ? left : right;
        }
    }
}

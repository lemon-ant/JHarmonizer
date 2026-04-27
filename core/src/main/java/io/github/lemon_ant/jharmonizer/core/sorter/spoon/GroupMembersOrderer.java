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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMembers);

        // Resolve cluster property names for all bundle members before building sortable members,
        // so each SortableTypeMember is created fully initialized with its clusterPropertyName.
        Map<CtTypeMember, String> clusterPropertyNames = compiledMemberGroup.isKeepAccessorsTogether()
                ? resolveClusterPropertyNames(groupMembers, groupMemberSet, memberDependencyGraph)
                : Map.of();

        // Build sortable members in a LinkedHashMap, preserving source order.
        // Capacity * 2 ensures no resize at the default 0.75 load factor (threshold = capacity * 0.75).
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, SortableTypeMember> sortableMap = new LinkedHashMap<>(groupMembers.size() * 2);
        groupMembers.forEach(member -> sortableMap.put(
                member,
                new SortableTypeMember(
                        member, SortableTypeMember.OrderingKey.derive(member, clusterPropertyNames.get(member)))));

        Groups<SortableTypeMember> groups = compiledMemberGroup.isKeepAccessorsTogether()
                ? buildAccessorBundleGroups(groupMembers, groupMemberSet, memberDependencyGraph, sortableMap)
                : Groups.empty();

        List<SortableTypeMember> sortableTypeMembers = List.copyOf(sortableMap.values());
        Map<CtTypeMember, SortableTypeMember> typeMemberToSortable = Collections.unmodifiableMap(sortableMap);
        Comparator<SortableTypeMember> comparator =
                Comparator.comparing(SortableTypeMember::getOrderingKey, orderingKeyComparator);

        // Collect accessor-bundle members so declaration-dependency edges involving them are skipped.
        // SimplifiedDependencyAwareSorter requires groups and dependencies to be mutually exclusive.
        Set<CtTypeMember> bundledMembers = groups.getGroups().stream()
                .flatMap(group -> group.getItems().stream())
                .map(SortableTypeMember::getTypeMember)
                .collect(Collectors.toUnmodifiableSet());

        Dependencies<SortableTypeMember> dependencies = buildDeclarationDependencies(
                groupMembers, groupMemberSet, bundledMembers, memberDependencyGraph, typeMemberToSortable);

        return SimplifiedDependencyAwareSorter.sort(sortableTypeMembers, groups, dependencies, comparator).stream()
                .map(SortableTypeMember::getTypeMember)
                .toList();
    }

    /**
     * Walks the accessor-bundle edges of the dependency graph once and resolves the cluster property
     * name for every member that belongs to a bundle. The property name is determined from the anchor
     * method using the full JavaBeans accessor contract (prefix, return type, and parameter count).
     *
     * @param groupMembers list of members in the group (preserves iteration order)
     * @param groupMemberSet set view of {@code groupMembers} for fast membership checks
     * @param memberDependencyGraph the dependency graph
     * @return an unmodifiable map from bundled {@link CtTypeMember} to its cluster property name;
     *         members that are not part of any bundle are absent from the map
     */
    @NonNull
    private static Map<CtTypeMember, String> resolveClusterPropertyNames(
            List<CtTypeMember> groupMembers,
            Set<CtTypeMember> groupMemberSet,
            MemberDependencyGraph memberDependencyGraph) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, String> propertyNames = new HashMap<>();
        Set<CtTypeMember> processed = new HashSet<>();

        for (CtTypeMember member : groupMembers) {
            if (processed.contains(member) || !(member instanceof CtMethod<?> anchorMethod)) {
                continue;
            }
            Set<CtTypeMember> bundleDependents =
                    memberDependencyGraph.findDirectDependents(member, ACCESSOR_BUNDLE_ONLY).stream()
                            .filter(groupMemberSet::contains)
                            .collect(Collectors.toUnmodifiableSet());
            if (bundleDependents.isEmpty()) {
                continue;
            }
            processed.add(member);
            processed.addAll(bundleDependents);

            SpoonJavaBeansAccessorUtils.findAccessorPropertyName(anchorMethod).ifPresent(name -> {
                propertyNames.put(member, name);
                bundleDependents.forEach(dep -> propertyNames.put(dep, name));
            });
        }

        return Collections.unmodifiableMap(propertyNames);
    }

    /**
     * Builds the accessor-bundle {@link Groups} from the pre-annotated sortable map. Relies on
     * {@link #resolveClusterPropertyNames} having already populated the sortable map with the correct
     * cluster property names; this method only constructs the grouping structure.
     *
     * @param groupMembers list of members in the group (preserves iteration order)
     * @param groupMemberSet set view of {@code groupMembers} for fast membership checks
     * @param memberDependencyGraph the dependency graph
     * @param sortableMap the fully initialized sortable map (cluster property names already set)
     * @return the {@link Groups} built from the bundle traversal; empty when no bundles were found
     */
    @NonNull
    private static Groups<SortableTypeMember> buildAccessorBundleGroups(
            List<CtTypeMember> groupMembers,
            Set<CtTypeMember> groupMemberSet,
            MemberDependencyGraph memberDependencyGraph,
            Map<CtTypeMember, SortableTypeMember> sortableMap) {
        Set<CtTypeMember> alreadyGrouped = new HashSet<>();
        List<Group<SortableTypeMember>> bundles = new ArrayList<>();

        for (CtTypeMember member : groupMembers) {
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
                    .map(sortableMap::get)
                    .toList();
            bundles.add(new Group<>(bundleMembers));
        }

        return bundles.isEmpty() ? Groups.empty() : new Groups<>(List.copyOf(bundles));
    }

    @NonNull
    private static Dependencies<SortableTypeMember> buildDeclarationDependencies(
            List<CtTypeMember> groupMembers,
            Set<CtTypeMember> groupMemberSet,
            Set<CtTypeMember> bundledMembers,
            MemberDependencyGraph memberDependencyGraph,
            Map<CtTypeMember, SortableTypeMember> typeMemberToSortable) {
        List<Dependencies.Dependency<SortableTypeMember>> edges = new ArrayList<>();

        for (CtTypeMember provider : groupMembers) {
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

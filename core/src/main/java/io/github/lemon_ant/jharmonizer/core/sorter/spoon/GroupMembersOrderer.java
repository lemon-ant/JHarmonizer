package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

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
import java.util.List;
import java.util.Map;
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

        Function<CtTypeMember, SortableTypeMember.OrderingKey> orderingKeyProvider =
                SortableTypeMember.OrderingKey.getOrderingKeyProvider();
        List<SortableTypeMember> sortableTypeMembers = groupMembers.stream()
                .map(member -> new SortableTypeMember(member, orderingKeyProvider.apply(member)))
                .toList();

        Map<CtTypeMember, SortableTypeMember> typeMemberToSortable = buildTypeMemberToSortableMap(sortableTypeMembers);
        Comparator<SortableTypeMember> comparator =
                Comparator.comparing(SortableTypeMember::getOrderingKey, orderingKeyComparator);
        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMembers);

        Groups<SortableTypeMember> groups = compiledMemberGroup.isKeepAccessorsTogether()
                ? buildAccessorBundleGroups(groupMembers, groupMemberSet, memberDependencyGraph, typeMemberToSortable)
                : Groups.empty();

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

    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<CtTypeMember, SortableTypeMember> buildTypeMemberToSortableMap(
            List<SortableTypeMember> sortableTypeMembers) {
        Map<CtTypeMember, SortableTypeMember> map = new HashMap<>(sortableTypeMembers.size() * 2);
        sortableTypeMembers.forEach(sortable -> map.put(sortable.getTypeMember(), sortable));
        return Collections.unmodifiableMap(map);
    }

    @NonNull
    private static Groups<SortableTypeMember> buildAccessorBundleGroups(
            List<CtTypeMember> groupMembers,
            Set<CtTypeMember> groupMemberSet,
            MemberDependencyGraph memberDependencyGraph,
            Map<CtTypeMember, SortableTypeMember> typeMemberToSortable) {
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
                    .map(typeMemberToSortable::get)
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

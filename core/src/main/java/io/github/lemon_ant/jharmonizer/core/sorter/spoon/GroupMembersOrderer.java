package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.OrderingRule;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyEdgeKind;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import io.github.lemon_ant.jharmonizer.sorting.Dependencies;
import io.github.lemon_ant.jharmonizer.sorting.Group;
import io.github.lemon_ant.jharmonizer.sorting.Groups;
import io.github.lemon_ant.jharmonizer.sorting.SimplifiedDependencyAwareSorter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Orders type members inside each {@link MemberGroupBlock} according to the group's configured ordering rules,
 * respecting declaration dependencies and optional accessor-pair bundling.
 *
 * <p>Both accessor-pair bundles and declaration dependency chains are mapped to
 * {@link Groups} — members connected by accessor-bundle or declaration-dependency
 * edges form an indivisible block, internally sorted by topological order with
 * comparator tie-breaking.  The actual constrained sort is then delegated to
 * {@link SimplifiedDependencyAwareSorter}.</p>
 */
@UtilityClass
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.GodClass", "PMD.TooManyMethods"})
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
                .map(m -> new SortableTypeMember(m, orderingKeyProvider.apply(m)))
                .toList();

        Map<CtTypeMember, SortableTypeMember> typeMemberToSortable = buildTypeMemberToSortableMap(sortableTypeMembers);
        Comparator<SortableTypeMember> comparator =
                Comparator.comparing(SortableTypeMember::getOrderingKey, orderingKeyComparator);
        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMembers);

        Groups<SortableTypeMember> groups = buildConstraintGroups(
                groupMembers,
                groupMemberSet,
                compiledMemberGroup.isKeepAccessorsTogether(),
                memberDependencyGraph,
                typeMemberToSortable,
                comparator);

        return SimplifiedDependencyAwareSorter.sort(sortableTypeMembers, groups, Dependencies.empty(), comparator)
                .stream()
                .map(SortableTypeMember::getTypeMember)
                .toList();
    }

    // ------------------------------------------------------------------ //
    //  Mapping: typeMember -> SortableTypeMember                         //
    // ------------------------------------------------------------------ //

    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<CtTypeMember, SortableTypeMember> buildTypeMemberToSortableMap(
            List<SortableTypeMember> sortableTypeMembers) {
        Map<CtTypeMember, SortableTypeMember> map = new HashMap<>(sortableTypeMembers.size() * 2);
        sortableTypeMembers.forEach(s -> map.put(s.getTypeMember(), s));
        return map;
    }

    // ------------------------------------------------------------------ //
    //  Constraint groups (accessor bundles + declaration dep chains)      //
    // ------------------------------------------------------------------ //

    /**
     * Builds groups from connected components in the combined accessor-bundle +
     * declaration-dependency graph.  Members connected by either edge type form an
     * indivisible block.  Within each block, accessor sub-groups are treated as
     * atomic units and topologically sorted by declaration dependencies, then
     * expanded into members sorted by comparator.
     */
    @NonNull
    @SuppressWarnings("PMD.CognitiveComplexity")
    private static Groups<SortableTypeMember> buildConstraintGroups(
            List<CtTypeMember> groupMembers,
            Set<CtTypeMember> groupMemberSet,
            boolean keepAccessorsTogether,
            MemberDependencyGraph memberDependencyGraph,
            Map<CtTypeMember, SortableTypeMember> typeMemberToSortable,
            Comparator<SortableTypeMember> comparator) {

        // Build undirected adjacency for connected-component discovery
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, Set<CtTypeMember>> adjacency = new HashMap<>(groupMembers.size() * 2);
        for (CtTypeMember member : groupMembers) {
            adjacency.put(member, new HashSet<>());
        }

        addDeclarationDependencyEdges(groupMembers, groupMemberSet, memberDependencyGraph, adjacency);
        if (keepAccessorsTogether) {
            addAccessorBundleEdges(groupMembers, groupMemberSet, memberDependencyGraph, adjacency);
        }

        // Discover connected components via BFS
        Set<CtTypeMember> visited = new HashSet<>();
        List<Group<SortableTypeMember>> groups = new ArrayList<>();

        for (CtTypeMember member : groupMembers) {
            if (visited.contains(member)) {
                continue;
            }

            List<CtTypeMember> component = collectComponent(member, adjacency, visited);
            if (component.size() <= ONE) {
                continue;
            }

            List<SortableTypeMember> sortedComponent = topologicalSortComponent(
                    component,
                    groupMemberSet,
                    keepAccessorsTogether,
                    memberDependencyGraph,
                    typeMemberToSortable,
                    comparator);
            groups.add(new Group<>(sortedComponent));
        }

        return groups.isEmpty() ? Groups.empty() : new Groups<>(List.copyOf(groups));
    }

    // ------------------------------------------------------------------ //
    //  Edge collection                                                    //
    // ------------------------------------------------------------------ //

    private static void addDeclarationDependencyEdges(
            List<CtTypeMember> groupMembers,
            Set<CtTypeMember> groupMemberSet,
            MemberDependencyGraph memberDependencyGraph,
            Map<CtTypeMember, Set<CtTypeMember>> adjacency) {
        for (CtTypeMember provider : groupMembers) {
            for (CtTypeMember dependent :
                    memberDependencyGraph.findTransitiveDependents(provider, DECLARATION_DEPENDENCY_ONLY)) {
                if (groupMemberSet.contains(dependent)) {
                    adjacency.get(provider).add(dependent);
                    adjacency.get(dependent).add(provider);
                }
            }
        }
    }

    private static void addAccessorBundleEdges(
            List<CtTypeMember> groupMembers,
            Set<CtTypeMember> groupMemberSet,
            MemberDependencyGraph memberDependencyGraph,
            Map<CtTypeMember, Set<CtTypeMember>> adjacency) {
        for (CtTypeMember member : groupMembers) {
            for (CtTypeMember bundled : memberDependencyGraph.findDirectDependents(member, ACCESSOR_BUNDLE_ONLY)) {
                if (groupMemberSet.contains(bundled)) {
                    adjacency.get(member).add(bundled);
                    adjacency.get(bundled).add(member);
                }
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  Component discovery (BFS)                                          //
    // ------------------------------------------------------------------ //

    @NonNull
    private static List<CtTypeMember> collectComponent(
            CtTypeMember start, Map<CtTypeMember, Set<CtTypeMember>> adjacency, Set<CtTypeMember> visited) {
        List<CtTypeMember> component = new ArrayList<>();
        Queue<CtTypeMember> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            CtTypeMember current = queue.poll();
            component.add(current);
            for (CtTypeMember neighbor : adjacency.get(current)) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return component;
    }

    // ------------------------------------------------------------------ //
    //  Intra-component topological sort with accessor sub-groups          //
    // ------------------------------------------------------------------ //

    /**
     * Topologically sorts members within a connected component.  Accessor bundle
     * sub-groups are treated as atomic units during topological sorting, then
     * expanded into members sorted by comparator.
     */
    @NonNull
    @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
    private static List<SortableTypeMember> topologicalSortComponent(
            List<CtTypeMember> component,
            Set<CtTypeMember> groupMemberSet,
            boolean keepAccessorsTogether,
            MemberDependencyGraph memberDependencyGraph,
            Map<CtTypeMember, SortableTypeMember> typeMemberToSortable,
            Comparator<SortableTypeMember> comparator) {

        Set<CtTypeMember> componentSet = new HashSet<>(component);

        // Phase 1: identify accessor bundle sub-groups within the component
        Map<CtTypeMember, List<CtTypeMember>> subGroupByRep = buildAccessorSubGroups(
                component, componentSet, groupMemberSet, keepAccessorsTogether, memberDependencyGraph);

        // Map each member to its sub-group representative
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, CtTypeMember> memberToRep = new HashMap<>(component.size() * 2);
        subGroupByRep.forEach((rep, members) -> members.forEach(member -> memberToRep.put(member, rep)));

        // Phase 2: topological sort over sub-group representatives
        Set<CtTypeMember> representatives = subGroupByRep.keySet();

        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, Set<CtTypeMember>> repSuccessors = new HashMap<>(representatives.size() * 2);
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<CtTypeMember, Integer> repInDegree = new HashMap<>(representatives.size() * 2);
        for (CtTypeMember rep : representatives) {
            repSuccessors.put(rep, new HashSet<>());
            repInDegree.put(rep, 0);
        }

        for (CtTypeMember provider : component) {
            CtTypeMember providerRep = memberToRep.get(provider);
            for (CtTypeMember dependent :
                    memberDependencyGraph.findTransitiveDependents(provider, DECLARATION_DEPENDENCY_ONLY)) {
                if (componentSet.contains(dependent) && groupMemberSet.contains(dependent)) {
                    CtTypeMember dependentRep = memberToRep.get(dependent);
                    if (!providerRep.equals(dependentRep)
                            && repSuccessors.get(providerRep).add(dependentRep)) {
                        repInDegree.merge(dependentRep, 1, Integer::sum);
                    }
                }
            }
        }

        // Kahn's algorithm on sub-group representatives
        Comparator<CtTypeMember> repComparator = (repA, repB) -> {
            SortableTypeMember minA = findSubGroupMinimum(subGroupByRep.get(repA), typeMemberToSortable, comparator);
            SortableTypeMember minB = findSubGroupMinimum(subGroupByRep.get(repB), typeMemberToSortable, comparator);
            return comparator.compare(minA, minB);
        };
        PriorityQueue<CtTypeMember> ready = new PriorityQueue<>(repComparator);
        for (CtTypeMember rep : representatives) {
            if (repInDegree.get(rep) == 0) {
                ready.add(rep);
            }
        }

        List<SortableTypeMember> result = new ArrayList<>(component.size());
        while (!ready.isEmpty()) {
            CtTypeMember currentRep = ready.poll();

            subGroupByRep.get(currentRep).stream()
                    .map(typeMemberToSortable::get)
                    .sorted(comparator)
                    .forEach(result::add);

            for (CtTypeMember dependentRep : repSuccessors.get(currentRep)) {
                int newDegree = repInDegree.compute(dependentRep, (k, v) -> v - 1);
                if (newDegree == 0) {
                    ready.add(dependentRep);
                }
            }
        }

        return result;
    }

    @NonNull
    private static SortableTypeMember findSubGroupMinimum(
            List<CtTypeMember> subGroup,
            Map<CtTypeMember, SortableTypeMember> typeMemberToSortable,
            Comparator<SortableTypeMember> comparator) {
        SortableTypeMember minimum = typeMemberToSortable.get(subGroup.getFirst());
        for (int i = 1; i < subGroup.size(); i++) {
            SortableTypeMember candidate = typeMemberToSortable.get(subGroup.get(i));
            if (comparator.compare(candidate, minimum) < 0) {
                minimum = candidate;
            }
        }
        return minimum;
    }

    /**
     * Groups members into accessor bundle sub-groups within a component.
     * Members not in any accessor bundle become singleton sub-groups.
     *
     * @return map from sub-group representative to the list of members
     */
    @NonNull
    @SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.CognitiveComplexity"})
    private static Map<CtTypeMember, List<CtTypeMember>> buildAccessorSubGroups(
            List<CtTypeMember> component,
            Set<CtTypeMember> componentSet,
            Set<CtTypeMember> groupMemberSet,
            boolean keepAccessorsTogether,
            MemberDependencyGraph memberDependencyGraph) {

        Map<CtTypeMember, List<CtTypeMember>> subGroups = new HashMap<>();
        Set<CtTypeMember> assigned = new HashSet<>();

        if (keepAccessorsTogether) {
            Map<CtTypeMember, Set<CtTypeMember>> accAdj = new HashMap<>(component.size() * 2);
            for (CtTypeMember member : component) {
                accAdj.put(member, new HashSet<>());
            }
            for (CtTypeMember member : component) {
                for (CtTypeMember bundled : memberDependencyGraph.findDirectDependents(member, ACCESSOR_BUNDLE_ONLY)) {
                    if (componentSet.contains(bundled) && groupMemberSet.contains(bundled)) {
                        accAdj.get(member).add(bundled);
                        accAdj.get(bundled).add(member);
                    }
                }
            }

            for (CtTypeMember member : component) {
                if (assigned.contains(member) || accAdj.get(member).isEmpty()) {
                    continue;
                }
                List<CtTypeMember> subGroup = new ArrayList<>();
                Queue<CtTypeMember> queue = new ArrayDeque<>();
                queue.add(member);
                assigned.add(member);
                while (!queue.isEmpty()) {
                    CtTypeMember current = queue.poll();
                    subGroup.add(current);
                    for (CtTypeMember neighbor : accAdj.get(current)) {
                        if (assigned.add(neighbor)) {
                            queue.add(neighbor);
                        }
                    }
                }
                subGroups.put(member, subGroup);
            }
        }

        for (CtTypeMember member : component) {
            if (!assigned.contains(member)) {
                subGroups.put(member, List.of(member));
            }
        }

        return subGroups;
    }
}

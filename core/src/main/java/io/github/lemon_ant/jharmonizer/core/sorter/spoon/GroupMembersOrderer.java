package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.SortKey;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyEdgeKind;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

@UtilityClass
class GroupMembersOrderer {

    private static final EnumSet<MemberDependencyEdgeKind> DECLARATION_DEPENDENCY_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

    private static final EnumSet<MemberDependencyEdgeKind> ACCESSOR_BUNDLE_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE);

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

        if (groupMembers.size() <= 1) {
            return List.copyOf(groupMembers);
        }

        boolean keepAccessorsTogether = compiledMemberGroup.isKeepAccessorsTogether();
        List<SortKey> sortKeys = compiledMemberGroup.getSortKeys();
        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMembers);

        Map<CtTypeMember, SortableTypeMember.SortKeyValues> sortKeyValuesByMember = new LinkedHashMap<>();
        Function<CtTypeMember, SortableTypeMember.SortKeyValues> sortKeyValuesProvider = typeMember ->
                sortKeyValuesByMember.computeIfAbsent(typeMember, GroupMembersOrderer::deriveSortKeyValues);

        Comparator<CtTypeMember> typeMemberBaseComparator = buildConfiguredComparator(sortKeys, sortKeyValuesProvider);

        Map<CtTypeMember, CtTypeMember> accessorBundleRepresentativeByMember =
                keepAccessorsTogether ? new LinkedHashMap<>() : Map.of();

        Function<CtTypeMember, CtTypeMember> accessorBundleRepresentativeProvider = typeMember -> keepAccessorsTogether
                ? resolveAccessorBundleRepresentative(
                        typeMember,
                        groupMemberSet,
                        memberDependencyGraph,
                        typeMemberBaseComparator,
                        accessorBundleRepresentativeByMember)
                : typeMember;

        List<SortableTypeMember> sortableTypeMembers = groupMembers.stream()
                .map(typeMember -> convertToSortableTypeMember(
                        typeMember,
                        groupMemberSet,
                        memberDependencyGraph,
                        keepAccessorsTogether,
                        sortKeyValuesProvider,
                        typeMemberBaseComparator,
                        accessorBundleRepresentativeProvider))
                .toList();

        Comparator<SortableTypeMember> sortableBaseComparator =
                buildConfiguredComparator(sortKeys, SortableTypeMember::getSortKeyValues);

        Comparator<SortableTypeMember> groupComparator =
                buildGroupComparator(sortableBaseComparator, typeMemberBaseComparator);

        return sortableTypeMembers.stream()
                .sorted(groupComparator)
                .map(SortableTypeMember::getTypeMember)
                .toList();
    }

    @NonNull
    private static SortableTypeMember convertToSortableTypeMember(
            @NonNull CtTypeMember typeMember,
            @NonNull Set<CtTypeMember> groupMemberSet,
            @NonNull MemberDependencyGraph memberDependencyGraph,
            boolean keepAccessorsTogether,
            @NonNull Function<CtTypeMember, SortableTypeMember.SortKeyValues> sortKeyValuesProvider,
            @NonNull Comparator<CtTypeMember> typeMemberBaseComparator,
            @NonNull Function<CtTypeMember, CtTypeMember> accessorBundleRepresentativeProvider) {

        SortableTypeMember.SortKeyValues sortKeyValues = sortKeyValuesProvider.apply(typeMember);

        Set<CtTypeMember> declarationDependentsInGroup =
                memberDependencyGraph.findTransitiveDependents(typeMember, DECLARATION_DEPENDENCY_ONLY).stream()
                        .filter(groupMemberSet::contains)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<CtTypeMember> orderingDependentsInGroup = keepAccessorsTogether
                ? expandDependentsWithAccessorBundles(
                        declarationDependentsInGroup, groupMemberSet, memberDependencyGraph)
                : Set.copyOf(declarationDependentsInGroup);

        CtTypeMember accessorBundleRepresentative = accessorBundleRepresentativeProvider.apply(typeMember);

        CtTypeMember earliestByBaseComparator = Stream.concat(Stream.of(typeMember), orderingDependentsInGroup.stream())
                .min(typeMemberBaseComparator)
                .orElse(typeMember);

        CtTypeMember representativeTypeMember =
                typeMemberBaseComparator.compare(earliestByBaseComparator, accessorBundleRepresentative) < 0
                        ? earliestByBaseComparator
                        : accessorBundleRepresentative;

        return new SortableTypeMember(
                typeMember, sortKeyValues, representativeTypeMember, Set.copyOf(orderingDependentsInGroup));
    }

    @NonNull
    private static Set<@NonNull CtTypeMember> expandDependentsWithAccessorBundles(
            @NonNull Set<CtTypeMember> declarationDependentsInGroup,
            @NonNull Set<CtTypeMember> groupMemberSet,
            @NonNull MemberDependencyGraph memberDependencyGraph) {

        return declarationDependentsInGroup.stream()
                .flatMap(dependentMember -> Stream.concat(
                        Stream.of(dependentMember),
                        memberDependencyGraph.findTransitiveDependents(dependentMember, ACCESSOR_BUNDLE_ONLY).stream()))
                .filter(groupMemberSet::contains)
                .collect(Collectors.toUnmodifiableSet());
    }

    @NonNull
    private static CtTypeMember resolveAccessorBundleRepresentative(
            @NonNull CtTypeMember typeMember,
            @NonNull Set<CtTypeMember> groupMemberSet,
            @NonNull MemberDependencyGraph memberDependencyGraph,
            @NonNull Comparator<CtTypeMember> typeMemberBaseComparator,
            @NonNull Map<CtTypeMember, CtTypeMember> accessorBundleRepresentativeByMember) {

        CtTypeMember cachedRepresentative = accessorBundleRepresentativeByMember.get(typeMember);
        if (cachedRepresentative != null) {
            return cachedRepresentative;
        }

        Set<CtTypeMember> bundleMembers = new LinkedHashSet<>();
        bundleMembers.add(typeMember);

        // ACCESSOR_BUNDLE edges are expected to be bidirectional.
        // We still use transitive traversal to be robust if the bundle is not a clique.
        bundleMembers.addAll(memberDependencyGraph.findTransitiveDependents(typeMember, ACCESSOR_BUNDLE_ONLY));
        bundleMembers.retainAll(groupMemberSet);

        CtTypeMember bundleRepresentative =
                bundleMembers.stream().min(typeMemberBaseComparator).orElse(typeMember);

        bundleMembers.forEach(
                bundleMember -> accessorBundleRepresentativeByMember.put(bundleMember, bundleRepresentative));
        return bundleRepresentative;
    }

    @NonNull
    private static Comparator<SortableTypeMember> buildGroupComparator(
            @NonNull Comparator<SortableTypeMember> sortableBaseComparator,
            @NonNull Comparator<CtTypeMember> typeMemberBaseComparator) {

        return (leftSortable, rightSortable) -> {
            CtTypeMember leftMember = leftSortable.getTypeMember();
            CtTypeMember rightMember = rightSortable.getTypeMember();

            // Comparator contract: same reference must be equal.
            if (leftMember == rightMember) {
                return 0;
            }

            boolean leftMustBeBeforeRight =
                    leftSortable.getOrderingDependentsInGroup().contains(rightMember);

            boolean rightMustBeBeforeLeft =
                    rightSortable.getOrderingDependentsInGroup().contains(leftMember);

            if (leftMustBeBeforeRight && !rightMustBeBeforeLeft) {
                return -1;
            }
            if (rightMustBeBeforeLeft && !leftMustBeBeforeRight) {
                return 1;
            }

            // Corner case: cycles in declaration dependencies. We must not break antisymmetry.
            // Fall back to deterministic base ordering.
            if (leftMustBeBeforeRight && rightMustBeBeforeLeft) {
                int cycleFallback = sortableBaseComparator.compare(leftSortable, rightSortable);
                if (cycleFallback != 0) {
                    return cycleFallback;
                }
                return Integer.compare(System.identityHashCode(leftMember), System.identityHashCode(rightMember));
            }

            CtTypeMember leftRepresentative = leftSortable.getRepresentativeTypeMember();
            CtTypeMember rightRepresentative = rightSortable.getRepresentativeTypeMember();

            if (leftRepresentative != rightRepresentative) {
                int representativeComparison =
                        typeMemberBaseComparator.compare(leftRepresentative, rightRepresentative);
                if (representativeComparison != 0) {
                    return representativeComparison;
                }
            }

            int directComparison = sortableBaseComparator.compare(leftSortable, rightSortable);
            if (directComparison != 0) {
                return directComparison;
            }

            // Last-resort deterministic tie-breaker (should be very rare).
            return Integer.compare(System.identityHashCode(leftMember), System.identityHashCode(rightMember));
        };
    }

    @NonNull
    private static SortableTypeMember.SortKeyValues deriveSortKeyValues(@NonNull CtTypeMember typeMember) {
        return new SortableTypeMember.SortKeyValues(
                SpoonTypeMemberUtils.extractSourceStart(typeMember),
                SpoonTypeMemberUtils.deriveAlphaKey(typeMember),
                SpoonTypeMemberUtils.deriveVisibilityRankAscending(typeMember),
                SpoonTypeMemberUtils.deriveVisibilityRankDescending(typeMember));
    }

    @NonNull
    private static <TSortKeyProvider> Comparator<TSortKeyProvider> buildConfiguredComparator(
            @NonNull List<SortKey> sortKeys,
            @NonNull Function<TSortKeyProvider, SortableTypeMember.SortKeyValues> sortKeyValuesProvider) {

        Comparator<TSortKeyProvider> configuredComparator = sortKeys.stream()
                .map(sortKey -> buildComparatorForSortKey(sortKey, sortKeyValuesProvider))
                .reduce(Comparator::thenComparing)
                .orElseGet(() -> Comparator.comparingInt((TSortKeyProvider element) ->
                                sortKeyValuesProvider.apply(element).getSourceStart())
                        .thenComparing(
                                element -> sortKeyValuesProvider.apply(element).getAlphaKey()));

        // Deterministic tie-breakers regardless of configured keys.
        if (!sortKeys.contains(SortKey.PRESERVE)) {
            configuredComparator = configuredComparator.thenComparingInt(
                    element -> sortKeyValuesProvider.apply(element).getSourceStart());
        }
        if (!sortKeys.contains(SortKey.ALPHA)) {
            configuredComparator = configuredComparator.thenComparing(
                    element -> sortKeyValuesProvider.apply(element).getAlphaKey());
        }

        return configuredComparator;
    }

    @NonNull
    private static <TSortKeyProvider> Comparator<TSortKeyProvider> buildComparatorForSortKey(
            @NonNull SortKey sortKey, @NonNull Function<TSortKeyProvider, SortableTypeMember.SortKeyValues> sortKeyValuesProvider) {

        return switch (sortKey) {
            case PRESERVE ->
                Comparator.comparingInt(
                        element -> sortKeyValuesProvider.apply(element).getSourceStart());
            case ALPHA ->
                Comparator.comparing(
                        element -> sortKeyValuesProvider.apply(element).getAlphaKey());
            case VISIBILITY_ASC ->
                Comparator.comparingInt(
                        element -> sortKeyValuesProvider.apply(element).getVisibilityRankAscending());
            case VISIBILITY_DESC ->
                Comparator.comparingInt(
                        element -> sortKeyValuesProvider.apply(element).getVisibilityRankDescending());
        };
    }

    @Value
    private static class SortableTypeMember {

        @NonNull
        CtTypeMember typeMember;

        @NonNull
        SortKeyValues sortKeyValues;

        @NonNull
        CtTypeMember representativeTypeMember;

        @NonNull
        Set<@NonNull CtTypeMember> orderingDependentsInGroup;

        @Value
        private static class SortKeyValues {
            int sourceStart;

            @NonNull
            String alphaKey;

            int visibilityRankAscending;
            int visibilityRankDescending;
        }
    }
}

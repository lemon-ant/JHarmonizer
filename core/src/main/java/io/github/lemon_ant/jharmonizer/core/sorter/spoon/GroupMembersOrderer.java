package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.SortKey;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyEdgeKind;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
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

        Comparator<SortableTypeMember.SortKeyValues> sortKeyValuesComparator = buildSortKeyValuesComparator(sortKeys);

        Map<CtTypeMember, SortableTypeMember.SortKeyValues> sortKeyValuesByMember = new HashMap<>();
        Function<CtTypeMember, SortableTypeMember.SortKeyValues> sortKeyValuesProvider = typeMember ->
                sortKeyValuesByMember.computeIfAbsent(typeMember, GroupMembersOrderer::deriveSortKeyValues);

        Comparator<CtTypeMember> typeMemberBaseComparator =
                Comparator.comparing(sortKeyValuesProvider, sortKeyValuesComparator);

        Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember = keepAccessorsTogether
                ? buildAccessorBundleMembersByMember(
                        groupMembers, groupMemberSet, memberDependencyGraph, typeMemberBaseComparator)
                : Map.of();

        List<SortableTypeMember> sortableTypeMembers = groupMembers.stream()
                .map(typeMember -> convertToSortableTypeMember(
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
            @NonNull Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember,
            @NonNull Function<CtTypeMember, SortableTypeMember.SortKeyValues> sortKeyValuesProvider,
            @NonNull Comparator<CtTypeMember> typeMemberBaseComparator) {

        SortableTypeMember.SortKeyValues sortKeyValues = sortKeyValuesProvider.apply(typeMember);

        Set<CtTypeMember> declarationDependentsInGroup =
                memberDependencyGraph.findTransitiveDependents(typeMember, DECLARATION_DEPENDENCY_ONLY).stream()
                        .filter(groupMemberSet::contains)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<CtTypeMember> orderingDependentsInGroup = keepAccessorsTogether
                ? expandDependentsWithAccessorBundles(declarationDependentsInGroup, accessorBundleMembersByMember)
                : Set.copyOf(declarationDependentsInGroup);

        CtTypeMember accessorBundleRepresentative = keepAccessorsTogether
                ? resolveAccessorBundleRepresentative(typeMember, accessorBundleMembersByMember)
                : typeMember;

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
            @NonNull Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember) {

        return declarationDependentsInGroup.stream()
                .flatMap(dependentMember ->
                        resolveAccessorBundleMembersInGroup(dependentMember, accessorBundleMembersByMember).stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @NonNull
    private static Map<CtTypeMember, List<CtTypeMember>> buildAccessorBundleMembersByMember(
            @NonNull List<@NonNull CtTypeMember> groupMembers,
            @NonNull Set<CtTypeMember> groupMemberSet,
            @NonNull MemberDependencyGraph memberDependencyGraph,
            @NonNull Comparator<CtTypeMember> typeMemberBaseComparator) {

        Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember = new HashMap<>();
        Set<CtTypeMember> alreadyIndexedMembers = new HashSet<>();

        for (CtTypeMember groupMember : groupMembers) {
            if (alreadyIndexedMembers.contains(groupMember)) {
                continue;
            }

            Set<CtTypeMember> bundleMembersInGroup = new HashSet<>();
            bundleMembersInGroup.add(groupMember);

            // ACCESSOR_BUNDLE edges are expected to be bidirectional.
            // We still use transitive traversal to be robust if the bundle is not a clique.
            bundleMembersInGroup.addAll(
                    memberDependencyGraph.findTransitiveDependents(groupMember, ACCESSOR_BUNDLE_ONLY));
            bundleMembersInGroup.retainAll(groupMemberSet);

            List<CtTypeMember> sortedBundleMembersInGroup = bundleMembersInGroup.stream()
                    .sorted(typeMemberBaseComparator)
                    .toList();

            bundleMembersInGroup.forEach(bundleMember ->
                    accessorBundleMembersByMember.put(bundleMember, sortedBundleMembersInGroup));

            alreadyIndexedMembers.addAll(bundleMembersInGroup);
        }

        return Map.copyOf(accessorBundleMembersByMember);
    }

    @NonNull
    private static CtTypeMember resolveAccessorBundleRepresentative(
            @NonNull CtTypeMember typeMember,
            @NonNull Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember) {

        List<CtTypeMember> sortedBundleMembersInGroup = accessorBundleMembersByMember.get(typeMember);
        if (sortedBundleMembersInGroup == null || sortedBundleMembersInGroup.isEmpty()) {
            return typeMember;
        }
        return sortedBundleMembersInGroup.get(0);
    }

    @NonNull
    private static List<@NonNull CtTypeMember> resolveAccessorBundleMembersInGroup(
            @NonNull CtTypeMember typeMember,
            @NonNull Map<CtTypeMember, List<CtTypeMember>> accessorBundleMembersByMember) {

        List<CtTypeMember> sortedBundleMembersInGroup = accessorBundleMembersByMember.get(typeMember);
        return sortedBundleMembersInGroup == null ? List.of(typeMember) : sortedBundleMembersInGroup;
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

            // Cycles in declaration dependencies (mutual reachability).
            if (leftMustBeBeforeRight) {
                throw new IllegalStateException(composeCyclicDeclarationDependencyMessage(leftSortable, rightSortable));
            }

            CtTypeMember leftRepresentative = leftSortable.getRepresentativeTypeMember();
            CtTypeMember rightRepresentative = rightSortable.getRepresentativeTypeMember();

            if (leftRepresentative != rightRepresentative) {
                int representativeComparison =
                        typeMemberBaseComparator.compare(leftRepresentative, rightRepresentative);
                if (representativeComparison != 0) {
                    return representativeComparison;
                }
                throw new IllegalStateException(composeEqualRepresentativesMessage(leftSortable, rightSortable));
            }

            int directComparison = sortableBaseComparator.compare(leftSortable, rightSortable);
            if (directComparison != 0) {
                return directComparison;
            }
            throw new IllegalStateException(composeEqualMembersMessage(leftSortable, rightSortable));
        };
    }

    @NonNull
    private static String composeCyclicDeclarationDependencyMessage(
            @NonNull SortableTypeMember leftSortable, @NonNull SortableTypeMember rightSortable) {

        return "Detected a cyclic DECLARATION_DEPENDENCY ordering inside a member group. "
                + "Both members are mutually reachable via declaration dependency edges, so a strict provider-before-dependent "
                + "order cannot be derived for this pair.\n"
                + "Left:  " + describeSortableTypeMember(leftSortable) + "\n"
                + "Right: " + describeSortableTypeMember(rightSortable) + "\n"
                + "Hint: validate and report cycles in MemberDependencyGraph (or in a dedicated validator) before ordering.";
    }

    @NonNull
    private static String composeEqualRepresentativesMessage(
            @NonNull SortableTypeMember leftSortable, @NonNull SortableTypeMember rightSortable) {

        return "Two different representative members compare as equal by the base comparator. "
                + "This breaks deterministic representative ordering.\n"
                + "Left:  " + describeSortableTypeMember(leftSortable) + "\n"
                + "Right: " + describeSortableTypeMember(rightSortable) + "\n"
                + "Left representative:  " + describeTypeMemberForDebug(leftSortable.getRepresentativeTypeMember())
                + "\n"
                + "Right representative: " + describeTypeMemberForDebug(rightSortable.getRepresentativeTypeMember())
                + "\n"
                + "Hint: ensure the SortKeyValues comparator has a deterministic tie-breaker for representatives.";
    }

    @NonNull
    private static String composeEqualMembersMessage(
            @NonNull SortableTypeMember leftSortable, @NonNull SortableTypeMember rightSortable) {

        return "Two distinct members compare as equal by the configured base comparator, which violates deterministic ordering.\n"
                + "Left:  " + describeSortableTypeMember(leftSortable) + "\n"
                + "Right: " + describeSortableTypeMember(rightSortable) + "\n"
                + "Hint: ensure the SortKeyValues comparator produces a strict order for distinct members "
                + "(e.g., add a stable tie-breaker when all configured keys match).";
    }

    @NonNull
    private static String describeSortableTypeMember(@NonNull SortableTypeMember sortableTypeMember) {
        return "member=" + describeTypeMemberForDebug(sortableTypeMember.getTypeMember())
                + ", sortKeyValues=" + sortableTypeMember.getSortKeyValues()
                + ", representative=" + describeTypeMemberForDebug(sortableTypeMember.getRepresentativeTypeMember())
                + ", orderingDependentsInGroupCount="
                + sortableTypeMember.getOrderingDependentsInGroup().size();
    }

    @NonNull
    private static String describeTypeMemberForDebug(@NonNull CtTypeMember typeMember) {
        return typeMember.getClass().getSimpleName() + "@" + System.identityHashCode(typeMember);
    }

    @NonNull
    private static Comparator<SortableTypeMember.SortKeyValues> buildSortKeyValuesComparator(
            @NonNull List<SortKey> sortKeys) {

        Comparator<SortableTypeMember.SortKeyValues> configuredComparator = sortKeys.stream()
                .map(GroupMembersOrderer::buildSortKeyValuesComparatorForSortKey)
                .reduce(Comparator::thenComparing)
                .orElseGet(() -> Comparator.comparingInt(SortableTypeMember.SortKeyValues::getSourceStart)
                        .thenComparing(SortableTypeMember.SortKeyValues::getAlphaKey));

        // Deterministic tie-breakers regardless of configured keys.
        if (!sortKeys.contains(SortKey.PRESERVE)) {
            configuredComparator =
                    configuredComparator.thenComparing(buildSortKeyValuesComparatorForSortKey(SortKey.PRESERVE));
        }
        if (!sortKeys.contains(SortKey.ALPHA)) {
            configuredComparator =
                    configuredComparator.thenComparing(buildSortKeyValuesComparatorForSortKey(SortKey.ALPHA));
        }

        return configuredComparator;
    }

    @NonNull
    private static Comparator<SortableTypeMember.SortKeyValues> buildSortKeyValuesComparatorForSortKey(
            @NonNull SortKey sortKey) {

        return switch (sortKey) {
            case PRESERVE -> Comparator.comparingInt(SortableTypeMember.SortKeyValues::getSourceStart);
            case ALPHA -> Comparator.comparing(SortableTypeMember.SortKeyValues::getAlphaKey);
            case VISIBILITY_ASC -> Comparator.comparingInt(SortableTypeMember.SortKeyValues::getVisibilityRank);
            case VISIBILITY_DESC ->
                buildSortKeyValuesComparatorForSortKey(SortKey.VISIBILITY_ASC).reversed();
        };
    }

    @NonNull
    private static SortableTypeMember.SortKeyValues deriveSortKeyValues(@NonNull CtTypeMember typeMember) {
        return new SortableTypeMember.SortKeyValues(
                SpoonTypeMemberUtils.extractSourceStart(typeMember),
                SpoonTypeMemberUtils.deriveAlphaKey(typeMember),
                SpoonTypeMemberUtils.deriveVisibilityRank(typeMember));
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

            int visibilityRank;
        }
    }
}

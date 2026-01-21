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

        Set<CtTypeMember> groupMemberSetInStableIterationOrder = new LinkedHashSet<>(groupMembers);
        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMemberSetInStableIterationOrder);

        Map<CtTypeMember, SortableTypeMember> baseSortableTypeMemberByMember = groupMembers.stream()
                .collect(collectToLinkedHashMap(Function.identity(), SortableTypeMember::createBase));

        Comparator<SortableTypeMember> baseComparator = buildBaseComparatorForGroup(compiledMemberGroup);

        Map<CtTypeMember, Set<CtTypeMember>> accessorBundleMembersByMember = keepAccessorsTogether
                ? buildAccessorBundleMembersByMember(groupMembers, groupMemberSet, memberDependencyGraph)
                : Map.of();

        Map<CtTypeMember, Set<CtTypeMember>> effectiveTransitiveDependentsByProviderInGroup =
                buildEffectiveTransitiveDependentsByProviderInGroup(
                        groupMembers,
                        groupMemberSet,
                        memberDependencyGraph,
                        accessorBundleMembersByMember,
                        keepAccessorsTogether);

        Map<CtTypeMember, Set<CtTypeMember>> transitiveDeclarationProvidersByDependentInGroup =
                invertDependentsToProviders(effectiveTransitiveDependentsByProviderInGroup);

        Map<CtTypeMember, CtTypeMember> representativeMemberByMember = buildRepresentativeMemberByMember(
                groupMembers,
                baseSortableTypeMemberByMember,
                baseComparator,
                accessorBundleMembersByMember,
                effectiveTransitiveDependentsByProviderInGroup,
                keepAccessorsTogether);

        Map<CtTypeMember, SortableTypeMember> sortableTypeMemberByMember = groupMembers.stream()
                .collect(collectToLinkedHashMap(Function.identity(), groupMember -> {
                    SortableTypeMember baseSortableTypeMember =
                            requireSortableTypeMember(baseSortableTypeMemberByMember, groupMember);

                    CtTypeMember representativeMember =
                            representativeMemberByMember.getOrDefault(groupMember, groupMember);

                    Set<CtTypeMember> transitiveDeclarationProvidersInGroup =
                            transitiveDeclarationProvidersByDependentInGroup.getOrDefault(groupMember, Set.of());

                    return baseSortableTypeMember.withRelationships(
                            representativeMember, transitiveDeclarationProvidersInGroup);
                }));

        Comparator<SortableTypeMember> groupComparator =
                buildGroupComparator(baseComparator, sortableTypeMemberByMember);

        return groupMembers.stream()
                .map(sortableTypeMemberByMember::get)
                .sorted(groupComparator)
                .map(SortableTypeMember::getTypeMember)
                .toList();
    }

    @NonNull
    private static Map<CtTypeMember, Set<CtTypeMember>> buildAccessorBundleMembersByMember(
            @NonNull List<CtTypeMember> groupMembers,
            @NonNull Set<CtTypeMember> groupMemberSet,
            @NonNull MemberDependencyGraph memberDependencyGraph) {

        Map<CtTypeMember, Set<CtTypeMember>> accessorBundleMembersByMember = new LinkedHashMap<>();
        Set<CtTypeMember> visitedMembers = new LinkedHashSet<>();

        for (CtTypeMember member : groupMembers) {
            if (visitedMembers.contains(member)) {
                continue;
            }

            Set<CtTypeMember> bundleMembers = new LinkedHashSet<>();
            bundleMembers.add(member);

            // ACCESSOR_BUNDLE edges are expected to be bidirectional. We still use transitive traversal to be robust.
            bundleMembers.addAll(memberDependencyGraph.findTransitiveDependents(member, ACCESSOR_BUNDLE_ONLY));
            bundleMembers.retainAll(groupMemberSet);

            if (bundleMembers.size() <= 1) {
                continue;
            }

            visitedMembers.addAll(bundleMembers);

            Set<CtTypeMember> immutableBundleMembers = Set.copyOf(bundleMembers);
            bundleMembers.forEach(
                    bundleMember -> accessorBundleMembersByMember.put(bundleMember, immutableBundleMembers));
        }

        return Map.copyOf(accessorBundleMembersByMember);
    }

    @NonNull
    private static Map<CtTypeMember, Set<CtTypeMember>> buildEffectiveTransitiveDependentsByProviderInGroup(
            @NonNull List<CtTypeMember> groupMembers,
            @NonNull Set<CtTypeMember> groupMemberSet,
            @NonNull MemberDependencyGraph memberDependencyGraph,
            @NonNull Map<CtTypeMember, Set<CtTypeMember>> accessorBundleMembersByMember,
            boolean keepAccessorsTogether) {

        Map<CtTypeMember, Set<CtTypeMember>> effectiveTransitiveDependentsByProviderInGroup = new LinkedHashMap<>();

        for (CtTypeMember providerMember : groupMembers) {
            Set<CtTypeMember> transitiveDependentsInGroup =
                    memberDependencyGraph.findTransitiveDependents(providerMember, DECLARATION_DEPENDENCY_ONLY).stream()
                            .filter(groupMemberSet::contains)
                            .collect(LinkedHashSet::new, Set::add, Set::addAll);

            if (transitiveDependentsInGroup.isEmpty()) {
                continue;
            }

            Set<CtTypeMember> effectiveDependentsInGroup = keepAccessorsTogether
                    ? transitiveDependentsInGroup.stream()
                            .flatMap(dependentMember ->
                                    accessorBundleMembersByMember
                                            .getOrDefault(dependentMember, Set.of(dependentMember))
                                            .stream())
                            .filter(groupMemberSet::contains)
                            .collect(LinkedHashSet::new, Set::add, Set::addAll)
                    : transitiveDependentsInGroup;

            effectiveDependentsInGroup.remove(providerMember);

            if (!effectiveDependentsInGroup.isEmpty()) {
                effectiveTransitiveDependentsByProviderInGroup.put(
                        providerMember, Set.copyOf(effectiveDependentsInGroup));
            }
        }

        return Map.copyOf(effectiveTransitiveDependentsByProviderInGroup);
    }

    @NonNull
    private static Map<CtTypeMember, Set<CtTypeMember>> invertDependentsToProviders(
            @NonNull Map<CtTypeMember, Set<CtTypeMember>> transitiveDependentsByProvider) {

        Map<CtTypeMember, Set<CtTypeMember>> providersByDependent = new LinkedHashMap<>();

        transitiveDependentsByProvider.forEach(
                (providerMember, dependentMembers) -> dependentMembers.forEach(dependent -> {
                    providersByDependent
                            .computeIfAbsent(dependent, ignored -> new LinkedHashSet<>())
                            .add(providerMember);
                }));

        Map<CtTypeMember, Set<CtTypeMember>> immutableProvidersByDependent = new LinkedHashMap<>();
        providersByDependent.forEach(
                (dependent, providers) -> immutableProvidersByDependent.put(dependent, Set.copyOf(providers)));

        return Map.copyOf(immutableProvidersByDependent);
    }

    @NonNull
    private static Map<CtTypeMember, CtTypeMember> buildRepresentativeMemberByMember(
            @NonNull List<CtTypeMember> groupMembers,
            @NonNull Map<CtTypeMember, SortableTypeMember> baseSortableTypeMemberByMember,
            @NonNull Comparator<SortableTypeMember> baseComparator,
            @NonNull Map<CtTypeMember, Set<CtTypeMember>> accessorBundleMembersByMember,
            @NonNull Map<CtTypeMember, Set<CtTypeMember>> effectiveTransitiveDependentsByProviderInGroup,
            boolean keepAccessorsTogether) {

        Map<CtTypeMember, CtTypeMember> representativeMemberByMember = new LinkedHashMap<>();
        groupMembers.forEach(member -> representativeMemberByMember.put(member, member));

        if (keepAccessorsTogether && !accessorBundleMembersByMember.isEmpty()) {
            Set<Set<CtTypeMember>> uniqueBundlesInGroup = new LinkedHashSet<>(accessorBundleMembersByMember.values());

            for (Set<CtTypeMember> bundleMembers : uniqueBundlesInGroup) {
                CtTypeMember bundleRepresentative = bundleMembers.stream()
                        .map(bundleMember -> requireSortableTypeMember(baseSortableTypeMemberByMember, bundleMember))
                        .min(baseComparator)
                        .map(SortableTypeMember::getTypeMember)
                        .orElseThrow(() -> new IllegalStateException(
                                "Failed to resolve accessor bundle representative. Bundle members: " + bundleMembers));

                bundleMembers.forEach(
                        bundleMember -> representativeMemberByMember.put(bundleMember, bundleRepresentative));
            }
        }

        effectiveTransitiveDependentsByProviderInGroup.forEach((providerMember, effectiveDependentsInGroup) -> {
            CtTypeMember providerRepresentative = Stream.concat(
                            Stream.of(providerMember), effectiveDependentsInGroup.stream())
                    .map(candidateMember -> requireSortableTypeMember(baseSortableTypeMemberByMember, candidateMember))
                    .min(baseComparator)
                    .map(SortableTypeMember::getTypeMember)
                    .orElseThrow(() -> new IllegalStateException("Failed to resolve provider representative. Provider: "
                            + describeTypeMember(providerMember)));

            representativeMemberByMember.put(providerMember, providerRepresentative);
        });

        return Map.copyOf(representativeMemberByMember);
    }

    @NonNull
    private static Comparator<SortableTypeMember> buildGroupComparator(
            @NonNull Comparator<SortableTypeMember> baseComparator,
            @NonNull Map<CtTypeMember, SortableTypeMember> sortableTypeMemberByMember) {

        return (leftSortableMember, rightSortableMember) -> {
            CtTypeMember leftMember = leftSortableMember.getTypeMember();
            CtTypeMember rightMember = rightSortableMember.getTypeMember();

            if (leftMember == rightMember) {
                return 0;
            }

            boolean leftIsProviderOfRight = rightSortableMember
                    .getTransitiveDeclarationProvidersInGroup()
                    .contains(leftMember);

            boolean rightIsProviderOfLeft = leftSortableMember
                    .getTransitiveDeclarationProvidersInGroup()
                    .contains(rightMember);

            if (leftIsProviderOfRight && rightIsProviderOfLeft) {
                int fallbackComparison = baseComparator.compare(leftSortableMember, rightSortableMember);
                if (fallbackComparison != 0) {
                    return fallbackComparison;
                }
                return Integer.compare(
                        leftSortableMember.getIdentityHashCode(), rightSortableMember.getIdentityHashCode());
            }

            if (leftIsProviderOfRight) {
                return -1;
            }
            if (rightIsProviderOfLeft) {
                return 1;
            }

            CtTypeMember leftRepresentativeMember = leftSortableMember.getRepresentativeMember();
            CtTypeMember rightRepresentativeMember = rightSortableMember.getRepresentativeMember();

            if (leftRepresentativeMember != rightRepresentativeMember) {
                SortableTypeMember leftRepresentativeSortable =
                        requireSortableTypeMember(sortableTypeMemberByMember, leftRepresentativeMember);
                SortableTypeMember rightRepresentativeSortable =
                        requireSortableTypeMember(sortableTypeMemberByMember, rightRepresentativeMember);

                int representativeComparison =
                        baseComparator.compare(leftRepresentativeSortable, rightRepresentativeSortable);
                if (representativeComparison != 0) {
                    return representativeComparison;
                }
            }

            int directComparison = baseComparator.compare(leftSortableMember, rightSortableMember);
            if (directComparison != 0) {
                return directComparison;
            }

            // Last-resort deterministic tie-breaker.
            return Integer.compare(leftSortableMember.getIdentityHashCode(), rightSortableMember.getIdentityHashCode());
        };
    }

    @NonNull
    private static Comparator<SortableTypeMember> buildBaseComparatorForGroup(
            @NonNull CompiledMemberGroup compiledMemberGroup) {
        List<SortKey> sortKeys = compiledMemberGroup.getSortKeys();

        Comparator<SortableTypeMember> configuredComparator = sortKeys.stream()
                .map(GroupMembersOrderer::buildComparatorForSortKey)
                .reduce(Comparator::thenComparing)
                .orElseGet(() -> Comparator.comparingInt(SortableTypeMember::getSourceStart)
                        .thenComparing(SortableTypeMember::getAlphaKey));

        // Deterministic tie-breakers regardless of configured keys.
        if (!sortKeys.contains(SortKey.PRESERVE)) {
            configuredComparator = configuredComparator.thenComparingInt(SortableTypeMember::getSourceStart);
        }
        if (!sortKeys.contains(SortKey.ALPHA)) {
            configuredComparator = configuredComparator.thenComparing(SortableTypeMember::getAlphaKey);
        }
        return configuredComparator;
    }

    @NonNull
    private static Comparator<SortableTypeMember> buildComparatorForSortKey(@NonNull SortKey sortKey) {
        return switch (sortKey) {
            case PRESERVE -> Comparator.comparingInt(SortableTypeMember::getSourceStart);
            case ALPHA -> Comparator.comparing(SortableTypeMember::getAlphaKey);
            case VISIBILITY_ASC -> Comparator.comparingInt(SortableTypeMember::getVisibilityRankAscending);
            case VISIBILITY_DESC -> Comparator.comparingInt(SortableTypeMember::getVisibilityRankDescending);
        };
    }

    @NonNull
    private static SortableTypeMember requireSortableTypeMember(
            @NonNull Map<CtTypeMember, SortableTypeMember> sortableTypeMemberByMember, @NonNull CtTypeMember member) {

        SortableTypeMember sortableTypeMember = sortableTypeMemberByMember.get(member);
        if (sortableTypeMember == null) {
            throw new IllegalStateException(
                    "SortableTypeMember was not found for member: " + describeTypeMember(member));
        }
        return sortableTypeMember;
    }

    @NonNull
    private static String describeTypeMember(@NonNull CtTypeMember typeMember) {
        return typeMember.getClass().getSimpleName()
                + "{signature=" + SpoonTypeMemberUtils.deriveAlphaKey(typeMember)
                + ",sourceStart=" + SpoonTypeMemberUtils.extractSourceStart(typeMember)
                + "}";
    }

    private static <K, V> java.util.stream.Collector<Map.Entry<K, V>, ?, Map<K, V>> unused() {
        throw new UnsupportedOperationException();
    }

    private static <K, V> java.util.stream.Collector<K, ?, Map<K, V>> collectToLinkedHashMap(
            Function<K, K> keyMapper, Function<K, V> valueMapper) {

        return java.util.stream.Collectors.toMap(
                keyMapper,
                valueMapper,
                (existingValue, newValue) -> {
                    throw new IllegalStateException("Unexpected duplicate key while building a map.");
                },
                LinkedHashMap::new);
    }

    @Value
    private static class SortableTypeMember {

        @NonNull
        CtTypeMember typeMember;

        int sourceStart;

        @NonNull
        String alphaKey;

        int visibilityRankAscending;

        int visibilityRankDescending;

        int identityHashCode;

        @NonNull
        CtTypeMember representativeMember;

        @NonNull
        Set<@NonNull CtTypeMember> transitiveDeclarationProvidersInGroup;

        static SortableTypeMember createBase(@NonNull CtTypeMember typeMember) {
            return new SortableTypeMember(
                    typeMember,
                    SpoonTypeMemberUtils.extractSourceStart(typeMember),
                    SpoonTypeMemberUtils.deriveAlphaKey(typeMember),
                    SpoonTypeMemberUtils.deriveVisibilityRankAscending(typeMember),
                    SpoonTypeMemberUtils.deriveVisibilityRankDescending(typeMember),
                    System.identityHashCode(typeMember),
                    typeMember,
                    Set.of());
        }

        @NonNull
        SortableTypeMember withRelationships(
                @NonNull CtTypeMember representativeMember,
                @NonNull Set<@NonNull CtTypeMember> transitiveDeclarationProvidersInGroup) {

            return new SortableTypeMember(
                    typeMember,
                    sourceStart,
                    alphaKey,
                    visibilityRankAscending,
                    visibilityRankDescending,
                    identityHashCode,
                    representativeMember,
                    Set.copyOf(transitiveDeclarationProvidersInGroup));
        }
    }
}

package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMembers);

        GroupOrderingContext groupOrderingContext = new GroupOrderingContext(
                compiledMemberGroup, groupMembers, groupMemberSet, keepAccessorsTogether, memberDependencyGraph);

        Map<CtTypeMember, SortableTypeMember> sortableByMember = groupMembers.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        groupOrderingContext::convertToSortableTypeMember,
                        (existingValue, newValue) -> {
                            throw new IllegalStateException(
                                    "Unexpected duplicate member while building member->sortable mapping. member="
                                            + describeTypeMember(existingValue.getTypeMember()));
                        },
                        LinkedHashMap::new));

        Comparator<SortableTypeMember> baseComparator = buildBaseComparatorForGroup(compiledMemberGroup);
        Comparator<SortableTypeMember> groupComparator = buildGroupComparator(baseComparator, sortableByMember);

        return groupMembers.stream()
                .map(sortableByMember::get)
                .sorted(groupComparator)
                .map(SortableTypeMember::getTypeMember)
                .toList();
    }

    @NonNull
    private static Comparator<SortableTypeMember> buildGroupComparator(
            @NonNull Comparator<SortableTypeMember> baseComparator,
            @NonNull Map<CtTypeMember, SortableTypeMember> sortableByMember) {

        return (leftSortableMember, rightSortableMember) -> {
            CtTypeMember leftMember = leftSortableMember.getTypeMember();
            CtTypeMember rightMember = rightSortableMember.getTypeMember();

            // Comparator contract: same reference must be equal.
            if (leftMember == rightMember) {
                return 0;
            }

            // Ordering constraints (DECLARATION_DEPENDENCY): provider must be before dependent.
            boolean leftMustBeBeforeRight = rightSortableMember
                    .getTransitiveDeclarationProvidersInGroup()
                    .contains(leftMember);
            boolean rightMustBeBeforeLeft = leftSortableMember
                    .getTransitiveDeclarationProvidersInGroup()
                    .contains(rightMember);

            if (leftMustBeBeforeRight && !rightMustBeBeforeLeft) {
                return -1;
            }
            if (rightMustBeBeforeLeft && !leftMustBeBeforeRight) {
                return 1;
            }

            // Corner case: cycles in declaration dependencies. We must not break antisymmetry.
            // Fall back to deterministic base ordering.
            if (leftMustBeBeforeRight && rightMustBeBeforeLeft) {
                int cycleFallback = baseComparator.compare(leftSortableMember, rightSortableMember);
                if (cycleFallback != 0) {
                    return cycleFallback;
                }
                return Integer.compare(
                        leftSortableMember.getIdentityHashCode(), rightSortableMember.getIdentityHashCode());
            }

            CtTypeMember leftRepresentativeMember = leftSortableMember.getRepresentativeMember();
            CtTypeMember rightRepresentativeMember = rightSortableMember.getRepresentativeMember();

            if (leftRepresentativeMember != rightRepresentativeMember) {
                SortableTypeMember leftRepresentative =
                        sortableByMember.getOrDefault(leftRepresentativeMember, leftSortableMember);
                SortableTypeMember rightRepresentative =
                        sortableByMember.getOrDefault(rightRepresentativeMember, rightSortableMember);

                int representativeComparison = baseComparator.compare(leftRepresentative, rightRepresentative);
                if (representativeComparison != 0) {
                    return representativeComparison;
                }
            }

            int directComparison = baseComparator.compare(leftSortableMember, rightSortableMember);
            if (directComparison != 0) {
                return directComparison;
            }

            // Last-resort deterministic tie-breaker (should be very rare).
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

        // Final deterministic tie-breaker.
        return configuredComparator.thenComparingInt(SortableTypeMember::getIdentityHashCode);
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
    private static String describeTypeMember(@NonNull CtTypeMember typeMember) {
        return typeMember.getClass().getSimpleName()
                + "{signature=" + SpoonTypeMemberUtils.deriveAlphaKey(typeMember)
                + ",sourceStart=" + SpoonTypeMemberUtils.extractSourceStart(typeMember)
                + "}";
    }

    @Value
    private static class SortKeysSnapshot {
        int sourceStart;

        @NonNull
        String alphaKey;

        int visibilityRankAscending;
        int visibilityRankDescending;
        int identityHashCode;
    }

    @Value
    private static class SortableTypeMember {
        @NonNull
        CtTypeMember typeMember;

        @NonNull
        CtTypeMember representativeMember;

        @NonNull
        Set<@NonNull CtTypeMember> transitiveDeclarationProvidersInGroup;

        int sourceStart;

        @NonNull
        String alphaKey;

        int visibilityRankAscending;
        int visibilityRankDescending;
        int identityHashCode;
    }

    private static class GroupOrderingContext {
        @NonNull
        private final CompiledMemberGroup compiledMemberGroup;

        @NonNull
        private final List<CtTypeMember> groupMembers;

        @NonNull
        private final Set<CtTypeMember> groupMemberSet;

        private final boolean keepAccessorsTogether;

        @NonNull
        private final MemberDependencyGraph memberDependencyGraph;

        @NonNull
        private final Map<CtTypeMember, SortKeysSnapshot> sortKeysSnapshotByMember = new LinkedHashMap<>();

        @NonNull
        private final Map<CtTypeMember, Set<CtTypeMember>> accessorBundleMembersByMember = new LinkedHashMap<>();

        @NonNull
        private final Map<CtTypeMember, Set<CtTypeMember>> effectiveTransitiveDependentsByProvider =
                new LinkedHashMap<>();

        @NonNull
        private final Map<CtTypeMember, Set<CtTypeMember>> transitiveDeclarationProvidersByDependent =
                new LinkedHashMap<>();

        @NonNull
        private final Map<CtTypeMember, CtTypeMember> representativeByMember = new LinkedHashMap<>();

        @NonNull
        private final Comparator<CtTypeMember> typeMemberBaseComparator;

        @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
        private GroupOrderingContext(
                @NonNull CompiledMemberGroup compiledMemberGroup,
                @NonNull List<CtTypeMember> groupMembers,
                @NonNull Set<CtTypeMember> groupMemberSet,
                boolean keepAccessorsTogether,
                @NonNull MemberDependencyGraph memberDependencyGraph) {
            this.compiledMemberGroup = compiledMemberGroup;
            this.groupMembers = groupMembers;
            this.groupMemberSet = groupMemberSet;
            this.keepAccessorsTogether = keepAccessorsTogether;
            this.memberDependencyGraph = memberDependencyGraph;
            this.typeMemberBaseComparator =
                    buildTypeMemberBaseComparatorForGroup(compiledMemberGroup, this::requireSortKeysSnapshot);
        }

        @NonNull
        private SortableTypeMember convertToSortableTypeMember(@NonNull CtTypeMember typeMember) {
            SortKeysSnapshot sortKeysSnapshot = requireSortKeysSnapshot(typeMember);
            Set<CtTypeMember> transitiveDeclarationProvidersInGroup =
                    resolveTransitiveDeclarationProvidersInGroup(typeMember);
            CtTypeMember representativeMember = resolveRepresentativeMember(typeMember);

            return new SortableTypeMember(
                    typeMember,
                    representativeMember,
                    transitiveDeclarationProvidersInGroup,
                    sortKeysSnapshot.getSourceStart(),
                    sortKeysSnapshot.getAlphaKey(),
                    sortKeysSnapshot.getVisibilityRankAscending(),
                    sortKeysSnapshot.getVisibilityRankDescending(),
                    sortKeysSnapshot.getIdentityHashCode());
        }

        @NonNull
        private SortKeysSnapshot requireSortKeysSnapshot(@NonNull CtTypeMember typeMember) {
            return sortKeysSnapshotByMember.computeIfAbsent(
                    typeMember,
                    ignored -> new SortKeysSnapshot(
                            SpoonTypeMemberUtils.extractSourceStart(typeMember),
                            SpoonTypeMemberUtils.deriveAlphaKey(typeMember),
                            SpoonTypeMemberUtils.deriveVisibilityRankAscending(typeMember),
                            SpoonTypeMemberUtils.deriveVisibilityRankDescending(typeMember),
                            System.identityHashCode(typeMember)));
        }

        @NonNull
        private Set<CtTypeMember> resolveTransitiveDeclarationProvidersInGroup(@NonNull CtTypeMember dependentMember) {
            return transitiveDeclarationProvidersByDependent.computeIfAbsent(dependentMember, ignored -> {
                Set<CtTypeMember> providerMembers = groupMembers.stream()
                        .filter(providerCandidate -> resolveEffectiveTransitiveDependentsInGroup(providerCandidate)
                                .contains(dependentMember))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                return Set.copyOf(providerMembers);
            });
        }

        @NonNull
        private Set<CtTypeMember> resolveEffectiveTransitiveDependentsInGroup(@NonNull CtTypeMember providerMember) {
            return effectiveTransitiveDependentsByProvider.computeIfAbsent(providerMember, ignored -> {
                Set<CtTypeMember> transitiveDependentsInGroup =
                        memberDependencyGraph
                                .findTransitiveDependents(providerMember, DECLARATION_DEPENDENCY_ONLY)
                                .stream()
                                .filter(groupMemberSet::contains)
                                .collect(Collectors.toCollection(LinkedHashSet::new));

                // The transitive traversal may return the provider itself; remove to avoid self-dependency.
                transitiveDependentsInGroup.remove(providerMember);

                if (transitiveDependentsInGroup.isEmpty() || !keepAccessorsTogether) {
                    return Set.copyOf(transitiveDependentsInGroup);
                }

                // If a provider is required by any accessor in a bundle, we want it to be ordered before the whole
                // bundle.
                // We achieve it by expanding provider dependents to include full accessor bundles for all dependent
                // members.
                Set<CtTypeMember> expandedDependents = transitiveDependentsInGroup.stream()
                        .flatMap(dependent -> resolveAccessorBundleMembersInGroup(dependent).stream())
                        .filter(groupMemberSet::contains)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                expandedDependents.remove(providerMember);
                return Set.copyOf(expandedDependents);
            });
        }

        @NonNull
        private Set<CtTypeMember> resolveAccessorBundleMembersInGroup(@NonNull CtTypeMember typeMember) {
            if (!keepAccessorsTogether) {
                return Set.of(typeMember);
            }

            Set<CtTypeMember> cachedBundleMembers = accessorBundleMembersByMember.get(typeMember);
            if (cachedBundleMembers != null) {
                return cachedBundleMembers;
            }

            Set<CtTypeMember> bundleMembers = new LinkedHashSet<>();
            bundleMembers.add(typeMember);

            // ACCESSOR_BUNDLE edges are expected to be bidirectional.
            // We still use transitive traversal to be robust if the bundle is not a "clique".
            bundleMembers.addAll(memberDependencyGraph.findTransitiveDependents(typeMember, ACCESSOR_BUNDLE_ONLY));
            bundleMembers.retainAll(groupMemberSet);

            Set<CtTypeMember> unmodifiableBundleMembers = Set.copyOf(bundleMembers);
            bundleMembers.forEach(
                    bundleMember -> accessorBundleMembersByMember.put(bundleMember, unmodifiableBundleMembers));
            return unmodifiableBundleMembers;
        }

        @NonNull
        private CtTypeMember resolveRepresentativeMember(@NonNull CtTypeMember typeMember) {
            CtTypeMember cachedRepresentative = representativeByMember.get(typeMember);
            if (cachedRepresentative != null) {
                return cachedRepresentative;
            }

            if (keepAccessorsTogether) {
                Set<CtTypeMember> bundleMembers = resolveAccessorBundleMembersInGroup(typeMember);
                if (bundleMembers.size() > 1) {
                    CtTypeMember bundleRepresentative =
                            bundleMembers.stream().min(typeMemberBaseComparator).orElse(typeMember);

                    // All bundle members must share the same representative.
                    bundleMembers.forEach(
                            bundleMember -> representativeByMember.put(bundleMember, bundleRepresentative));
                    return bundleRepresentative;
                }
            }

            Set<CtTypeMember> effectiveTransitiveDependents = resolveEffectiveTransitiveDependentsInGroup(typeMember);
            CtTypeMember providerOrSelfRepresentative = Stream.concat(
                            Stream.of(typeMember), effectiveTransitiveDependents.stream())
                    .min(typeMemberBaseComparator)
                    .orElse(typeMember);

            representativeByMember.put(typeMember, providerOrSelfRepresentative);
            return providerOrSelfRepresentative;
        }
    }

    @NonNull
    private static Comparator<CtTypeMember> buildTypeMemberBaseComparatorForGroup(
            @NonNull CompiledMemberGroup compiledMemberGroup,
            @NonNull Function<CtTypeMember, SortKeysSnapshot> sortKeysSnapshotProvider) {

        List<SortKey> sortKeys = compiledMemberGroup.getSortKeys();

        Comparator<CtTypeMember> configuredComparator = sortKeys.stream()
                .map(sortKey -> buildComparatorForSortKey(sortKey, sortKeysSnapshotProvider))
                .reduce((leftComparator, rightComparator) -> leftComparator.thenComparing(rightComparator))
                .orElseGet(() -> Comparator.comparingInt((CtTypeMember member) ->
                                sortKeysSnapshotProvider.apply(member).getSourceStart())
                        .thenComparing((CtTypeMember member) ->
                                sortKeysSnapshotProvider.apply(member).getAlphaKey()));

        // Deterministic tie-breakers regardless of configured keys.
        if (!sortKeys.contains(SortKey.PRESERVE)) {
            configuredComparator = configuredComparator.thenComparingInt(
                    member -> sortKeysSnapshotProvider.apply(member).getSourceStart());
        }
        if (!sortKeys.contains(SortKey.ALPHA)) {
            configuredComparator = configuredComparator.thenComparing(
                    member -> sortKeysSnapshotProvider.apply(member).getAlphaKey());
        }

        return configuredComparator.thenComparingInt(
                member -> sortKeysSnapshotProvider.apply(member).getIdentityHashCode());
    }

    @NonNull
    private static Comparator<CtTypeMember> buildComparatorForSortKey(
            @NonNull SortKey sortKey, @NonNull Function<CtTypeMember, SortKeysSnapshot> sortKeysSnapshotProvider) {

        return switch (sortKey) {
            case PRESERVE ->
                Comparator.comparingInt(
                        member -> sortKeysSnapshotProvider.apply(member).getSourceStart());
            case ALPHA ->
                Comparator.comparing(
                        member -> sortKeysSnapshotProvider.apply(member).getAlphaKey());
            case VISIBILITY_ASC ->
                Comparator.comparingInt(
                        member -> sortKeysSnapshotProvider.apply(member).getVisibilityRankAscending());
            case VISIBILITY_DESC ->
                Comparator.comparingInt(
                        member -> sortKeysSnapshotProvider.apply(member).getVisibilityRankDescending());
        };
    }
}

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

        GroupSortingContext sortingContext =
                new GroupSortingContext(compiledMemberGroup, groupMembers, memberDependencyGraph);

        // One-pass conversion: CtTypeMember -> SortableTypeMember.
        Map<CtTypeMember, SortableTypeMember> sortableByMember = groupMembers.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        sortingContext::convertToSortableTypeMember,
                        (existingValue, newValue) -> {
                            throw new IllegalStateException("Unexpected duplicate member while building sortable map. "
                                    + "member=" + describeTypeMember(existingValue.getTypeMember()));
                        },
                        LinkedHashMap::new));

        Comparator<SortableTypeMember> groupComparator =
                buildGroupComparator(sortingContext.getSortableBaseComparator(), sortableByMember);

        return sortableByMember.values().stream()
                .sorted(groupComparator)
                .map(SortableTypeMember::getTypeMember)
                .toList();
    }

    @NonNull
    private static Comparator<SortableTypeMember> buildGroupComparator(
            @NonNull Comparator<SortableTypeMember> sortableBaseComparator,
            @NonNull Map<CtTypeMember, SortableTypeMember> sortableByMember) {

        return (leftSortable, rightSortable) -> {
            if (leftSortable == rightSortable) {
                return 0;
            }

            CtTypeMember leftMember = leftSortable.getTypeMember();
            CtTypeMember rightMember = rightSortable.getTypeMember();

            boolean leftMustBeBeforeRight = leftSortable
                    .getEffectiveTransitiveDeclarationDependentsInGroup()
                    .contains(rightMember);
            boolean rightMustBeBeforeLeft = rightSortable
                    .getEffectiveTransitiveDeclarationDependentsInGroup()
                    .contains(leftMember);

            if (leftMustBeBeforeRight && !rightMustBeBeforeLeft) {
                return -1;
            }
            if (rightMustBeBeforeLeft && !leftMustBeBeforeRight) {
                return 1;
            }

            // Cycles in declaration dependencies must not break antisymmetry.
            if (leftMustBeBeforeRight && rightMustBeBeforeLeft) {
                int cycleFallback = sortableBaseComparator.compare(leftSortable, rightSortable);
                if (cycleFallback != 0) {
                    return cycleFallback;
                }
                return Integer.compare(leftSortable.getIdentityHashCode(), rightSortable.getIdentityHashCode());
            }

            CtTypeMember leftRepresentative = leftSortable.getRepresentativeMember();
            CtTypeMember rightRepresentative = rightSortable.getRepresentativeMember();

            if (leftRepresentative != rightRepresentative) {
                SortableTypeMember leftRepSortable = requireSortable(sortableByMember, leftRepresentative);
                SortableTypeMember rightRepSortable = requireSortable(sortableByMember, rightRepresentative);

                int repComparison = sortableBaseComparator.compare(leftRepSortable, rightRepSortable);
                if (repComparison != 0) {
                    return repComparison;
                }
            }

            int directComparison = sortableBaseComparator.compare(leftSortable, rightSortable);
            if (directComparison != 0) {
                return directComparison;
            }

            return Integer.compare(leftSortable.getIdentityHashCode(), rightSortable.getIdentityHashCode());
        };
    }

    @NonNull
    private static SortableTypeMember requireSortable(
            @NonNull Map<CtTypeMember, SortableTypeMember> sortableByMember, @NonNull CtTypeMember member) {

        SortableTypeMember sortable = sortableByMember.get(member);
        if (sortable == null) {
            throw new IllegalStateException("SortableTypeMember was not created for: " + describeTypeMember(member));
        }
        return sortable;
    }

    @NonNull
    private static Comparator<SortableTypeMember> buildSortableBaseComparatorForGroup(
            @NonNull CompiledMemberGroup compiledMemberGroup) {

        List<SortKey> sortKeys = compiledMemberGroup.getSortKeys();

        Comparator<SortableTypeMember> configuredComparator = sortKeys.stream()
                .map(GroupMembersOrderer::buildSortableComparatorForSortKey)
                .reduce(Comparator::thenComparing)
                .orElseGet(() -> Comparator.comparingInt(SortableTypeMember::getSourceStart)
                        .thenComparing(SortableTypeMember::getAlphaKey));

        if (!sortKeys.contains(SortKey.PRESERVE)) {
            configuredComparator = configuredComparator.thenComparingInt(SortableTypeMember::getSourceStart);
        }
        if (!sortKeys.contains(SortKey.ALPHA)) {
            configuredComparator = configuredComparator.thenComparing(SortableTypeMember::getAlphaKey);
        }

        return configuredComparator;
    }

    @NonNull
    private static Comparator<SortableTypeMember> buildSortableComparatorForSortKey(@NonNull SortKey sortKey) {
        return switch (sortKey) {
            case PRESERVE -> Comparator.comparingInt(SortableTypeMember::getSourceStart);
            case ALPHA -> Comparator.comparing(SortableTypeMember::getAlphaKey);
            case VISIBILITY_ASC -> Comparator.comparingInt(SortableTypeMember::getVisibilityRankAscending);
            case VISIBILITY_DESC -> Comparator.comparingInt(SortableTypeMember::getVisibilityRankDescending);
        };
    }

    @NonNull
    private static Comparator<CtTypeMember> buildTypeMemberBaseComparatorForGroup(
            @NonNull CompiledMemberGroup compiledMemberGroup,
            @NonNull Function<CtTypeMember, SortKeysSnapshot> snapshotProvider) {

        List<SortKey> sortKeys = compiledMemberGroup.getSortKeys();

        Comparator<CtTypeMember> configuredComparator = sortKeys.stream()
                .map(sortKey -> buildTypeMemberComparatorForSortKey(sortKey, snapshotProvider))
                .reduce(Comparator::thenComparing)
                .orElseGet(() -> Comparator.<CtTypeMember>comparingInt(
                                m -> snapshotProvider.apply(m).getSourceStart())
                        .thenComparing(m -> snapshotProvider.apply(m).getAlphaKey()));

        if (!sortKeys.contains(SortKey.PRESERVE)) {
            configuredComparator = configuredComparator.thenComparingInt(
                    m -> snapshotProvider.apply(m).getSourceStart());
        }
        if (!sortKeys.contains(SortKey.ALPHA)) {
            configuredComparator = configuredComparator.thenComparing(
                    m -> snapshotProvider.apply(m).getAlphaKey());
        }

        return configuredComparator;
    }

    @NonNull
    private static Comparator<CtTypeMember> buildTypeMemberComparatorForSortKey(
            @NonNull SortKey sortKey, @NonNull Function<CtTypeMember, SortKeysSnapshot> snapshotProvider) {

        return switch (sortKey) {
            case PRESERVE ->
                Comparator.comparingInt(m -> snapshotProvider.apply(m).getSourceStart());
            case ALPHA -> Comparator.comparing(m -> snapshotProvider.apply(m).getAlphaKey());
            case VISIBILITY_ASC ->
                Comparator.comparingInt(m -> snapshotProvider.apply(m).getVisibilityRankAscending());
            case VISIBILITY_DESC ->
                Comparator.comparingInt(m -> snapshotProvider.apply(m).getVisibilityRankDescending());
        };
    }

    @NonNull
    private static String describeTypeMember(@NonNull CtTypeMember typeMember) {
        return typeMember.getClass().getSimpleName()
                + "{signature=" + SpoonTypeMemberUtils.deriveAlphaKey(typeMember)
                + ",sourceStart=" + SpoonTypeMemberUtils.extractSourceStart(typeMember)
                + "}";
    }

    private static final class GroupSortingContext {

        private final CompiledMemberGroup compiledMemberGroup;
        private final MemberDependencyGraph memberDependencyGraph;
        private final boolean keepAccessorsTogether;

        private final Set<CtTypeMember> groupMemberSet;

        private final Map<CtTypeMember, SortKeysSnapshot> snapshotByMember = new LinkedHashMap<>();
        private final Map<CtTypeMember, Set<CtTypeMember>> accessorBundleMembersByMember = new LinkedHashMap<>();
        private final Map<CtTypeMember, CtTypeMember> accessorBundleRepresentativeByMember = new LinkedHashMap<>();

        private final Map<CtTypeMember, Set<CtTypeMember>> effectiveDependentsByMember = new LinkedHashMap<>();
        private final Map<CtTypeMember, CtTypeMember> representativeByMember = new LinkedHashMap<>();

        private final Comparator<CtTypeMember> typeMemberBaseComparator;
        private final Comparator<SortableTypeMember> sortableBaseComparator;

        private GroupSortingContext(
                @NonNull CompiledMemberGroup compiledMemberGroup,
                @NonNull List<CtTypeMember> groupMembers,
                @NonNull MemberDependencyGraph memberDependencyGraph) {

            this.compiledMemberGroup = compiledMemberGroup;
            this.memberDependencyGraph = memberDependencyGraph;
            this.keepAccessorsTogether = compiledMemberGroup.isKeepAccessorsTogether();
            this.groupMemberSet = Set.copyOf(groupMembers);

            this.typeMemberBaseComparator =
                    buildTypeMemberBaseComparatorForGroup(compiledMemberGroup, this::requireSnapshot);

            this.sortableBaseComparator = buildSortableBaseComparatorForGroup(compiledMemberGroup);
        }

        @NonNull
        private Comparator<SortableTypeMember> getSortableBaseComparator() {
            return sortableBaseComparator;
        }

        @NonNull
        private SortableTypeMember convertToSortableTypeMember(@NonNull CtTypeMember member) {
            SortKeysSnapshot snapshot = requireSnapshot(member);

            Set<CtTypeMember> effectiveDependents = resolveEffectiveDependentsInGroup(member);
            CtTypeMember representative = resolveRepresentative(member, effectiveDependents);

            return new SortableTypeMember(
                    member,
                    representative,
                    effectiveDependents,
                    snapshot.getSourceStart(),
                    snapshot.getAlphaKey(),
                    snapshot.getVisibilityRankAscending(),
                    snapshot.getVisibilityRankDescending(),
                    snapshot.getIdentityHashCode());
        }

        @NonNull
        private SortKeysSnapshot requireSnapshot(@NonNull CtTypeMember member) {
            return snapshotByMember.computeIfAbsent(
                    member,
                    ignored -> new SortKeysSnapshot(
                            SpoonTypeMemberUtils.extractSourceStart(member),
                            SpoonTypeMemberUtils.deriveAlphaKey(member),
                            SpoonTypeMemberUtils.deriveVisibilityRankAscending(member),
                            SpoonTypeMemberUtils.deriveVisibilityRankDescending(member),
                            System.identityHashCode(member)));
        }

        @NonNull
        private Set<CtTypeMember> resolveEffectiveDependentsInGroup(@NonNull CtTypeMember providerMember) {
            Set<CtTypeMember> cached = effectiveDependentsByMember.get(providerMember);
            if (cached != null) {
                return cached;
            }

            LinkedHashSet<CtTypeMember> transitiveDependentsInGroup =
                    memberDependencyGraph.findTransitiveDependents(providerMember, DECLARATION_DEPENDENCY_ONLY).stream()
                            .filter(groupMemberSet::contains)
                            .collect(Collectors.toCollection(LinkedHashSet::new));

            if (transitiveDependentsInGroup.isEmpty()) {
                Set<CtTypeMember> empty = Set.of();
                effectiveDependentsByMember.put(providerMember, empty);
                return empty;
            }

            LinkedHashSet<CtTypeMember> effectiveDependents = keepAccessorsTogether
                    ? transitiveDependentsInGroup.stream()
                            .flatMap(dependent -> resolveAccessorBundleMembers(dependent).stream())
                            .filter(groupMemberSet::contains)
                            .collect(Collectors.toCollection(LinkedHashSet::new))
                    : transitiveDependentsInGroup;

            Set<CtTypeMember> result = Set.copyOf(effectiveDependents);
            effectiveDependentsByMember.put(providerMember, result);
            return result;
        }

        @NonNull
        private CtTypeMember resolveRepresentative(
                @NonNull CtTypeMember member, @NonNull Set<CtTypeMember> effectiveDependents) {

            CtTypeMember cached = representativeByMember.get(member);
            if (cached != null) {
                return cached;
            }

            if (keepAccessorsTogether) {
                CtTypeMember bundleRepresentative = resolveAccessorBundleRepresentative(member);
                if (bundleRepresentative != member) {
                    representativeByMember.put(member, bundleRepresentative);
                    return bundleRepresentative;
                }
            }

            CtTypeMember representative = effectiveDependents.isEmpty()
                    ? member
                    : Stream.concat(Stream.of(member), effectiveDependents.stream())
                            .min(typeMemberBaseComparator)
                            .orElse(member);

            representativeByMember.put(member, representative);
            return representative;
        }

        @NonNull
        private CtTypeMember resolveAccessorBundleRepresentative(@NonNull CtTypeMember member) {
            if (!keepAccessorsTogether) {
                return member;
            }

            CtTypeMember cached = accessorBundleRepresentativeByMember.get(member);
            if (cached != null) {
                return cached;
            }

            Set<CtTypeMember> bundleMembers = resolveAccessorBundleMembers(member);
            if (bundleMembers.size() <= 1) {
                accessorBundleRepresentativeByMember.put(member, member);
                return member;
            }

            CtTypeMember bundleRepresentative =
                    bundleMembers.stream().min(typeMemberBaseComparator).orElse(member);

            bundleMembers.forEach(
                    bundleMember -> accessorBundleRepresentativeByMember.put(bundleMember, bundleRepresentative));

            return bundleRepresentative;
        }

        @NonNull
        private Set<CtTypeMember> resolveAccessorBundleMembers(@NonNull CtTypeMember member) {
            if (!keepAccessorsTogether) {
                return Set.of(member);
            }

            Set<CtTypeMember> cached = accessorBundleMembersByMember.get(member);
            if (cached != null) {
                return cached;
            }

            LinkedHashSet<CtTypeMember> bundleMembers =
                    memberDependencyGraph.findTransitiveDependents(member, ACCESSOR_BUNDLE_ONLY).stream()
                            .filter(groupMemberSet::contains)
                            .collect(Collectors.toCollection(LinkedHashSet::new));

            bundleMembers.add(member);

            Set<CtTypeMember> result = Set.copyOf(bundleMembers);

            result.forEach(bundleMember -> accessorBundleMembersByMember.put(bundleMember, result));

            return result;
        }
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
        Set<@NonNull CtTypeMember> effectiveTransitiveDeclarationDependentsInGroup;

        int sourceStart;

        @NonNull
        String alphaKey;

        int visibilityRankAscending;

        int visibilityRankDescending;

        int identityHashCode;
    }
}

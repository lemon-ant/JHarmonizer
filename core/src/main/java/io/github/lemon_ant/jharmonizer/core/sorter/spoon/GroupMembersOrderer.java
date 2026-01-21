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
import lombok.NonNull;
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

        Comparator<CtTypeMember> baseComparator = buildBaseComparatorForGroup(compiledMemberGroup);
        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMembers);

        boolean keepAccessorsTogether = compiledMemberGroup.isKeepAccessorsTogether();

        Map<CtTypeMember, CtTypeMember> representativeByMember = buildRepresentativeByMember(
                groupMembers, groupMemberSet, memberDependencyGraph, baseComparator, keepAccessorsTogether);

        Comparator<CtTypeMember> groupComparator =
                buildGroupComparator(baseComparator, memberDependencyGraph, representativeByMember);

        return groupMembers.stream().sorted(groupComparator).toList();
    }

    @NonNull
    private static Map<CtTypeMember, CtTypeMember> buildRepresentativeByMember(
            @NonNull List<CtTypeMember> groupMembers,
            @NonNull Set<CtTypeMember> groupMemberSet,
            @NonNull MemberDependencyGraph memberDependencyGraph,
            @NonNull Comparator<CtTypeMember> baseComparator,
            boolean keepAccessorsTogether) {

        Map<CtTypeMember, CtTypeMember> representativeByMember = new LinkedHashMap<>();
        Set<CtTypeMember> visitedAccessorBundleMembers = new LinkedHashSet<>();

        for (CtTypeMember member : groupMembers) {
            if (representativeByMember.containsKey(member)) {
                continue;
            }

            // 1) Declaration provider representative: min({provider} ∪ transitiveDependents(provider)).
            Set<CtTypeMember> declarationClosure = new LinkedHashSet<>();
            declarationClosure.add(member);
            declarationClosure.addAll(
                    memberDependencyGraph.findTransitiveDependents(member, DECLARATION_DEPENDENCY_ONLY));
            declarationClosure.retainAll(groupMemberSet);

            boolean hasAnyDeclarationDependentInGroup = declarationClosure.size() > 1;
            if (hasAnyDeclarationDependentInGroup) {
                CtTypeMember providerRepresentative =
                        declarationClosure.stream().min(baseComparator).orElse(member);
                representativeByMember.put(member, providerRepresentative);
                continue;
            }

            // 2) Accessor bundle representative (only when enabled and only for bundle members).
            if (!keepAccessorsTogether || visitedAccessorBundleMembers.contains(member)) {
                representativeByMember.put(member, member);
                continue;
            }

            Set<CtTypeMember> accessorBundleMembers = new LinkedHashSet<>();
            accessorBundleMembers.add(member);
            accessorBundleMembers.addAll(memberDependencyGraph.findTransitiveDependents(member, ACCESSOR_BUNDLE_ONLY));
            accessorBundleMembers.retainAll(groupMemberSet);

            boolean isRealBundle = accessorBundleMembers.size() > 1;
            if (!isRealBundle) {
                representativeByMember.put(member, member);
                continue;
            }

            CtTypeMember bundleRepresentative =
                    accessorBundleMembers.stream().min(baseComparator).orElse(member);

            accessorBundleMembers.forEach(
                    bundleMember -> representativeByMember.put(bundleMember, bundleRepresentative));
            visitedAccessorBundleMembers.addAll(accessorBundleMembers);
        }

        // Ensure total mapping.
        groupMembers.forEach(member -> representativeByMember.putIfAbsent(member, member));

        return Map.copyOf(representativeByMember);
    }

    @NonNull
    private static Comparator<CtTypeMember> buildGroupComparator(
            @NonNull Comparator<CtTypeMember> baseComparator,
            @NonNull MemberDependencyGraph memberDependencyGraph,
            @NonNull Map<CtTypeMember, CtTypeMember> representativeByMember) {

        return (leftMember, rightMember) -> {
            // Comparator contract: same reference must be equal.
            if (leftMember == rightMember) {
                return 0;
            }

            boolean leftMustBeBeforeRight = memberDependencyGraph
                    .findTransitiveDependents(leftMember, DECLARATION_DEPENDENCY_ONLY)
                    .contains(rightMember);

            boolean rightMustBeBeforeLeft = memberDependencyGraph
                    .findTransitiveDependents(rightMember, DECLARATION_DEPENDENCY_ONLY)
                    .contains(leftMember);

            if (leftMustBeBeforeRight && !rightMustBeBeforeLeft) {
                return -1;
            }
            if (rightMustBeBeforeLeft && !leftMustBeBeforeRight) {
                return 1;
            }

            // Corner case: cycles in declaration dependencies. We must not break antisymmetry.
            // TODO: move cycle handling into MemberDependencyGraph.
            if (leftMustBeBeforeRight && rightMustBeBeforeLeft) {
                int cycleFallback = baseComparator.compare(leftMember, rightMember);
                if (cycleFallback != 0) {
                    return cycleFallback;
                }
                return Integer.compare(System.identityHashCode(leftMember), System.identityHashCode(rightMember));
            }

            CtTypeMember leftRepresentative = representativeByMember.getOrDefault(leftMember, leftMember);
            CtTypeMember rightRepresentative = representativeByMember.getOrDefault(rightMember, rightMember);

            if (leftRepresentative != rightRepresentative) {
                int representativeComparison = baseComparator.compare(leftRepresentative, rightRepresentative);
                if (representativeComparison != 0) {
                    return representativeComparison;
                }
            }

            int directComparison = baseComparator.compare(leftMember, rightMember);
            if (directComparison != 0) {
                return directComparison;
            }

            // Last-resort deterministic tie-breaker (should be very rare).
            return Integer.compare(System.identityHashCode(leftMember), System.identityHashCode(rightMember));
        };
    }

    @NonNull
    private static Comparator<CtTypeMember> buildBaseComparatorForGroup(CompiledMemberGroup compiledMemberGroup) {
        List<SortKey> sortKeys = compiledMemberGroup.getSortKeys();

        Comparator<CtTypeMember> configuredComparator = sortKeys.stream()
                .map(GroupMembersOrderer::buildComparatorForSortKey)
                .reduce(Comparator::thenComparing)
                .orElseGet(() -> Comparator.comparingInt(SpoonTypeMemberUtils::extractSourceStart)
                        .thenComparing(SpoonTypeMemberUtils::deriveAlphaKey));

        // Deterministic tie-breakers regardless of configured keys.
        if (!sortKeys.contains(SortKey.PRESERVE)) {
            configuredComparator = configuredComparator.thenComparingInt(SpoonTypeMemberUtils::extractSourceStart);
        }
        if (!sortKeys.contains(SortKey.ALPHA)) {
            configuredComparator = configuredComparator.thenComparing(SpoonTypeMemberUtils::deriveAlphaKey);
        }
        return configuredComparator;
    }

    @NonNull
    private static Comparator<CtTypeMember> buildComparatorForSortKey(@NonNull SortKey sortKey) {
        return switch (sortKey) {
            case PRESERVE -> Comparator.comparingInt(SpoonTypeMemberUtils::extractSourceStart);
            case ALPHA -> Comparator.comparing(SpoonTypeMemberUtils::deriveAlphaKey);
            case VISIBILITY_ASC -> Comparator.comparingInt(SpoonTypeMemberUtils::deriveVisibilityRankAscending);
            case VISIBILITY_DESC -> Comparator.comparingInt(SpoonTypeMemberUtils::deriveVisibilityRankDescending);
        };
    }

    @NonNull
    private static String describeTypeMember(@NonNull CtTypeMember typeMember) {
        return typeMember.getClass().getSimpleName()
                + "{signature=" + SpoonTypeMemberUtils.deriveAlphaKey(typeMember)
                + ",sourceStart=" + SpoonTypeMemberUtils.extractSourceStart(typeMember)
                + "}";
    }
}

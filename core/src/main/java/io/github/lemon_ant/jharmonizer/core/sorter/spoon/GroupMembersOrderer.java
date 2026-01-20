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

        Comparator<CtTypeMember> baseComparator = buildBaseComparatorForGroup(compiledMemberGroup);
        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMembers);

        boolean keepAccessorsTogether = compiledMemberGroup.isKeepAccessorsTogether();

        AccessorBundleLookup accessorBundleLookup = keepAccessorsTogether
                ? buildAccessorBundleLookup(groupMembers, groupMemberSet, memberDependencyGraph, baseComparator)
                : AccessorBundleLookup.identity(groupMembers);

        Map<CtTypeMember, CtTypeMember> representativeByMember = buildRepresentativeByMember(
                groupMembers,
                groupMemberSet,
                memberDependencyGraph,
                accessorBundleLookup.getBundleRepresentativeByMember(),
                baseComparator);

        Comparator<CtTypeMember> groupComparator =
                buildGroupComparator(baseComparator, memberDependencyGraph, representativeByMember);

        return groupMembers.stream().sorted(groupComparator).toList();
    }

    @NonNull
    private static AccessorBundleLookup buildAccessorBundleLookup(
            @NonNull List<CtTypeMember> groupMembers,
            @NonNull Set<CtTypeMember> groupMemberSet,
            @NonNull MemberDependencyGraph memberDependencyGraph,
            @NonNull Comparator<CtTypeMember> baseComparator) {

        Map<CtTypeMember, CtTypeMember> bundleRepresentativeByMember = new LinkedHashMap<>();
        Set<CtTypeMember> visitedMembers = new LinkedHashSet<>();

        for (CtTypeMember member : groupMembers) {
            if (visitedMembers.contains(member)) {
                continue;
            }

            Set<CtTypeMember> bundleMembers = new LinkedHashSet<>();
            bundleMembers.add(member);

            // ACCESSOR_BUNDLE edges are expected to be bidirectional.
            // We still use transitive traversal to be robust if the bundle is not a "clique".
            bundleMembers.addAll(memberDependencyGraph.findTransitiveDependents(member, ACCESSOR_BUNDLE_ONLY));
            bundleMembers.retainAll(groupMemberSet);

            visitedMembers.addAll(bundleMembers);

            CtTypeMember bundleRepresentative =
                    bundleMembers.stream().min(baseComparator).orElse(member);

            bundleMembers.forEach(bundleMember -> bundleRepresentativeByMember.put(bundleMember, bundleRepresentative));
        }

        // Ensure identity mapping for members that are not in any accessor bundle.
        groupMembers.forEach(member -> bundleRepresentativeByMember.putIfAbsent(member, member));

        return new AccessorBundleLookup(Map.copyOf(bundleRepresentativeByMember));
    }

    @NonNull
    private static Map<CtTypeMember, CtTypeMember> buildRepresentativeByMember(
            @NonNull List<CtTypeMember> groupMembers,
            @NonNull Set<CtTypeMember> groupMemberSet,
            @NonNull MemberDependencyGraph memberDependencyGraph,
            @NonNull Map<CtTypeMember, CtTypeMember> bundleRepresentativeByMember,
            @NonNull Comparator<CtTypeMember> baseComparator) {

        // Representative rules:
        // 1) Accessor bundle members share the same representative (the "first" member of the bundle).
        // 2) A declaration provider is represented by the earliest element (by base comparator) from:
        //    {provider} ∪ transitiveDependents(provider, DECLARATION_DEPENDENCY).
        return Map.copyOf(groupMembers.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        member -> {
                            CtTypeMember accessorBundleRepresentative =
                                    bundleRepresentativeByMember.getOrDefault(member, member);

                            // Accessor bundling has priority and is orthogonal to declaration dependencies.
                            if (accessorBundleRepresentative != member) {
                                return accessorBundleRepresentative;
                            }

                            Set<CtTypeMember> declarationClosure = new LinkedHashSet<>();
                            declarationClosure.add(member);
                            declarationClosure.addAll(memberDependencyGraph.findTransitiveDependents(
                                    member, DECLARATION_DEPENDENCY_ONLY));
                            declarationClosure.retainAll(groupMemberSet);

                            return declarationClosure.stream()
                                    .min(baseComparator)
                                    .orElse(member);
                        },
                        (existingValue, newValue) -> {
                            throw new IllegalStateException(
                                    "Unexpected duplicate member while building member->representative map. member="
                                            + describeTypeMember(existingValue));
                        },
                        LinkedHashMap::new)));
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
            // Fall back to deterministic base ordering.
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

    @Value
    private static class AccessorBundleLookup {

        @NonNull
        Map<@NonNull CtTypeMember, @NonNull CtTypeMember> bundleRepresentativeByMember;

        static AccessorBundleLookup identity(@NonNull List<CtTypeMember> members) {
            Map<CtTypeMember, CtTypeMember> identityMapping = members.stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            Function.identity(),
                            (existingValue, newValue) -> {
                                throw new IllegalStateException(
                                        "Unexpected duplicate member while building identity mapping.");
                            },
                            LinkedHashMap::new));
            return new AccessorBundleLookup(Map.copyOf(identityMapping));
        }
    }
}

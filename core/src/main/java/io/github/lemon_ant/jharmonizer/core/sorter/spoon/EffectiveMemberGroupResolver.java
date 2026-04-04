package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Effective-group resolver based on transitive dependents.
 * <p>
 * effectiveGroup(provider) = min(
 * naturalGroup(provider),
 * naturalGroup(dependent) for all transitive dependents(provider)
 * )
 * <p>
 * Since {@link MemberDependencyGraph#findTransitiveDependents(CtTypeMember)} already provides transitive closure,
 * a single pass is sufficient.
 */
@UtilityClass
class EffectiveMemberGroupResolver {

    /**
     * Resolves the effective groups.
     * @param typeMember2NaturalMemberGroup the type member2 natural member group
     * @param memberDependencyGraph the member dependency graph
     * @return the effective groups
     */
    @NonNull
    Map<@NonNull CtTypeMember, @NonNull CompiledMemberGroup> resolveEffectiveGroups(
            @NonNull Map<@NonNull CtTypeMember, @NonNull CompiledMemberGroup> typeMember2NaturalMemberGroup,
            @NonNull MemberDependencyGraph memberDependencyGraph) {
        return typeMember2NaturalMemberGroup.keySet().stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                providerMember -> providerMember,
                                providerMember -> resolveEffectiveGroupForProvider(
                                        providerMember, typeMember2NaturalMemberGroup, memberDependencyGraph),
                                (existingGroup, newGroup) -> {
                                    throw new IllegalStateException(
                                            "Unexpected duplicate CtTypeMember key while building "
                                                    + "effective group mapping. This indicates a bug in the collector setup.");
                                },
                                LinkedHashMap::new),
                        Collections::unmodifiableMap));
    }

    @NonNull
    private static CompiledMemberGroup resolveEffectiveGroupForProvider(
            CtTypeMember providerMember,
            Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup,
            MemberDependencyGraph memberDependencyGraph) {

        Set<CtTypeMember> transitiveDependents = memberDependencyGraph.findTransitiveDependents(providerMember);

        return Stream.concat(Stream.of(providerMember), transitiveDependents.stream())
                .map(typeMember -> requireNaturalMemberGroup(typeMember2NaturalMemberGroup, providerMember, typeMember))
                .reduce(EffectiveMemberGroupResolver::selectEarlierGroup)
                .orElseThrow(() -> new IllegalStateException(
                        "Expected at least the provider member to be present when resolving effective group. "
                                + "providerMember=" + describeTypeMember(providerMember)));
    }

    @NonNull
    private static CompiledMemberGroup requireNaturalMemberGroup(
            Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup,
            CtTypeMember providerMember,
            CtTypeMember referencedMember) {

        CompiledMemberGroup naturalMemberGroup = typeMember2NaturalMemberGroup.get(referencedMember);
        if (naturalMemberGroup == null) {
            throw new IllegalStateException(
                    "Expected every CtTypeMember to be mapped to a natural CompiledMemberGroup, "
                            + "but it was missing. missingMember=" + describeTypeMember(referencedMember)
                            + ", providerMember=" + describeTypeMember(providerMember));
        }

        return naturalMemberGroup;
    }

    @NonNull
    private static String describeTypeMember(CtTypeMember typeMember) {
        return typeMember.getClass().getSimpleName() + "(" + typeMember.getShortRepresentation() + ")";
    }

    @NonNull
    private static CompiledMemberGroup selectEarlierGroup(
            CompiledMemberGroup leftGroup, CompiledMemberGroup rightGroup) {
        return leftGroup.getOrderIndex() <= rightGroup.getOrderIndex() ? leftGroup : rightGroup;
    }
}

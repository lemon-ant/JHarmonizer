package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Builds a directed dependency graph between members of a single {@link CtType}.
 */
@UtilityClass
public class MemberDependencyGraphBuilder {

    private static final Collection<@NonNull MemberDependencyProvider> memberDependencyProviders = List.of(
            new AccessorPairDependencyProvider(),
            new EnumConstantInitializerDependencyProvider(),
            new BlankFinalDefiniteAssignmentDependencyProvider(),
            new FieldInitializerBackwardReferenceDependencyProvider(),
            new ExplicitThisInitializerFieldDependencyProvider(),
            new ExplicitDeclaringTypeInitializerFieldDependencyProvider(),
            new InitializerBlockDependencyProvider());

    /**
     * Performs the build dependency graph.
     * @param typeMember2NaturalMemberGroup the type member2 natural member group
     * @return the result
     */
    @NonNull
    public static MemberDependencyGraph buildDependencyGraph(
            @NonNull Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup) {

        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();

        // typeMember2NaturalMemberGroup is expected to contain only explicit source members.
        typeMember2NaturalMemberGroup.keySet().stream()
                .sorted(Comparator.comparingInt(MemberDependencyGraphBuilder::extractSourceStart)
                        .thenComparing(MemberDependencyGraphBuilder::deriveStableMemberKey))
                .forEach(dependentMember -> {
                    CompiledMemberGroup dependentNaturalGroup =
                            resolveNaturalGroupOrThrow(dependentMember, typeMember2NaturalMemberGroup);

                    boolean keepAccessorsTogether = dependentNaturalGroup.isKeepAccessorsTogether();

                    memberDependencyProviders.stream()
                            .flatMap(memberDependencyProvider ->
                                    memberDependencyProvider
                                            .findDirectProviderEdges(dependentMember, keepAccessorsTogether)
                                            .stream())
                            .forEach(providerEdge -> memberDependencyGraph.addEdge(
                                    providerEdge.getAdjacentMember(), dependentMember, providerEdge.getEdgeKind()));
                });

        return memberDependencyGraph;
    }

    private static int extractSourceStart(CtTypeMember typeMember) {
        if (typeMember.getPosition() == null || !typeMember.getPosition().isValidPosition()) {
            return Integer.MAX_VALUE;
        }
        return typeMember.getPosition().getSourceStart();
    }

    @NonNull
    private static String deriveStableMemberKey(CtTypeMember typeMember) {
        return String.valueOf(typeMember.getShortRepresentation());
    }

    @NonNull
    private static CompiledMemberGroup resolveNaturalGroupOrThrow(
            CtTypeMember typeMember, Map<CtTypeMember, CompiledMemberGroup> typeMember2CompiledMemberGroup) {

        return Optional.ofNullable(typeMember2CompiledMemberGroup.get(typeMember))
                .orElseThrow(() -> new IllegalStateException("Natural group was not resolved for type member. "
                        + "Expected typeMember2CompiledMemberGroup to contain all explicit (source) type members. "
                        + "Missing member: " + typeMember));
    }
}

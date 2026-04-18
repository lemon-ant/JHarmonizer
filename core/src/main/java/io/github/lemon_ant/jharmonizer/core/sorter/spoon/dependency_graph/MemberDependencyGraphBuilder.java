package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import java.util.Collection;
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
            new InitializerBlockDependencyProvider(),
            new InitializerBlockMutableFieldReadDependencyProvider(),
            new CrossTypeConstantBackRefDependencyProvider());

    /**
     * Builds a dependency graph using each member's natural-group configuration.
     *
     * @param typeMember2NaturalMemberGroup maps each explicit source member to its natural member group
     * @return the populated dependency graph
     */
    @NonNull
    public static MemberDependencyGraph buildDependencyGraph(
            @NonNull Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup) {
        return buildDependencyGraph(typeMember2NaturalMemberGroup, false);
    }

    /**
     * Builds a dependency graph, optionally overriding all groups to use relaxed forward-reference mode.
     *
     * <p>When {@code forceRelaxedForwardReferences} is {@code true}, every member's
     * {@link DependencyDetectorConfig#isRelaxedForwardReferences()} is forced to {@code true}
     * regardless of its natural-group setting.  This is used as a fallback when the strict-mode
     * graph produces a dependency cycle that would otherwise cause a {@link io.github.lemon_ant.jharmonizer.sorting.SortingException}.
     *
     * @param typeMember2NaturalMemberGroup  maps each explicit source member to its natural member group
     * @param forceRelaxedForwardReferences  when {@code true}, overrides relaxed-forward-references to {@code true}
     *                                       for every member regardless of the group configuration
     * @return the populated dependency graph
     */
    @NonNull
    public static MemberDependencyGraph buildDependencyGraph(
            @NonNull Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup,
            boolean forceRelaxedForwardReferences) {

        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();

        // typeMember2NaturalMemberGroup is expected to contain only explicit source members.
        typeMember2NaturalMemberGroup.keySet().forEach(dependentMember -> {
            CompiledMemberGroup dependentNaturalGroup =
                    resolveNaturalGroupOrThrow(dependentMember, typeMember2NaturalMemberGroup);

            DependencyDetectorConfig detectorConfig = new DependencyDetectorConfig(
                    dependentNaturalGroup.isKeepAccessorsTogether(),
                    forceRelaxedForwardReferences || dependentNaturalGroup.isRelaxedForwardReferences());

            memberDependencyProviders.stream()
                    .flatMap(memberDependencyProvider ->
                            memberDependencyProvider.findDirectProviderEdges(dependentMember, detectorConfig).stream())
                    .forEach(providerEdge -> memberDependencyGraph.addEdge(
                            providerEdge.getAdjacentMember(), dependentMember, providerEdge.getEdgeKind()));
        });

        return memberDependencyGraph;
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

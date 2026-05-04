// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.sorting.SortingException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Builds a directed dependency graph between members of a single {@link CtType}.
 *
 * <p>When any member group is configured for strict forward-reference mode
 * ({@code relaxedForwardReferences = false}) and the resulting graph contains a dependency cycle,
 * this builder automatically retries with all groups forced to relaxed mode.  If the relaxed graph
 * is also cyclic, a {@link SortingException} is thrown.
 */
@Slf4j
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
     * <p>If any member group uses strict forward-reference mode and the resulting graph has a
     * declaration-dependency cycle, the graph is rebuilt with all groups forced to relaxed mode.
     * If the relaxed graph is still cyclic, a {@link SortingException} is thrown.
     *
     * @param typeMember2NaturalMemberGroup maps each explicit source member to its natural member group
     * @return the acyclic populated dependency graph
     * @throws SortingException if a dependency cycle exists that cannot be resolved by falling back to relaxed mode
     */
    @NonNull
    @SuppressWarnings("PMD.GuardLogStatement")
    public static MemberDependencyGraph buildDependencyGraph(
            @NonNull Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup) {

        MemberDependencyGraph memberDependencyGraph =
                buildDependencyGraphInternal(typeMember2NaturalMemberGroup, false);

        List<CtTypeMember> strictCyclePath = memberDependencyGraph.findDeclarationDependencyCyclePath();
        if (strictCyclePath.isEmpty()) {
            return memberDependencyGraph;
        }

        boolean anyGroupUsesStrictMode = typeMember2NaturalMemberGroup.values().stream()
                .anyMatch(memberGroup -> !memberGroup.isRelaxedForwardReferences());
        if (!anyGroupUsesStrictMode) {
            throw new SortingException(buildCycleErrorMessage(strictCyclePath));
        }

        log.warn(
                "Dependency cycle detected in strict forward-reference mode for type '{}': {}. "
                        + "Retrying dependency analysis with relaxed forward references.",
                requireDeclaringTypeName(strictCyclePath.get(0)),
                formatCyclePath(strictCyclePath));

        MemberDependencyGraph relaxedDependencyGraph =
                buildDependencyGraphInternal(typeMember2NaturalMemberGroup, true);
        List<CtTypeMember> relaxedCyclePath = relaxedDependencyGraph.findDeclarationDependencyCyclePath();
        if (!relaxedCyclePath.isEmpty()) {
            throw new SortingException(buildCycleErrorMessage(relaxedCyclePath));
        }

        return relaxedDependencyGraph;
    }

    @NonNull
    private static MemberDependencyGraph buildDependencyGraphInternal(
            @NonNull Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup,
            boolean forceRelaxedForwardReferences) {

        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();

        // typeMember2NaturalMemberGroup is expected to contain only explicit source members.
        typeMember2NaturalMemberGroup.keySet().forEach(dependentMember -> {
            CompiledMemberGroup dependentNaturalGroup =
                    resolveNaturalGroupOrThrow(dependentMember, typeMember2NaturalMemberGroup);

            MemberDependencyProvider.ProviderConfig providerConfig = new MemberDependencyProvider.ProviderConfig(
                    dependentNaturalGroup.isKeepAccessorsTogether(),
                    forceRelaxedForwardReferences || dependentNaturalGroup.isRelaxedForwardReferences());

            memberDependencyProviders.stream()
                    .flatMap(memberDependencyProvider ->
                            memberDependencyProvider.findDirectProviderEdges(dependentMember, providerConfig).stream())
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

    @NonNull
    private static String requireDeclaringTypeName(CtTypeMember typeMember) {
        CtType<?> declaringType = typeMember.getDeclaringType();
        if (declaringType == null) {
            throw new IllegalStateException(
                    "Expected type member to have a declaring type, but it was null. Member: " + typeMember);
        }
        return declaringType.getQualifiedName();
    }

    @NonNull
    private static String buildCycleErrorMessage(List<CtTypeMember> cyclePath) {
        return "Dependency cycle detected among members of type '"
                + requireDeclaringTypeName(cyclePath.get(0))
                + "': "
                + formatCyclePath(cyclePath);
    }

    @NonNull
    private static String formatCyclePath(List<CtTypeMember> cyclePath) {
        return cyclePath.stream().map(CtTypeMember::getSimpleName).collect(Collectors.joining(" -> "));
    }
}

package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides declaration dependencies for earlier same-type compile-time constants referenced through implicit/simple
 * field access in initializers.
 */
final class ImplicitCompileTimeConstantDeclarationDependencyProvider implements MemberDependencyProvider {

    /**
     * Finds declaration dependency edges for earlier compile-time constants referenced through implicit/simple-name
     * field access in the dependent member's initializer.
     *
     * <p>Explicit declaring-type qualified constant reads remain reorderable, but implicit same-type reads can become
     * illegal forward references if the constant provider moves below the dependent member.
     *
     * @param dependentMember the dependent member
     * @param keepAccessorsTogether ignored interface flag; compile-time constant declaration dependencies do not depend
     *                              on accessor bundling
     * @return the matching direct provider edges
     */
    @NonNull
    @Override
    public Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {
        return InitializationOrderDependencyUtils.resolveInitializationAstRoot(dependentMember)
                .map(initializationAstRoot -> DeclaringTypeFieldReferenceUtils
                        .findImplicitCompileTimeConstantProvidersRequiredByDependentMember(
                                dependentMember, initializationAstRoot)
                        .stream()
                        .map(providerMember -> new MemberDependencyArc(
                                providerMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                        .collect(Collectors.toUnmodifiableSet()))
                .orElseGet(Set::of);
    }
}

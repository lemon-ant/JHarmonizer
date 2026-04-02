package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides ordering-only constraints for earlier same-type compile-time constants referenced through implicit/simple
 * field access in initializers.
 *
 * @see MemberDependencyEdgeKind#SOURCE_ORDER_CONSTRAINT
 */
final class ImplicitCompileTimeConstantOrderConstraintProvider implements MemberDependencyProvider {

    /**
     * Finds ordering-only edges for earlier compile-time constants referenced through implicit/simple-name field
     * access in the dependent member's initializer.
     *
     * <p>These edges are separate from general declaration dependencies because explicit declaring-type qualified
     * constant reads remain reorderable, while implicit same-type reads can become illegal forward references if the
     * constant provider moves below the dependent member.
     *
     * @param dependentMember the dependent member
     * @param keepAccessorsTogether ignored interface flag; compile-time constant source-order constraints do not depend
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
                                providerMember, MemberDependencyEdgeKind.SOURCE_ORDER_CONSTRAINT))
                        .collect(Collectors.toUnmodifiableSet()))
                .orElseGet(Set::of);
    }
}

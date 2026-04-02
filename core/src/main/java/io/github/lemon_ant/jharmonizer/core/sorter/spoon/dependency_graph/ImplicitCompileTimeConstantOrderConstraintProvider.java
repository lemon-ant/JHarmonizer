package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides ordering-only constraints for earlier same-type compile-time constants referenced through implicit/simple
 * field access in initializers.
 */
final class ImplicitCompileTimeConstantOrderConstraintProvider implements MemberDependencyProvider {

    /**
     * Finds the direct provider edges.
     * @param dependentMember the dependent member
     * @param keepAccessorsTogether the keep accessors together
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

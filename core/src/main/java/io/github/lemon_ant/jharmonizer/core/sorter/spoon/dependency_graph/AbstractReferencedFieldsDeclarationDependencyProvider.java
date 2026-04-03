package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Base provider for initializer-like members that produce {@link MemberDependencyEdgeKind#DECLARATION_DEPENDENCY}
 * edges based on order-dependent field references.
 */
abstract class AbstractReferencedFieldsDeclarationDependencyProvider implements MemberDependencyProvider {

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

        Optional<CtElement> dependentInitializationAst = resolveDependentInitializationAst(dependentMember);
        return dependentInitializationAst
                .map(ctElement ->
                        DeclaringTypeFieldReferenceUtils.findProviderFieldsRequiredByDependentMember(
                                        dependentMember, ctElement)
                                .stream()
                                .filter(providerMember ->
                                        !InitializationOrderDependencyUtils.isStaticCompileTimeConstantVariable(
                                                providerMember))
                                .map(providerMember -> new MemberDependencyArc(
                                        providerMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                                .collect(Collectors.toUnmodifiableSet()))
                .orElseGet(Set::of);
    }

    /**
     * Resolves the dependent member's initialization AST that should be scanned for order-dependent field references.
     *
     * <p>Examples include a field or enum-value default expression and an initializer-block body.
     * If the member has no initialization AST or is not supported, implementations must return
     * {@link Optional#empty()}.
     */
    @NonNull
    protected abstract Optional<CtElement> resolveDependentInitializationAst(@NonNull CtTypeMember dependentMember);
}

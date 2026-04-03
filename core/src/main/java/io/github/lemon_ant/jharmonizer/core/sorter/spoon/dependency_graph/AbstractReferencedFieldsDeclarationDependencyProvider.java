package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils.ReferencedFieldAccess;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
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

        Optional<CtElement> dependentAstRoot = resolveDependentAstRoot(dependentMember);
        return dependentAstRoot
                .map(ctElement ->
                        DeclaringTypeFieldReferenceUtils.findReferencedFieldAccessesDeclaredBeforeMember(
                                        dependentMember, ctElement)
                                .stream()
                                .filter(this::isNonConstantFieldAccessOrImplicitConstantAccess)
                                .map(ReferencedFieldAccess::getProviderField)
                                .map(providerField -> new MemberDependencyArc(
                                        providerField, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                                .collect(Collectors.toUnmodifiableSet()))
                .orElseGet(Set::of);
    }

    /**
     * Returns whether the access is either a non-constant field access or an implicit compile-time-constant access.
     *
     * @param referencedFieldAccess the referenced field access
     * @return {@code true} when the access matches the allowed declaration-dependency shapes; otherwise {@code false}
     */
    private boolean isNonConstantFieldAccessOrImplicitConstantAccess(
            @NonNull ReferencedFieldAccess referencedFieldAccess) {
        return !isStaticCompileTimeConstantProviderField(referencedFieldAccess)
                || isImplicitFieldAccess(referencedFieldAccess);
    }

    private boolean isStaticCompileTimeConstantProviderField(@NonNull ReferencedFieldAccess referencedFieldAccess) {
        CtField<?> providerField = referencedFieldAccess.getProviderField();
        return InitializationOrderDependencyUtils.isStaticCompileTimeConstantVariable(providerField);
    }

    /**
     * Returns whether the field access is implicit/simple-name.
     *
     * @param referencedFieldAccess the referenced field access
     * @return {@code true} for implicit/simple-name accesses; otherwise {@code false}
     */
    private boolean isImplicitFieldAccess(@NonNull ReferencedFieldAccess referencedFieldAccess) {
        // Java allows qualified forward reads of compile-time constants, but same-type simple-name reads can become
        // illegal forward references after reordering, so only implicit accesses must keep declaration dependencies.
        return referencedFieldAccess.getFieldAccess().getTarget() == null
                || referencedFieldAccess.getFieldAccess().getTarget().isImplicit();
    }

    /**
     * Returns the AST root that should be scanned for order-dependent field references.
     *
     * <p>If the member is not supported, implementations must return {@link Optional#empty()}.
     */
    @NonNull
    protected abstract Optional<CtElement> resolveDependentAstRoot(@NonNull CtTypeMember dependentMember);
}

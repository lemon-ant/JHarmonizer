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

    @NonNull
    @Override
    public Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {

        Optional<CtElement> dependentAstRoot = resolveDependentAstRoot(dependentMember);
        return dependentAstRoot
                .map(ctElement ->
                        OrderDependentFieldReferenceUtils.findProviderFieldsRequiredByDependentMember(dependentMember, ctElement).stream()
                                .map(providerMember -> new MemberDependencyArc(
                                        providerMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                                .collect(Collectors.toUnmodifiableSet()))
                .orElseGet(Set::of);
    }

    /**
     * Returns the AST root that should be scanned for order-dependent field references.
     *
     * <p>If the member is not supported, implementations must return {@link Optional#empty()}.
     */
    @NonNull
    protected abstract Optional<CtElement> resolveDependentAstRoot(@NonNull CtTypeMember dependentMember);
}

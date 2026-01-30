package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.OrderDependentFieldReferenceUtils.requireDeclaringType;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides declaration dependency edges caused by field initializers.
 *
 * <p>If fieldB initializer references fieldA, then fieldA -> fieldB.
 */
final class FieldInitializerDependencyProvider implements MemberDependencyProvider {

    @NonNull
    @Override
    public Set<@NonNull ProviderEdge> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {
        if (!(dependentMember instanceof CtField<?> dependentField)) {
            return Set.of();
        }

        if (dependentField.getDefaultExpression() == null) {
            return Set.of();
        }

        CtType<?> declaringType = requireDeclaringType(dependentField);

        return OrderDependentFieldReferenceUtils.findReferencedFields(
                        dependentField.getDefaultExpression(), declaringType)
                .stream()
                .map(referencedField ->
                        new ProviderEdge(referencedField, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }
}

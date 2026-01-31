package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides declaration dependency edges caused by field initializers.
 *
 * <p>If fieldB initializer references fieldA, then fieldA -> fieldB.
 */
final class FieldInitializerDependencyProvider implements MemberDependencyProvider {

    @NonNull
    @Override
    public Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {
        if (!(dependentMember instanceof CtField<?> dependentField)) {
            return Set.of();
        }

        if (dependentField.getDefaultExpression() == null) {
            return Set.of();
        }

        return OrderDependentFieldReferenceUtils.findReferencedFields(
                        dependentMember, dependentField.getDefaultExpression())
                .stream()
                .map(referencedField ->
                        new MemberDependencyArc(referencedField, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }
}

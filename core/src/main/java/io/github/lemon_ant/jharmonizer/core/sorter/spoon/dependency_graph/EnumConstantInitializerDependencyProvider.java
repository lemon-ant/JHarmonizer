package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides declaration-order dependencies originating from enum constant initializers.
 *
 * <p>Enum constants are effectively static fields initialized in source order.
 * Any (order-dependent) field access from an enum constant initializer must not be reordered past
 * its provider.
 */
final class EnumConstantInitializerDependencyProvider implements MemberDependencyProvider {

    @NonNull
    @Override
    public Set<MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {
        if (!(dependentMember instanceof CtEnumValue<?> dependentEnumValue)) {
            return Set.of();
        }

        CtExpression<?> enumValueInitializerExpression = dependentEnumValue.getDefaultExpression();
        if (enumValueInitializerExpression == null) {
            return Set.of();
        }

        return OrderDependentFieldReferenceUtils.findReferencedFields(dependentMember, enumValueInitializerExpression)
                .stream()
                .map(referencedField ->
                        new MemberDependencyArc(referencedField, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }
}

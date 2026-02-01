package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Optional;
import lombok.NonNull;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides declaration-order dependencies originating from enum constant initializers.
 *
 * <p>Enum constants are effectively static fields initialized in source order.
 * Any (order-dependent) field access from an enum constant initializer must not be reordered past
 * its provider.
 */
final class EnumConstantInitializerDependencyProvider extends AbstractReferencedFieldsDeclarationDependencyProvider {

    @NonNull
    @Override
    protected Optional<CtElement> resolveDependentAstRoot(@NonNull CtTypeMember dependentMember) {
        if (!(dependentMember instanceof CtEnumValue<?> dependentEnumValue)) {
            return Optional.empty();
        }

        return Optional.ofNullable(dependentEnumValue.getDefaultExpression());
    }
}

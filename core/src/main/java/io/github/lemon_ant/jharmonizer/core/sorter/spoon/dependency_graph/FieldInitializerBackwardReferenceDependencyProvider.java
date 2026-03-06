package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Optional;
import lombok.NonNull;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides declaration dependency edges caused by regular field initializer references resolved by
 * {@link DeclaringTypeFieldReferenceUtils#findReferencedFields(CtTypeMember, CtElement)}.
 *
 * <p>Explicit {@code this.<field>} forward-reference handling is delegated to
 * {@link ExplicitThisInitializerFieldDependencyProvider}.
 */
final class FieldInitializerBackwardReferenceDependencyProvider
        extends AbstractReferencedFieldsDeclarationDependencyProvider {

    @NonNull
    @Override
    protected Optional<CtElement> resolveDependentAstRoot(@NonNull CtTypeMember dependentMember) {
        if (!(dependentMember instanceof CtField<?> dependentField)) {
            return Optional.empty();
        }

        return Optional.ofNullable(dependentField.getDefaultExpression());
    }
}

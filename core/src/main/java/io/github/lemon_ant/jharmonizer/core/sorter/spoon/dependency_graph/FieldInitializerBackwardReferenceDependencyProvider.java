// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Optional;
import lombok.NonNull;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides declaration dependency edges caused by regular field initializer references resolved by
 * {@link DeclaringTypeFieldReferenceUtils#findReferencedFieldAccesses(CtTypeMember, CtElement, boolean)}.
 *
 * <p>Explicit {@code this.<field>} forward-reference handling is delegated to
 * {@link ExplicitThisInitializerFieldDependencyProvider}.
 */
final class FieldInitializerBackwardReferenceDependencyProvider
        extends AbstractReferencedFieldsDeclarationDependencyProvider {

    /**
     * Resolves the field initialization AST that should be scanned for field references.
     * @param dependentMember the dependent member
     * @return the field initializer expression when the member is a field
     */
    @NonNull
    @Override
    protected Optional<CtElement> resolveDependentInitializationAst(@NonNull CtTypeMember dependentMember) {
        if (!(dependentMember instanceof CtField<?> dependentField)) {
            return Optional.empty();
        }

        return Optional.ofNullable(dependentField.getDefaultExpression());
    }
}

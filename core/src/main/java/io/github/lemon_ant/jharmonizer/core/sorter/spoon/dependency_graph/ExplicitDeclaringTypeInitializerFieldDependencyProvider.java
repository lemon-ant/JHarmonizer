package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.OrderDependentFieldReferenceUtils.requireDeclaringType;

import lombok.NonNull;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;

/**
 * Provides declaration dependencies created by explicit {@code <DeclaringType>.<field>} references in static field
 * initializers.
 */
final class ExplicitDeclaringTypeInitializerFieldDependencyProvider
        extends AbstractExplicitInitializerForwardReferenceDependencyProvider {

    private static boolean isStaticField(@NonNull CtField<?> field) {
        return field.getModifiers().contains(ModifierKind.STATIC);
    }

    @Override
    protected boolean isSupportedReferencedField(@NonNull CtField<?> referencedField) {
        return isStaticField(referencedField);
    }

    @Override
    protected boolean isSupportedReferrerField(@NonNull CtField<?> referrerField) {
        return isStaticField(referrerField);
    }

    @Override
    protected boolean hasExplicitReferenceTo(@NonNull CtField<?> referrerField, @NonNull CtField<?> referencedField) {
        CtType<?> referrerDeclaringType = requireDeclaringType(referrerField);

        return hasExplicitQualifiedReferenceTo(
                referrerField,
                referencedField,
                (fieldAccess, ignoredDeclaringType) ->
                        isExplicitDeclaringTypeQualifiedAccess(fieldAccess, referrerDeclaringType));
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean isExplicitDeclaringTypeQualifiedAccess(
            CtFieldAccess<?> fieldAccess, CtType<?> declaringType) {
        if (!(fieldAccess.getTarget() instanceof CtTypeAccess<?> typeAccess) || typeAccess.isImplicit()) {
            return false;
        }

        return typeAccess.getAccessedType() != null
                && typeAccess.getAccessedType().getTypeDeclaration() == declaringType;
    }
}

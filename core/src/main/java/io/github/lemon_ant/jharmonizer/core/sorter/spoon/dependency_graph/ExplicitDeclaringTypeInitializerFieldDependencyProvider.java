package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils.requireDeclaringType;

import lombok.NonNull;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;

/**
 * Provides declaration dependencies created by explicit {@code <DeclaringType>.<field>} references in static field
 * initializers.
 */
final class ExplicitDeclaringTypeInitializerFieldDependencyProvider
        extends AbstractExplicitInitializerForwardReferenceDependencyProvider {

    /**
     * Returns whether is supported referenced field.
     * @param referencedField the referenced field
     * @return {@code true} if is supported referenced field; otherwise {@code false}
     */
    @Override
    protected boolean isSupportedReferencedField(@NonNull CtField<?> referencedField) {
        return !InitializationOrderDependencyUtils.isStaticCompileTimeConstantVariable(referencedField);
    }

    /**
     * Returns whether is supported referrer field.
     * @param referrerField the referrer field
     * @return {@code true} if is supported referrer field; otherwise {@code false}
     */
    @Override
    protected boolean isSupportedReferrerField(@NonNull CtField<?> referrerField) {
        return isStaticField(referrerField);
    }

    /**
     * Returns whether has explicit reference to.
     * @param referrerField the referrer field
     * @param referencedField the referenced field
     * @return {@code true} if has explicit reference to; otherwise {@code false}
     */
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

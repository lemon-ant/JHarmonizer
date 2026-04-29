package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import lombok.NonNull;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;

/**
 * Provides declaration dependencies created by explicit {@code this.<field>} references in field initializers.
 */
final class ExplicitThisInitializerFieldDependencyProvider
        extends AbstractExplicitInitializerForwardReferenceDependencyProvider {

    /**
     * Returns whether is supported referenced field.
     * @param referencedField the referenced field
     * @return {@code true} if is supported referenced field; otherwise {@code false}
     */
    @Override
    protected boolean isSupportedReferencedField(@NonNull CtField<?> referencedField) {
        return !isStaticField(referencedField);
    }

    /**
     * Returns whether is supported referrer field.
     * @param referrerField the referrer field
     * @return {@code true} if is supported referrer field; otherwise {@code false}
     */
    @Override
    protected boolean isSupportedReferrerField(@NonNull CtField<?> referrerField) {
        return !isStaticField(referrerField);
    }

    /**
     * Returns whether has explicit reference to.
     * @param referrerField the referrer field
     * @param referencedField the referenced field
     * @return {@code true} if has explicit reference to; otherwise {@code false}
     */
    @Override
    protected boolean hasExplicitReferenceTo(@NonNull CtField<?> referrerField, @NonNull CtField<?> referencedField) {
        return hasExplicitQualifiedReferenceTo(
                referrerField,
                referencedField,
                ExplicitThisInitializerFieldDependencyProvider::isExplicitThisQualifiedAccess);
    }

    private static boolean isExplicitThisQualifiedAccess(CtFieldAccess<?> fieldAccess, CtType<?> ignoredDeclaringType) {
        return fieldAccess.getTarget() instanceof CtThisAccess<?> thisAccess && !thisAccess.isImplicit();
    }
}

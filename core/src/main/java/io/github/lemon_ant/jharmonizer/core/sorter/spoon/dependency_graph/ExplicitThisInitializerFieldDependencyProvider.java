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

    @Override
    protected boolean isSupportedReferencedField(@NonNull CtField<?> referencedField) {
        return !isStaticField(referencedField);
    }

    @Override
    protected boolean isSupportedReferrerField(@NonNull CtField<?> referrerField) {
        return true;
    }

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

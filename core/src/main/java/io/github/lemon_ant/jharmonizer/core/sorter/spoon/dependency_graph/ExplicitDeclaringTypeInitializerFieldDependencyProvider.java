package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import lombok.NonNull;
import spoon.reflect.declaration.CtField;
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
        return hasExplicitDeclaringTypeReferenceTo(referrerField, referencedField);
    }
}

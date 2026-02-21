package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import lombok.NonNull;
import spoon.reflect.declaration.CtField;

/**
 * Provides declaration dependencies created by explicit {@code this.<field>} references in field initializers.
 */
final class ExplicitThisInitializerFieldDependencyProvider
        extends AbstractExplicitInitializerForwardReferenceDependencyProvider {

    @Override
    protected boolean isSupportedReferencedField(@NonNull CtField<?> referencedField) {
        return true;
    }

    @Override
    protected boolean isSupportedReferrerField(@NonNull CtField<?> referrerField) {
        return true;
    }

    @Override
    protected boolean hasExplicitReferenceTo(@NonNull CtField<?> referrerField, @NonNull CtField<?> referencedField) {
        return OrderDependentFieldReferenceUtils.hasExplicitThisReferenceTo(referrerField, referencedField);
    }
}

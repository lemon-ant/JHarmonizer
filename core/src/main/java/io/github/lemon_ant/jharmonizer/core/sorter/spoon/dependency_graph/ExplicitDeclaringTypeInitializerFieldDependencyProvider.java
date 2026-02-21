package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;

/**
 * Provides declaration dependencies created by explicit {@code <DeclaringType>.<field>} references in static field
 * initializers.
 */
final class ExplicitDeclaringTypeInitializerFieldDependencyProvider implements MemberDependencyProvider {

    private static boolean isStaticField(@NonNull CtField<?> field) {
        return field.getModifiers().contains(ModifierKind.STATIC);
    }

    @NonNull
    @Override
    public Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {
        if (!(dependentMember instanceof CtField<?> referencedField) || !isStaticField(referencedField)) {
            return Set.of();
        }

        if (ExplicitInitializerForwardReferenceDependencyUtils.isDefaultValueInitializer(referencedField)) {
            return Set.of();
        }

        return ExplicitInitializerForwardReferenceDependencyUtils.findEarlierFieldsWithReferenceTo(
                        referencedField,
                        ExplicitDeclaringTypeInitializerFieldDependencyProvider::isStaticField,
                        OrderDependentFieldReferenceUtils::hasExplicitDeclaringTypeReferenceTo)
                .stream()
                .map(providerMember ->
                        new MemberDependencyArc(providerMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }
}

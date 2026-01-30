package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.visitor.filter.TypeFilter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class OrderDependentFieldReferenceUtils {
    static CtType<?> requireDeclaringType(@NonNull CtTypeMember typeMember) {
        CtType<?> declaringType = typeMember.getDeclaringType();
        if (declaringType != null) {
            return declaringType;
        }

        SourcePosition memberPosition = typeMember.getPosition();

        throw new IllegalStateException(
                "Expected type member to have declaring type (member must come from CtType.getTypeMembers()). "
                        + "typeMember=" + typeMember.getShortRepresentation()
                        + ", position=" + memberPosition);
    }

    static Set<CtField<?>> findReferencedFields(@NonNull CtElement astRoot, @NonNull CtType<?> declaringType) {
        @SuppressWarnings("unchecked")
        Class<CtFieldAccess<?>> fieldAccessClassToken = (Class<CtFieldAccess<?>>) (Class<?>) CtFieldAccess.class;

        return astRoot.getElements(new TypeFilter<>(fieldAccessClassToken)).stream()
                .map(CtFieldAccess::getVariable)
                //  .filter(Objects::nonNull)
                .map(CtFieldReference::getDeclaration)
                // TODO Why do we need  it????
                .filter(Objects::nonNull)
                .filter(referencedField -> referencedField.getDeclaringType() == declaringType)
                .collect(Collectors.toUnmodifiableSet());
    }

    static boolean shouldCreateDeclarationDependencyEdge(
            @NonNull CtTypeMember providerMember, @NonNull CtTypeMember dependentMember) {
        if (providerMember == dependentMember) {
            return true;
        }

        SourcePosition providerPosition = providerMember.getPosition();
        SourcePosition dependentPosition = dependentMember.getPosition();

        if (providerPosition == null || dependentPosition == null) {
            return false; // Conservative fallback.
        }

        if (!providerPosition.isValidPosition() || !dependentPosition.isValidPosition()) {
            return false; // Conservative fallback.
        }

        return providerPosition.getSourceStart() >= dependentPosition.getSourceStart();
    }
}

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

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    static Set<CtField<?>> findReferencedFields(@NonNull CtElement astRoot, @NonNull CtType<?> declaringType) {
        @SuppressWarnings("unchecked")
        Class<CtFieldAccess<?>> fieldAccessClass = (Class<CtFieldAccess<?>>) (Class<?>) CtFieldAccess.class;
        TypeFilter<CtFieldAccess<?>> fieldAccessTypeFilter = new TypeFilter<>(fieldAccessClass);
        return astRoot.getElements(fieldAccessTypeFilter).stream()
                .map(CtFieldAccess::getVariable)
                .map(CtFieldReference::getDeclaration)
                // TODO Why do we need this???
                .filter(Objects::nonNull)
                .filter(referencedField -> referencedField.getDeclaringType() == declaringType)
                .collect(Collectors.toUnmodifiableSet());
    }
}

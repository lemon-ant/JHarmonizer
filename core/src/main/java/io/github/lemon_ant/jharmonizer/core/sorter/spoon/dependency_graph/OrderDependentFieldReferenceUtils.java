package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.visitor.filter.TypeFilter;

@UtilityClass
// TODO Rename it
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

    /**
     * TODO: Reduce over-conservative ordering constraints for initializer dependencies.
     */
    static Set<CtField<?>> findReferencedFields(
            @NonNull CtTypeMember dependentMember, @NonNull CtElement dependentAstRoot) {
        CtType<?> declaringType = requireDeclaringType(dependentMember);
        int dependentSourceStart = requireSourceStart(dependentMember);

        return collectDeclarationDependencyFieldCandidates(dependentAstRoot, declaringType).stream()
                .filter(referencedField -> isDeclaredBeforeDependent(referencedField, dependentSourceStart))
                .collect(Collectors.toUnmodifiableSet());
    }

    static Set<CtField<?>> findReadFields(@NonNull CtTypeMember dependentMember, @NonNull CtElement astRoot) {
        CtType<?> declaringType = requireDeclaringType(dependentMember);
        return collectReadFieldDeclarations(astRoot, declaringType);
    }

    static Set<CtField<?>> findWrittenFields(@NonNull CtTypeMember dependentMember, @NonNull CtElement astRoot) {
        CtType<?> declaringType = requireDeclaringType(dependentMember);
        return collectWrittenFieldDeclarations(astRoot, declaringType);
    }

    private static Set<CtField<?>> collectDeclarationDependencyFieldCandidates(
            CtElement dependentAstRoot, CtType<?> declaringType) {
        return collectDeclaringTypeFieldDeclarations(dependentAstRoot, declaringType, CtFieldAccess.class).stream()
                .filter(fieldAccess -> !isPureWriteOnlyAssignment(fieldAccess))
                .map(CtFieldAccess::getVariable)
                .map(CtFieldReference::getDeclaration)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<CtField<?>> collectReadFieldDeclarations(CtElement astRoot, CtType<?> declaringType) {
        return collectDeclaringTypeFieldDeclarations(astRoot, declaringType, CtFieldRead.class).stream()
                .map(CtFieldAccess::getVariable)
                .map(CtFieldReference::getDeclaration)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<CtField<?>> collectWrittenFieldDeclarations(CtElement astRoot, CtType<?> declaringType) {
        return collectDeclaringTypeFieldDeclarations(astRoot, declaringType, CtFieldWrite.class).stream()
                .map(CtFieldAccess::getVariable)
                .map(CtFieldReference::getDeclaration)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static <T extends CtFieldAccess<?>> List<T> collectDeclaringTypeFieldDeclarations(
            CtElement astRoot, CtType<?> declaringType, Class<T> fieldAccessClass) {
        TypeFilter<T> fieldAccessTypeFilter = new TypeFilter<>(fieldAccessClass);
        List<T> fieldAccesses = astRoot.getElements(fieldAccessTypeFilter);
        return fieldAccesses.stream()
                .filter(fieldAccess -> !isInsideLazyContext(declaringType, astRoot, fieldAccess))
                .filter(fieldAccess -> isDeclaredInType(fieldAccess, declaringType))
                .collect(Collectors.toUnmodifiableList());
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean isDeclaredInType(CtFieldAccess<?> fieldAccess, CtType<?> declaringType) {
        CtFieldReference<?> fieldReference = fieldAccess.getVariable();
        CtField<?> declaration = fieldReference.getDeclaration();
        return declaration != null && declaration.getDeclaringType() == declaringType;
    }

    private static boolean isPureWriteOnlyAssignment(CtFieldAccess<?> fieldAccess) {
        if (!(fieldAccess instanceof CtFieldWrite<?>)) {
            return false;
        }

        CtElement parent = fieldAccess.getParent();
        if (!(parent instanceof CtAssignment<?, ?> assignment) || parent instanceof CtOperatorAssignment<?, ?>) {
            return false;
        }

        return assignment.getAssigned() == fieldAccess;
    }

    private static boolean isDeclaredBeforeDependent(CtTypeMember providerMember, int dependentSourceStart) {
        int providerSourceStart = requireSourceStart(providerMember);
        return providerSourceStart < dependentSourceStart;
    }

    static int requireSourceStart(CtTypeMember typeMember) {
        SourcePosition memberPosition = typeMember.getPosition();
        if (memberPosition != null && memberPosition.isValidPosition()) {
            return memberPosition.getSourceStart();
        }

        throw new IllegalStateException(
                "Expected type member to have a valid SourcePosition (member must come from parsed source and "
                        + "CtType.getTypeMembers()). "
                        + "typeMember=" + typeMember.getShortRepresentation()
                        + ", position=" + memberPosition);
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean isInsideLazyContext(CtType<?> declaringType, CtElement astRoot, CtElement element) {
        CtElement currentParent = element.getParent();

        while (currentParent != null) {
            if (currentParent instanceof CtLambda<?>) {
                return true;
            }

            if (currentParent instanceof CtExecutableReferenceExpression<?, ?>) {
                return true;
            }

            if (currentParent instanceof CtType<?> parentType && parentType != declaringType) {
                return true;
            }

            if (currentParent == astRoot) {
                return false;
            }

            currentParent = currentParent.getParent();
        }

        return false;
    }
}

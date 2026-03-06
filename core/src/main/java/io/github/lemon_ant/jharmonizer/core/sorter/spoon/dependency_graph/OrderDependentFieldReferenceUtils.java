package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

        return streamDeclarationDependencyFieldCandidates(dependentAstRoot, declaringType)
                .filter(referencedField -> isDeclaredBeforeDependent(referencedField, dependentSourceStart))
                .collect(Collectors.toUnmodifiableSet());
    }

    static Set<CtField<?>> findReadFields(@NonNull CtTypeMember dependentMember, @NonNull CtElement astRoot) {
        CtType<?> declaringType = requireDeclaringType(dependentMember);
        return streamReadFieldDeclarations(astRoot, declaringType).collect(Collectors.toUnmodifiableSet());
    }

    static Set<CtField<?>> findWrittenFields(@NonNull CtTypeMember dependentMember, @NonNull CtElement astRoot) {
        CtType<?> declaringType = requireDeclaringType(dependentMember);
        return streamWrittenFieldDeclarations(astRoot, declaringType).collect(Collectors.toUnmodifiableSet());
    }

    private static Stream<CtField<?>> streamDeclarationDependencyFieldCandidates(
            CtElement dependentAstRoot, CtType<?> declaringType) {
        return streamFieldDeclarationsByAccessClass(dependentAstRoot, declaringType, CtFieldAccess.class)
                .filter(fieldAccess -> !isPureWriteOnlyAssignment(fieldAccess))
                .map(CtFieldAccess::getVariable)
                .map(CtFieldReference::getDeclaration)
                .filter(Objects::nonNull);
    }

    private static Stream<CtField<?>> streamReadFieldDeclarations(CtElement astRoot, CtType<?> declaringType) {
        return streamFieldDeclarationsByAccessClass(astRoot, declaringType, CtFieldRead.class)
                .map(CtFieldAccess::getVariable)
                .map(CtFieldReference::getDeclaration)
                .filter(Objects::nonNull);
    }

    private static Stream<CtField<?>> streamWrittenFieldDeclarations(CtElement astRoot, CtType<?> declaringType) {
        return streamFieldDeclarationsByAccessClass(astRoot, declaringType, CtFieldWrite.class)
                .map(CtFieldAccess::getVariable)
                .map(CtFieldReference::getDeclaration)
                .filter(Objects::nonNull);
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static <T extends CtFieldAccess<?>> Stream<T> streamFieldDeclarationsByAccessClass(
            CtElement astRoot, CtType<?> declaringType, Class<T> fieldAccessClass) {
        TypeFilter<T> fieldAccessTypeFilter = new TypeFilter<>(fieldAccessClass);
        return astRoot.getElements(fieldAccessTypeFilter).stream()
                .filter(fieldAccess -> !isInsideLazyContext(declaringType, astRoot, fieldAccess))
                .filter(fieldAccess -> isDeclaredInType(fieldAccess, declaringType));
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

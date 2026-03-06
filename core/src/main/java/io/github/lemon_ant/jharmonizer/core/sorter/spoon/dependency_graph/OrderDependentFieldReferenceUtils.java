package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

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
     * Finds fields declared in the same type that act as provider-members for a dependent initialization member.
     */
    static Set<CtField<?>> findProviderFieldsRequiredByDependentMember(
            @NonNull CtTypeMember dependentMember, @NonNull CtElement dependentMemberAstRoot) {
        CtType<?> declaringType = requireDeclaringType(dependentMember);
        int dependentSourceStart = requireSourceStart(dependentMember);

        return streamProviderFieldCandidatesForDependentMember(dependentMemberAstRoot, declaringType)
                .filter(providerField -> isProviderDeclaredBeforeDependentMember(providerField, dependentSourceStart))
                .collect(Collectors.toUnmodifiableSet());
    }

    static Set<CtField<?>> findFieldsReadByMember(@NonNull CtTypeMember member, @NonNull CtElement memberAstRoot) {
        CtType<?> declaringType = requireDeclaringType(member);
        return streamFieldDeclarationsReadByMember(memberAstRoot, declaringType).collect(Collectors.toUnmodifiableSet());
    }

    static Set<CtField<?>> findFieldsWrittenByMember(@NonNull CtTypeMember member, @NonNull CtElement memberAstRoot) {
        CtType<?> declaringType = requireDeclaringType(member);
        return streamFieldDeclarationsWrittenByMember(memberAstRoot, declaringType).collect(Collectors.toUnmodifiableSet());
    }

    private static Stream<CtField<?>> streamProviderFieldCandidatesForDependentMember(
            CtElement dependentMemberAstRoot, CtType<?> declaringType) {
        return streamFieldAccessesInSameType(dependentMemberAstRoot, declaringType, CtFieldAccess.class)
                .filter(fieldAccess -> !isPureWriteOnlyAssignment(fieldAccess))
                .map(OrderDependentFieldReferenceUtils::resolveFieldDeclaration);
    }

    private static Stream<CtField<?>> streamFieldDeclarationsReadByMember(CtElement memberAstRoot, CtType<?> declaringType) {
        return streamFieldAccessesInSameType(memberAstRoot, declaringType, CtFieldRead.class)
                .map(OrderDependentFieldReferenceUtils::resolveFieldDeclaration);
    }

    private static Stream<CtField<?>> streamFieldDeclarationsWrittenByMember(CtElement memberAstRoot, CtType<?> declaringType) {
        return streamFieldAccessesInSameType(memberAstRoot, declaringType, CtFieldWrite.class)
                .map(OrderDependentFieldReferenceUtils::resolveFieldDeclaration);
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static <T extends CtFieldAccess<?>> Stream<T> streamFieldAccessesInSameType(
            CtElement memberAstRoot, CtType<?> declaringType, Class<T> fieldAccessClass) {
        TypeFilter<T> fieldAccessTypeFilter = new TypeFilter<>(fieldAccessClass);
        return memberAstRoot.getElements(fieldAccessTypeFilter).stream()
                .filter(fieldAccess -> !isInsideLazyContext(declaringType, memberAstRoot, fieldAccess))
                .filter(fieldAccess -> isDeclaredInType(resolveFieldDeclaration(fieldAccess), declaringType));
    }

    private static CtField<?> resolveFieldDeclaration(CtFieldAccess<?> fieldAccess) {
        CtFieldReference<?> fieldReference = fieldAccess.getVariable();
        return fieldReference.getDeclaration();
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean isDeclaredInType(CtField<?> fieldDeclaration, CtType<?> declaringType) {
        return fieldDeclaration != null && fieldDeclaration.getDeclaringType() == declaringType;
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

    private static boolean isProviderDeclaredBeforeDependentMember(int providerSourceStart, int dependentSourceStart) {
        return providerSourceStart < dependentSourceStart;
    }

    private static boolean isProviderDeclaredBeforeDependentMember(CtTypeMember providerMember, int dependentSourceStart) {
        int providerSourceStart = requireSourceStart(providerMember);
        return isProviderDeclaredBeforeDependentMember(providerSourceStart, dependentSourceStart);
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

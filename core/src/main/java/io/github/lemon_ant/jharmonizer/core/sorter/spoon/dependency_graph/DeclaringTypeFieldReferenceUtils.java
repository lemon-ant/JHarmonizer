package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Optional;
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
class DeclaringTypeFieldReferenceUtils {

    @NonNull
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
    @NonNull
    static Set<CtField<?>> findProviderFieldsRequiredByDependentMember(
            @NonNull CtTypeMember dependentMember, @NonNull CtElement dependentMemberAstRoot) {
        CtType<?> declaringType = requireDeclaringType(dependentMember);

        return streamFieldAccessesInSameType(dependentMemberAstRoot, declaringType, CtFieldAccess.class)
                .filter(fieldAccess -> !isPureWriteOnlyAssignment(fieldAccess))
                .flatMap(fieldAccess -> resolveFieldDeclaration(fieldAccess).stream())
                .filter(providerField -> isProviderDeclaredBeforeDependentMember(providerField, dependentMember))
                .collect(Collectors.toUnmodifiableSet());
    }

    @NonNull
    static Set<CtField<?>> findFieldsReadByMember(@NonNull CtTypeMember member, @NonNull CtElement memberAstRoot) {
        CtType<?> declaringType = requireDeclaringType(member);
        return streamFieldAccessesInSameType(memberAstRoot, declaringType, CtFieldRead.class)
                .flatMap(fieldAccess -> resolveFieldDeclaration(fieldAccess).stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @NonNull
    static Set<CtField<?>> findFieldsWrittenByMember(@NonNull CtTypeMember member, @NonNull CtElement memberAstRoot) {
        CtType<?> declaringType = requireDeclaringType(member);
        return streamFieldAccessesInSameType(memberAstRoot, declaringType, CtFieldWrite.class)
                .flatMap(fieldAccess -> resolveFieldDeclaration(fieldAccess).stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @NonNull
    private static <T extends CtFieldAccess<?>> Stream<T> streamFieldAccessesInSameType(
            CtElement memberAstRoot, CtType<?> declaringType, Class<T> fieldAccessClass) {
        TypeFilter<T> fieldAccessTypeFilter = new TypeFilter<>(fieldAccessClass);
        return memberAstRoot.getElements(fieldAccessTypeFilter).stream()
                // TODO Check that we need it for all providers
                .filter(fieldAccess -> !isInsideLazyContext(declaringType, memberAstRoot, fieldAccess))
                .filter(fieldAccess -> resolveFieldDeclaration(fieldAccess)
                        .map(field -> isDeclaredInType(field, declaringType))
                        .orElse(false));
    }

    @NonNull
    private static Optional<CtField<?>> resolveFieldDeclaration(CtFieldAccess<?> fieldAccess) {
        CtFieldReference<?> fieldReference = fieldAccess.getVariable();
        return Optional.ofNullable(fieldReference.getDeclaration());
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean isDeclaredInType(CtField<?> fieldDeclaration, CtType<?> declaringType) {
        return fieldDeclaration.getDeclaringType() == declaringType;
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
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

    private static boolean isProviderDeclaredBeforeDependentMember(
            CtTypeMember providerMember, CtTypeMember dependentMember) {
        int dependentSourceStart = requireSourceStart(dependentMember);
        int providerSourceStart = requireSourceStart(providerMember);
        return providerSourceStart < dependentSourceStart;
    }

    static int requireSourceStart(@NonNull CtTypeMember typeMember) {
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

package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtExpression;
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
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.SpoonClassNotFoundException;

@UtilityClass
class DeclaringTypeFieldReferenceUtils {

    /**
     * Performs the require declaring type.
     * @param typeMember the type member
     * @return the result
     */
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
                .map(fieldAccess -> fieldAccess.getVariable().getDeclaration())
                .filter(providerField -> isProviderDeclaredBeforeDependentMember(providerField, dependentMember))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Finds the fields read by member.
     * @param member the member
     * @param memberAstRoot the member ast root
     * @return the matching fields read by member
     */
    @NonNull
    static Set<CtField<?>> findFieldsReadByMember(@NonNull CtTypeMember member, @NonNull CtElement memberAstRoot) {
        CtType<?> declaringType = requireDeclaringType(member);
        return streamFieldAccessesInSameType(memberAstRoot, declaringType, CtFieldRead.class)
                .map(fieldAccess -> fieldAccess.getVariable().getDeclaration())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Finds the fields written by member.
     * @param member the member
     * @param memberAstRoot the member ast root
     * @return the matching fields written by member
     */
    @NonNull
    static Set<CtField<?>> findFieldsWrittenByMember(@NonNull CtTypeMember member, @NonNull CtElement memberAstRoot) {
        CtType<?> declaringType = requireDeclaringType(member);
        return streamFieldAccessesInSameType(memberAstRoot, declaringType, CtFieldWrite.class)
                .map(fieldAccess -> fieldAccess.getVariable().getDeclaration())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Attempts to partially evaluate an expression without failing on missing runtime classes.
     *
     * @param expression the expression to evaluate
     * @return partially evaluated expression, or empty when Spoon cannot resolve required classes
     */
    @NonNull
    static Optional<CtExpression<?>> findPartiallyEvaluatedExpression(@NonNull CtExpression<?> expression) {
        try {
            return Optional.of(expression.partiallyEvaluate());
        } catch (SpoonClassNotFoundException ignored) {
            return Optional.empty();
        }
    }

    @NonNull
    private static <T extends CtFieldAccess<?>> Stream<T> streamFieldAccessesInSameType(
            CtElement memberAstRoot, CtType<?> declaringType, Class<T> fieldAccessClass) {
        TypeFilter<T> fieldAccessTypeFilter = new TypeFilter<>(fieldAccessClass);
        return memberAstRoot.getElements(fieldAccessTypeFilter).stream()
                .filter(fieldAccess -> !isInsideLazyContext(declaringType, memberAstRoot, fieldAccess))
                .filter(fieldAccess -> Optional.ofNullable(fieldAccess.getVariable().getDeclaration())
                        .map(field -> isFieldDeclaredInType(field, declaringType))
                        .orElse(false));
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static boolean isFieldDeclaredInType(CtField<?> field, CtType<?> declaringType) {
        return field.getDeclaringType() == declaringType;
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
        int dependentSrcStart = requireSrcStart(dependentMember);
        int providerSrcStart = requireSrcStart(providerMember);
        return providerSrcStart < dependentSrcStart;
    }

    /**
     * Performs the require source start.
     * @param typeMember the type member
     * @return the result
     */
    static int requireSrcStart(@NonNull CtTypeMember typeMember) {
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

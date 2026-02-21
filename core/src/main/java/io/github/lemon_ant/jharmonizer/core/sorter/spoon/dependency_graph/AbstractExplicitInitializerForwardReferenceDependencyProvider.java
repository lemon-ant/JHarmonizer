package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.OrderDependentFieldReferenceUtils.requireDeclaringType;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.OrderDependentFieldReferenceUtils.requireSourceStart;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.visitor.filter.TypeFilter;

abstract class AbstractExplicitInitializerForwardReferenceDependencyProvider implements MemberDependencyProvider {

    @NonNull
    @Override
    public final Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {
        if (!(dependentMember instanceof CtField<?> referencedField) || !isSupportedReferencedField(referencedField)) {
            return Set.of();
        }

        if (isDefaultValueInitializer(referencedField)) {
            return Set.of();
        }

        return findEarlierReferrerFieldsWithExplicitReferenceTo(referencedField).stream()
                .map(providerMember ->
                        new MemberDependencyArc(providerMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }

    protected abstract boolean isSupportedReferencedField(@NonNull CtField<?> referencedField);

    protected abstract boolean isSupportedReferrerField(@NonNull CtField<?> referrerField);

    protected abstract boolean hasExplicitReferenceTo(
            @NonNull CtField<?> referrerField, @NonNull CtField<?> referencedField);

    protected final boolean hasExplicitThisReferenceTo(
            @NonNull CtField<?> referrerField, @NonNull CtField<?> referencedField) {
        return hasExplicitQualifiedReferenceTo(referrerField, referencedField, this::isExplicitThisQualifiedAccess);
    }

    protected final boolean hasExplicitDeclaringTypeReferenceTo(
            @NonNull CtField<?> referrerField, @NonNull CtField<?> referencedField) {
        CtType<?> referrerDeclaringType = requireDeclaringType(referrerField);

        return hasExplicitQualifiedReferenceTo(
                referrerField,
                referencedField,
                (fieldAccess, ignoredDeclaringType) ->
                        isExplicitDeclaringTypeQualifiedAccess(fieldAccess, referrerDeclaringType));
    }

    private boolean hasExplicitQualifiedReferenceTo(
            @NonNull CtField<?> referrerField,
            @NonNull CtField<?> referencedField,
            @NonNull BiPredicate<CtFieldAccess<?>, CtType<?>> qualifierMatcher) {
        CtElement referrerAstRoot = referrerField.getDefaultExpression();
        if (referrerAstRoot == null) {
            return false;
        }

        CtType<?> referrerDeclaringType = requireDeclaringType(referrerField);
        if (referencedField.getDeclaringType() != referrerDeclaringType) {
            return false;
        }

        List<CtFieldAccess<?>> fieldAccesses = referrerAstRoot.getElements(new TypeFilter<>(CtFieldAccess.class));

        return fieldAccesses.stream()
                .filter(fieldAccess -> qualifierMatcher.test(fieldAccess, referrerDeclaringType))
                .map(CtFieldAccess::getVariable)
                .map(CtFieldReference::getDeclaration)
                .filter(Objects::nonNull)
                .map(declaredField -> (CtField<?>) declaredField)
                .anyMatch(candidateReferencedField -> candidateReferencedField == referencedField);
    }

    private static boolean isExplicitThisQualifiedAccess(CtFieldAccess<?> fieldAccess, CtType<?> ignoredDeclaringType) {
        return fieldAccess.getTarget() instanceof CtThisAccess<?> thisAccess && !thisAccess.isImplicit();
    }

    private static boolean isExplicitDeclaringTypeQualifiedAccess(
            CtFieldAccess<?> fieldAccess, CtType<?> declaringType) {
        if (!(fieldAccess.getTarget() instanceof CtTypeAccess<?> typeAccess) || typeAccess.isImplicit()) {
            return false;
        }

        return typeAccess.getAccessedType() != null
                && typeAccess.getAccessedType().getTypeDeclaration() == declaringType;
    }

    private Set<CtTypeMember> findEarlierReferrerFieldsWithExplicitReferenceTo(@NonNull CtField<?> referencedField) {
        int referencedFieldSourceStart = requireSourceStart(referencedField);

        return referencedField.getDeclaringType().getTypeMembers().stream()
                .filter(typeMember -> typeMember instanceof CtField<?>)
                .filter(typeMember -> requireSourceStart(typeMember) < referencedFieldSourceStart)
                .map(typeMember -> (CtField<?>) typeMember)
                .filter(this::isSupportedReferrerField)
                .filter(field -> field.getDefaultExpression() != null)
                .filter(referrerField -> hasExplicitReferenceTo(referrerField, referencedField))
                .map(field -> (CtTypeMember) field)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isDefaultValueInitializer(@NonNull CtField<?> referencedField) {
        CtExpression<?> defaultExpression = referencedField.getDefaultExpression();
        if (defaultExpression == null) {
            return true;
        }

        CtExpression<?> foldedExpression = defaultExpression.partiallyEvaluate();
        return isUnaryMinusZeroLiteral(foldedExpression)
                || castLiteralExpression(foldedExpression)
                        .map(literalExpression -> isDefaultLiteralValue(referencedField, literalExpression.getValue()))
                        .orElse(false);
    }

    private static <T> Optional<CtLiteral<T>> castLiteralExpression(@NonNull CtExpression<T> expression) {
        if (expression instanceof CtLiteral<T> literalExpression) {
            return Optional.of(literalExpression);
        }

        return Optional.empty();
    }

    private static boolean isDefaultLiteralValue(@NonNull CtField<?> referencedField, Object literalValue) {
        if (!referencedField.getType().isPrimitive()) {
            return literalValue == null;
        }

        return isDefaultPrimitiveLiteralValue(referencedField.getType().getQualifiedName(), literalValue);
    }

    private static boolean isDefaultPrimitiveLiteralValue(@NonNull String primitiveTypeName, Object literalValue) {
        return switch (primitiveTypeName) {
            case "boolean" -> Objects.equals(Boolean.FALSE, literalValue);
            case "char" -> literalValue instanceof Character characterValue && characterValue == 0;
            case "byte", "short", "int", "long", "float", "double" -> isNumericZeroLiteral(literalValue);
            default -> false;
        };
    }

    private static boolean isNumericZeroLiteral(Object literalValue) {
        return literalValue instanceof Number numericLiteral && numericLiteral.doubleValue() == 0D;
    }

    private static boolean isUnaryMinusZeroLiteral(@NonNull CtExpression<?> expression) {
        return expression instanceof CtUnaryOperator<?> unaryOperator
                && unaryOperator.getKind() == UnaryOperatorKind.NEG
                && unaryOperator.getOperand() instanceof CtLiteral<?> operandLiteral
                && isNumericZeroLiteral(operandLiteral.getValue());
    }
}

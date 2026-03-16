package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils.requireDeclaringType;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils.requireSourceStart;

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
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * Base provider that detects forward-reference declaration dependencies between fields
 * that have an explicit (non-default-value) initializer.
 * Subclasses specify which fields qualify as referenced/referrer fields and how to detect the reference.
 */
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

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    protected boolean hasExplicitQualifiedReferenceTo(
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
                .anyMatch(candidateReferencedField -> candidateReferencedField == referencedField);
    }

    @NonNull
    private Set<CtTypeMember> findEarlierReferrerFieldsWithExplicitReferenceTo(CtField<?> referencedField) {
        int referencedFieldSourceStart = requireSourceStart(referencedField);

        return referencedField.getDeclaringType().getTypeMembers().stream()
                .filter(typeMember -> typeMember instanceof CtField<?>)
                .filter(typeMember -> requireSourceStart(typeMember) < referencedFieldSourceStart)
                .map(typeMember -> (CtField<?>) typeMember)
                .filter(this::isSupportedReferrerField)
                .filter(referrerField -> hasExplicitReferenceTo(referrerField, referencedField))
                .map(field -> (CtTypeMember) field)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean isDefaultValueInitializer(CtField<?> referencedField) {
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

    @NonNull
    private static <T> Optional<CtLiteral<T>> castLiteralExpression(CtExpression<T> expression) {
        if (expression instanceof CtLiteral<T> literalExpression) {
            return Optional.of(literalExpression);
        }

        return Optional.empty();
    }

    private static boolean isDefaultLiteralValue(CtField<?> referencedField, Object literalValue) {
        if (!referencedField.getType().isPrimitive()) {
            return literalValue == null;
        }

        return isDefaultPrimitiveLiteralValue(referencedField.getType().getQualifiedName(), literalValue);
    }

    private static boolean isDefaultPrimitiveLiteralValue(String primitiveTypeName, Object literalValue) {
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

    private static boolean isUnaryMinusZeroLiteral(CtExpression<?> expression) {
        return expression instanceof CtUnaryOperator<?> unaryOperator
                && unaryOperator.getKind() == UnaryOperatorKind.NEG
                && unaryOperator.getOperand() instanceof CtLiteral<?> operandLiteral
                && isNumericZeroLiteral(operandLiteral.getValue());
    }

    protected static boolean isStaticField(@NonNull CtField<?> field) {
        return field.getModifiers().contains(ModifierKind.STATIC);
    }
}

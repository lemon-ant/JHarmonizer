package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils.requireDeclaringType;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.DeclaringTypeFieldReferenceUtils.requireSrcStart;

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

    /**
     * Finds the direct provider edges.
     * @param dependentMember the dependent member
     * @param keepAccessorsTogether the keep accessors together
     * @param relaxedForwardReferences whether forward references to fields declared later in source order
     *     are ignored for dependency resolution
     * @return the matching direct provider edges
     */
    @NonNull
    @Override
    public final Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether, boolean relaxedForwardReferences) {
        if (!(dependentMember instanceof CtField<?> referencedField) || !isSupportedReferencedField(referencedField)) {
            return Set.of();
        }

        if (isDefaultValueInitializer(referencedField)) {
            return Set.of();
        }

        return findReferrerFieldsWithExplicitReferenceTo(referencedField, relaxedForwardReferences).stream()
                .map(providerMember ->
                        new MemberDependencyArc(providerMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns whether the referenced field participates in this dependency rule.
     *
     * @param referencedField the referenced field to inspect
     * @return {@code true} if the referenced field is supported; otherwise {@code false}
     */
    protected abstract boolean isSupportedReferencedField(@NonNull CtField<?> referencedField);

    /**
     * Returns whether the referrer field participates in this dependency rule.
     *
     * @param referrerField the referrer field to inspect
     * @return {@code true} if the referrer field is supported; otherwise {@code false}
     */
    protected abstract boolean isSupportedReferrerField(@NonNull CtField<?> referrerField);

    /**
     * Returns whether the referrer field explicitly references the referenced field.
     *
     * @param referrerField the field containing the reference
     * @param referencedField the referenced field to match
     * @return {@code true} if the explicit reference exists; otherwise {@code false}
     */
    protected abstract boolean hasExplicitReferenceTo(
            @NonNull CtField<?> referrerField, @NonNull CtField<?> referencedField);

    /**
     * Returns whether has explicit qualified reference to.
     * @param referrerField the referrer field
     * @param referencedField the referenced field
     * @param qualifierMatcher the qualifier matcher
     * @return {@code true} if has explicit qualified reference to; otherwise {@code false}
     */
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
    private Set<CtTypeMember> findReferrerFieldsWithExplicitReferenceTo(
            CtField<?> referencedField, boolean relaxedForwardReferences) {
        int referencedFieldSrcStart = requireSrcStart(referencedField);

        return referencedField.getDeclaringType().getTypeMembers().stream()
                .filter(typeMember -> typeMember instanceof CtField<?>)
                .filter(typeMember ->
                        !relaxedForwardReferences || requireSrcStart(typeMember) < referencedFieldSrcStart)
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

        Optional<CtExpression<?>> foldedExpression =
                DeclaringTypeFieldReferenceUtils.findPartiallyEvaluatedExpression(defaultExpression);
        if (foldedExpression.isEmpty()) {
            return false;
        }

        CtExpression<?> evaluatedExpression = foldedExpression.get();
        return isUnaryMinusZeroLiteral(evaluatedExpression)
                || castLiteralExpression(evaluatedExpression)
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

    /**
     * Returns whether is static field.
     * @param field the field
     * @return {@code true} if is static field; otherwise {@code false}
     */
    protected static boolean isStaticField(@NonNull CtField<?> field) {
        return field.getModifiers().contains(ModifierKind.STATIC);
    }
}

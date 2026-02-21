package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.OrderDependentFieldReferenceUtils.requireSourceStart;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtTypeMember;

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

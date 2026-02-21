package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.OrderDependentFieldReferenceUtils.requireSourceStart;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.UnaryOperatorKind;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Provides declaration dependencies created by explicit {@code this.<field>} references in field initializers.
 */
final class ExplicitThisInitializerFieldDependencyProvider implements MemberDependencyProvider {

    @NonNull
    @Override
    public Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, boolean keepAccessorsTogether) {
        if (!(dependentMember instanceof CtField<?> referencedField)) {
            return Set.of();
        }

        if (!hasOrderSensitiveInitialization(referencedField)) {
            return Set.of();
        }

        return findEarlierFieldsWithExplicitThisReferenceTo(referencedField).stream()
                .map(providerMember ->
                        new MemberDependencyArc(providerMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<CtTypeMember> findEarlierFieldsWithExplicitThisReferenceTo(@NonNull CtField<?> referencedField) {
        int referencedFieldSourceStart = requireSourceStart(referencedField);

        return referencedField.getDeclaringType().getTypeMembers().stream()
                .filter(typeMember -> typeMember instanceof CtField<?>)
                .filter(typeMember -> requireSourceStart(typeMember) < referencedFieldSourceStart)
                .map(typeMember -> (CtField<?>) typeMember)
                .filter(field -> field.getDefaultExpression() != null)
                .filter(field -> OrderDependentFieldReferenceUtils.hasExplicitThisReferenceTo(field, referencedField))
                .map(field -> (CtTypeMember) field)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean hasOrderSensitiveInitialization(@NonNull CtField<?> referencedField) {
        CtExpression<?> defaultExpression = referencedField.getDefaultExpression();
        if (defaultExpression == null) {
            return false;
        }

        return !isExplicitDefaultValueInitializer(referencedField, defaultExpression);
    }

    private static boolean isExplicitDefaultValueInitializer(
            @NonNull CtField<?> referencedField, @NonNull CtExpression<?> defaultExpression) {
        CtExpression<?> foldedExpression = defaultExpression.partiallyEvaluate();
        if (isUnaryMinusZeroLiteral(foldedExpression)) {
            return true;
        }

        if (!(foldedExpression instanceof CtLiteral<?> literalExpression)) {
            return false;
        }

        Object literalValue = literalExpression.getValue();
        if (referencedField.getType().isPrimitive()) {
            String primitiveTypeName = referencedField.getType().getQualifiedName();
            return switch (primitiveTypeName) {
                case "boolean" -> Objects.equals(Boolean.FALSE, literalValue);
                case "char" -> literalValue instanceof Character characterValue && characterValue == 0;
                case "byte", "short", "int", "long", "float", "double" -> isNumericZeroLiteral(literalValue);
                default -> false;
            };
        }

        return literalValue == null;
    }

    private static boolean isNumericZeroLiteral(Object literalValue) {
        if (!(literalValue instanceof Number numericLiteral)) {
            return false;
        }

        return numericLiteral.doubleValue() == 0D;
    }

    private static boolean isUnaryMinusZeroLiteral(@NonNull CtExpression<?> expression) {
        return expression instanceof CtUnaryOperator<?> unaryOperator
                && unaryOperator.getKind() == UnaryOperatorKind.NEG
                && unaryOperator.getOperand() instanceof CtLiteral<?> operandLiteral
                && isNumericZeroLiteral(operandLiteral.getValue());
    }
}

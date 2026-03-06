package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.ModifierKind;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class CompileTimeConstantFieldUtils {

    static boolean isCompileTimeConstantVariable(@NonNull CtField<?> field) {
        if (!field.getModifiers().contains(ModifierKind.FINAL)) {
            return false;
        }

        CtExpression<?> defaultExpression = field.getDefaultExpression();
        if (defaultExpression == null) {
            return false;
        }

        String typeQualifiedName = field.getType().getQualifiedName();
        boolean isPrimitiveOrString = field.getType().isPrimitive() || "java.lang.String".equals(typeQualifiedName);
        return isPrimitiveOrString && defaultExpression.partiallyEvaluate() instanceof CtLiteral<?>;
    }
}

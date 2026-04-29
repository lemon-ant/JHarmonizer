package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.visitor.filter.TypeFilter;

class DeclaringTypeFieldReferenceUtilsTest {

    @Test
    @SuppressWarnings("unchecked")
    void findPartiallyEvaluatedExpression_anonymousClassInitializer_returnsEmptyOptionalWithoutPartialEvaluation() {
        // Given
        CtExpression<?> expression = mock(CtExpression.class);
        CtNewClass<?> anonymousClassExpression = mock(CtNewClass.class);
        when(expression.getElements(any(TypeFilter.class))).thenReturn(List.of(anonymousClassExpression));

        // When
        Optional<CtExpression<?>> partiallyEvaluatedExpression =
                DeclaringTypeFieldReferenceUtils.findPartiallyEvaluatedExpression(expression);

        // Then
        assertThat(partiallyEvaluatedExpression).isEmpty();
        verify(expression, never()).partiallyEvaluate();
    }
}

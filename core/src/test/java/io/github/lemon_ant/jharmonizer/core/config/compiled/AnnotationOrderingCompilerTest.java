// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedAnnotationOrderingRule.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedAnnotationOrderingRule;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AnnotationOrderingCompilerTest {

    @Test
    void compile_alphabeticalFirst_usesLengthOnlyForEqualNames() {
        // Given
        AnnotationDescriptor shortAlpha = new AnnotationDescriptor("", 10, "Alpha");
        AnnotationDescriptor longAlpha = new AnnotationDescriptor("z", 20, "Alpha");
        AnnotationDescriptor tiedAlpha = new AnnotationDescriptor("a", 20, "Alpha");
        AnnotationDescriptor beta = new AnnotationDescriptor("", 2, "Beta");
        List<AnnotationDescriptor> annotations = List.of(beta, longAlpha, tiedAlpha, shortAlpha);

        // When
        Comparator<AnnotationDescriptor> annotationComparator =
                AnnotationOrderingCompiler.compile(List.of(ALPHA, DECLARATION_LENGTH_ASC));

        // Then
        assertThat(annotations.stream().sorted(annotationComparator).toList())
                .containsExactly(shortAlpha, longAlpha, tiedAlpha, beta);
    }

    @Test
    void compile_emptyCriteria_preservesSourceOrder() {
        // Given
        List<AnnotationDescriptor> annotations =
                List.of(new AnnotationDescriptor("z", 8, "Z"), new AnnotationDescriptor("a", 2, "A"));

        // When
        Comparator<AnnotationDescriptor> annotationComparator = AnnotationOrderingCompiler.compile(List.of());

        // Then
        assertThat(annotations.stream().sorted(annotationComparator).toList()).containsExactlyElementsOf(annotations);
    }

    @Test
    void compile_multipleCriteria_resolvesTiesInConfiguredOrder() {
        // Given
        AnnotationDescriptor first = new AnnotationDescriptor("z", 2, "B");
        AnnotationDescriptor second = new AnnotationDescriptor("a", 12, "Aa");
        AnnotationDescriptor third = new AnnotationDescriptor("z", 12, "Aa");
        AnnotationDescriptor fourth = new AnnotationDescriptor("a", 12, "Ab");
        AnnotationDescriptor fifth = new AnnotationDescriptor("a", 12, "Zzz");
        List<AnnotationDescriptor> annotations = List.of(fifth, fourth, third, second, first);

        // When
        Comparator<AnnotationDescriptor> annotationComparator = AnnotationOrderingCompiler.compile(
                List.of(DECLARATION_LENGTH_ASC, NAME_LENGTH_ASC, ALPHA, ARGUMENTS_ALPHA));

        // Then
        assertThat(annotations.stream().sorted(annotationComparator).toList())
                .containsExactly(first, second, third, fourth, fifth);
    }

    @ParameterizedTest
    @MethodSource("provideCriteria")
    void compile_singleCriterion_ordersAnnotations(
            @NonNull UnifiedAnnotationOrderingRule rule, @NonNull List<String> expectedNames) {
        // Given
        List<AnnotationDescriptor> annotations = List.of(
                new AnnotationDescriptor("z", 10, "Bb"),
                new AnnotationDescriptor("a", 10, "Cc"),
                new AnnotationDescriptor("", 30, "Z"),
                new AnnotationDescriptor("a", 5, "ALong"),
                new AnnotationDescriptor(null, 8, "Missing"));

        // When
        Comparator<AnnotationDescriptor> annotationComparator = AnnotationOrderingCompiler.compile(List.of(rule));

        // Then
        assertThat(annotations.stream()
                        .sorted(annotationComparator)
                        .map(AnnotationDescriptor::getName)
                        .toList())
                .containsExactlyElementsOf(expectedNames);
    }

    @NonNull
    private static Stream<Arguments> provideCriteria() {
        return Stream.of(
                Arguments.of(ALPHA, List.of("ALong", "Bb", "Cc", "Missing", "Z")),
                Arguments.of(NAME_LENGTH_ASC, List.of("Z", "Bb", "Cc", "ALong", "Missing")),
                Arguments.of(DECLARATION_LENGTH_ASC, List.of("ALong", "Missing", "Bb", "Cc", "Z")),
                Arguments.of(ARGUMENTS_ALPHA, List.of("Missing", "Z", "Cc", "ALong", "Bb")));
    }
}

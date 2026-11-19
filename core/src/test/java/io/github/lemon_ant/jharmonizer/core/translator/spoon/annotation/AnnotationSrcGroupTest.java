// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.scanAnnotationSrcGroups;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroupRenderer.renderReplacement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationSrcGap;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class AnnotationSrcGroupTest {

    @Test
    void construct_emptyAnnotations_rejectsGroup() {
        assertThatThrownBy(() -> new AnnotationSrcGroup(List.of(), List.of(), 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one annotation");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void construct_mismatchedGapCount_rejectsGroup(int gapCount) {
        // Given
        AnnotationSrcGroup group = scanAnnotationSrcGroups("@Deprecated @SuppressWarnings(\"all\") class Sample {}")
                .get(0);
        List<AnnotationSrcGap> gapsInSrcOrder = Collections.nCopies(
                gapCount, group.getAnnotationSrcGapsInSrcOrder().get(0));

        // When / Then
        assertThatThrownBy(() -> new AnnotationSrcGroup(
                        group.getAnnotationSrcFragmentsInSrcOrder(),
                        gapsInSrcOrder,
                        group.getStart(),
                        group.getEndExclusive()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one gap per annotation");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void renderReplacement_mismatchedAnnotationCount_rejectsOrder(int annotationCount) {
        AnnotationSrcGroup group =
                scanAnnotationSrcGroups("@A @B class Sample {}").getFirst();
        List<AnnotationSrcFragment> annotationOrder = Collections.nCopies(
                annotationCount, group.getAnnotationSrcFragmentsInSrcOrder().getFirst());

        assertThatThrownBy(() -> renderReplacement(group, annotationOrder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one fragment per source gap");
    }

    @Test
    void renderReplacement_reorderedThenRestored_preservesOriginalFragmentsAndCachedCode() {
        // Given
        AnnotationSrcGroup originalGroup = scanAnnotationSrcGroups(
                        "@Deprecated\n@SuppressWarnings(\"all\") class Sample {}")
                .getFirst();
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragmentsInSrcOrder();
        List<AnnotationSrcGap> gapsInSrcOrder = originalGroup.getAnnotationSrcGapsInSrcOrder();
        List<String> originalGapCodes =
                gapsInSrcOrder.stream().map(AnnotationSrcGap::getSrcCode).toList();
        String originalReplacementCode = renderReplacement(originalGroup, null);

        // When
        String reorderedCode =
                renderReplacement(originalGroup, List.of(originalAnnotations.get(1), originalAnnotations.get(0)));
        String restoredCode = renderReplacement(originalGroup, originalAnnotations);

        // Then
        assertThat(reorderedCode).isEqualTo("@SuppressWarnings(\"all\")\n@Deprecated ");
        assertThat(restoredCode).isEqualTo(originalReplacementCode);
        assertThat(renderReplacement(originalGroup, null))
                .isSameAs(originalReplacementCode)
                .isEqualTo("@Deprecated\n@SuppressWarnings(\"all\") ");
        assertThat(originalGroup.getAnnotationSrcFragmentsInSrcOrder()).isSameAs(originalAnnotations);
        assertThat(originalAnnotations)
                .extracting(fragment -> fragment.getDescriptor().getName())
                .containsExactly("Deprecated", "SuppressWarnings");
        assertThat(originalGroup.getAnnotationSrcGapsInSrcOrder()).isSameAs(gapsInSrcOrder);
        assertThat(gapsInSrcOrder).extracting(AnnotationSrcGap::getSrcCode).containsExactlyElementsOf(originalGapCodes);
    }

    @ParameterizedTest
    @MethodSource("provideGapRenderingCases")
    void renderReplacement_swappedFragments_usesPreparedGapLayouts(
            @NonNull String annotationGroupCode, @NonNull String expectedReorderedCode) {
        // Given
        AnnotationSrcGroup group =
                scanAnnotationSrcGroups(annotationGroupCode + "class Sample {}").getFirst();
        List<AnnotationSrcFragment> originalAnnotations = group.getAnnotationSrcFragmentsInSrcOrder();
        List<String> originalGapCodes = group.getAnnotationSrcGapsInSrcOrder().stream()
                .map(AnnotationSrcGap::getSrcCode)
                .toList();

        // When
        String reorderedCode =
                renderReplacement(group, List.of(originalAnnotations.get(1), originalAnnotations.get(0)));

        // Then
        assertThat(reorderedCode).isEqualTo(expectedReorderedCode);
        assertThat(renderReplacement(group, originalAnnotations)).isEqualTo(annotationGroupCode);
        assertThat(renderReplacement(group, null)).isEqualTo(annotationGroupCode);
        assertThat(group.getStart()).isZero();
        assertThat(group.getEndExclusive()).isEqualTo(annotationGroupCode.length());
        assertThat(group.getAnnotationSrcGapsInSrcOrder())
                .extracting(AnnotationSrcGap::getSrcCode)
                .containsExactlyElementsOf(originalGapCodes);
    }

    @NonNull
    private static Stream<Arguments> provideGapRenderingCases() {
        return Stream.of(
                Arguments.of("@A @B ", "@B @A "),
                Arguments.of("@A\n@B ", "@B\n@A "),
                Arguments.of("@A // note\n@B ", "@B\n@A // note\n"),
                Arguments.of("@A // note\n    @B ", "@B\n    @A // note\n    "),
                Arguments.of("@A\n// lower\n\n@B ", "@B\n@A\n// lower\n\n"),
                Arguments.of("@A // note\r\n@B ", "@B\r\n@A // note\r\n"),
                Arguments.of("@A // note\r\n@B\n", "@B\r\n@A // note\n"),
                Arguments.of("@A // note\r\n@B\n\n", "@B\r\n@A // note\n\n"),
                Arguments.of("@A // note\\u000a @B ", "@B @A // note\\u000a "));
    }
}

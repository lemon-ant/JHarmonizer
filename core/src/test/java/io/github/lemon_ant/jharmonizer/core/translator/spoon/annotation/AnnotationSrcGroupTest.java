// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.scanAnnotationSrcGroups;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationSrcGap;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
                        group.getAnnotationSrcFragments(), gapsInSrcOrder, group.getStart(), group.getEndExclusive()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one gap per annotation");
    }

    @Test
    void copyWithFragments_cachedOriginal_preservesRangesAndBuildsIndependentReplacement() {
        // Given
        AnnotationSrcGroup originalGroup = scanAnnotationSrcGroups(
                        "@Deprecated\n@SuppressWarnings(\"all\") class Sample {}")
                .getFirst();
        List<AnnotationSrcFragment> originalAnnotations = originalGroup.getAnnotationSrcFragments();
        List<AnnotationSrcGap> gapsInSrcOrder = originalGroup.getAnnotationSrcGapsInSrcOrder();
        String originalReplacementCode = originalGroup.getReplacementCode();

        // When
        AnnotationSrcGroup copiedGroup = originalGroup.copyWithFragments(
                List.of(originalAnnotations.get(1), originalAnnotations.get(0)), gapsInSrcOrder);

        // Then
        assertThat(copiedGroup.getStart()).isEqualTo(originalGroup.getStart());
        assertThat(copiedGroup.getEndExclusive()).isEqualTo(originalGroup.getEndExclusive());
        assertThat(copiedGroup.getAnnotationSrcGapsInSrcOrder()).containsExactlyElementsOf(gapsInSrcOrder);
        assertThat(copiedGroup.getAnnotationSrcFragments())
                .containsExactly(originalAnnotations.get(1), originalAnnotations.get(0));
        assertThat(copiedGroup.getReplacementCode()).isEqualTo("@SuppressWarnings(\"all\")\n@Deprecated ");
        assertThat(originalGroup.getReplacementCode())
                .isSameAs(originalReplacementCode)
                .isEqualTo("@Deprecated\n@SuppressWarnings(\"all\") ");
        assertThat(originalGroup.getAnnotationSrcFragments()).containsExactlyElementsOf(originalAnnotations);
    }

    @Test
    void copyWithLeadingWhitespace_scannedGap_preservesSourceMetadataAndChangesPrefix() {
        // Given
        String srcCode = "class Sample {\n"
                + "    @Deprecated\n"
                + "    /* before type */\n"
                + "    @interface Marker {}\n"
                + "}";
        AnnotationSrcGap originalGap = scanAnnotationSrcGroups(srcCode)
                .getFirst()
                .getAnnotationSrcGapsInSrcOrder()
                .getFirst();
        String originalGapCode = originalGap.getSrcCode();

        // When
        AnnotationSrcGap copiedGap = originalGap.copyWithLeadingWhitespace("\r\n\t");

        // Then
        assertThat(copiedGap.getSrcCode()).isEqualTo("\r\n\t/* before type */\n    ");
        assertThat(copiedGap.getOriginalLeadingWhitespace()).isEqualTo("\n    ");
        assertThat(copiedGap.getRelocatedLeadingWhitespace()).isEqualTo(originalGap.getRelocatedLeadingWhitespace());
        assertThat(copiedGap.getIndentation()).isEqualTo("    ");
        assertThat(copiedGap.getRetainedLineSeparatorCount()).isEqualTo(originalGap.getRetainedLineSeparatorCount());
        assertThat(copiedGap.getStart()).isEqualTo(originalGap.getStart());
        assertThat(copiedGap.getEndExclusive()).isEqualTo(originalGap.getEndExclusive());
        assertThat(copiedGap
                        .copyWithLeadingWhitespace(copiedGap.getOriginalLeadingWhitespace())
                        .getSrcCode())
                .isEqualTo(originalGapCode);
        assertThat(originalGap.getSrcCode()).isEqualTo(originalGapCode);
        assertThat(originalGapCode).isEqualTo(srcCode.substring(originalGap.getStart(), originalGap.getEndExclusive()));
    }
}

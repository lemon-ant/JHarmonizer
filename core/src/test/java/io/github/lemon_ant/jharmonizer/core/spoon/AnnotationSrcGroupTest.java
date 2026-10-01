// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationSrcGap;
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
        AnnotationSrcGroup group = AnnotationSourceScanner.scan(
                        "@Deprecated @SuppressWarnings(\"all\") class Sample {}")
                .get(0);
        List<AnnotationSrcGap> gapsInSrcOrder = Collections.nCopies(
                gapCount, group.getAnnotationSrcGapsInSrcOrder().get(0));

        // When / Then
        assertThatThrownBy(() -> new AnnotationSrcGroup(
                        group.getAnnotationSrcFragments(), gapsInSrcOrder, group.getStart(), group.getEndExclusive()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one gap per annotation");
    }
}

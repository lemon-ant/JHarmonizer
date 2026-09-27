// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSourceScanner;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSourceScanner.AnnotationSrcGroup;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnnotationGroupPrinterTest {

    @NonNull
    private static final String ANNOTATIONS = "@SuppressWarnings(\"all\") @Deprecated ";

    @NonNull
    private static final String REORDERED_ANNOTATIONS = "@Deprecated @SuppressWarnings(\"all\") ";

    @Test
    void append_groupsInReverseSrcOrder_preservesSliceBoundaries() {
        // Given
        String firstType = ANNOTATIONS + "class First {}";
        String secondType = ANNOTATIONS + "class Second {}";
        String srcCode = firstType + "\n" + secondType;
        AnnotationGroupPrinter printer = createPrinterWithReversedAnnotations(srcCode);
        StringBuilder output = new StringBuilder();

        // When
        printer.append(output, srcCode, firstType.length() + 1, srcCode.length());
        printer.append(output, srcCode, 0, firstType.length());

        // Then
        assertThat(output)
                .hasToString(REORDERED_ANNOTATIONS + "class Second {}" + REORDERED_ANNOTATIONS + "class First {}");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void append_partialGroup_preservesOriginalSlice(boolean startsAtGroupBoundary) {
        // Given
        String srcCode = ANNOTATIONS + "class Sample {}";
        int start = startsAtGroupBoundary ? 0 : srcCode.indexOf("@Deprecated");
        int endExclusive = startsAtGroupBoundary ? srcCode.indexOf("@Deprecated") : srcCode.indexOf("class");
        AnnotationGroupPrinter printer = createPrinterWithReversedAnnotations(srcCode);
        StringBuilder output = new StringBuilder();

        // When
        printer.append(output, srcCode, start, endExclusive);

        // Then
        assertThat(output).hasToString(srcCode.substring(start, endExclusive));
    }

    @NonNull
    private static AnnotationGroupPrinter createPrinterWithReversedAnnotations(String srcCode) {
        List<AnnotationSrcGroup> annotationSrcGroups = AnnotationSourceScanner.scan(srcCode).stream()
                .map(group -> group.withFragmentsInPrintOrder(
                        group.getFragmentsInPrintOrder().reversed()))
                .toList();
        return new AnnotationGroupPrinter(annotationSrcGroups);
    }
}

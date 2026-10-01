// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonAnnotationSorterTestUtils.sortAnnotationGroups;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSourceScanner;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup;
import java.util.Comparator;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SrcPrinterOutputTest {

    @NonNull
    private static final String ANNOTATIONS = "@SuppressWarnings(\"all\") @Deprecated ";

    @NonNull
    private static final String REORDERED_ANNOTATIONS = "@Deprecated @SuppressWarnings(\"all\") ";

    @Test
    void printSrcRange_groupsInReverseSrcOrder_preservesSliceBoundaries() {
        // Given
        String firstType = ANNOTATIONS + "class First {}";
        String secondType = ANNOTATIONS + "class Second {}";
        String srcCode = firstType + "\n" + secondType;
        SrcPrinterOutput output = createOutputWithSortedAnnotations(srcCode);

        // When
        output.printSrcRange(firstType.length() + 1, srcCode.length());
        output.printSrcRange(0, firstType.length());

        // Then
        assertThat(output)
                .hasToString(REORDERED_ANNOTATIONS + "class Second {}" + REORDERED_ANNOTATIONS + "class First {}");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void printSrcRange_partialGroup_preservesOriginalSlice(boolean startsAtGroupBoundary) {
        // Given
        String srcCode = ANNOTATIONS + "class Sample {}";
        int start = startsAtGroupBoundary ? 0 : srcCode.indexOf("@Deprecated");
        int endExclusive = startsAtGroupBoundary ? srcCode.indexOf("@Deprecated") : srcCode.indexOf("class");
        SrcPrinterOutput output = createOutputWithSortedAnnotations(srcCode);

        // When
        output.printSrcRange(start, endExclusive);

        // Then
        assertThat(output).hasToString(srcCode.substring(start, endExclusive));
    }

    @NonNull
    private static SrcPrinterOutput createOutputWithSortedAnnotations(String srcCode) {
        List<AnnotationSrcGroup> annotationSrcGroups = sortAnnotationGroups(
                AnnotationSourceScanner.scan(srcCode), Comparator.comparing(AnnotationDescriptor::getName));
        return new SrcPrinterOutput(srcCode, annotationSrcGroups);
    }
}

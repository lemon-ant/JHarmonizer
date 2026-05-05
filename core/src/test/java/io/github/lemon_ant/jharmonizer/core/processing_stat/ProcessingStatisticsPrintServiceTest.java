// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.processing_stat;

import static io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsTestLabels.FILES_WITH_UNEXPECTED_ERRORS;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsTestLabels.FORMATTING_TIME_SHARE;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsTestLabels.MAX_SIZE_FILE_PREFIX;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsTestLabels.MIN_SIZE_FILE_PREFIX;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsTestLabels.SERIALIZATION_TIME_SHARE;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsTestLabels.TOTAL_CPU_TIME;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsTestLabels.UNEXPECTED_ERROR_FILES_HEADER;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsTestLabels.WALL_CLOCK_TIME;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.flow.FileProcessingResultCreator;
import io.github.lemon_ant.jharmonizer.core.flow.FileProcessingStatus;
import io.github.lemon_ant.jharmonizer.core.processing_stat.FlowProcessingStats.AggregatedProcessingStatistic;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

class ProcessingStatisticsPrintServiceTest {

    @Test
    void renderMinimal_withNoErrors_returnsCompactSummaryLine() {
        // Given
        AggregatedProcessingStatistic stats = AggregatedProcessingStatistic.builder()
                .fileCount(7)
                .totalSizeInBytes(12_345)
                .totalProcessingTimeNanos(500_000_000L)
                .totalParsingTimeNanos(200_000_000L)
                .totalSortingTimeNanos(100_000_000L)
                .totalSerializationTimeNanos(50_000_000L)
                .totalFormattingTimeNanos(150_000_000L)
                .filesWithUnexpectedErrors(List.of())
                .stopTriggerPaths(List.of())
                .statusCounts(Map.of())
                .build();

        // When
        String report = ProcessingStatisticsPrintService.renderMinimal(stats);

        // Then
        assertThat(report).contains("7 file(s)").contains("wall-clock").doesNotContain("unexpected error");
    }

    @Test
    void renderMinimal_withErrors_includesErrorCount() {
        // Given
        AggregatedProcessingStatistic stats = AggregatedProcessingStatistic.builder()
                .fileCount(3)
                .totalSizeInBytes(0)
                .totalProcessingTimeNanos(0)
                .totalParsingTimeNanos(0)
                .totalSortingTimeNanos(0)
                .totalSerializationTimeNanos(0)
                .totalFormattingTimeNanos(0)
                .filesWithUnexpectedErrors(List.of(Path.of("Broken.java"), Path.of("Also.java")))
                .stopTriggerPaths(List.of())
                .statusCounts(Map.of())
                .build();

        // When
        String report = ProcessingStatisticsPrintService.renderMinimal(stats);

        // Then
        assertThat(report).contains("2 unexpected error(s)");
    }

    @Test
    void render_containsPseudoTableAndMultilineUnexpectedErrorList() {
        // Given
        Path brokenPath = Path.of("zeta", "Broken.java");
        Path failurePath = Path.of("alpha", "Failure.java");
        AggregatedProcessingStatistic stats = AggregatedProcessingStatistic.builder()
                .fileCount(2)
                .totalSizeInBytes(6_500)
                .totalProcessingTimeNanos(2_300_000_000L)
                .totalParsingTimeNanos(1_100_000_000L)
                .totalSortingTimeNanos(700_000_000L)
                .totalSerializationTimeNanos(200_000_000L)
                .totalFormattingTimeNanos(500_000_000L)
                .filesWithUnexpectedErrors(List.of(brokenPath, failurePath))
                .stopTriggerPaths(List.of())
                .statusCounts(Map.of())
                .build();

        // When
        String report = ProcessingStatisticsPrintService.render(stats);

        // Then
        assertThat(report)
                .startsWith(System.lineSeparator())
                .containsPattern("(?m)^\\| JHarmonization summary\\s*\\|$")
                .contains("| Files processed")
                .contains("| Total size")
                .contains("| " + WALL_CLOCK_TIME)
                .contains("| " + TOTAL_CPU_TIME)
                .contains("| Parsing time (share)")
                .contains("| Sorting time (share)")
                .contains("| " + SERIALIZATION_TIME_SHARE)
                .contains("| " + FORMATTING_TIME_SHARE)
                .contains("| " + FILES_WITH_UNEXPECTED_ERRORS)
                .contains(UNEXPECTED_ERROR_FILES_HEADER)
                .contains("- " + PathDisplayFormatUtil.abbreviatePathForDisplay(failurePath, 120))
                .contains("- " + PathDisplayFormatUtil.abbreviatePathForDisplay(brokenPath, 120));
        assertThat(report.indexOf("| " + WALL_CLOCK_TIME)).isLessThan(report.indexOf("| " + TOTAL_CPU_TIME));
        assertThat(report.indexOf("| " + SERIALIZATION_TIME_SHARE))
                .isLessThan(report.indexOf("| " + FORMATTING_TIME_SHARE));
    }

    @Test
    void render_withoutUnexpectedErrors_printsNoneBullet() {
        // Given
        AggregatedProcessingStatistic stats = AggregatedProcessingStatistic.builder()
                .fileCount(0)
                .totalSizeInBytes(0)
                .totalProcessingTimeNanos(0)
                .totalParsingTimeNanos(0)
                .totalSortingTimeNanos(0)
                .totalSerializationTimeNanos(0)
                .totalFormattingTimeNanos(0)
                .filesWithUnexpectedErrors(List.of())
                .stopTriggerPaths(List.of())
                .statusCounts(Map.of())
                .build();

        // When
        String report = ProcessingStatisticsPrintService.render(stats);

        // Then
        assertThat(report).contains(UNEXPECTED_ERROR_FILES_HEADER + " none");
        assertThat(report).doesNotContain(UNEXPECTED_ERROR_FILES_HEADER + "\n");
    }

    @Test
    void render_longValues_keepsPseudoTableHeaderAligned() {
        // Given
        Path longPath = Path.of(
                "very-long-module-name",
                "deeply",
                "nested",
                "feature",
                "InternalToolForVeryLongStatisticsOutputVerification.java");
        AggregatedProcessingStatistic stats = AggregatedProcessingStatistic.builder()
                .fileCount(1)
                .totalSizeInBytes(123_456)
                .totalProcessingTimeNanos(1_234_567_890L)
                .totalParsingTimeNanos(456_000_000L)
                .totalSortingTimeNanos(400_000_000L)
                .totalSerializationTimeNanos(78_000_000L)
                .totalFormattingTimeNanos(300_000_000L)
                .filesWithUnexpectedErrors(List.of(longPath))
                .stopTriggerPaths(List.of())
                .statusCounts(Map.of())
                .build();

        // When
        String report = ProcessingStatisticsPrintService.render(stats);

        // Then
        assertThat(report)
                .contains("| " + FILES_WITH_UNEXPECTED_ERRORS)
                .contains("| Min size")
                .doesNotEndWith("=");
    }

    @Test
    @ResourceLock(Resources.LOCALE)
    void render_polishLocaleSet_usesRootFormattedCount() {
        // Given
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("pl-PL"));
            AggregatedProcessingStatistic stats = AggregatedProcessingStatistic.builder()
                    .fileCount(1_234)
                    .totalSizeInBytes(0)
                    .totalProcessingTimeNanos(0)
                    .totalParsingTimeNanos(0)
                    .totalSortingTimeNanos(0)
                    .totalSerializationTimeNanos(0)
                    .totalFormattingTimeNanos(0)
                    .filesWithUnexpectedErrors(List.of())
                    .stopTriggerPaths(List.of())
                    .statusCounts(Map.of())
                    .build();

            // When
            String report = ProcessingStatisticsPrintService.render(stats);

            // Then
            assertThat(report).contains("1,234");
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    void render_withSmallestAndLargestFiles_includesSizeBoundarySection() {
        // Given
        FileProcessingStatistic smallestFile = FileProcessingStatistic.convert(FileProcessingResultCreator.createResult(
                FileProcessingStatus.CHECKED, Path.of("Small.java"), 10, false));
        FileProcessingStatistic largestFile = FileProcessingStatistic.convert(FileProcessingResultCreator.createResult(
                FileProcessingStatus.CHECKED, Path.of("Large.java"), 500, false));
        AggregatedProcessingStatistic stats = AggregatedProcessingStatistic.builder()
                .fileCount(2)
                .totalSizeInBytes(510)
                .totalProcessingTimeNanos(0)
                .totalParsingTimeNanos(0)
                .totalSortingTimeNanos(0)
                .totalSerializationTimeNanos(0)
                .totalFormattingTimeNanos(0)
                .smallestFile(smallestFile)
                .largestFile(largestFile)
                .filesWithUnexpectedErrors(List.of())
                .stopTriggerPaths(List.of())
                .statusCounts(Map.of())
                .build();

        // When
        String report = ProcessingStatisticsPrintService.render(stats);

        // Then
        assertThat(report)
                .contains("Size boundary files:")
                .contains(MIN_SIZE_FILE_PREFIX)
                .contains("Small.java")
                .contains(MAX_SIZE_FILE_PREFIX)
                .contains("Large.java");
    }

    @Test
    void render_withSmallestFileOnly_includesMinSizePath() {
        // Given
        FileProcessingStatistic smallestFile = FileProcessingStatistic.convert(
                FileProcessingResultCreator.createResult(FileProcessingStatus.CHECKED, Path.of("Tiny.java"), 1, false));
        AggregatedProcessingStatistic stats = AggregatedProcessingStatistic.builder()
                .fileCount(1)
                .totalSizeInBytes(1)
                .totalProcessingTimeNanos(0)
                .totalParsingTimeNanos(0)
                .totalSortingTimeNanos(0)
                .totalSerializationTimeNanos(0)
                .totalFormattingTimeNanos(0)
                .smallestFile(smallestFile)
                .filesWithUnexpectedErrors(List.of())
                .stopTriggerPaths(List.of())
                .statusCounts(Map.of())
                .build();

        // When
        String report = ProcessingStatisticsPrintService.render(stats);

        // Then
        assertThat(report).contains(MIN_SIZE_FILE_PREFIX).contains("Tiny.java");
        assertThat(report).doesNotContain(MAX_SIZE_FILE_PREFIX);
    }
}

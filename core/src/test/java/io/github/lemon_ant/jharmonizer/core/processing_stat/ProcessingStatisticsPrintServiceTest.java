package io.github.lemon_ant.jharmonizer.core.processing_stat;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.processing_stat.SrcProcessingStats.AggregatedProcessingStatistic;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessingStatisticsPrintServiceTest {

    @Test
    void render_containsPseudoTableAndMultilineUnexpectedErrorList() {
        // Given
        Path brokenPath = Path.of("zeta", "Broken.java");
        Path failurePath = Path.of("alpha", "Failure.java");
        AggregatedProcessingStatistic stats = AggregatedProcessingStatistic.builder()
                .fileCount(2)
                .totalSize(6_500)
                .totalProcessingTimeNanos(2_300_000_000L)
                .totalParsingTimeNanos(1_100_000_000L)
                .totalSortingTimeNanos(700_000_000L)
                .totalSerializationTimeNanos(200_000_000L)
                .totalFormattingTimeNanos(500_000_000L)
                .filesWithUnexpectedErrors(List.of(brokenPath, failurePath))
                .build();

        // When
        String report = ProcessingStatisticsPrintService.render(stats);

        // Then
        assertThat(report)
                .startsWith(System.lineSeparator())
                .contains("JHarmonizer harmonization summary")
                .contains("| Files processed")
                .contains("| Total size")
                .contains("| Parsing time (share)")
                .contains("| Sorting time (share)")
                .contains("| Serialization time (share)")
                .contains("| Formatting time (share)")
                .contains("| Files with unexpected internal errors")
                .contains("Unexpected internal error files:")
                .contains("- " + PathDisplayFormatUtil.abbreviatePathForDisplay(failurePath, 120))
                .contains("- " + PathDisplayFormatUtil.abbreviatePathForDisplay(brokenPath, 120));
    }

    @Test
    void render_withoutUnexpectedErrors_printsNoneBullet() {
        // Given
        AggregatedProcessingStatistic stats = AggregatedProcessingStatistic.builder()
                .fileCount(0)
                .totalSize(0)
                .totalProcessingTimeNanos(0)
                .totalParsingTimeNanos(0)
                .totalSortingTimeNanos(0)
                .totalSerializationTimeNanos(0)
                .totalFormattingTimeNanos(0)
                .filesWithUnexpectedErrors(List.of())
                .build();

        // When
        String report = ProcessingStatisticsPrintService.render(stats);

        // Then
        assertThat(report).contains("Unexpected internal error files: none");
        assertThat(report).doesNotContain("Unexpected internal error files:\n");
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
                .totalSize(123_456)
                .totalProcessingTimeNanos(1_234_567_890L)
                .totalParsingTimeNanos(456_000_000L)
                .totalSortingTimeNanos(400_000_000L)
                .totalSerializationTimeNanos(78_000_000L)
                .totalFormattingTimeNanos(300_000_000L)
                .filesWithUnexpectedErrors(List.of(longPath))
                .build();

        // When
        String report = ProcessingStatisticsPrintService.render(stats);

        // Then
        assertThat(report)
                .contains("| Files with unexpected internal errors")
                .contains("| Min size")
                .doesNotEndWith("=");
    }
}

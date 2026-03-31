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
        AggregatedProcessingStatistic stats = new AggregatedProcessingStatistic(
                2,
                6_500,
                2_300_000_000L,
                1_100_000_000L,
                700_000_000L,
                500_000_000L,
                null,
                null,
                List.of(brokenPath, failurePath));

        // When
        String report = ProcessingStatisticsPrintService.render(stats);

        // Then
        assertThat(report)
                .startsWith(System.lineSeparator())
                .contains("JHarmonizer harmonization summary")
                .contains("| Files processed")
                .contains("| Total source length (chars)")
                .contains("| Parsing time (share)")
                .contains("| Sorting time (share)")
                .contains("| Formatting time (share)")
                .contains("| Files with unexpected internal errors")
                .contains("Unexpected internal error files:")
                .contains("- " + PathDisplayFormatUtil.abbreviatePathForDisplay(failurePath, 120))
                .contains("- " + PathDisplayFormatUtil.abbreviatePathForDisplay(brokenPath, 120));
    }

    @Test
    void render_withoutUnexpectedErrors_printsNoneBullet() {
        // Given
        AggregatedProcessingStatistic stats =
                new AggregatedProcessingStatistic(0, 0, 0, 0, 0, 0, null, null, List.of());

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
        AggregatedProcessingStatistic stats = new AggregatedProcessingStatistic(
                1, 123_456, 1_234_567_890L, 456_000_000L, 400_000_000L, 300_000_000L, null, null, List.of(longPath));

        // When
        String report = ProcessingStatisticsPrintService.render(stats);

        // Then
        assertThat(report)
                .contains("| Files with unexpected internal errors")
                .contains("| Min source length (chars)")
                .doesNotEndWith("=");
    }
}

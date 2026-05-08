// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.processing_stat;

import static io.github.lemon_ant.jharmonizer.core.flow.FileProcessingResultCreator.createResult;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.flow.FileProcessingResult;
import io.github.lemon_ant.jharmonizer.core.flow.FileProcessingStatus;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FlowProcessingStatsCollectorTest {

    @Test
    void aggregatedStats_emptyStream_calculateAverageMethodsReturnZero() {
        // When
        FlowProcessingStats.AggregatedProcessingStatistic aggregatedProcessingStatistic =
                Stream.<FileProcessingResult>empty().collect(FlowProcessingStats.statsCollector());

        // Then
        assertThat(aggregatedProcessingStatistic.calculateAverageProcessingTime())
                .isZero();
        assertThat(aggregatedProcessingStatistic.calculateAverageSize()).isZero();
        assertThat(aggregatedProcessingStatistic.computeNonConformingFileCount())
                .isZero();
    }

    @Test
    void aggregatedStats_reorderedAndFormattedFiles_computeNonConformingFileCountIncludesBoth() {
        // Given
        List<FileProcessingResult> fileProcessingResults = List.of(
                createResult(FileProcessingStatus.REORDERED, Path.of("R.java"), 10, false),
                createResult(FileProcessingStatus.FORMATTED, Path.of("F.java"), 20, false),
                createResult(FileProcessingStatus.CHECKED, Path.of("C.java"), 30, false));

        // When
        FlowProcessingStats.AggregatedProcessingStatistic aggregatedProcessingStatistic =
                fileProcessingResults.stream().collect(FlowProcessingStats.statsCollector());

        // Then
        assertThat(aggregatedProcessingStatistic.computeNonConformingFileCount())
                .isEqualTo(2L);
    }

    @Test
    void statsCollector_emptyStream_returnsZeroCountAndNullMinMax() {
        // When
        FlowProcessingStats.AggregatedProcessingStatistic aggregatedProcessingStatistic =
                Stream.<FileProcessingResult>empty().collect(FlowProcessingStats.statsCollector());

        // Then
        assertThat(aggregatedProcessingStatistic.getFileCount()).isZero();
        assertThat(aggregatedProcessingStatistic.getSmallestFile()).isNull();
        assertThat(aggregatedProcessingStatistic.getLargestFile()).isNull();
        assertThat(aggregatedProcessingStatistic.getStopTriggerPaths()).isEmpty();
        assertThat(aggregatedProcessingStatistic.getFilesWithUnexpectedErrors()).isEmpty();
    }

    @Test
    void statsCollector_errorFile_recordsInUnexpectedErrorPaths() {
        // Given
        FileProcessingResult errorFileProcessingResult =
                createResult(FileProcessingStatus.ERROR, Path.of("Broken.java"), 50, false);

        // When
        FlowProcessingStats.AggregatedProcessingStatistic aggregatedProcessingStatistic =
                Stream.of(errorFileProcessingResult).collect(FlowProcessingStats.statsCollector());

        // Then
        assertThat(aggregatedProcessingStatistic.getFilesWithUnexpectedErrors())
                .containsExactly(Path.of("Broken.java"));
    }

    @Test
    void statsCollector_multipleFiles_tracksCorrectMinAndMax() {
        // Given
        FileProcessingResult smallFile = createResult(FileProcessingStatus.CHECKED, Path.of("Small.java"), 10, false);
        FileProcessingResult mediumFile =
                createResult(FileProcessingStatus.FORMATTED, Path.of("Medium.java"), 500, false);
        FileProcessingResult largeFile =
                createResult(FileProcessingStatus.REORDERED, Path.of("Large.java"), 2000, false);

        // When
        FlowProcessingStats.AggregatedProcessingStatistic aggregatedProcessingStatistic =
                Stream.of(smallFile, mediumFile, largeFile).collect(FlowProcessingStats.statsCollector());

        // Then
        assertThat(aggregatedProcessingStatistic.getFileCount()).isEqualTo(3);
        assertThat(aggregatedProcessingStatistic.getSmallestFile())
                .isNotNull()
                .extracting(FileProcessingStatistic::getSizeInBytes)
                .isEqualTo(10L);
        assertThat(aggregatedProcessingStatistic.getLargestFile())
                .isNotNull()
                .extracting(FileProcessingStatistic::getSizeInBytes)
                .isEqualTo(2000L);
        assertThat(aggregatedProcessingStatistic.getTotalSizeInBytes()).isEqualTo(2510L);
    }

    @Test
    void statsCollector_singleFile_minEqualsMax() {
        // Given
        FileProcessingResult fileProcessingResult =
                createResult(FileProcessingStatus.CHECKED, Path.of("A.java"), 100, false);

        // When
        FlowProcessingStats.AggregatedProcessingStatistic aggregatedProcessingStatistic =
                Stream.of(fileProcessingResult).collect(FlowProcessingStats.statsCollector());

        // Then
        assertThat(aggregatedProcessingStatistic.getFileCount()).isEqualTo(1);
        assertThat(aggregatedProcessingStatistic.getSmallestFile())
                .isNotNull()
                .extracting(FileProcessingStatistic::getSizeInBytes)
                .isEqualTo(100L);
        assertThat(aggregatedProcessingStatistic.getLargestFile())
                .isNotNull()
                .extracting(FileProcessingStatistic::getSizeInBytes)
                .isEqualTo(100L);
    }

    @Test
    void statsCollector_statusCounts_aggregatesAllStatuses() {
        // Given
        List<FileProcessingResult> fileProcessingResults = List.of(
                createResult(FileProcessingStatus.REORDERED, Path.of("R1.java"), 10, false),
                createResult(FileProcessingStatus.FORMATTED, Path.of("F1.java"), 20, false),
                createResult(FileProcessingStatus.CHECKED, Path.of("C1.java"), 30, false),
                createResult(FileProcessingStatus.SKIPPED, Path.of("S1.java"), 5, false),
                createResult(FileProcessingStatus.UNCHANGED, Path.of("U1.java"), 15, false),
                createResult(FileProcessingStatus.ERROR, Path.of("E1.java"), 0, false));

        // When
        FlowProcessingStats.AggregatedProcessingStatistic aggregatedProcessingStatistic =
                fileProcessingResults.stream().collect(FlowProcessingStats.statsCollector());

        // Then
        assertThat(aggregatedProcessingStatistic.getStatusCounts())
                .containsEntry(FileProcessingStatus.REORDERED, 1L)
                .containsEntry(FileProcessingStatus.FORMATTED, 1L)
                .containsEntry(FileProcessingStatus.CHECKED, 1L)
                .containsEntry(FileProcessingStatus.SKIPPED, 1L)
                .containsEntry(FileProcessingStatus.UNCHANGED, 1L)
                .containsEntry(FileProcessingStatus.ERROR, 1L);
    }

    @Test
    void statsCollector_stopRequestedFile_recordsInStopTriggerPaths() {
        // Given
        FileProcessingResult stopRequestedFileProcessingResult =
                createResult(FileProcessingStatus.CHECKED, Path.of("Stop.java"), 100, true);

        // When
        FlowProcessingStats.AggregatedProcessingStatistic aggregatedProcessingStatistic =
                Stream.of(stopRequestedFileProcessingResult).collect(FlowProcessingStats.statsCollector());

        // Then
        assertThat(aggregatedProcessingStatistic.getStopTriggerPaths()).containsExactly(Path.of("Stop.java"));
    }

    @Test
    void statsContainerCombine_bothEmptyContainers_resultHasNullMinMax() {
        // Given
        FlowProcessingStats.StatsContainer containerA = new FlowProcessingStats.StatsContainer();
        FlowProcessingStats.StatsContainer containerB = new FlowProcessingStats.StatsContainer();

        // When
        FlowProcessingStats.StatsContainer merged = containerA.combine(containerB);

        // Then
        FlowProcessingStats.AggregatedProcessingStatistic aggregatedProcessingStatistic = merged.toAggregatedStats();
        assertThat(aggregatedProcessingStatistic.getSmallestFile()).isNull();
        assertThat(aggregatedProcessingStatistic.getLargestFile()).isNull();
    }

    @Test
    void statsContainerCombine_emptyOtherContainer_keepsCurrentMinMax() {
        // Given
        FlowProcessingStats.StatsContainer containerA = new FlowProcessingStats.StatsContainer();
        containerA.accumulate(createResult(FileProcessingStatus.CHECKED, Path.of("A.java"), 200, false));
        FlowProcessingStats.StatsContainer emptyContainer = new FlowProcessingStats.StatsContainer();

        // When
        FlowProcessingStats.StatsContainer merged = containerA.combine(emptyContainer);

        // Then
        FlowProcessingStats.AggregatedProcessingStatistic aggregatedProcessingStatistic = merged.toAggregatedStats();
        assertThat(aggregatedProcessingStatistic.getSmallestFile())
                .isNotNull()
                .extracting(FileProcessingStatistic::getSizeInBytes)
                .isEqualTo(200L);
        assertThat(aggregatedProcessingStatistic.getLargestFile())
                .isNotNull()
                .extracting(FileProcessingStatistic::getSizeInBytes)
                .isEqualTo(200L);
    }

    @Test
    void statsContainerCombine_twoNonEmptyContainers_correctlyMergesMinMax() {
        // Given
        FlowProcessingStats.StatsContainer containerA = new FlowProcessingStats.StatsContainer();
        containerA.accumulate(createResult(FileProcessingStatus.CHECKED, Path.of("Small.java"), 100, false));

        FlowProcessingStats.StatsContainer containerB = new FlowProcessingStats.StatsContainer();
        containerB.accumulate(createResult(FileProcessingStatus.CHECKED, Path.of("Large.java"), 1000, false));

        // When
        FlowProcessingStats.StatsContainer merged = containerA.combine(containerB);

        // Then
        FlowProcessingStats.AggregatedProcessingStatistic aggregatedProcessingStatistic = merged.toAggregatedStats();
        assertThat(aggregatedProcessingStatistic.getFileCount()).isEqualTo(2);
        assertThat(aggregatedProcessingStatistic.getSmallestFile())
                .isNotNull()
                .extracting(FileProcessingStatistic::getSizeInBytes)
                .isEqualTo(100L);
        assertThat(aggregatedProcessingStatistic.getLargestFile())
                .isNotNull()
                .extracting(FileProcessingStatistic::getSizeInBytes)
                .isEqualTo(1000L);
    }
}

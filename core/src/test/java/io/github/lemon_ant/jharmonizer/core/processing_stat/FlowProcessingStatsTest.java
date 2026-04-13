package io.github.lemon_ant.jharmonizer.core.processing_stat;

import static io.github.lemon_ant.jharmonizer.core.flow.FileProcessingResultTestCreator.create;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.flow.FileProcessingResult;
import io.github.lemon_ant.jharmonizer.core.flow.FileProcessingStatus;
import io.github.lemon_ant.jharmonizer.core.processing_stat.FlowProcessingStats.AggregatedProcessingStatistic;
import io.github.lemon_ant.jharmonizer.core.processing_stat.FlowProcessingStats.StatsContainer;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class FlowProcessingStatsTest {

    @Test
    void statsCollector_singleCheckedFile_countsCorrectly() {
        // Given
        FileProcessingResult checkedResult =
                create(Path.of("A.java"), FileProcessingStatus.CHECKED, false, 100L, 50L, 50L, 200L, 1000L);
        Collector<FileProcessingResult, ?, AggregatedProcessingStatistic> collector =
                FlowProcessingStats.statsCollector();

        // When
        AggregatedProcessingStatistic stats = Stream.of(checkedResult).collect(collector);

        // Then
        assertThat(stats.getFileCount()).isEqualTo(1L);
        assertThat(stats.getStatusCounts()).containsKey(FileProcessingStatus.CHECKED);
        assertThat(stats.getFilesWithUnexpectedErrors()).isEmpty();
        assertThat(stats.getStopTriggerPaths()).isEmpty();
    }

    @Test
    void statsCollector_errorFile_addsToUnexpectedErrorPaths() {
        // Given
        FileProcessingResult errorResult =
                create(Path.of("Bad.java"), FileProcessingStatus.ERROR, false, 0L, 0L, 0L, 0L, 0L);
        Collector<FileProcessingResult, ?, AggregatedProcessingStatistic> collector =
                FlowProcessingStats.statsCollector();

        // When
        AggregatedProcessingStatistic stats = Stream.of(errorResult).collect(collector);

        // Then
        assertThat(stats.getFilesWithUnexpectedErrors()).containsExactly(Path.of("Bad.java"));
        assertThat(stats.getStatusCounts()).containsKey(FileProcessingStatus.ERROR);
    }

    @Test
    void statsCollector_stopRequestedFile_addsToStopTriggerPaths() {
        // Given
        FileProcessingResult stopResult =
                create(Path.of("Stop.java"), FileProcessingStatus.REORDERED, true, 100L, 50L, 50L, 200L, 1000L);
        Collector<FileProcessingResult, ?, AggregatedProcessingStatistic> collector =
                FlowProcessingStats.statsCollector();

        // When
        AggregatedProcessingStatistic stats = Stream.of(stopResult).collect(collector);

        // Then
        assertThat(stats.getStopTriggerPaths()).containsExactly(Path.of("Stop.java"));
    }

    @Test
    void statsCollector_smallerAndLargerFiles_tracksMinAndMax() {
        // Given
        FileProcessingResult smallFile =
                create(Path.of("Small.java"), FileProcessingStatus.CHECKED, false, 10L, 50L, 50L, 200L, 1000L);
        FileProcessingResult largeFile =
                create(Path.of("Large.java"), FileProcessingStatus.CHECKED, false, 10000L, 50L, 50L, 200L, 1000L);
        Collector<FileProcessingResult, ?, AggregatedProcessingStatistic> collector =
                FlowProcessingStats.statsCollector();

        // When
        AggregatedProcessingStatistic stats = Stream.of(smallFile, largeFile).collect(collector);

        // Then
        assertThat(stats.getSmallestFile()).isNotNull();
        assertThat(stats.getLargestFile()).isNotNull();
        assertThat(stats.getSmallestFile().getPath()).isEqualTo(Path.of("Small.java"));
        assertThat(stats.getLargestFile().getPath()).isEqualTo(Path.of("Large.java"));
    }

    @Test
    void combine_twoContainers_mergesAllFields() {
        // Given
        StatsContainer first = new StatsContainer();
        first.accumulate(create(Path.of("A.java"), FileProcessingStatus.CHECKED, false, 100L, 50L, 50L, 200L, 1000L));

        StatsContainer second = new StatsContainer();
        second.accumulate(
                create(Path.of("B.java"), FileProcessingStatus.REORDERED, false, 200L, 50L, 50L, 200L, 2000L));

        // When
        StatsContainer combined = first.combine(second);
        AggregatedProcessingStatistic stats = combined.toAggregatedStats();

        // Then
        assertThat(stats.getFileCount()).isEqualTo(2L);
        assertThat(stats.getStatusCounts()).containsKeys(FileProcessingStatus.CHECKED, FileProcessingStatus.REORDERED);
    }

    @Test
    void combine_containerWithSmallerFile_picksMinCorrectly() {
        // Given
        StatsContainer first = new StatsContainer();
        first.accumulate(
                create(Path.of("Large.java"), FileProcessingStatus.CHECKED, false, 9000L, 50L, 50L, 200L, 1000L));

        StatsContainer second = new StatsContainer();
        second.accumulate(
                create(Path.of("Tiny.java"), FileProcessingStatus.CHECKED, false, 10L, 50L, 50L, 200L, 1000L));

        // When
        AggregatedProcessingStatistic stats = first.combine(second).toAggregatedStats();

        // Then
        assertThat(stats.getSmallestFile()).isNotNull();
        assertThat(stats.getSmallestFile().getPath()).isEqualTo(Path.of("Tiny.java"));
        assertThat(stats.getLargestFile()).isNotNull();
        assertThat(stats.getLargestFile().getPath()).isEqualTo(Path.of("Large.java"));
    }

    @Test
    void combine_emptyContainers_producesZeroStats() {
        // Given
        StatsContainer first = new StatsContainer();
        StatsContainer second = new StatsContainer();

        // When
        AggregatedProcessingStatistic stats = first.combine(second).toAggregatedStats();

        // Then
        assertThat(stats.getFileCount()).isZero();
        assertThat(stats.getSmallestFile()).isNull();
        assertThat(stats.getLargestFile()).isNull();
    }

    @Test
    void toAggregatedStats_noFiles_returnsZeroAverages() {
        // Given
        StatsContainer container = new StatsContainer();

        // When
        AggregatedProcessingStatistic stats = container.toAggregatedStats();

        // Then
        assertThat(stats.calculateAverageProcessingTime()).isZero();
        assertThat(stats.calculateAverageSize()).isZero();
    }

    @Test
    void toAggregatedStats_withFiles_computesNonZeroAverages() {
        // Given
        StatsContainer container = new StatsContainer();
        container.accumulate(
                create(Path.of("A.java"), FileProcessingStatus.CHECKED, false, 100L, 1000L, 2000L, 500L, 3000L));

        // When
        AggregatedProcessingStatistic stats = container.toAggregatedStats();

        // Then
        assertThat(stats.calculateAverageProcessingTime()).isGreaterThan(0L);
        assertThat(stats.calculateAverageSize()).isGreaterThan(0L);
    }

    @Test
    void computeNonConformingFileCount_reorderedAndFormatted_sumsCorrectly() {
        // Given
        Collector<FileProcessingResult, ?, AggregatedProcessingStatistic> collector =
                FlowProcessingStats.statsCollector();
        List<FileProcessingResult> results = List.of(
                create(Path.of("A.java"), FileProcessingStatus.REORDERED, false, 100L, 50L, 50L, 200L, 1000L),
                create(Path.of("B.java"), FileProcessingStatus.FORMATTED, false, 100L, 50L, 50L, 200L, 1000L),
                create(Path.of("C.java"), FileProcessingStatus.CHECKED, false, 100L, 50L, 50L, 200L, 1000L));

        // When
        AggregatedProcessingStatistic stats = results.stream().collect(collector);

        // Then
        assertThat(stats.computeNonConformingFileCount()).isEqualTo(2L);
    }

    @Test
    void calculatePhasePercents_withPositiveTotalTime_returnsNonZeroPercents() {
        // Given
        StatsContainer container = new StatsContainer();
        container.accumulate(create(
                Path.of("A.java"),
                FileProcessingStatus.CHECKED,
                false,
                100L,
                1_000_000L,
                2_000_000L,
                500_000L,
                3_000_000L));

        // When
        AggregatedProcessingStatistic stats = container.toAggregatedStats();

        // Then
        assertThat(stats.calculateParsingTimePercent()).isGreaterThan(0.0);
        assertThat(stats.calculateSortingTimePercent()).isGreaterThan(0.0);
        assertThat(stats.calculateSerializationTimePercent()).isGreaterThan(0.0);
        assertThat(stats.calculateFormattingTimePercent()).isGreaterThan(0.0);
    }

    @Test
    void calculatePhasePercents_withZeroTotalTime_returnsZeroPercents() {
        // Given
        StatsContainer container = new StatsContainer();
        container.accumulate(create(Path.of("A.java"), FileProcessingStatus.CHECKED, false, 100L, 0L, 0L, 0L, 0L));

        // When
        AggregatedProcessingStatistic stats = container.toAggregatedStats();

        // Then
        assertThat(stats.calculateParsingTimePercent()).isZero();
        assertThat(stats.calculateSortingTimePercent()).isZero();
    }
}

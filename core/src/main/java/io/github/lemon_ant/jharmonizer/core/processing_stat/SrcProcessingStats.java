package io.github.lemon_ant.jharmonizer.core.processing_stat;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingResult;
import io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collector;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;

/**
 * Collector-based aggregation of per-file processing statistics into a single summary.
 * Thread-safe and suitable for use with parallel streams.
 */
@UtilityClass
// TODO Review this
public class SrcProcessingStats {

    // Collector for parallel processing
    /**
     * Performs the stats collector.
     * @return the result
     */
    @NonNull
    public Collector<FlowProcessingResult, ?, AggregatedProcessingStatistic> statsCollector() {
        return Collector.of(
                StatsContainer::new,
                StatsContainer::accumulate,
                StatsContainer::combine,
                StatsContainer::toAggregatedStats,
                Collector.Characteristics.CONCURRENT,
                Collector.Characteristics.UNORDERED);
    }

    // Aggregated statistics after full parse run
    @Value
    public static class AggregatedProcessingStatistic {
        public static final int MAX_PATH_LENGTH = 100;
        long fileCount;
        long totalSize;
        long totalProcessingTimeNanos;
        long totalParsingTimeNanos;
        long totalSortingTimeNanos;
        long totalSerializationTimeNanos;
        long totalFormattingTimeNanos;
        long wallClockTimeNanos;

        @Nullable
        FileProcessingStatistic smallestFile;

        @Nullable
        FileProcessingStatistic largestFile;

        @NonNull
        List<@NonNull Path> filesWithUnexpectedErrors;

        @NonNull
        Map<@NonNull FlowProcessingStatus, @NonNull Long> statusCounts;

        @Builder(access = AccessLevel.PACKAGE)
        private AggregatedProcessingStatistic(
                long fileCount,
                long totalSize,
                long totalProcessingTimeNanos,
                long totalParsingTimeNanos,
                long totalSortingTimeNanos,
                long totalSerializationTimeNanos,
                long totalFormattingTimeNanos,
                long wallClockTimeNanos,
                @Nullable FileProcessingStatistic smallestFile,
                @Nullable FileProcessingStatistic largestFile,
                @NonNull List<@NonNull Path> filesWithUnexpectedErrors,
                @NonNull Map<@NonNull FlowProcessingStatus, @NonNull Long> statusCounts) {
            this.fileCount = fileCount;
            this.totalSize = totalSize;
            this.totalProcessingTimeNanos = totalProcessingTimeNanos;
            this.totalParsingTimeNanos = totalParsingTimeNanos;
            this.totalSortingTimeNanos = totalSortingTimeNanos;
            this.totalSerializationTimeNanos = totalSerializationTimeNanos;
            this.totalFormattingTimeNanos = totalFormattingTimeNanos;
            this.wallClockTimeNanos = wallClockTimeNanos;
            this.smallestFile = smallestFile;
            this.largestFile = largestFile;
            this.filesWithUnexpectedErrors = Collections.unmodifiableList(filesWithUnexpectedErrors);
            this.statusCounts = Collections.unmodifiableMap(statusCounts);
        }

        /**
         * Computes the number of files that do not conform to the expected order or formatting.
         * Relevant for check flows where {@code REORDERED} and {@code FORMATTED} indicate violations.
         *
         * @return count of non-conforming files
         */
        public long computeNonConformingFileCount() {
            return statusCounts.getOrDefault(FlowProcessingStatus.REORDERED, 0L)
                    + statusCounts.getOrDefault(FlowProcessingStatus.FORMATTED, 0L);
        }

        // Average time spent on processing a file
        /**
         * Performs the calculate average processing time.
         * @return the result
         */
        long calculateAverageProcessingTime() {
            return fileCount > 0 ? totalProcessingTimeNanos / fileCount : 0;
        }

        // Average size of all files processed
        /**
         * Calculates the average processed file size.
         *
         * @return the average size
         */
        long calculateAverageSize() {
            return fileCount > 0 ? totalSize / fileCount : 0;
        }

        /**
         * Calculates the parsing time percentage.
         *
         * @return the parsing time percentage
         */
        double calculateParsingTimePercent() {
            return calculatePhasePercent(totalParsingTimeNanos);
        }

        /**
         * Calculates the sorting time percentage.
         *
         * @return the sorting time percentage
         */
        double calculateSortingTimePercent() {
            return calculatePhasePercent(totalSortingTimeNanos);
        }

        /**
         * Calculates the serialization time percentage.
         *
         * @return the serialization time percentage
         */
        double calculateSerializationTimePercent() {
            return calculatePhasePercent(totalSerializationTimeNanos);
        }

        /**
         * Calculates the formatting time percentage.
         *
         * @return the formatting time percentage
         */
        double calculateFormattingTimePercent() {
            return calculatePhasePercent(totalFormattingTimeNanos);
        }

        private double calculatePhasePercent(long phaseTotalNanos) {
            if (totalProcessingTimeNanos <= 0) {
                return 0D;
            }
            return (phaseTotalNanos * 100D) / totalProcessingTimeNanos;
        }
    }

    // Statistics container for collector
    static class StatsContainer {
        private final AtomicLong wallClockStartNanos = new AtomicLong(System.nanoTime());
        private final LongAdder count = new LongAdder();
        private final AtomicReference<FileProcessingStatistic> maxSize = new AtomicReference<>();
        private final AtomicReference<FileProcessingStatistic> minSize = new AtomicReference<>();
        private final List<Path> unexpectedErrorPaths = Collections.synchronizedList(new ArrayList<>());
        private final Map<FlowProcessingStatus, LongAdder> statusCounts = new ConcurrentHashMap<>();
        private final LongAdder totalSize = new LongAdder();
        private final LongAdder totalTime = new LongAdder();
        private final LongAdder totalParsingTime = new LongAdder();
        private final LongAdder totalSortingTime = new LongAdder();
        private final LongAdder totalSerializationTime = new LongAdder();
        private final LongAdder totalFormattingTime = new LongAdder();

        /**
         * Performs the accumulate.
         * @param flowProcessingResult the result
         */
        void accumulate(@NonNull FlowProcessingResult flowProcessingResult) {
            FileProcessingStatistic stats = FileProcessingStatistic.convert(flowProcessingResult);
            count.increment();
            totalSize.add(stats.getSize());
            totalTime.add(stats.getProcessingTimeNanos());
            totalParsingTime.add(flowProcessingResult.getParsingStatistic().getParsingTimeInNanos());
            totalSortingTime.add(flowProcessingResult.getSortingStatistic().getSortingTimeInNanos());
            totalSerializationTime.add(stats.getSerializationTimeNanos());
            totalFormattingTime.add(
                    flowProcessingResult.getFormattingStatistic().getFormattingTimeInNanos());
            FlowProcessingStatus status = flowProcessingResult.getFlowProcessingStatus();
            if (status == FlowProcessingStatus.ERROR) {
                unexpectedErrorPaths.add(flowProcessingResult.getPath());
            }
            statusCounts.computeIfAbsent(status, key -> new LongAdder()).increment();

            minSize.accumulateAndGet(
                    stats, (current, next) -> current == null || next.getSize() < current.getSize() ? next : current);

            maxSize.accumulateAndGet(
                    stats, (current, next) -> current == null || next.getSize() > current.getSize() ? next : current);
        }

        /**
         * Performs the combine.
         * @param other the object to compare with
         * @return the result
         */
        @NonNull
        StatsContainer combine(@NonNull StatsContainer other) {
            wallClockStartNanos.accumulateAndGet(other.wallClockStartNanos.get(), Math::min);
            count.add(other.count.sum());
            totalSize.add(other.totalSize.sum());
            totalTime.add(other.totalTime.sum());
            totalParsingTime.add(other.totalParsingTime.sum());
            totalSortingTime.add(other.totalSortingTime.sum());
            totalSerializationTime.add(other.totalSerializationTime.sum());
            totalFormattingTime.add(other.totalFormattingTime.sum());
            unexpectedErrorPaths.addAll(other.unexpectedErrorPaths);
            other.statusCounts.forEach((status, adder) ->
                    statusCounts.computeIfAbsent(status, key -> new LongAdder()).add(adder.sum()));

            minSize.accumulateAndGet(
                    other.minSize.get(),
                    (current, next) ->
                            current == null || (next != null && next.getSize() < current.getSize()) ? next : current);

            maxSize.accumulateAndGet(
                    other.maxSize.get(),
                    (current, next) ->
                            current == null || (next != null && next.getSize() > current.getSize()) ? next : current);

            return this;
        }

        /**
         * Performs the to aggregated stats.
         * @return the result
         */
        @NonNull
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        AggregatedProcessingStatistic toAggregatedStats() {
            long wallClockElapsedNanos = System.nanoTime() - wallClockStartNanos.get();
            Map<FlowProcessingStatus, Long> resolvedStatusCounts = new EnumMap<>(FlowProcessingStatus.class);
            statusCounts.forEach((status, adder) -> resolvedStatusCounts.put(status, adder.sum()));
            return AggregatedProcessingStatistic.builder()
                    .fileCount(count.sum())
                    .totalSize(totalSize.sum())
                    .totalProcessingTimeNanos(totalTime.sum())
                    .totalParsingTimeNanos(totalParsingTime.sum())
                    .totalSortingTimeNanos(totalSortingTime.sum())
                    .totalSerializationTimeNanos(totalSerializationTime.sum())
                    .totalFormattingTimeNanos(totalFormattingTime.sum())
                    .wallClockTimeNanos(wallClockElapsedNanos)
                    .smallestFile(minSize.get())
                    .largestFile(maxSize.get())
                    .filesWithUnexpectedErrors(Collections.unmodifiableList(unexpectedErrorPaths))
                    .statusCounts(resolvedStatusCounts)
                    .build();
        }
    }
}

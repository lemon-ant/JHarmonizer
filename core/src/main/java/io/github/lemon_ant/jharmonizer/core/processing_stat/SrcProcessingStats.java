package io.github.lemon_ant.jharmonizer.core.processing_stat;

import static io.github.lemon_ant.jharmonizer.core.processing_stat.HumanReadableFormatsUtils.formatBytes;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.HumanReadableFormatsUtils.formatHmsMillisFromNanos;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.HumanReadableFormatsUtils.formatSecondsMicrosecondsFromNanos;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.PathDisplayFormatUtil.abbreviatePathForDisplay;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingResult;
import io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
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
    public Collector<FlowProcessingResult, StatsContainer, AggregatedProcessingStatistic> statsCollector() {
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

        @Nullable
        FileProcessingStatistic smallestFile;

        @Nullable
        FileProcessingStatistic largestFile;

        @NonNull
        List<@NonNull Path> filesWithUnexpectedErrors;

        AggregatedProcessingStatistic(
                long fileCount,
                long totalSize,
                long totalProcessingTimeNanos,
                @Nullable FileProcessingStatistic smallestFile,
                @Nullable FileProcessingStatistic largestFile,
                @NonNull List<@NonNull Path> filesWithUnexpectedErrors) {
            this.fileCount = fileCount;
            this.totalSize = totalSize;
            this.totalProcessingTimeNanos = totalProcessingTimeNanos;
            this.smallestFile = smallestFile;
            this.largestFile = largestFile;
            this.filesWithUnexpectedErrors = Collections.unmodifiableList(filesWithUnexpectedErrors);
        }

        @Override
        public String toString() {
            return String.format(
                    "Harmonization result:%nFiles processed: %,d%n" + "Total size: %s%n" + "Average size: %s%n"
                            + "Min size: %s %s%n"
                            + "Max size: %s %s%n"
                            + "Total processing time: %s%n"
                            + "Average processing time: %s s/file%n"
                            + "Files with unexpected internal errors: %s",
                    fileCount,
                    formatBytes(totalSize),
                    formatBytes(calculateAverageSize()),
                    formatBytes(Optional.ofNullable(smallestFile)
                            .map(FileProcessingStatistic::getSize)
                            .orElse(0L)),
                    Optional.ofNullable(smallestFile)
                            .map(FileProcessingStatistic::getPath)
                            .map(path -> " (" + abbreviatePathForDisplay(path, MAX_PATH_LENGTH) + ")")
                            .orElse(""),
                    formatBytes(Optional.ofNullable(largestFile)
                            .map(FileProcessingStatistic::getSize)
                            .orElse(0L)),
                    Optional.ofNullable(largestFile)
                            .map(FileProcessingStatistic::getPath)
                            .map(path -> " (" + abbreviatePathForDisplay(path, MAX_PATH_LENGTH) + ")")
                            .orElse(""),
                    formatHmsMillisFromNanos(totalProcessingTimeNanos),
                    formatSecondsMicrosecondsFromNanos(calculateAverageProcessingTime()),
                    formatUnexpectedErrorFilesForDisplay());
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
         * Performs the calculate average size.
         * @return the result
         */
        long calculateAverageSize() {
            return fileCount > 0 ? totalSize / fileCount : 0;
        }

        @NonNull
        private String formatUnexpectedErrorFilesForDisplay() {
            if (filesWithUnexpectedErrors.isEmpty()) {
                return "none";
            }
            return filesWithUnexpectedErrors.stream()
                    .map(path -> abbreviatePathForDisplay(path, MAX_PATH_LENGTH))
                    .sorted()
                    .collect(Collectors.joining(", ", "[", "]"));
        }
    }

    // Statistics container for collector
    @NoArgsConstructor
    public class StatsContainer {
        private final LongAdder count = new LongAdder();
        private final AtomicReference<FileProcessingStatistic> maxSize = new AtomicReference<>();
        private final AtomicReference<FileProcessingStatistic> minSize = new AtomicReference<>();
        private final List<Path> unexpectedErrorPaths = Collections.synchronizedList(new ArrayList<>());
        private final LongAdder totalSize = new LongAdder();
        private final LongAdder totalTime = new LongAdder();

        /**
         * Performs the accumulate.
         * @param flowProcessingResult the result
         */
        void accumulate(@NonNull FlowProcessingResult flowProcessingResult) {
            FileProcessingStatistic stats = FileProcessingStatistic.convert(flowProcessingResult);
            count.increment();
            totalSize.add(stats.getSize());
            totalTime.add(stats.getProcessingTimeNanos());
            if (flowProcessingResult.getFlowProcessingStatus() == FlowProcessingStatus.ERROR) {
                unexpectedErrorPaths.add(flowProcessingResult.getPath());
            }

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
            count.add(other.count.sum());
            totalSize.add(other.totalSize.sum());
            totalTime.add(other.totalTime.sum());
            unexpectedErrorPaths.addAll(other.unexpectedErrorPaths);

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
        AggregatedProcessingStatistic toAggregatedStats() {
            return new AggregatedProcessingStatistic(
                    count.sum(),
                    totalSize.sum(),
                    totalTime.sum(),
                    minSize.get(),
                    maxSize.get(),
                    Collections.unmodifiableList(unexpectedErrorPaths));
        }
    }
}

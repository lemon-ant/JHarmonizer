package io.github.lemon_ant.jharmonizer.core.processing_stat;

import static io.github.lemon_ant.jharmonizer.core.processing_stat.HumanReadableFormatsUtils.formatBytes;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.HumanReadableFormatsUtils.formatHmsMillisFromNanos;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.HumanReadableFormatsUtils.formatSecondsMicrosecondsFromNanos;
import static io.github.lemon_ant.jharmonizer.core.processing_stat.PathDisplayFormatUtil.abbreviatePathForDisplay;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collector;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.UtilityClass;

@UtilityClass
// TODO Review this
public class SourceProcessingStats {

    // Collector for parallel processing
    public Collector<FileProcessingStatistic, StatsContainer, AggregatedProcessingStatistic> statsCollector() {
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

        @Override
        public String toString() {
            return String.format(
                    "Harmonization result:%nFiles processed: %,d%n" + "Total size: %s%n" + "Average size: %s%n"
                            + "Min size: %s %s%n"
                            + "Max size: %s %s%n"
                            + "Total processing time: %s%n"
                            + "Average processing time: %s s/file",
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
                    formatSecondsMicrosecondsFromNanos(calculateAverageProcessingTime()));
        }

        // Average time spent on processing a file
        long calculateAverageProcessingTime() {
            return fileCount > 0 ? totalProcessingTimeNanos / fileCount : 0;
        }

        // Average size of all files processed
        long calculateAverageSize() {
            return fileCount > 0 ? totalSize / fileCount : 0;
        }
    }

    // Statistics container for collector
    @NoArgsConstructor
    public class StatsContainer {
        private final LongAdder count = new LongAdder();
        private final AtomicReference<FileProcessingStatistic> maxSize = new AtomicReference<>();
        private final AtomicReference<FileProcessingStatistic> minSize = new AtomicReference<>();
        private final LongAdder totalSize = new LongAdder();
        private final LongAdder totalTime = new LongAdder();

        void accumulate(FileProcessingStatistic stats) {
            count.increment();
            totalSize.add(stats.getSize());
            totalTime.add(stats.getProcessingTimeNanos());

            minSize.accumulateAndGet(
                    stats, (current, next) -> current == null || next.getSize() < current.getSize() ? next : current);

            maxSize.accumulateAndGet(
                    stats, (current, next) -> current == null || next.getSize() > current.getSize() ? next : current);
        }

        StatsContainer combine(StatsContainer other) {
            count.add(other.count.sum());
            totalSize.add(other.totalSize.sum());
            totalTime.add(other.totalTime.sum());

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

        AggregatedProcessingStatistic toAggregatedStats() {
            return new AggregatedProcessingStatistic(
                    count.sum(), totalSize.sum(), totalTime.sum(), minSize.get(), maxSize.get());
        }
    }
}

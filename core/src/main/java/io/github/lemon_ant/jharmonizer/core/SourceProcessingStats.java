package io.github.lemon_ant.jharmonizer.core;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
                    "Files processed: %,d%n" + "Total size: %,d bytes%n" + "Average size: %,d bytes%n"
                            + "Min size: %,d byte%s%n"
                            + "Max size: %,d bytes%s%n"
                            + "Total processing time: %,d ms%n"
                            + "Average processing time: %,d ms/file",
                    fileCount,
                    totalSize,
                    calculateAverageSize(),
                    Optional.ofNullable(smallestFile)
                            .map(FileProcessingStatistic::getSize)
                            .orElse(0L),
                    Optional.ofNullable(smallestFile)
                            .map(FileProcessingStatistic::getPath)
                            .map(path -> " (" + path + ")")
                            .orElse(""),
                    Optional.ofNullable(largestFile)
                            .map(FileProcessingStatistic::getSize)
                            .orElse(0L),
                    Optional.ofNullable(largestFile)
                            .map(FileProcessingStatistic::getPath)
                            .map(path -> " (" + path + ")")
                            .orElse(""),
                    TimeUnit.NANOSECONDS.toMillis(totalProcessingTimeNanos),
                    TimeUnit.NANOSECONDS.toMillis(calculateAverageProcessingTime()));
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
    class StatsContainer {
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

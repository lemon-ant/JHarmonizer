package io.github.lemon_ant.jharmonizer.core;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collector;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.UtilityClass;

@UtilityClass
// TODO Review this
public class SourceProcessingStats {

    // Collector for parallel processing
    public Collector<FileStats, StatsContainer, AggregatedStats> statsCollector() {
        return Collector.of(
                StatsContainer::new,
                StatsContainer::accumulate,
                StatsContainer::combine,
                StatsContainer::toAggregatedStats,
                Collector.Characteristics.CONCURRENT,
                Collector.Characteristics.UNORDERED);
    }

    // Aggregated statistics after full parse run
    public record AggregatedStats(
            long fileCount,
            long totalSize,
            long totalProcessingTimeNanos,
            FileStats smallestFile,
            FileStats largestFile) {

        @Override
        public String toString() {
            return String.format(
                    "Files processed: %,d%n" + "Total size: %,d bytes%n" + "Average size: %,d bytes%n"
                            + "Min size: %,d bytes (%s)%n"
                            + "Max size: %,d bytes (%s)%n"
                            + "Total processing time: %,d ms%n"
                            + "Average processing time: %,d ms/file",
                    fileCount,
                    totalSize,
                    calculateAverageSize(),
                    smallestFile == null ? 0 : smallestFile.size,
                    smallestFile == null ? "" : smallestFile.path.getFileName(),
                    largestFile == null ? 0 : largestFile.size,
                    largestFile == null ? "" : largestFile.path.getFileName(),
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

    // File processing statistics record
    @Value
    @Builder
    public class FileStats {
        Path path;
        long processingTimeNanos;
        long size;
    }

    // Statistics container for collector
    @NoArgsConstructor
    public class StatsContainer {
        final LongAdder count = new LongAdder();
        final AtomicReference<FileStats> maxSize = new AtomicReference<>();
        final AtomicReference<FileStats> minSize = new AtomicReference<>();
        final LongAdder totalSize = new LongAdder();
        final LongAdder totalTime = new LongAdder();

        void accumulate(FileStats stats) {
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

        AggregatedStats toAggregatedStats() {
            return new AggregatedStats(count.sum(), totalSize.sum(), totalTime.sum(), minSize.get(), maxSize.get());
        }
    }
}

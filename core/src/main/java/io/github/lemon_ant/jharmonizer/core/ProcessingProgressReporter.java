package io.github.lemon_ant.jharmonizer.core;

import io.github.lemon_ant.jharmonizer.core.flow.FlowProcessingStatus;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Thread-safe progress reporter that emits periodic batch summaries at INFO level.
 * Designed for use with parallel streams — counters are updated atomically and
 * an INFO progress line is logged every {@value #PROGRESS_BATCH_SIZE} files.
 */
@Slf4j
final class ProcessingProgressReporter {

    static final int PROGRESS_BATCH_SIZE = 100;

    private final AtomicLong totalProcessed = new AtomicLong();
    private final LongAdder reorderedCount = new LongAdder();
    private final LongAdder formattedCount = new LongAdder();
    private final LongAdder unchangedCount = new LongAdder();
    private final LongAdder checkedCount = new LongAdder();
    private final LongAdder skippedCount = new LongAdder();
    private final LongAdder errorCount = new LongAdder();

    /**
     * Returns the total number of files recorded so far.
     *
     * @return current count of processed files
     */
    long getTotalProcessedCount() {
        return totalProcessed.get();
    }

    /**
     * Records a single processed file and logs periodic progress at INFO level.
     *
     * @param status the processing outcome for the file
     */
    void recordProcessedFile(@NonNull FlowProcessingStatus status) {
        long count = totalProcessed.incrementAndGet();
        incrementStatusCounter(status);
        if (count % PROGRESS_BATCH_SIZE == 0) {
            logBatchProgress(count);
        }
    }

    // PMD 7.23.0 ExhaustiveSwitchHasDefault conflicts with NonExhaustiveSwitch for exhaustive enum switches.
    @SuppressWarnings("PMD.ExhaustiveSwitchHasDefault")
    private void incrementStatusCounter(FlowProcessingStatus status) {
        switch (status) {
            case REORDERED -> reorderedCount.increment();
            case FORMATTED -> formattedCount.increment();
            case UNCHANGED -> unchangedCount.increment();
            case CHECKED -> checkedCount.increment();
            case SKIPPED -> skippedCount.increment();
            case ERROR -> errorCount.increment();
            default -> throw new IllegalStateException("Unexpected value: " + status);
        }
    }

    @SuppressWarnings("PMD.GuardLogStatement")
    private void logBatchProgress(long count) {
        log.info(
                "Progress: {} files processed (reordered={}, formatted={}, unchanged={}, checked={}, skipped={}, errors={})",
                count,
                reorderedCount.sum(),
                formattedCount.sum(),
                unchangedCount.sum(),
                checkedCount.sum(),
                skippedCount.sum(),
                errorCount.sum());
    }
}

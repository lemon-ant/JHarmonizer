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
     * Records a single processed file and logs periodic progress at INFO level.
     *
     * @param status the processing outcome for the file
     */
    void recordProcessedFile(@NonNull FlowProcessingStatus status) {
        incrementStatusCounter(status);
        long count = totalProcessed.incrementAndGet();
        if (count % PROGRESS_BATCH_SIZE == 0) {
            logBatchProgress(count);
        }
    }

    private void incrementStatusCounter(FlowProcessingStatus status) {
        switch (status) {
            case REORDERED -> reorderedCount.increment();
            case FORMATTED -> formattedCount.increment();
            case UNCHANGED -> unchangedCount.increment();
            case CHECKED -> checkedCount.increment();
            case SKIPPED -> skippedCount.increment();
            case ERROR -> errorCount.increment();
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

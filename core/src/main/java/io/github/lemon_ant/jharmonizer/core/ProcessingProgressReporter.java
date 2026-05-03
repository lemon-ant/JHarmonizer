// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core;

import io.github.lemon_ant.jharmonizer.core.flow.FileProcessingStatus;
import java.util.Locale;
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

    private static final int PROGRESS_BATCH_SIZE = 100;
    private static final String PADDED_NUMBERS_FORMAT = "%4d";

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
    void recordProcessedFile(@NonNull FileProcessingStatus status) {
        long count = totalProcessed.incrementAndGet();
        incrementStatusCounter(status);
        if (count % PROGRESS_BATCH_SIZE == 0) {
            logBatchProgress(count);
        }
    }

    private void incrementStatusCounter(FileProcessingStatus status) {
        switch (status) { // NOPMD - ExhaustiveSwitchHasDefault: defensive default guards against future enum additions
            case REORDERED -> reorderedCount.increment();
            case FORMATTED -> formattedCount.increment();
            case UNCHANGED -> unchangedCount.increment();
            case CHECKED -> checkedCount.increment();
            case SKIPPED -> skippedCount.increment();
            case ERROR -> errorCount.increment();
            default -> throw new IllegalStateException("Unexpected status: " + status);
        }
    }

    @SuppressWarnings("PMD.GuardLogStatement")
    private void logBatchProgress(long count) {
        log.info(
                "JHarmonization: {} files processed (reordered={}, formatted={}, unchanged={}, checked={}, skipped={}, errors={})",
                String.format(Locale.ROOT, PADDED_NUMBERS_FORMAT, count),
                String.format(Locale.ROOT, PADDED_NUMBERS_FORMAT, reorderedCount.sum()),
                String.format(Locale.ROOT, PADDED_NUMBERS_FORMAT, formattedCount.sum()),
                String.format(Locale.ROOT, PADDED_NUMBERS_FORMAT, unchangedCount.sum()),
                String.format(Locale.ROOT, PADDED_NUMBERS_FORMAT, checkedCount.sum()),
                String.format(Locale.ROOT, PADDED_NUMBERS_FORMAT, skippedCount.sum()),
                String.format(Locale.ROOT, PADDED_NUMBERS_FORMAT, errorCount.sum()));
    }
}

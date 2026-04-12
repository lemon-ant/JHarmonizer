package io.github.lemon_ant.jharmonizer.core.flow;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.processing_stat.FlowProcessingStats.AggregatedProcessingStatistic;
import java.util.stream.Stream;
import lombok.NonNull;

/**
 * Contract for a source file processing flow.
 * Each implementation encapsulates a distinct processing strategy
 * (check-all, check-fail-fast, reorder) and controls its own stream
 * pipeline, success criteria, and completion logging.
 */
public interface IFlow {
    /**
     * Processes a single source file with the current flow strategy.
     *
     * @param srcFile the source file to process
     * @return the processing result for the source file
     */
    @NonNull
    FileProcessingResult processSrc(@NonNull SrcFile srcFile);

    /**
     * Processes a stream of source files, applying the flow strategy to each.
     * Implementations may extend the pipeline with additional steps
     * (e.g. fail-fast flows add early termination logic).
     *
     * @param srcFiles the stream of source files to process
     * @return a stream of per-file processing results
     */
    @NonNull
    Stream<FileProcessingResult> processStream(@NonNull Stream<SrcFile> srcFiles);

    /**
     * Determines whether the processing run was successful based on the aggregated statistics.
     *
     * @param stats the aggregated processing statistics
     * @return {@code true} if the flow considers the run successful
     */
    boolean isSuccessful(@NonNull AggregatedProcessingStatistic stats);

    /**
     * Logs a human-readable completion summary for the processing run.
     *
     * @param stats the aggregated processing statistics
     */
    void logCompletion(@NonNull AggregatedProcessingStatistic stats);
}

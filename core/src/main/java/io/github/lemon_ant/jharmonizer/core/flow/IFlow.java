package io.github.lemon_ant.jharmonizer.core.flow;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import java.util.stream.Stream;
import lombok.NonNull;

/**
 * Contract for a source file processing flow.
 * Each implementation encapsulates a distinct processing strategy
 * (check-all, check-fail-fast, reorder) and controls its own stream
 * pipeline and success criteria.
 */
public interface IFlow {

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
     * Determines whether the processing run was successful based on
     * whether any files were modified (or would need modification).
     *
     * @param hasModifications {@code true} if at least one file was modified or non-conforming
     * @return {@code true} if the flow considers the run successful
     */
    boolean isSuccessful(boolean hasModifications);
}

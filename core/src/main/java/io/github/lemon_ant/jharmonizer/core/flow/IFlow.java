package io.github.lemon_ant.jharmonizer.core.flow;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import lombok.NonNull;

/**
 * Contract for a single-file processing flow.
 * Each implementation encapsulates a distinct processing strategy
 * (check-all, check-fail-fast, restructure).
 */
@FunctionalInterface
public interface IFlow {
    /**
     * Processes a single source file with the current flow strategy.
     *
     * @param srcFile the source file to process
     * @return the processing result for the source file
     */
    @NonNull
    FlowProcessingResult processSource(@NonNull SrcFile srcFile);
}

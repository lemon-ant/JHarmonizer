package io.github.lemon_ant.jharmonizer.core.flow;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import lombok.NonNull;

/**
 * Contract for a single-file processing flow.
 * Each implementation encapsulates a distinct processing strategy
 * (check-all, check-fail-fast, restructure).
 */
@FunctionalInterface
public interface IFlow {
    @NonNull
    FlowProcessingResult processSource(@NonNull SourceFilesHandler.SrcFile srcFile);
}

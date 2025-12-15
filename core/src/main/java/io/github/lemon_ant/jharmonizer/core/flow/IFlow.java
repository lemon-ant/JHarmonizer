package io.github.lemon_ant.jharmonizer.core.flow;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import lombok.NonNull;

@FunctionalInterface
public interface IFlow {
    @NonNull
    FlowProcessingResult processSource(@NonNull SourceFilesHandler.SrcFile srcFile);
}

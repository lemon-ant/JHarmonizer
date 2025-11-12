package io.github.lemon_ant.jharmonizer.core.flow;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.FileContent;
import lombok.NonNull;

@FunctionalInterface
public interface IFlow {
    @NonNull
    FlowProcessingResult processSource(@NonNull FileContent srcFileContent);
}

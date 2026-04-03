package io.github.lemon_ant.jharmonizer.core.translator;

import java.nio.file.Path;
import lombok.Getter;
import lombok.NonNull;

/**
 * Signals that Spoon failed to build an AST model for a specific source file.
 */
@Getter
public class SpoonModelBuildException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    @NonNull
    private final Path srcPath;

    /**
     * Creates an exception describing a Spoon model-build failure for one source file.
     *
     * @param srcPath the source path that failed to parse
     * @param message the short failure reason suitable for logging
     * @param cause the original Spoon runtime failure
     */
    public SpoonModelBuildException(@NonNull Path srcPath, @NonNull String message, @NonNull RuntimeException cause) {
        super(message, cause);
        this.srcPath = srcPath;
    }
}

package io.github.lemon_ant.jharmonizer.core.flow;

import java.io.Serial;
import java.nio.file.Path;
import lombok.NonNull;

/**
 * Thrown by {@link CheckFailFastFlow} when the formatted output of a source file
 * differs from its original content, indicating that the file is not properly formatted.
 */
public class NotFormattedException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 6019397228008880777L;

    private final String diff; // String of diff output or similar information about the formatting issue
    private final Path offendingFile;

    /**
     * Creates a new NotFormattedException.
     *
     * @param offendingFile the offending file
     * @param diff the diff
     */
    public NotFormattedException(@NonNull Path offendingFile, @NonNull String diff) {
        this.offendingFile = offendingFile;
        this.diff = diff;
    }

    /**
     * Returns the message.
     * @return the message
     */
    @Override
    @NonNull
    public String getMessage() {
        return String.format("[NotFormattedException] File not formatted: %s%n%s", offendingFile.getFileName(), diff);
    }
}

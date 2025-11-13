package io.github.lemon_ant.jharmonizer.core.flow;

import java.io.Serial;
import java.nio.file.Path;

public class NotFormattedException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 6019397228008880777L;

    private final String diff; // String of diff output or similar information about the formatting issue
    private final Path offendingFile;

    public NotFormattedException(Path offendingFile, String diff) {
        this.offendingFile = offendingFile;
        this.diff = diff;
    }

    @Override
    public String getMessage() {
        return String.format("[NotFormattedException] File not formatted: %s%n%s", offendingFile.getFileName(), diff);
    }
}

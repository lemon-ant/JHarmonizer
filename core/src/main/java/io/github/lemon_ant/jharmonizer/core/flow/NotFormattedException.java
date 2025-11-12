package io.github.lemon_ant.jharmonizer.core.flow;

import java.io.Serial;
import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class NotFormattedException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 6019397228008880777L;

    private final Path offendingFile;
    private final String diff; // String of diff output or similar information about the formatting issue

    @Override
    public String getMessage() {
        return String.format("[NotFormattedException] File not formatted: %s%n%s", offendingFile.getFileName(), diff);
    }

    public void printMessage() {
        if (log.isInfoEnabled()) log.info("[NotFormattedException] {}", getMessage());
    }
}

package io.github.lemon_ant.jharmonizer.cli.e2e;

import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import lombok.Value;

@Value
class ExternalCliProcessResult {
    @NonNull
    List<String> command;

    @NonNull
    Path workingDirectory;

    int exitCode;

    @NonNull
    String stdout;

    @NonNull
    String stderr;

    boolean timedOut;

    String combinedOutput() {
        return stdout + System.lineSeparator() + stderr;
    }

    @Override
    public String toString() {
        return "Command: "
                + String.join(" ", command)
                + System.lineSeparator()
                + "Working directory: "
                + workingDirectory
                + System.lineSeparator()
                + "Exit code: "
                + exitCode
                + System.lineSeparator()
                + "Timed out: "
                + timedOut
                + System.lineSeparator()
                + "--- stdout ---"
                + System.lineSeparator()
                + stdout
                + System.lineSeparator()
                + "--- stderr ---"
                + System.lineSeparator()
                + stderr;
    }
}

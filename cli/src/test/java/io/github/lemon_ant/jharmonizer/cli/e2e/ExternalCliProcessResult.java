package io.github.lemon_ant.jharmonizer.cli.e2e;

import java.nio.file.Path;
import java.util.List;

record ExternalCliProcessResult(
        List<String> command, Path workingDirectory, int exitCode, String stdout, String stderr, boolean timedOut) {

    String diagnostics() {
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

    String combinedOutput() {
        return stdout + System.lineSeparator() + stderr;
    }
}

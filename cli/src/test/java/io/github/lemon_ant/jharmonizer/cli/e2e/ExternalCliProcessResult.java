// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.cli.e2e;

import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import lombok.Value;

@Value
class ExternalCliProcessResult {

    @NonNull
    List<String> command;

    int exitCode;

    @NonNull
    String stderr;

    @NonNull
    String stdout;

    boolean timedOut;

    @NonNull
    Path workingDirectory;

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

    String combinedOutput() {
        return stdout + System.lineSeparator() + stderr;
    }
}

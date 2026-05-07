// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.cli.e2e;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders dependent static fields in test classes;
// remove this directive once jharmonizer is upgraded to a version that respects field initialization order.
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

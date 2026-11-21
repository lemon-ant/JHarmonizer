// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.testutils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JavaCompileTestUtils {
    private static final int JAVA_RELEASE = 21;
    private static final String TEST_COMPILE_PREFIX = "test-compile-";

    /**
     * Compiles the supplied Java source files together with the JDK 21 language level.
     * @param srcFilePaths source files belonging to one compilation
     * @param outputDirectoryPath destination for class files and compiler diagnostics
     * @return compiler exit status, output, and saved diagnostic location
     * @throws IOException if the compiler cannot be launched or diagnostics cannot be saved
     * @throws InterruptedException if waiting for the compiler is interrupted
     */
    @NonNull
    public static CompileResult compileJavaSourcesWithRelease21(
            @NonNull List<Path> srcFilePaths, @NonNull Path outputDirectoryPath)
            throws IOException, InterruptedException {
        ensureOutputDirectoryExists(outputDirectoryPath);

        Path diagnosticsPath = outputDirectoryPath.resolve(TEST_COMPILE_PREFIX + "logs.txt");

        List<String> command = new ArrayList<>(List.of(
                "javac",
                "--release",
                Integer.toString(JAVA_RELEASE),
                "-encoding",
                StandardCharsets.UTF_8.name(),
                "-d",
                outputDirectoryPath.toString()));
        for (Path srcFilePath : srcFilePaths) {
            if (!Files.isRegularFile(srcFilePath)) {
                throw new IllegalArgumentException("Expected Java source file to compile, but got: " + srcFilePath);
            }
            command.add(srcFilePath.toAbsolutePath().toString());
        }

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String javacOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int processExitCode = process.waitFor();

        String diagnostics =
                "Command: " + String.join(" ", command) + System.lineSeparator() + javacOutput + System.lineSeparator();
        Files.writeString(
                diagnosticsPath,
                diagnostics,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);

        return new CompileResult(diagnosticsPath, processExitCode, javacOutput);
    }

    private static void ensureOutputDirectoryExists(Path outputDirectoryPath) throws IOException {
        Files.createDirectories(outputDirectoryPath);
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CompileResult {

        @NonNull
        Path diagnosticsPath;

        int exitCode;

        @NonNull
        String output;
    }
}

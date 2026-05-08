// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;

@UtilityClass
class JavaRunMainTestUtils {
    private static final String TEST_RUN_PREFIX = "test-run-";

    static RunResult runJavaMainMethod(@NonNull Path srcFilePath, @NonNull Path classesOutputDirectoryPath)
            throws IOException, InterruptedException {
        E2EFileUtils.requireRegularFile(srcFilePath, "Expected Java source file to run main method from, but got: ");

        String className = resolveClassName(srcFilePath);
        List<String> command = List.of("java", "-cp", classesOutputDirectoryPath.toString(), className);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String javaOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int processExitCode = process.waitFor();

        Path diagnosticsPath = classesOutputDirectoryPath.resolve(TEST_RUN_PREFIX + "logs.txt");
        String diagnostics =
                "Command: " + String.join(" ", command) + System.lineSeparator() + javaOutput + System.lineSeparator();
        Files.writeString(
                diagnosticsPath,
                diagnostics,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);

        return new RunResult(className, diagnosticsPath, processExitCode, javaOutput);
    }

    @NonNull
    private static String extractPackageName(Path javaFile) {
        try (Stream<String> lines = Files.lines(javaFile, StandardCharsets.UTF_8)) {
            return lines.map(String::trim)
                    .filter(line -> line.startsWith("package ") && line.endsWith(";"))
                    .findFirst()
                    .map(line -> line.substring("package ".length(), line.length() - 1)
                            .trim())
                    .orElse("");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to resolve package for source file: " + javaFile, exception);
        }
    }

    @NonNull
    private static String resolveClassName(Path javaFile) {
        String className = javaFile.getFileName().toString().replaceFirst("\\.java$", "");
        String packageName = extractPackageName(javaFile);
        return packageName.isBlank() ? className : packageName + "." + className;
    }

    @Value
    static class RunResult {
        String className;
        Path diagnosticsPath;
        int exitCode;
        String output;
    }
}

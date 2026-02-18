package io.github.lemon_ant.jharmonizer.core.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
class JavaRunMainTestUtils {

    private static final String TEST_RUN_PREFIX = "test-run-";

    static void assertJavaMainMethodsRunSuccessfully(
            @NonNull Path sourceDirectoryPath, @NonNull Path classesOutputDirectoryPath) throws IOException {
        List<String> classNames = resolveTopLevelClassNames(sourceDirectoryPath);
        assertThat(classNames)
                .as("Expected Java source files in directory: %s", sourceDirectoryPath)
                .isNotEmpty();

        Path diagnosticsPath = classesOutputDirectoryPath.resolve(TEST_RUN_PREFIX + "logs.txt");
        String diagnostics = classNames.stream()
                .map(className -> runMainMethodAndGetDiagnostics(className, classesOutputDirectoryPath))
                .collect(Collectors.joining(System.lineSeparator()));

        Files.writeString(diagnosticsPath, diagnostics, StandardCharsets.UTF_8);
    }

    private static String runMainMethodAndGetDiagnostics(String className, Path classesOutputDirectoryPath) {
        List<String> command = List.of("java", "-cp", classesOutputDirectoryPath.toString(), className);

        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String javaOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int processExitCode = process.waitFor();

            assertThat(processExitCode)
                    .as("Expected main method execution to succeed for %s. Output:%n%s", className, javaOutput)
                    .isZero();

            return "Command: " + String.join(" ", command) + System.lineSeparator() + javaOutput;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to execute command: " + String.join(" ", command), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while executing command: " + String.join(" ", command), exception);
        }
    }

    private static List<String> resolveTopLevelClassNames(Path sourceDirectoryPath) throws IOException {
        try (Stream<Path> javaPathStream = SourceFilesHandler.findJavaFiles(sourceDirectoryPath, Set.of(), List.of())) {
            return javaPathStream
                    .map(JavaRunMainTestUtils::resolveClassName)
                    .toList();
        }
    }

    private static String resolveClassName(Path javaFile) {
        String className = javaFile.getFileName().toString().replaceFirst("\\.java$", "");
        String packageName = extractPackageName(javaFile);
        return packageName.isBlank() ? className : packageName + "." + className;
    }

    private static String extractPackageName(Path javaFile) {
        try (Stream<String> lines = Files.lines(javaFile, StandardCharsets.UTF_8)) {
            return lines.map(String::trim)
                    .filter(line -> line.startsWith("package ") && line.endsWith(";"))
                    .findFirst()
                    .map(line -> line.substring("package ".length(), line.length() - 1).trim())
                    .orElse("");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to resolve package for source file: " + javaFile, exception);
        }
    }
}

package io.github.lemon_ant.jharmonizer.core.e2e;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;

@UtilityClass
final class JavaCompileTestUtils {

    static void assertJavaSourcesCompileWithRelease21(@NonNull Path sourceDirectoryPath, @NonNull Path outputDirectoryPath)
            throws IOException, InterruptedException {
        List<Path> javaSources;
        try (Stream<Path> sourcePathStream = Files.walk(sourceDirectoryPath)) {
            javaSources = sourcePathStream
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }

        if (javaSources.isEmpty()) {
            throw new IllegalStateException("No Java sources found in directory: " + sourceDirectoryPath);
        }

        Files.createDirectories(outputDirectoryPath);
        Path diagnosticsPath = outputDirectoryPath.resolve("javac-diagnostics.txt");
        Path javacSourcesArgFilePath = outputDirectoryPath.resolve("javac-sources.argfile");
        String javacArgFileContent = javaSources.stream()
                .map(JavaCompileTestUtils::toJavacArgFileEntry)
                .collect(Collectors.joining(System.lineSeparator()));
        Files.writeString(javacSourcesArgFilePath, javacArgFileContent, StandardCharsets.UTF_8);

        List<String> command = List.of(
                "javac",
                "--release",
                "21",
                "-encoding",
                StandardCharsets.UTF_8.name(),
                "-d",
                outputDirectoryPath.toString(),
                "@" + javacSourcesArgFilePath);

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String javacOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int processExitCode = process.waitFor();
        Files.writeString(diagnosticsPath, javacOutput, StandardCharsets.UTF_8);

        Assertions.assertThat(processExitCode)
                .as("Expected javac --release 21 to compile fixtures under %s. Diagnostics:%n%s"
                        .formatted(sourceDirectoryPath, javacOutput))
                .isZero();
    }

    @NonNull
    static String normalizeSourceForFixtureComparison(@NonNull String sourceCode) {
        return sourceCode
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .lines()
                .map(JavaCompileTestUtils::trimTrailingWhitespace)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("")
                .stripTrailing()
                .concat("\n");
    }

    @NonNull
    private static String trimTrailingWhitespace(@NonNull String textLine) {
        return StringUtils.stripEnd(textLine, null);
    }

    @NonNull
    private static String toJavacArgFileEntry(@NonNull Path sourcePath) {
        String absolutePath = sourcePath.toAbsolutePath().toString();
        String escapedPath = absolutePath.replace("\\", "\\\\").replace("\"", "\\\"");
        return '"' + escapedPath + '"';
    }
}

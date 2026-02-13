package io.github.lemon_ant.jharmonizer.core.e2e;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
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

        List<String> command = new ArrayList<>();
        command.add("javac");
        command.add("--release");
        command.add("21");
        command.add("-encoding");
        command.add(StandardCharsets.UTF_8.name());
        command.add("-d");
        command.add(outputDirectoryPath.toString());
        javaSources.stream().map(Path::toString).forEach(command::add);

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
        int endIndex = textLine.length();
        while (endIndex > 0 && Character.isWhitespace(textLine.charAt(endIndex - 1))) {
            endIndex--;
        }
        return textLine.substring(0, endIndex);
    }
}

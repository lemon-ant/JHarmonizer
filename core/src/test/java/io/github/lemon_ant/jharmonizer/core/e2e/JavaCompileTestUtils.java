package io.github.lemon_ant.jharmonizer.core.e2e;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.assertj.core.api.Assertions;

@UtilityClass
class JavaCompileTestUtils {

    private static final String JAVA_RELEASE = "21";

    static void assertJavaSourcesCompileWithRelease21(@NonNull Path sourceDirectoryPath, @NonNull Path outputDirectoryPath)
            throws IOException, InterruptedException {
        Files.createDirectories(outputDirectoryPath);

        String sourceDirectoryName = sourceDirectoryPath.getFileName() == null
                ? "source-root"
                : sourceDirectoryPath.getFileName().toString();
        Path diagnosticsPath = outputDirectoryPath.resolve(sourceDirectoryName + "-javac-diagnostics.txt");
        Path javacSourcesArgFilePath = outputDirectoryPath.resolve(sourceDirectoryName + "-javac-sources.argfile");

        int javaSourceCount = writeJavaSourcePathsArgFile(sourceDirectoryPath, javacSourcesArgFilePath);
        if (javaSourceCount == 0) {
            throw new IllegalStateException("No Java sources found in directory: " + sourceDirectoryPath);
        }

        List<String> command = List.of(
                "javac",
                "--release",
                JAVA_RELEASE,
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
                .as("Expected javac --release %s to compile fixtures under %s. Diagnostics:%n%s"
                        .formatted(JAVA_RELEASE, sourceDirectoryPath, javacOutput))
                .isZero();
    }

    private static int writeJavaSourcePathsArgFile(Path sourceDirectoryPath, Path javacSourcesArgFilePath)
            throws IOException {
        try (Stream<Path> javaPathStream = SourceFilesHandler.findJavaFiles(sourceDirectoryPath, Set.of(), List.of())) {
            List<String> javacArgFileEntries = javaPathStream
                    .map(JavaCompileTestUtils::toJavacArgFileEntry)
                    .toList();
            Files.writeString(
                    javacSourcesArgFilePath,
                    String.join(System.lineSeparator(), javacArgFileEntries),
                    StandardCharsets.UTF_8);
            return javacArgFileEntries.size();
        }
    }

    @NonNull
    private static String toJavacArgFileEntry(@NonNull Path sourcePath) {
        String absolutePath = sourcePath.toAbsolutePath().toString();
        String escapedPath = absolutePath.replace("\\", "\\\\").replace("\"", "\\\"");
        return '"' + escapedPath + '"';
    }
}

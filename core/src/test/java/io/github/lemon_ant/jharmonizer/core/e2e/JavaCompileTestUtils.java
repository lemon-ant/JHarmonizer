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

    private static final int JAVA_RELEASE = 21;
    private static final String E2E_TEST_ARTIFACT_PREFIX = "source-processor-e2e";

    static void assertJavaSourcesCompileWithRelease21(
            @NonNull Path sourceDirectoryPath, @NonNull Path outputDirectoryPath)
            throws IOException, InterruptedException {
        Files.createDirectories(outputDirectoryPath);

        Path diagnosticsPath = outputDirectoryPath.resolve(E2E_TEST_ARTIFACT_PREFIX + "-javac-diagnostics.txt");
        Path javacSourcesArgFilePath = outputDirectoryPath.resolve(E2E_TEST_ARTIFACT_PREFIX + "-javac-sources.argfile");

        int javaSourceCount = writeJavaSourcePathsArgFile(sourceDirectoryPath, javacSourcesArgFilePath);
        if (javaSourceCount == 0) {
            throw new IllegalStateException("No Java sources found in directory: " + sourceDirectoryPath);
        }

        List<String> command = List.of(
                "javac",
                "--release",
                Integer.toString(JAVA_RELEASE),
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
            StringBuilder argFileContentBuilder = new StringBuilder();
            int[] javaSourceCount = {0};
            javaPathStream.map(JavaCompileTestUtils::toJavacArgFileEntry)
                    .forEachOrdered(argFileEntry -> {
                        if (javaSourceCount[0] > 0) {
                            argFileContentBuilder.append(System.lineSeparator());
                        }
                        argFileContentBuilder.append(argFileEntry);
                        javaSourceCount[0]++;
                    });
            Files.writeString(javacSourcesArgFilePath, argFileContentBuilder.toString(), StandardCharsets.UTF_8);
            return javaSourceCount[0];
        }
    }

    @NonNull
    private static String toJavacArgFileEntry(@NonNull Path sourcePath) {
        String absolutePath = sourcePath.toAbsolutePath().toString();
        String escapedPath = absolutePath.replace("\\", "\\\\").replace("\"", "\\\"");
        return '"' + escapedPath + '"';
    }
}

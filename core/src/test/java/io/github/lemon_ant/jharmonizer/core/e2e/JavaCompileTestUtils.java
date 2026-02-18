package io.github.lemon_ant.jharmonizer.core.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;

@UtilityClass
class JavaCompileTestUtils {

    private static final int JAVA_RELEASE = 21;
    private static final String TEST_COMPILE_PREFIX = "test-compile-";

    static void assertJavaSourcesCompileWithRelease21(
            @NonNull Path sourceDirectoryPath, @NonNull Path outputDirectoryPath)
            throws IOException, InterruptedException {
        resetOutputDirectory(outputDirectoryPath);

        Path diagnosticsPath = outputDirectoryPath.resolve(TEST_COMPILE_PREFIX + "logs.txt");
        Path javacSourcesArgFilePath = outputDirectoryPath.resolve(TEST_COMPILE_PREFIX + "javac-sources.argfile");

        boolean hasJavaSources = writeJavaSourcePathsArgFile(sourceDirectoryPath, javacSourcesArgFilePath);
        if (!hasJavaSources) {
            throw new IllegalArgumentException("No Java sources found in directory: " + sourceDirectoryPath);
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
        if (!javacOutput.isBlank()) {
            System.out.print(javacOutput);
        }
        int processExitCode = process.waitFor();
        Files.writeString(diagnosticsPath, javacOutput, StandardCharsets.UTF_8);

        assertThat(processExitCode)
                .as("Expected javac --release %s to compile fixtures under %s. Diagnostics:%n%s"
                        .formatted(JAVA_RELEASE, sourceDirectoryPath, javacOutput))
                .isZero();
    }

    private static void resetOutputDirectory(Path outputDirectoryPath) throws IOException {
        FileUtils.forceMkdir(outputDirectoryPath.toFile());
        FileUtils.cleanDirectory(outputDirectoryPath.toFile());
    }

    private static boolean writeJavaSourcePathsArgFile(Path sourceDirectoryPath, Path javacSourcesArgFilePath)
            throws IOException {
        try (Stream<Path> javaPathStream = SourceFilesHandler.findJavaFiles(sourceDirectoryPath, Set.of(), List.of())) {
            String argFileContent = javaPathStream
                    .map(JavaCompileTestUtils::toJavacArgFileEntry)
                    .collect(Collectors.joining(System.lineSeparator()));
            if (argFileContent.isBlank()) {
                return false;
            }
            try (Writer argFileWriter = Files.newBufferedWriter(javacSourcesArgFilePath, StandardCharsets.UTF_8)) {
                argFileWriter.write(argFileContent);
            }
            return true;
        }
    }

    @NonNull
    private static String toJavacArgFileEntry(@NonNull Path sourcePath) {
        String absolutePath = sourcePath.toAbsolutePath().toString();
        return '"' + StringEscapeUtils.escapeJava(absolutePath) + '"';
    }
}

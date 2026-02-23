package io.github.lemon_ant.jharmonizer.core.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;

@UtilityClass
class JavaCompileTestUtils {

    private static final int JAVA_RELEASE = 21;
    private static final String TEST_COMPILE_PREFIX = "test-compile-";

    static void assertJavaSourceCompilesWithRelease21(@NonNull Path sourceFilePath, @NonNull Path outputDirectoryPath)
            throws IOException, InterruptedException {
        resetOutputDirectory(outputDirectoryPath);

        assertThat(sourceFilePath)
                .as("Expected Java source file to compile: %s", sourceFilePath)
                .exists()
                .isRegularFile();

        Path diagnosticsPath = outputDirectoryPath.resolve(TEST_COMPILE_PREFIX + "logs.txt");

        List<String> command = List.of(
                "javac",
                "--release",
                Integer.toString(JAVA_RELEASE),
                "-encoding",
                StandardCharsets.UTF_8.name(),
                "-d",
                outputDirectoryPath.toString(),
                sourceFilePath.toAbsolutePath().toString());

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String javacOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!javacOutput.isBlank()) {
            System.out.print(javacOutput);
        }
        int processExitCode = process.waitFor();
        Files.writeString(diagnosticsPath, javacOutput, StandardCharsets.UTF_8);

        assertThat(processExitCode)
                .as("Expected javac --release %s to compile file %s. Diagnostics:%n%s"
                        .formatted(JAVA_RELEASE, sourceFilePath, javacOutput))
                .isZero();
    }

    private static void resetOutputDirectory(Path outputDirectoryPath) throws IOException {
        FileUtils.forceMkdir(outputDirectoryPath.toFile());
        FileUtils.cleanDirectory(outputDirectoryPath.toFile());
    }
}

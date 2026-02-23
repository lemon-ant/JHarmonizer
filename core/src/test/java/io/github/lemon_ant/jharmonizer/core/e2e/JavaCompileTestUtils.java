package io.github.lemon_ant.jharmonizer.core.e2e;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;

@UtilityClass
class JavaCompileTestUtils {

    private static final int JAVA_RELEASE = 21;
    private static final String TEST_COMPILE_PREFIX = "test-compile-";

    static CompileResult compileJavaSourceWithRelease21(@NonNull Path sourceFilePath, @NonNull Path outputDirectoryPath)
            throws IOException, InterruptedException {
        ensureOutputDirectoryExists(outputDirectoryPath);
        JavaFilePreconditions.requireRegularFile(sourceFilePath, "Expected Java source file to compile, but got: ");

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
        int processExitCode = process.waitFor();

        String diagnostics = "Command: " + String.join(" ", command)
                + System.lineSeparator()
                + javacOutput
                + System.lineSeparator();
        Files.writeString(
                diagnosticsPath,
                diagnostics,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);

        return new CompileResult(processExitCode, javacOutput, diagnosticsPath);
    }

    private static void ensureOutputDirectoryExists(Path outputDirectoryPath) throws IOException {
        FileUtils.forceMkdir(outputDirectoryPath.toFile());
    }

    @Value
    static class CompileResult {
        int exitCode;
        String output;
        Path diagnosticsPath;
    }
}

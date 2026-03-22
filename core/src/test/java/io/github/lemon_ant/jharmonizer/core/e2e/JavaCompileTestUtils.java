package io.github.lemon_ant.jharmonizer.core.e2e;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
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
        return compileJavaSourcesWithRelease21(List.of(sourceFilePath), outputDirectoryPath);
    }

    static CompileResult compileJavaSourcesWithRelease21(
            @NonNull List<Path> sourceFilePaths, @NonNull Path outputDirectoryPath)
            throws IOException, InterruptedException {
        ensureOutputDirectoryExists(outputDirectoryPath);
        if (sourceFilePaths.isEmpty()) {
            throw new IllegalArgumentException("Expected at least one Java source file to compile");
        }
        for (Path sourceFilePath : sourceFilePaths) {
            E2EFileUtils.requireRegularFile(sourceFilePath, "Expected Java source file to compile, but got: ");
        }

        Path diagnosticsPath = outputDirectoryPath.resolve(TEST_COMPILE_PREFIX + "logs.txt");

        List<String> command = new ArrayList<>();
        command.add("javac");
        command.add("--release");
        command.add(Integer.toString(JAVA_RELEASE));
        command.add("-encoding");
        command.add(StandardCharsets.UTF_8.name());
        command.add("-d");
        command.add(outputDirectoryPath.toString());
        sourceFilePaths.stream().map(Path::toAbsolutePath).map(Path::toString).forEach(command::add);

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

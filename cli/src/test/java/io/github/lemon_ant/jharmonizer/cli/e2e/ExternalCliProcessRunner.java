// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.cli.e2e;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
class ExternalCliProcessRunner {

    private static final long PROCESS_TIMEOUT_SECONDS = 90;
    private static final String JAVA_TOOL_OPTIONS_MESSAGE_PREFIX = "Picked up JAVA_TOOL_OPTIONS: ";

    static ExternalCliProcessResult run(
            @NonNull Path executableJar, @NonNull Path workingDirectory, String... arguments)
            throws IOException, InterruptedException {
        List<String> command = Stream.concat(
                        Stream.of("java", "-jar", executableJar.toString()), Arrays.stream(arguments))
                .toList();

        Process process =
                new ProcessBuilder(command).directory(workingDirectory.toFile()).start();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> stdoutFuture = executor.submit(() -> readAndMirror(process.getInputStream(), System.out));
            Future<String> stderrFuture = executor.submit(() -> readAndMirror(process.getErrorStream(), System.err));
            boolean completed = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
            }
            int exitCode = completed ? process.exitValue() : -1;
            return new ExternalCliProcessResult(
                    List.copyOf(command),
                    workingDirectory.toAbsolutePath().normalize(),
                    exitCode,
                    getOutput(stdoutFuture),
                    normalizeErrorOutput(getOutput(stderrFuture)),
                    !completed);
        }
    }

    @NonNull
    private static String readAndMirror(InputStream inputStream, PrintStream mirrorStream) throws IOException {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputBuffer.write(buffer, 0, bytesRead);
            mirrorStream.write(buffer, 0, bytesRead);
            mirrorStream.flush();
        }
        return outputBuffer.toString(StandardCharsets.UTF_8);
    }

    @NonNull
    private static String getOutput(Future<String> outputFuture) throws InterruptedException {
        try {
            return outputFuture.get();
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Failed to capture CLI process output", exception.getCause());
        }
    }

    static String normalizeErrorOutput(@NonNull String stderr) {
        return stderr.lines()
                .filter(line -> !line.startsWith(JAVA_TOOL_OPTIONS_MESSAGE_PREFIX))
                .collect(Collectors.joining(System.lineSeparator()));
    }
}

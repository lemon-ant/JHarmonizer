package io.github.lemon_ant.jharmonizer.cli.e2e;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
class ExternalCliProcessRunner {

    private static final long PROCESS_TIMEOUT_SECONDS = 30;

    static ExternalCliProcessResult run(
            @NonNull Path executableJar, @NonNull Path workingDirectory, String... arguments)
            throws IOException, InterruptedException {
        List<String> command = Stream.concat(
                        Stream.of("java", "-jar", executableJar.toString()), Arrays.stream(arguments))
                .toList();

        Process process =
                new ProcessBuilder(command).directory(workingDirectory.toFile()).start();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> stdoutFuture = executor.submit(() -> read(process.getInputStream()));
            Future<String> stderrFuture = executor.submit(() -> read(process.getErrorStream()));
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
                    getOutput(stderrFuture),
                    !completed);
        }
    }

    private static String read(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String getOutput(Future<String> outputFuture) throws InterruptedException {
        try {
            return outputFuture.get();
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Failed to capture CLI process output", exception.getCause());
        }
    }
}

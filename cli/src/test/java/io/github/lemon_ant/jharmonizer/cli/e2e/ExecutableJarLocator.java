package io.github.lemon_ant.jharmonizer.cli.e2e;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;

@UtilityClass
class ExecutableJarLocator {

    private static final String EXECUTABLE_JAR_PROPERTY = "jharmonizer.cli.executableJar";

    static Path locateExecutableJar() {
        String configuredJarPath = System.getProperty(EXECUTABLE_JAR_PROPERTY);
        Path executableJar =
                configuredJarPath == null ? Path.of("target", "jharmonizer-cli.jar") : Path.of(configuredJarPath);

        if (executableJar.getFileName() == null
                || !executableJar.getFileName().toString().endsWith(".jar")) {
            throw new IllegalStateException("Packaged CLI JAR path is invalid: " + executableJar);
        }
        if (!Files.isRegularFile(executableJar)) {
            throw new IllegalStateException("Expected packaged executable JAR at: " + executableJar);
        }
        return executableJar.toAbsolutePath().normalize();
    }
}

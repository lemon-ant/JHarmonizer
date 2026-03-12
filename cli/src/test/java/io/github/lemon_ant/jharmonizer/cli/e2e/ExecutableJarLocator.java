package io.github.lemon_ant.jharmonizer.cli.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;

@UtilityClass
class ExecutableJarLocator {

    private static final String EXECUTABLE_JAR_PROPERTY = "jharmonizer.cli.executableJar";

    static Path locateExecutableJar() {
        String configuredJarPath = System.getProperty(EXECUTABLE_JAR_PROPERTY);
        Path executableJar = configuredJarPath == null
                ? Path.of("target", "jharmonizer-cli.jar")
                : Path.of(configuredJarPath);

        assertThat(executableJar)
                .as("Packaged CLI JAR path")
                .matches(path -> path.getFileName() != null && path.getFileName().toString().endsWith(".jar"));
        assertThat(Files.isRegularFile(executableJar))
                .as("Expected packaged executable JAR at %s", executableJar)
                .isTrue();
        return executableJar.toAbsolutePath().normalize();
    }
}

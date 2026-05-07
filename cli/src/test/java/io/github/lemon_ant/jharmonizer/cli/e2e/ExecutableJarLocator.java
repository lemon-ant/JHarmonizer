// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.cli.e2e;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders dependent static fields in test classes;
// remove this directive once jharmonizer is upgraded to a version that respects field initialization order.
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;

@UtilityClass
class ExecutableJarLocator {

    private static final String EXECUTABLE_JAR_PROPERTY = "jharmonizer.cli.executableJar";

    static Path locateExecutableJar() {
        String configuredJarPath = System.getProperty(EXECUTABLE_JAR_PROPERTY);
        Path executableJar;
        if (configuredJarPath != null) {
            executableJar = Path.of(configuredJarPath);
            if (executableJar.getFileName() == null
                    || !executableJar.getFileName().toString().endsWith(".jar")) {
                throw new IllegalStateException("Packaged CLI JAR path is invalid: " + executableJar);
            }
        } else {
            executableJar = Path.of("target", "jharmonizer-cli.jar");
        }

        if (!Files.isRegularFile(executableJar)) {
            throw new IllegalStateException("Expected packaged executable JAR at: " + executableJar);
        }
        return executableJar.toAbsolutePath().normalize();
    }
}

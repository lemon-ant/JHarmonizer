// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.cli.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class CliLauncherDetectorTest {

    private static final String FALLBACK_LAUNCHER = "jharmonizer";

    @Test
    void resolveLauncherPrefix_commandAbsent_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefix(
                        Optional.empty(), Optional.of(new String[] {"-jar", "jharmonizer-cli.jar"})))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefix_argumentsAbsent_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefix(Optional.of("/usr/bin/java"), Optional.empty()))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefix_noJarFlagInArguments_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefix(
                        Optional.of("/usr/bin/java"),
                        Optional.of(new String[] {"-cp", "libs/*", "io.github.lemon_ant.Main"})))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefix_jarFlagIsLastArgument_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefix(
                        Optional.of("/usr/bin/java"), Optional.of(new String[] {"-jar"})))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefix_jarFlagPointsToNonJHarmonizerJar_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefix(
                        Optional.of("/usr/bin/java"),
                        Optional.of(new String[] {"-jar", "surefirebooter1234567890.jar"})))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefix_invalidPathInCommand_returnsFallbackLauncher() {
        // When / Then -- Path.of() throws InvalidPathException for NUL bytes; must fall back safely
        assertThat(CliLauncherDetector.resolveLauncherPrefix(
                        Optional.of("bad\0path"), Optional.of(new String[] {"-jar", "jharmonizer-cli.jar"})))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefix_windowsJarPathWithJHarmonizerInDirectory_returnsFallbackLauncher() {
        // When / Then -- "jharmonizer" appears only in a directory component, not the jar filename
        assertThat(CliLauncherDetector.resolveLauncherPrefix(
                        Optional.of("/usr/bin/java"),
                        Optional.of(new String[] {"-jar", "C:\\tools\\jharmonizer\\surefirebooter123.jar"})))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefix_jHarmonizerJarDetected_returnsJavaJarPrefix() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefix(
                Optional.of("/usr/bin/java"), Optional.of(new String[] {"-jar", "jharmonizer-cli.jar"}));

        // Then
        assertThat(result).isEqualTo("\"/usr/bin/java\" -jar \"jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefix_jvmFlagsBeforeJarFlag_returnsJavaJarPrefix() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefix(
                Optional.of("/usr/bin/java"),
                Optional.of(new String[] {"-Xmx512m", "-jar", "jharmonizer-cli.jar", "check-fast"}));

        // Then
        assertThat(result).isEqualTo("\"/usr/bin/java\" -jar \"jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefix_javaExePathWithSpaces_quotesJavaExe() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefix(
                Optional.of("C:/Program Files/jdk-21/bin/java.exe"),
                Optional.of(new String[] {"-jar", "jharmonizer-cli.jar"}));

        // Then
        assertThat(result).isEqualTo("\"C:/Program Files/jdk-21/bin/java.exe\" -jar \"jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefix_jarPathWithSpaces_quotesJarPath() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefix(
                Optional.of("/usr/bin/java"), Optional.of(new String[] {"-jar", "/my tools/jharmonizer-cli.jar"}));

        // Then
        assertThat(result).isEqualTo("\"/usr/bin/java\" -jar \"/my tools/jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefix_windowsBackslashPaths_normalizesToForwardSlashes() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefix(
                Optional.of("C:\\Program Files\\jdk-21\\bin\\java.exe"),
                Optional.of(new String[] {"-jar", ".\\jharmonizer-cli.jar"}));

        // Then
        assertThat(result).isEqualTo("\"C:/Program Files/jdk-21/bin/java.exe\" -jar \"./jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefix_pathWithShellMetacharacters_escapesMetacharacters() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefix(
                Optional.of("/usr/$JAVA_HOME/bin/java"), Optional.of(new String[] {"-jar", "jharmonizer-cli.jar"}));

        // Then
        assertThat(result).isEqualTo("\"/usr/\\$JAVA_HOME/bin/java\" -jar \"jharmonizer-cli.jar\"");
    }
}

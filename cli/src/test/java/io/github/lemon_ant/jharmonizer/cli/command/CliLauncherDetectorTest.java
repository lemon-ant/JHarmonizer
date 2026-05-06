// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.cli.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CliLauncherDetectorTest {

    private static final String FALLBACK_LAUNCHER = "jharmonizer";

    @Test
    void resolveLauncherPrefix_commandAbsent_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefix(null, new String[] {"-jar", "jharmonizer-cli.jar"}))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefix_argumentsAbsent_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefix("/usr/bin/java", null))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefix_noJarFlagInArguments_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefix(
                        "/usr/bin/java", new String[] {"-cp", "libs/*", "io.github.lemon_ant.Main"}))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefix_jarFlagIsLastArgument_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefix("/usr/bin/java", new String[] {"-jar"}))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefix_jarFlagPointsToNonJHarmonizerJar_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefix(
                        "/usr/bin/java", new String[] {"-jar", "surefirebooter1234567890.jar"}))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefix_invalidPathInCommand_returnsFallbackLauncher() {
        // When / Then -- Path.of() throws InvalidPathException for NUL bytes; must fall back safely
        assertThat(CliLauncherDetector.resolveLauncherPrefix("bad\0path", new String[] {"-jar", "jharmonizer-cli.jar"}))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefix_windowsJarPathWithJHarmonizerInDirectory_returnsFallbackLauncher() {
        // When / Then -- "jharmonizer" appears only in a directory component, not the jar filename
        assertThat(CliLauncherDetector.resolveLauncherPrefix(
                        "/usr/bin/java", new String[] {"-jar", "C:\\tools\\jharmonizer\\surefirebooter123.jar"}))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefix_jHarmonizerJarDetected_returnsJavaJarPrefix() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefix(
                "/usr/bin/java", new String[] {"-jar", "jharmonizer-cli.jar"});

        // Then
        assertThat(result).isEqualTo("\"/usr/bin/java\" -jar \"jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefix_jvmFlagsBeforeJarFlag_returnsJavaJarPrefix() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefix(
                "/usr/bin/java", new String[] {"-Xmx512m", "-jar", "jharmonizer-cli.jar", "check-fast"});

        // Then
        assertThat(result).isEqualTo("\"/usr/bin/java\" -jar \"jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefix_javaExePathWithSpaces_quotesJavaExe() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefix(
                "C:/Program Files/jdk-21/bin/java.exe", new String[] {"-jar", "jharmonizer-cli.jar"});

        // Then
        assertThat(result).isEqualTo("\"C:/Program Files/jdk-21/bin/java.exe\" -jar \"jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefix_jarPathWithSpaces_quotesJarPath() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefix(
                "/usr/bin/java", new String[] {"-jar", "/my tools/jharmonizer-cli.jar"});

        // Then
        assertThat(result).isEqualTo("\"/usr/bin/java\" -jar \"/my tools/jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefix_windowsBackslashPaths_normalizesToForwardSlashes() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefix(
                "C:\\Program Files\\jdk-21\\bin\\java.exe", new String[] {"-jar", ".\\jharmonizer-cli.jar"});

        // Then
        assertThat(result).isEqualTo("\"C:/Program Files/jdk-21/bin/java.exe\" -jar \"./jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefix_pathWithShellMetacharacters_escapesMetacharacters() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefix(
                "/usr/$JAVA_HOME/bin/java", new String[] {"-jar", "jharmonizer-cli.jar"});

        // Then
        assertThat(result).isEqualTo("\"/usr/\\$JAVA_HOME/bin/java\" -jar \"jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefixFromCommandLine_commandLineAbsent_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefixFromCommandLine(null))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefixFromCommandLine_emptyCommandLine_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefixFromCommandLine("")).isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefixFromCommandLine_onlyExecutable_returnsFallbackLauncher() {
        // When / Then — no arguments at all, so no -jar flag
        assertThat(CliLauncherDetector.resolveLauncherPrefixFromCommandLine("java"))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefixFromCommandLine_noJarFlag_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefixFromCommandLine(
                        "/usr/bin/java -cp libs/* io.github.lemon_ant.Main"))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefixFromCommandLine_nonJHarmonizerJar_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefixFromCommandLine(
                        "/usr/bin/java -jar surefirebooter1234567890.jar"))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefixFromCommandLine_unquotedPaths_returnsJavaJarPrefix() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefixFromCommandLine(
                "/usr/bin/java -jar jharmonizer-cli.jar check-fast");

        // Then
        assertThat(result).isEqualTo("\"/usr/bin/java\" -jar \"jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefixFromCommandLine_quotedJavaPathWithSpaces_returnsJavaJarPrefix() {
        // When — simulates a Windows invocation where the java.exe path contains spaces
        String result = CliLauncherDetector.resolveLauncherPrefixFromCommandLine(
                "\"C:/Program Files/jdk-21/bin/java.exe\" -jar jharmonizer-cli.jar check-fast");

        // Then
        assertThat(result).isEqualTo("\"C:/Program Files/jdk-21/bin/java.exe\" -jar \"jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefixFromCommandLine_windowsBackslashPaths_normalizesToForwardSlashes() {
        // When — simulates ProcessHandle.commandLine() on Windows where backslashes appear in paths
        String result = CliLauncherDetector.resolveLauncherPrefixFromCommandLine(
                "\"C:\\Program Files\\jdk-21\\bin\\java.exe\" -jar .\\jharmonizer-cli.jar check-fast");

        // Then
        assertThat(result).isEqualTo("\"C:/Program Files/jdk-21/bin/java.exe\" -jar \"./jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefixFromCommandLine_jvmFlagsBeforeJarFlag_returnsJavaJarPrefix() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefixFromCommandLine(
                "/usr/bin/java -Xmx512m -jar jharmonizer-cli.jar check-fast");

        // Then
        assertThat(result).isEqualTo("\"/usr/bin/java\" -jar \"jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefixFromCommandLine_quotedJarPathWithSpaces_returnsJavaJarPrefix() {
        // When
        String result = CliLauncherDetector.resolveLauncherPrefixFromCommandLine(
                "/usr/bin/java -jar \"/my tools/jharmonizer-cli.jar\" check-fast");

        // Then
        assertThat(result).isEqualTo("\"/usr/bin/java\" -jar \"/my tools/jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefixFromSunProperty_commandAbsent_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefixFromSunProperty(null, ".\\jharmonizer-cli.jar check-fast"))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefixFromSunProperty_sunPropertyNull_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefixFromSunProperty("/usr/bin/java", null))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefixFromSunProperty_sunPropertyBlank_returnsFallbackLauncher() {
        // When / Then
        assertThat(CliLauncherDetector.resolveLauncherPrefixFromSunProperty("/usr/bin/java", "   "))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefixFromSunProperty_nonJHarmonizerProgram_returnsFallbackLauncher() {
        // When / Then — sun.java.command starts with a class name, not a jharmonizer jar
        assertThat(CliLauncherDetector.resolveLauncherPrefixFromSunProperty(
                        "/usr/bin/java", "surefirebooter1234567890.jar"))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefixFromSunProperty_classNameLauncher_returnsFallbackLauncher() {
        // When / Then — invoked with -cp rather than -jar; sun.java.command = "com.example.Main args"
        assertThat(CliLauncherDetector.resolveLauncherPrefixFromSunProperty(
                        "/usr/bin/java", "com.example.Main arg1 arg2"))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefixFromSunProperty_simpleJarName_returnsJavaJarPrefix() {
        // When — simulates Windows: sun.java.command = "jharmonizer-cli.jar check-fast --base-dir ..."
        String result = CliLauncherDetector.resolveLauncherPrefixFromSunProperty(
                "C:\\Program Files\\Microsoft\\jdk-21\\bin\\java.exe",
                "jharmonizer-cli.jar check-fast --base-dir W:/nifi");

        // Then
        assertThat(result).isEqualTo("\"C:/Program Files/Microsoft/jdk-21/bin/java.exe\" -jar \"jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefixFromSunProperty_relativeJarPath_returnsJavaJarPrefix() {
        // When — simulates: java.exe -jar .\jharmonizer-cli.jar → sun.java.command = ".\jharmonizer-cli.jar ..."
        String result = CliLauncherDetector.resolveLauncherPrefixFromSunProperty(
                "C:\\Program Files\\Microsoft\\jdk-21\\bin\\java.exe",
                ".\\jharmonizer-cli.jar check-fast --base-dir W:/nifi");

        // Then
        assertThat(result)
                .isEqualTo("\"C:/Program Files/Microsoft/jdk-21/bin/java.exe\" -jar \"./jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefixFromSunProperty_noAppArgs_returnsJavaJarPrefix() {
        // When — sun.java.command has no trailing args (edge case)
        String result =
                CliLauncherDetector.resolveLauncherPrefixFromSunProperty("/usr/bin/java", "jharmonizer-cli.jar");

        // Then
        assertThat(result).isEqualTo("\"/usr/bin/java\" -jar \"jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefixFromSunProperty_invalidPathInCommand_returnsFallbackLauncher() {
        // When / Then — Path.of() throws for NUL bytes; must fall back safely
        assertThat(CliLauncherDetector.resolveLauncherPrefixFromSunProperty(
                        "bad\0path", "jharmonizer-cli.jar check-fast"))
                .isEqualTo(FALLBACK_LAUNCHER);
    }

    @Test
    void resolveLauncherPrefixFromSunProperty_jarPathWithSpaces_returnsJavaJarPrefix() {
        // When — jar path contains spaces; first-space split would cut it incorrectly
        String result = CliLauncherDetector.resolveLauncherPrefixFromSunProperty(
                "C:\\Program Files\\Microsoft\\jdk-21\\bin\\java.exe",
                "C:\\Program Files\\jharmonizer\\jharmonizer-cli.jar check-fast");

        // Then
        assertThat(result)
                .isEqualTo(
                        "\"C:/Program Files/Microsoft/jdk-21/bin/java.exe\" -jar \"C:/Program Files/jharmonizer/jharmonizer-cli.jar\"");
    }

    @Test
    void resolveLauncherPrefixFromSunProperty_jarPathWithSpacesAndNoArgs_returnsJavaJarPrefix() {
        // When — jar path contains spaces with no trailing app args
        String result = CliLauncherDetector.resolveLauncherPrefixFromSunProperty(
                "/usr/bin/java", "/my tools/jharmonizer-cli.jar");

        // Then
        assertThat(result).isEqualTo("\"/usr/bin/java\" -jar \"/my tools/jharmonizer-cli.jar\"");
    }
}

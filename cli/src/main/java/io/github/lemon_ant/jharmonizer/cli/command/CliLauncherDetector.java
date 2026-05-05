// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.utilities.PathUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

/**
 * Detects the actual command-line launcher prefix used to start the JHarmonizer CLI process.
 *
 * <p>When the tool is invoked as {@code java -jar jharmonizer-cli.jar}, this class reconstructs
 * the exact java-executable-and-jar prefix from the running process so the reorder fix hint
 * mirrors the user's own invocation. If detection fails, the hint falls back to the symbolic
 * name {@code jharmonizer}.
 */
@UtilityClass
class CliLauncherDetector {

    private static final String FALLBACK_LAUNCHER = "jharmonizer";
    private static final String JAR_FLAG = "-jar";
    private static final String JHARMONIZER_JAR_NAME_PART = "jharmonizer";
    private static final char DOUBLE_QUOTE = '"';
    private static final int MINIMUM_COMMANDLINE_TOKEN_COUNT = 2;

    /**
     * Detects the launcher prefix for the current process.
     *
     * @return the launcher prefix string, either a reconstructed {@code java -jar <jar>} form
     *     or the {@code jharmonizer} fallback when detection is unavailable
     */
    @NonNull
    static String detectLauncherPrefix() {
        ProcessHandle.Info processInfo = ProcessHandle.current().info();
        // Primary: command() + arguments() — works on Unix/macOS.
        String primaryPrefix = resolveLauncherPrefix(processInfo.command(), processInfo.arguments());
        if (!FALLBACK_LAUNCHER.equals(primaryPrefix)) {
            return primaryPrefix;
        }
        // Fallback 1: command() + sun.java.command — works on Windows with any HotSpot JVM.
        // On Windows, ProcessHandle.Info.arguments() returns Optional.empty() (JDK-8252698), but
        // the JVM launcher always sets sun.java.command to "<program> [app-args]" for -jar mode.
        String sunJavaCommandFallback = resolveLauncherPrefixFromSunProperty(
                processInfo.command().orElse(null), System.getProperty("sun.java.command"));
        if (!FALLBACK_LAUNCHER.equals(sunJavaCommandFallback)) {
            return sunJavaCommandFallback;
        }
        // Fallback 2: tokenize commandLine() — covers remaining edge cases where commandLine() is
        // populated but neither arguments() nor sun.java.command provided useful info.
        return resolveLauncherPrefixFromCommandLine(processInfo.commandLine().orElse(null));
    }

    /**
     * Resolves the launcher prefix from the given process command and arguments.
     *
     * <p>Exposed as package-private for unit testing without requiring a real process context.
     *
     * @param maybeCommand the Java executable path, if available
     * @param maybeArguments the JVM argument list, if available
     * @return the resolved launcher prefix, or the {@code jharmonizer} fallback
     */
    @NonNull
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    static String resolveLauncherPrefix(
            @NonNull Optional<String> maybeCommand, @NonNull Optional<String[]> maybeArguments) {
        if (maybeCommand.isEmpty() || maybeArguments.isEmpty()) {
            return FALLBACK_LAUNCHER;
        }
        try {
            return buildLauncherPrefix(maybeCommand.get(), maybeArguments.get());
        } catch (RuntimeException pathBuildingException) {
            // Path.of() can throw InvalidPathException for unusual process info values;
            // treat any failure as an unavailable detection and fall back to the symbolic name.
            return FALLBACK_LAUNCHER;
        }
    }

    /**
     * Resolves the launcher prefix by parsing the raw process command line.
     *
     * <p>Used as a fallback when {@link #resolveLauncherPrefix} cannot detect the prefix because
     * {@code ProcessHandle.Info.arguments()} returns an empty Optional — a known limitation on
     * Windows where the OS exposes a single command-line string rather than a pre-split argument
     * array. {@code ProcessHandle.Info.commandLine()} is more reliably populated on Windows.
     *
     * <p>Exposed as package-private for unit testing without requiring a real process context.
     *
     * @param commandLine the full raw command line of the process, or {@code null} if unavailable
     * @return the resolved launcher prefix, or the {@code jharmonizer} fallback
     */
    @NonNull
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    static String resolveLauncherPrefixFromCommandLine(@Nullable String commandLine) {
        if (commandLine == null) {
            return FALLBACK_LAUNCHER;
        }
        try {
            List<String> tokens = tokenizeCommandLine(commandLine);
            if (tokens.size() < MINIMUM_COMMANDLINE_TOKEN_COUNT) {
                return FALLBACK_LAUNCHER;
            }
            String command = tokens.get(0);
            String[] args = tokens.subList(1, tokens.size()).toArray(String[]::new);
            return buildLauncherPrefix(command, args);
        } catch (RuntimeException commandLineParseException) {
            return FALLBACK_LAUNCHER;
        }
    }

    /**
     * Resolves the launcher prefix using the Java executable path and the {@code sun.java.command}
     * system property.
     *
     * <p>When the JVM is started with {@code java -jar app.jar [args]}, the HotSpot launcher sets
     * {@code sun.java.command} to {@code "app.jar [args]"}. The first whitespace-delimited token
     * of that property is therefore the jar file name (or path), which — combined with
     * {@code command()} for the java executable — gives us the full launcher prefix without
     * relying on {@code arguments()} or {@code commandLine()}, both of which are unavailable on
     * Windows (JDK-8252698).
     *
     * <p>Exposed as package-private for unit testing without requiring a real process context.
     *
     * @param command the Java executable path from {@code ProcessHandle.Info.command()}, or
     *     {@code null} if unavailable
     * @param sunJavaCommand the value of the {@code sun.java.command} system property, or
     *     {@code null} if the property is not set
     * @return the resolved launcher prefix, or the {@code jharmonizer} fallback
     */
    @NonNull
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    static String resolveLauncherPrefixFromSunProperty(@Nullable String command, @Nullable String sunJavaCommand) {
        if (command == null || sunJavaCommand == null || sunJavaCommand.isBlank()) {
            return FALLBACK_LAUNCHER;
        }
        try {
            // sun.java.command = "<jar-or-class> [app-args...]"
            // The program name is the portion up to the first whitespace character.
            int firstSpace = sunJavaCommand.indexOf(' ');
            String programName = firstSpace >= 0 ? sunJavaCommand.substring(0, firstSpace) : sunJavaCommand;
            if (!isJHarmonizerJar(programName)) {
                return FALLBACK_LAUNCHER;
            }
            String normalizedJavaExe = PathUtils.normalizeSeparators(Path.of(command));
            String normalizedJarPath = PathUtils.normalizeSeparators(Path.of(programName));
            return quotePathForShell(normalizedJavaExe) + " " + JAR_FLAG + " " + quotePathForShell(normalizedJarPath);
        } catch (RuntimeException pathBuildingException) {
            return FALLBACK_LAUNCHER;
        }
    }

    /**
     * Splits a raw command-line string into tokens, respecting double-quoted sections.
     *
     * <p>Tokens are delimited by whitespace outside of double-quoted spans. Quotes are stripped
     * from the resulting tokens. This covers the shell quoting conventions used on both Windows
     * (cmd.exe / PowerShell) and Unix for the {@code java -jar} invocation patterns that
     * JHarmonizer users typically employ.
     *
     * @param commandLine the raw command-line string to tokenize
     * @return the list of tokens extracted from the command line
     */
    @NonNull
    private static List<String> tokenizeCommandLine(String commandLine) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;
        boolean buildingToken = false;
        for (int characterIndex = 0; characterIndex < commandLine.length(); characterIndex++) {
            char character = commandLine.charAt(characterIndex);
            if (character == DOUBLE_QUOTE) {
                inQuotes = !inQuotes;
                buildingToken = true;
            } else if (Character.isWhitespace(character) && !inQuotes) {
                if (buildingToken) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                    buildingToken = false;
                }
            } else {
                currentToken.append(character);
                buildingToken = true;
            }
        }
        if (buildingToken) {
            tokens.add(currentToken.toString());
        }
        return tokens;
    }

    @NonNull
    @SuppressWarnings("PMD.UseVarargs")
    private static String buildLauncherPrefix(String command, String[] args) {
        int jarFlagIndex = findJarFlagIndex(args);
        if (jarFlagIndex < 0) {
            return FALLBACK_LAUNCHER;
        }
        String jarPath = args[jarFlagIndex + 1];
        if (!isJHarmonizerJar(jarPath)) {
            return FALLBACK_LAUNCHER;
        }
        String normalizedJavaExe = PathUtils.normalizeSeparators(Path.of(command));
        String normalizedJarPath = PathUtils.normalizeSeparators(Path.of(jarPath));
        return quotePathForShell(normalizedJavaExe) + " " + JAR_FLAG + " " + quotePathForShell(normalizedJarPath);
    }

    @SuppressWarnings("PMD.UseVarargs")
    private static int findJarFlagIndex(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if (JAR_FLAG.equals(args[i])) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isJHarmonizerJar(@Nullable String jarPath) {
        if (jarPath == null || jarPath.isBlank()) {
            return false;
        }
        // Normalize separators on the raw string before extracting the filename so that
        // Windows-style backslashes are handled correctly on any OS (Path.of() is platform-dependent).
        String normalizedJarPath = jarPath.replace('\\', '/');
        int lastSlash = normalizedJarPath.lastIndexOf('/');
        String jarFileName = lastSlash >= 0 ? normalizedJarPath.substring(lastSlash + 1) : normalizedJarPath;
        return jarFileName.toLowerCase(Locale.ROOT).contains(JHARMONIZER_JAR_NAME_PART);
    }

    @NonNull
    private static String quotePathForShell(String normalizedPath) {
        // Apply the same escaping rules as ReorderCommandRenderer.quoteArg() so the launcher
        // prefix is safe for paths that contain shell metacharacters such as $, `, or !.
        // Paths are already separator-normalized, so backslashes do not need separate treatment.
        String escaped = normalizedPath
                .replace("\"", "\\\"")
                .replace("$", "\\$")
                .replace("`", "\\`")
                .replace("!", "\\!");
        return "\"" + escaped + "\"";
    }
}

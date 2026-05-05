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

    /**
     * Detects the launcher prefix for the current process.
     *
     * @return the launcher prefix string, either a reconstructed {@code java -jar <jar>} form
     *     or the {@code jharmonizer} fallback when detection is unavailable
     */
    @NonNull
    static String detectLauncherPrefix() {
        ProcessHandle.Info processInfo = ProcessHandle.current().info();
        String primaryPrefix = resolveLauncherPrefix(processInfo.command(), processInfo.arguments());
        if (!FALLBACK_LAUNCHER.equals(primaryPrefix)) {
            return primaryPrefix;
        }
        // On some platforms (notably Windows) arguments() may return Optional.empty() even
        // though commandLine() is available. Try parsing commandLine() as a fallback.
        return resolveLauncherPrefixFromCommandLine(processInfo.commandLine());
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
     * @param maybeCommandLine the full raw command line of the process, if available
     * @return the resolved launcher prefix, or the {@code jharmonizer} fallback
     */
    @NonNull
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    static String resolveLauncherPrefixFromCommandLine(@NonNull Optional<String> maybeCommandLine) {
        if (maybeCommandLine.isEmpty()) {
            return FALLBACK_LAUNCHER;
        }
        try {
            List<String> tokens = tokenizeCommandLine(maybeCommandLine.get());
            if (tokens.size() < 2) {
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
    private static List<String> tokenizeCommandLine(@NonNull String commandLine) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;
        boolean buildingToken = false;
        for (int charIndex = 0; charIndex < commandLine.length(); charIndex++) {
            char currentChar = commandLine.charAt(charIndex);
            if (currentChar == '"') {
                inQuotes = !inQuotes;
                buildingToken = true;
            } else if (Character.isWhitespace(currentChar) && !inQuotes) {
                if (buildingToken) {
                    tokens.add(currentToken.toString());
                    currentToken = new StringBuilder();
                    buildingToken = false;
                }
            } else {
                currentToken.append(currentChar);
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

// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.utilities.PathUtils;
import java.nio.file.Path;
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
        return resolveLauncherPrefix(processInfo.command(), processInfo.arguments());
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
    static String resolveLauncherPrefix(
            @NonNull Optional<String> maybeCommand, @NonNull Optional<String[]> maybeArguments) {
        if (maybeCommand.isEmpty() || maybeArguments.isEmpty()) {
            return FALLBACK_LAUNCHER;
        }
        String[] args = maybeArguments.get();
        int jarFlagIndex = findJarFlagIndex(args);
        if (jarFlagIndex < 0) {
            return FALLBACK_LAUNCHER;
        }
        String jarPath = args[jarFlagIndex + 1];
        if (!isJHarmonizerJar(jarPath)) {
            return FALLBACK_LAUNCHER;
        }
        String normalizedJavaExe = PathUtils.normalizeSeparators(Path.of(maybeCommand.get()));
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
        Path jarFilePath = Path.of(jarPath).getFileName();
        return jarFilePath != null
                && jarFilePath.toString().toLowerCase(Locale.ROOT).contains(JHARMONIZER_JAR_NAME_PART);
    }

    @NonNull
    private static String quotePathForShell(String normalizedPath) {
        String escaped = normalizedPath.replace("\"", "\\\"");
        if (escaped.contains(" ")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}

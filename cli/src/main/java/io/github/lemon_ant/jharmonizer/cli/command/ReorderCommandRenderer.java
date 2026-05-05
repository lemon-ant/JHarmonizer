// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.utilities.PathUtils;
import java.nio.file.Path;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

/**
 * Renders the ready-to-run {@code reorder} command string that users can paste
 * to automatically fix ordering violations detected by a check flow.
 */
@UtilityClass
class ReorderCommandRenderer {

    private static final int INITIAL_COMMAND_CAPACITY = 256;

    /**
     * Builds the {@code reorder} command with all resolved options preserved.
     *
     * @param launcherPrefix the launcher prefix to prepend (for example {@code jharmonizer} or
     *     {@code "C:/path/to/java.exe" -jar jharmonizer-cli.jar})
     * @param baseDir resolved, absolute base directory
     * @param includeGlobs glob patterns for files to include
     * @param excludeGlobs glob patterns for files to exclude
     * @param configFilePath optional path to the configuration file
     * @param noBackup whether the no-backup flag is set
     * @param noStatistics whether the no-statistics flag is set
     * @param fullStatistics whether the full-statistics flag is set
     * @return the ready-to-run reorder command string
     */
    @NonNull
    static String render(
            @NonNull String launcherPrefix,
            @NonNull Path baseDir,
            @NonNull Set<String> includeGlobs,
            @NonNull Set<String> excludeGlobs,
            @Nullable Path configFilePath,
            boolean noBackup,
            boolean noStatistics,
            boolean fullStatistics) {
        StringBuilder command = new StringBuilder(INITIAL_COMMAND_CAPACITY);
        command.append(launcherPrefix)
                .append(" reorder --base-dir ")
                .append(quoteArg(PathUtils.normalizeSeparators(baseDir)));
        for (String includeGlob : includeGlobs) {
            command.append(" --include ").append(quoteArg(includeGlob));
        }
        for (String excludeGlob : excludeGlobs) {
            command.append(" --exclude ").append(quoteArg(excludeGlob));
        }
        if (configFilePath != null) {
            command.append(" --config ").append(quoteArg(PathUtils.normalizeSeparators(configFilePath)));
        }
        if (noBackup) {
            command.append(" --no-backup");
        }
        if (noStatistics) {
            command.append(" --no-statistics");
        }
        if (fullStatistics) {
            command.append(" --full-statistics");
        }
        return command.toString();
    }

    @NonNull
    private static String quoteArg(String value) {
        return "\""
                + value.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("$", "\\$")
                        .replace("`", "\\`")
                        .replace("!", "\\!")
                + "\"";
    }
}

// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package io.github.lemon_ant.jharmonizer.core.utilities;

import java.nio.file.Path;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Provides cross-platform helpers for working with file-system paths.
 */
@UtilityClass
public class PathUtils {

    /**
     * Returns the string form of {@code path} with every platform path separator replaced by
     * a forward slash ({@code /}).
     *
     * <p>On Windows, {@link Path#toString()} uses {@code \} as separator. This method normalizes
     * the result to forward slashes so it is safe to embed in shell commands and glob patterns
     * that are always evaluated on a Unix shell regardless of the host OS.
     *
     * @param path the path to normalize
     * @return the string representation of {@code path} with all {@code \} replaced by {@code /}
     */
    @NonNull
    public static String normalizeSeparators(@NonNull Path path) {
        return path.toString().replace('\\', '/');
    }
}

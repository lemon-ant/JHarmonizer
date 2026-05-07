// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
class E2EFileUtils {
    static void requireRegularFile(@NonNull Path srcFilePath, @NonNull String messagePrefix) {
        if (!Files.isRegularFile(srcFilePath)) {
            throw new IllegalArgumentException(messagePrefix + srcFilePath);
        }
    }

    static URL toUrl(@NonNull Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Cannot convert path to URL: " + path, exception);
        }
    }
}

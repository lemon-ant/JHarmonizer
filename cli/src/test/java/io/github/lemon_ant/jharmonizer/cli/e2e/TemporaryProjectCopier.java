// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.cli.e2e;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;

@UtilityClass
class TemporaryProjectCopier {
    static Path copyProject(@NonNull String resourceRoot, @NonNull Path targetDirectory) throws IOException {
        Path srcDirectory = locateOriginalTestResource(resourceRoot);
        Files.createDirectories(targetDirectory);
        FileUtils.copyDirectory(srcDirectory.toFile(), targetDirectory.toFile());
        return targetDirectory;
    }

    static Path locateOriginalTestResource(@NonNull String resourceRoot) {
        URL resource = TemporaryProjectCopier.class.getClassLoader().getResource(resourceRoot);
        if (resource == null) {
            throw new IllegalStateException("Test resource directory not found: " + resourceRoot);
        }
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Failed to locate original test resource: " + resourceRoot, exception);
        }
    }
}

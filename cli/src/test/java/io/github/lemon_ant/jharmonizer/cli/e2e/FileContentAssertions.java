/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.cli.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import lombok.experimental.UtilityClass;

@UtilityClass
class FileContentAssertions {

    static void assertFileChanged(Path expectedProjectDirectory, Path actualProjectDirectory, String relativePath) {
        Path actualFile = actualProjectDirectory.resolve(relativePath);
        Path expectedFile = expectedProjectDirectory.resolve(relativePath);
        assertThatThrownBy(() -> assertThat(actualFile).hasSameTextualContentAs(expectedFile))
                .as("Expected file to be modified: %s", relativePath)
                .isInstanceOf(AssertionError.class);
    }

    static void assertFileUnchanged(Path expectedProjectDirectory, Path actualProjectDirectory, String relativePath) {
        assertThat(actualProjectDirectory.resolve(relativePath))
                .as("Expected file to remain unchanged: %s", relativePath)
                .hasSameTextualContentAs(expectedProjectDirectory.resolve(relativePath));
    }

    static void assertFileMatches(Path expectedFile, Path actualProjectDirectory, String relativePath) {
        assertThat(actualProjectDirectory.resolve(relativePath))
                .as("Expected file to match: %s", relativePath)
                .hasSameTextualContentAs(expectedFile);
    }
}

// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package io.github.lemon_ant.jharmonizer.cli.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReorderCommandRendererTest {

    private static final Path BASE_DIR = Path.of("/projects/my-project");

    @Test
    void render_noOptionalArguments_returnsBaseCommand() {
        // When / Then
        assertThat(ReorderCommandRenderer.render(BASE_DIR, Set.of(), Set.of(), null, false, false))
                .isEqualTo("jharmonizer reorder --base-dir \"" + BASE_DIR + "\"");
    }

    @Test
    void render_allOptionalArgumentsProvided_rendersCompleteCommand() {
        // Given
        Path configFilePath = Path.of("/path/to/config.yml");

        // When
        String command = ReorderCommandRenderer.render(
                BASE_DIR, Set.of("**/*.java"), Set.of("**/excluded/**"), configFilePath, true, true);

        // Then
        assertThat(command)
                .startsWith("jharmonizer reorder --base-dir \"" + BASE_DIR + "\"")
                .contains("--include \"**/*.java\"")
                .contains("--exclude \"**/excluded/**\"")
                .contains("--config \"" + configFilePath + "\"")
                .contains("--no-backup")
                .contains("--no-statistics");
    }

    @Test
    void render_baseDirWithSpaces_quotesArgumentsCorrectly() {
        // Given
        Path dirWithSpaces = Path.of("/my projects/my app");

        // When / Then
        assertThat(ReorderCommandRenderer.render(dirWithSpaces, Set.of(), Set.of(), null, false, false))
                .isEqualTo("jharmonizer reorder --base-dir \"/my projects/my app\"");
    }

    @Test
    void render_baseDirWithShellMetacharacters_escapesMetacharacters() {
        // Given
        Path dirWithMetachars = Path.of("/projects/$build");

        // When / Then
        assertThat(ReorderCommandRenderer.render(dirWithMetachars, Set.of(), Set.of(), null, false, false))
                .isEqualTo("jharmonizer reorder --base-dir \"/projects/\\$build\"");
    }
}

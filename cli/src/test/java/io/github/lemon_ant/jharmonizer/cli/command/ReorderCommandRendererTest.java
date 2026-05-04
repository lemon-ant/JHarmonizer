// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package io.github.lemon_ant.jharmonizer.cli.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ReorderCommandRendererTest {

    private static final Path BASE_DIR = Path.of("/projects/my-project");
    private static final String FALLBACK_LAUNCHER = "jharmonizer";

    @Test
    void render_noOptionalArguments_returnsBaseCommand() {
        // When / Then
        assertThat(ReorderCommandRenderer.render(FALLBACK_LAUNCHER, BASE_DIR, Set.of(), Set.of(), null, false, false))
                .isEqualTo("jharmonizer reorder --base-dir \"/projects/my-project\"");
    }

    @Test
    void render_allOptionalArgumentsProvided_rendersCompleteCommand() {
        // Given
        Path configFilePath = Path.of("/path/to/config.yml");

        // When
        String command = ReorderCommandRenderer.render(
                FALLBACK_LAUNCHER, BASE_DIR, Set.of("**/*.java"), Set.of("**/excluded/**"), configFilePath, true, true);

        // Then
        assertThat(command)
                .startsWith("jharmonizer reorder --base-dir \"/projects/my-project\"")
                .contains("--include \"**/*.java\"")
                .contains("--exclude \"**/excluded/**\"")
                .contains("--config \"/path/to/config.yml\"")
                .contains("--no-backup")
                .contains("--no-statistics");
    }

    @Test
    void render_baseDirWithSpaces_quotesArgumentsCorrectly() {
        // Given
        Path dirWithSpaces = Path.of("/my projects/my app");

        // When / Then
        assertThat(ReorderCommandRenderer.render(
                        FALLBACK_LAUNCHER, dirWithSpaces, Set.of(), Set.of(), null, false, false))
                .isEqualTo("jharmonizer reorder --base-dir \"/my projects/my app\"");
    }

    @Test
    void render_baseDirWithShellMetacharacters_escapesMetacharacters() {
        // Given
        Path dirWithMetachars = Path.of("/projects/$build");

        // When / Then
        assertThat(ReorderCommandRenderer.render(
                        FALLBACK_LAUNCHER, dirWithMetachars, Set.of(), Set.of(), null, false, false))
                .isEqualTo("jharmonizer reorder --base-dir \"/projects/\\$build\"");
    }

    @Test
    void render_globWithBackslash_escapesBackslash() {
        // When / Then
        assertThat(ReorderCommandRenderer.render(
                        FALLBACK_LAUNCHER, BASE_DIR, Set.of("**\\*.java"), Set.of(), null, false, false))
                .contains("--include \"**\\\\*.java\"");
    }

    @Test
    void render_customLauncherPrefix_prefixedsReorderSubcommand() {
        // Given
        String javaJarLauncher = "\"/opt/jdk-21/bin/java\" -jar jharmonizer-cli.jar";

        // When / Then
        assertThat(ReorderCommandRenderer.render(javaJarLauncher, BASE_DIR, Set.of(), Set.of(), null, false, false))
                .startsWith("\"/opt/jdk-21/bin/java\" -jar jharmonizer-cli.jar reorder");
    }
}

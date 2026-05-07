// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders dependent static fields in test classes;
// remove this directive once jharmonizer is upgraded to a version that respects field initialization order.
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StartupBannerRendererTest {

    private static final Path BASE_DIR = Path.of("/project/src");

    @Test
    void render_defaultParameters_containsHeaderWithUnderline() {
        // When
        String banner = StartupBannerRenderer.render(FlowType.REORDER, BASE_DIR, true, Set.of(), List.of());

        // Then
        assertThat(banner).contains("JHarmonizer started").contains("=".repeat("JHarmonizer started".length()));
    }

    @Test
    void render_allFlowParameters_containsFlowAndBaseDirAndBackups() {
        // When
        String banner = StartupBannerRenderer.render(FlowType.CHECK_ALL, BASE_DIR, false, Set.of(), List.of());

        // Then
        assertThat(banner)
                .contains("Flow:")
                .contains("CHECK_ALL")
                .contains("Base directory:")
                .contains(BASE_DIR.toString())
                .contains("Backups:")
                .contains("disabled");
    }

    @Test
    void render_backupsEnabled_showsEnabled() {
        // When
        String banner = StartupBannerRenderer.render(FlowType.REORDER, BASE_DIR, true, Set.of(), List.of());

        // Then
        assertThat(banner).contains("Backups:").contains("enabled");
    }

    @Nested
    class GlobFormatting {

        @Test
        void render_emptyIncludeGlobs_showsAllPlaceholder() {
            // When
            String banner = StartupBannerRenderer.render(FlowType.REORDER, BASE_DIR, true, Set.of(), List.of());

            // Then
            assertThat(banner).contains("Include globs:").contains("(all)");
        }

        @Test
        void render_emptyExcludeGlobs_showsNonePlaceholder() {
            // When
            String banner = StartupBannerRenderer.render(FlowType.REORDER, BASE_DIR, true, Set.of(), List.of());

            // Then
            assertThat(banner).contains("Exclude globs:").contains("(none)");
        }

        @Test
        void render_singleGlob_displayedWithoutPrefix() {
            // When
            String banner =
                    StartupBannerRenderer.render(FlowType.REORDER, BASE_DIR, true, Set.of("**/*.java"), List.of());

            // Then
            assertThat(banner).contains("Include globs:").contains("**/*.java").doesNotContain("- **/*.java");
        }

        @Test
        void render_globWithBackslashSeparator_displayedWithForwardSlashes() {
            // When
            String banner = StartupBannerRenderer.render(
                    FlowType.REORDER, BASE_DIR, true, Set.of("**\\org\\example\\MyClass.java"), List.of());

            // Then
            assertThat(banner).contains("**/org/example/MyClass.java").doesNotContain("**\\org\\example\\MyClass.java");
        }

        @Test
        void render_globWithEscapedMetachar_preservesEscapeSequence() {
            // When
            String banner = StartupBannerRenderer.render(
                    FlowType.REORDER, BASE_DIR, true, Set.of("src\\main\\java\\foo\\*bar.java"), List.of());

            // Then
            assertThat(banner).contains("src/main/java/foo\\*bar.java");
        }

        @Test
        void render_multipleGlobs_sortedAscending() {
            // Given
            Set<String> excludeGlobs = Set.of("**/target/**", "**/build/**", "**/.git/**");

            // When
            String banner = StartupBannerRenderer.render(FlowType.REORDER, BASE_DIR, true, Set.of(), excludeGlobs);

            // Then
            assertThat(banner).contains("**/.git/**").contains("**/build/**").contains("**/target/**");
            assertThat(banner.indexOf("**/.git/**")).isLessThan(banner.indexOf("**/build/**"));
            assertThat(banner.indexOf("**/build/**")).isLessThan(banner.indexOf("**/target/**"));
        }
    }
}

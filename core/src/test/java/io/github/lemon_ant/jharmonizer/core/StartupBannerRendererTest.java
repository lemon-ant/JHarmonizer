package io.github.lemon_ant.jharmonizer.core;

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
        void render_singleGlob_prefixedWithDash() {
            // When
            String banner =
                    StartupBannerRenderer.render(FlowType.REORDER, BASE_DIR, true, Set.of("**/*.java"), List.of());

            // Then
            assertThat(banner).contains("Include globs:").contains("- **/*.java");
        }

        @Test
        void render_multipleGlobs_allPrefixedWithDashAndSorted() {
            // Given
            Set<String> excludeGlobs = Set.of("**/target/**", "**/build/**", "**/.git/**");

            // When
            String banner = StartupBannerRenderer.render(FlowType.REORDER, BASE_DIR, true, Set.of(), excludeGlobs);

            // Then
            String[] lines = banner.split(System.lineSeparator());
            List<String> excludeLines = List.of(lines).stream()
                    .filter(line ->
                            line.contains("Exclude globs:") || line.trim().startsWith("- "))
                    .toList();
            assertThat(excludeLines).hasSize(3);
            assertThat(excludeLines.get(0)).contains("- **/.git/**");
            assertThat(excludeLines.get(1)).contains("- **/build/**");
            assertThat(excludeLines.get(2)).contains("- **/target/**");
        }
    }
}

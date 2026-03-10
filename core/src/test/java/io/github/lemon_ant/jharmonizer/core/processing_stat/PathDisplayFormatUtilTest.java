package io.github.lemon_ant.jharmonizer.core.processing_stat;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PathDisplayFormatUtilTest {

    @Test
    void abbreviatePathForDisplay_longPath_usesAsciiPrefixAndRespectsMaxLength() {
        // Given
        Path longPath = Path.of(
                "workspace",
                "very-long-module-name",
                "feature",
                "deeply",
                "nested",
                "DefaultConfigComplexTypesScenario.java");
        int maxLength = 60;

        // When
        String abbreviatedPath = PathDisplayFormatUtil.abbreviatePathForDisplay(longPath, maxLength);

        // Then
        String separator = longPath.getFileSystem().getSeparator();
        assertThat(abbreviatedPath)
                .startsWith("..." + separator)
                .doesNotContain("…")
                .hasSizeLessThanOrEqualTo(maxLength);
    }

    @Test
    void abbreviatePathForDisplay_shortPath_returnsOriginalPath() {
        // Given
        Path shortPath = Path.of("DefaultConfigComplexTypesScenario.java");

        // When
        String abbreviatedPath = PathDisplayFormatUtil.abbreviatePathForDisplay(shortPath, 100);

        // Then
        assertThat(abbreviatedPath).isEqualTo(shortPath.toString());
    }
}

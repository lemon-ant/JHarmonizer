// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.processing_stat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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

    @Test
    void abbreviatePathForDisplay_maxTotalLengthTooSmall_throwsIllegalArgumentException() {
        // Given
        Path path = Path.of("some/long/path/to/MyClass.java");
        int maxTotalLength = 10;

        // When
        Throwable thrown = catchThrowable(() -> PathDisplayFormatUtil.abbreviatePathForDisplay(path, maxTotalLength));

        // Then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxTotalLength is too small");
    }
}

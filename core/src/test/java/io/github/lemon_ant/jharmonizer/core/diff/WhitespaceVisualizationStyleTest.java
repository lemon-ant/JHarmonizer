// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.diff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WhitespaceVisualizationStyleTest {

    @Test
    void unicode_markers_useExpectedSymbols() {
        // When / Then
        assertThat(WhitespaceVisualizationStyle.UNICODE.getSpaceMark()).isEqualTo("·");
        assertThat(WhitespaceVisualizationStyle.UNICODE.getTabMark()).isEqualTo("→→→→");
        assertThat(WhitespaceVisualizationStyle.UNICODE.getEolMark()).isEqualTo("¶");
    }

    @Test
    void asciiSafe_markers_useExpectedSymbols() {
        // When / Then
        assertThat(WhitespaceVisualizationStyle.ASCII_SAFE.getSpaceMark()).isEqualTo(".");
        assertThat(WhitespaceVisualizationStyle.ASCII_SAFE.getTabMark()).isEqualTo("--->");
        assertThat(WhitespaceVisualizationStyle.ASCII_SAFE.getEolMark()).isEmpty();
    }
}

// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.diff;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WhitespaceVisualizationStyleTest {

    @Test
    void unicode_markers_useExpectedSymbols() {
        // When / Then
        assertThat(WhitespaceVisualizationStyle.UNICODE.getSpaceMark()).isEqualTo("·");
        assertThat(WhitespaceVisualizationStyle.UNICODE.getTabMark()).isEqualTo("→→→→");
        assertThat(WhitespaceVisualizationStyle.UNICODE.getEolMark()).isEqualTo("¶");
    }

    @Test
    void latinSafe_markers_useExpectedSymbols() {
        // When / Then
        assertThat(WhitespaceVisualizationStyle.LATIN_SAFE.getSpaceMark()).isEqualTo("·");
        assertThat(WhitespaceVisualizationStyle.LATIN_SAFE.getTabMark()).isEqualTo("--->");
        assertThat(WhitespaceVisualizationStyle.LATIN_SAFE.getEolMark()).isEqualTo("¶");
    }

    @Test
    void asciiSafe_markers_useExpectedSymbols() {
        // When / Then
        assertThat(WhitespaceVisualizationStyle.ASCII_SAFE.getSpaceMark()).isEqualTo(".");
        assertThat(WhitespaceVisualizationStyle.ASCII_SAFE.getTabMark()).isEqualTo("--->");
        assertThat(WhitespaceVisualizationStyle.ASCII_SAFE.getEolMark()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTF-8", "UTF-16"})
    void forCharset_unicodeCapableCharset_returnsUnicode(String charsetName) {
        // When
        WhitespaceVisualizationStyle style = WhitespaceVisualizationStyle.forCharset(Charset.forName(charsetName));

        // Then
        assertThat(style).isEqualTo(WhitespaceVisualizationStyle.UNICODE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"windows-1252", "ISO-8859-1", "IBM850"})
    void forCharset_latin1CapableCharset_returnsLatinSafe(String charsetName) {
        // When
        WhitespaceVisualizationStyle style = WhitespaceVisualizationStyle.forCharset(Charset.forName(charsetName));

        // Then
        assertThat(style).isEqualTo(WhitespaceVisualizationStyle.LATIN_SAFE);
    }

    @Test
    void forCharset_utf8Charset_returnsUnicode() {
        // When
        WhitespaceVisualizationStyle style = WhitespaceVisualizationStyle.forCharset(StandardCharsets.UTF_8);

        // Then
        assertThat(style).isEqualTo(WhitespaceVisualizationStyle.UNICODE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"IBM866", "US-ASCII"})
    void forCharset_asciiOnlyCharset_returnsAsciiSafe(String charsetName) {
        // When
        WhitespaceVisualizationStyle style = WhitespaceVisualizationStyle.forCharset(Charset.forName(charsetName));

        // Then
        assertThat(style).isEqualTo(WhitespaceVisualizationStyle.ASCII_SAFE);
    }
}

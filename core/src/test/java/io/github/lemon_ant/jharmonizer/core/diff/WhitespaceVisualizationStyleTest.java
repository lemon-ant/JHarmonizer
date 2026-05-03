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
    void forCharsets_sameUnicodeCapableCharset_returnsUnicode(String charsetName) {
        // Given
        Charset charset = Charset.forName(charsetName);

        // When
        WhitespaceVisualizationStyle style = WhitespaceVisualizationStyle.forCharsets(charset, charset);

        // Then
        assertThat(style).isEqualTo(WhitespaceVisualizationStyle.UNICODE);
    }

    @Test
    void forCharsets_sameUtf8Charset_returnsUnicode() {
        // When
        WhitespaceVisualizationStyle style =
                WhitespaceVisualizationStyle.forCharsets(StandardCharsets.UTF_8, StandardCharsets.UTF_8);

        // Then
        assertThat(style).isEqualTo(WhitespaceVisualizationStyle.UNICODE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"windows-1252", "ISO-8859-1", "IBM850"})
    void forCharsets_sameLatin1CompatibleCharset_returnsLatinSafe(String charsetName) {
        // Given
        Charset charset = Charset.forName(charsetName);

        // When
        WhitespaceVisualizationStyle style = WhitespaceVisualizationStyle.forCharsets(charset, charset);

        // Then
        assertThat(style).isEqualTo(WhitespaceVisualizationStyle.LATIN_SAFE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"IBM866", "US-ASCII"})
    void forCharsets_sameAsciiOnlyCharset_returnsAsciiSafe(String charsetName) {
        // Given
        Charset charset = Charset.forName(charsetName);

        // When
        WhitespaceVisualizationStyle style = WhitespaceVisualizationStyle.forCharsets(charset, charset);

        // Then
        assertThat(style).isEqualTo(WhitespaceVisualizationStyle.ASCII_SAFE);
    }

    @Test
    void forCharsets_cp850DisplayWithCp1252Encoder_returnsAsciiSafe() {
        // Given — CP850 and CP1252 assign different byte values to U+00B7 (·)
        Charset displayCharset = Charset.forName("IBM850");
        Charset encoderCharset = Charset.forName("windows-1252");

        // When
        WhitespaceVisualizationStyle style = WhitespaceVisualizationStyle.forCharsets(displayCharset, encoderCharset);

        // Then
        assertThat(style).isEqualTo(WhitespaceVisualizationStyle.ASCII_SAFE);
    }

    @Test
    void forCharsets_cp1252DisplayWithUtf8Encoder_returnsAsciiSafe() {
        // Given — when the terminal display charset is CP1252 but the encoder uses UTF-8
        // (e.g. Java 18+ with -Dfile.encoding=UTF-8 overriding the default), UTF-8 encodes ·
        // as two bytes (0xC2 0xB7) while CP1252 expects one byte (0xB7), so the bytes do not
        // match what the terminal can render — ASCII_SAFE prevents garbled output
        Charset displayCharset = Charset.forName("windows-1252");
        Charset encoderCharset = StandardCharsets.UTF_8;

        // When
        WhitespaceVisualizationStyle style = WhitespaceVisualizationStyle.forCharsets(displayCharset, encoderCharset);

        // Then
        assertThat(style).isEqualTo(WhitespaceVisualizationStyle.ASCII_SAFE);
    }

    @Test
    void forCharsets_cp850DisplayWithUtf8Encoder_returnsAsciiSafe() {
        // Given — IBM850 terminal display with UTF-8 encoder: UTF-8 encodes · as two bytes
        // (0xC2 0xB7) but IBM850 expects 0xF9 — bytes do not match, so ASCII_SAFE is required
        // to prevent garbled output such as ┬À (the CP850 interpretation of 0xC2 0xB7)
        Charset displayCharset = Charset.forName("IBM850");
        Charset encoderCharset = StandardCharsets.UTF_8;

        // When
        WhitespaceVisualizationStyle style = WhitespaceVisualizationStyle.forCharsets(displayCharset, encoderCharset);

        // Then
        assertThat(style).isEqualTo(WhitespaceVisualizationStyle.ASCII_SAFE);
    }
}

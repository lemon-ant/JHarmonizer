// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.diff;

import java.nio.charset.Charset;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Controls which symbol set is used to visualize whitespace characters in diff output.
 *
 * <p>Three styles are available, selected automatically via {@link #forCharset(Charset)}
 * based on what the given charset can actually encode:
 * <ul>
 *   <li>{@link #UNICODE} — full Unicode markers; requires a UTF-8 or similarly capable encoding
 *       that can render U+2192 (RIGHT ARROW)</li>
 *   <li>{@link #LATIN_SAFE} — Latin-1 supplement markers for spaces and end-of-line,
 *       ASCII {@code --->} for tabs; suitable for CP1252, ISO-8859-1, IBM850 and similar encodings</li>
 *   <li>{@link #ASCII_SAFE} — pure ASCII markers; safe on any encoding including IBM866, US-ASCII</li>
 * </ul>
 *
 * <p>Each constant carries the concrete marker strings ({@link #getSpaceMark()},
 * {@link #getTabMark()}, {@link #getEolMark()}) so callers can use them directly without
 * further conditional logic.
 */
@Getter
@RequiredArgsConstructor
enum WhitespaceVisualizationStyle {

    /**
     * Unicode whitespace markers: {@code ·} for spaces, {@code →→→→} for tabs, {@code ¶} for end-of-line.
     * Requires a terminal whose encoding can render U+2192 (e.g. UTF-8).
     */
    UNICODE("·", "→→→→", "¶"),

    /**
     * Latin-1 supplement markers: {@code ·} for spaces, {@code --->} for tabs, {@code ¶} for end-of-line.
     * Suitable for CP1252, ISO-8859-1, IBM850 and similar single-byte encodings that include both
     * U+00B7 (middle dot) and U+00B6 (pilcrow) but cannot render the Unicode arrow U+2192.
     */
    LATIN_SAFE("·", "--->", "¶"),

    /**
     * ASCII-only whitespace markers: {@code .} for spaces, {@code --->} for tabs, no end-of-line marker.
     * Works on any terminal regardless of encoding, including CP850 and CP866.
     */
    ASCII_SAFE(".", "--->", "");

    /** Symbol used to replace each space character in source lines. */
    private final String spaceMark;

    /** Symbol used to replace each tab character in source lines. */
    private final String tabMark;

    /** Symbol appended at the end of every source line, or empty if not applicable. */
    private final String eolMark;

    /**
     * Selects the appropriate style for the given charset by probing encodability of the
     * Unicode marker characters.
     *
     * <ul>
     *   <li>Charset can encode {@code →} (U+2192) → {@link #UNICODE}</li>
     *   <li>Charset can encode {@code ·} (U+00B7) and {@code ¶} (U+00B6) → {@link #LATIN_SAFE}
     *       (e.g. CP1252, ISO-8859-1, IBM850)</li>
     *   <li>Otherwise → {@link #ASCII_SAFE} (e.g. IBM866, US-ASCII)</li>
     * </ul>
     *
     * @param charset the charset to probe
     * @return the style best suited for that charset
     */
    @NonNull
    static WhitespaceVisualizationStyle forCharset(@NonNull Charset charset) {
        var encoder = charset.newEncoder();
        if (encoder.canEncode('\u2192')) {
            return UNICODE;
        }
        if (encoder.canEncode('\u00B7') && encoder.canEncode('\u00B6')) {
            return LATIN_SAFE;
        }
        return ASCII_SAFE;
    }
}

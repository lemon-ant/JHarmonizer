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
 * <p>Two styles are available, selected automatically via {@link #forCharset(Charset)}
 * based on what the given charset can encode:
 * <ul>
 *   <li>{@link #UNICODE} — full Unicode markers; requires an encoding that can render U+2192
 *       (RIGHT ARROW), such as UTF-8 or UTF-16</li>
 *   <li>{@link #ASCII_SAFE} — pure ASCII markers; safe on any encoding because ASCII bytes are
 *       identical across all standard single-byte and multi-byte charsets</li>
 * </ul>
 *
 * <p>Note: OEM/ANSI charsets (CP850, CP1252, ISO-8859-1 and similar) may technically encode
 * characters such as U+00B7 (·) and U+00B6 (¶), but in practice the output encoding used by
 * the logging framework ({@link java.nio.charset.Charset#defaultCharset()}) may differ from the
 * console display encoding, so non-ASCII markers can still arrive as garbled byte sequences.
 * Only ASCII markers are byte-identical across all charsets and are therefore always safe.
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
     * ASCII-only whitespace markers: {@code .} for spaces, {@code --->} for tabs, no end-of-line marker.
     * Works on any terminal regardless of encoding.
     */
    ASCII_SAFE(".", "--->", "");

    /** Symbol used to replace each space character in source lines. */
    private final String spaceMark;

    /** Symbol used to replace each tab character in source lines. */
    private final String tabMark;

    /** Symbol appended at the end of every source line, or empty if not applicable. */
    private final String eolMark;

    /**
     * Selects the appropriate style for the given charset by probing whether the charset can
     * encode the Unicode arrow character U+2192.
     *
     * <ul>
     *   <li>Charset can encode {@code →} (U+2192) → {@link #UNICODE} (e.g. UTF-8, UTF-16)</li>
     *   <li>Otherwise → {@link #ASCII_SAFE} (e.g. CP850, CP1252, IBM866, US-ASCII)</li>
     * </ul>
     *
     * <p>Single-byte charsets that technically encode Latin-1 supplement characters (U+00B7, U+00B6)
     * still map to {@link #ASCII_SAFE} because in practice the logging framework may encode output
     * using a different charset than the console display charset, causing non-ASCII bytes to be
     * misinterpreted. ASCII characters are byte-identical in all standard charsets and are
     * therefore always safe.
     *
     * @param charset the charset to probe
     * @return the style best suited for that charset
     */
    @NonNull
    static WhitespaceVisualizationStyle forCharset(@NonNull Charset charset) {
        if (charset.newEncoder().canEncode('\u2192')) {
            return UNICODE;
        }
        return ASCII_SAFE;
    }
}

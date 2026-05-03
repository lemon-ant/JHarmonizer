// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.diff;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Controls which symbol set is used to visualize whitespace characters in diff output.
 *
 * <p>Three styles are available, selected automatically via
 * {@link #forCharsets(Charset, Charset)} based on the display charset (what the console renders)
 * and the encoder charset (what the logging framework writes):
 * <ul>
 *   <li>{@link #UNICODE} — full Unicode markers; requires both charsets to encode U+2192
 *       (RIGHT ARROW) to identical bytes (e.g. UTF-8/UTF-8)</li>
 *   <li>{@link #LATIN_SAFE} — Latin-1 supplement markers for spaces and end-of-line,
 *       ASCII {@code --->} for tabs; selected when both charsets encode U+00B7 (·) and U+00B6 (¶)
 *       to identical bytes (e.g. CP1252/CP1252, ISO-8859-1/ISO-8859-1)</li>
 *   <li>{@link #ASCII_SAFE} — pure ASCII markers; used when display and encoder charsets produce
 *       different bytes for the non-ASCII markers, which would cause garbled output
 *       (e.g. CP850 display with CP1252 encoder)</li>
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
     * Requires both the display and encoder charsets to encode U+2192 identically (e.g. UTF-8).
     */
    UNICODE("·", "→→→→", "¶"),

    /**
     * Latin-1 supplement markers: {@code ·} for spaces, {@code --->} for tabs, {@code ¶} for end-of-line.
     * Selected when both the display and encoder charsets encode U+00B7 and U+00B6 to the same bytes
     * (e.g. CP1252/CP1252, ISO-8859-1/ISO-8859-1).
     */
    LATIN_SAFE("·", "--->", "¶"),

    /**
     * ASCII-only whitespace markers: {@code .} for spaces, {@code --->} for tabs, no end-of-line marker.
     * Used when the display and encoder charsets produce different byte sequences for non-ASCII markers,
     * which would otherwise cause garbled output (e.g. CP850 display with CP1252 encoder).
     */
    ASCII_SAFE(".", "--->", "");

    /** Symbol used to replace each space character in source lines. */
    private final String spaceMark;

    /** Symbol used to replace each tab character in source lines. */
    private final String tabMark;

    /** Symbol appended at the end of every source line, or empty if not applicable. */
    private final String eolMark;

    /**
     * Selects the appropriate style by comparing how the display charset (terminal) and the encoder
     * charset (logging framework) represent each candidate marker character.
     *
     * <p>A marker is safe to use only when both charsets encode it to the <em>same</em> byte
     * sequence, ensuring that what the encoder writes is exactly what the terminal can render.
     *
     * <ul>
     *   <li>Both charsets encode {@code →} (U+2192) identically → {@link #UNICODE}</li>
     *   <li>Both encode {@code ·} (U+00B7) and {@code ¶} (U+00B6) identically → {@link #LATIN_SAFE}
     *       (e.g. CP1252 display + CP1252 encoder)</li>
     *   <li>Otherwise → {@link #ASCII_SAFE} (e.g. CP850 display + CP1252 encoder, or any display
     *       with a UTF-8 encoder on Java 18+ where the ANSI and OEM code pages differ)</li>
     * </ul>
     *
     * @param displayCharset the charset used by the terminal to render output (e.g. from
     *                       {@code stdout.encoding})
     * @param encoderCharset the charset used by the logging framework to encode log messages
     *                       (typically {@link Charset#defaultCharset()})
     * @return the style best suited for the given charset pair
     */
    @NonNull
    static WhitespaceVisualizationStyle forCharsets(@NonNull Charset displayCharset, @NonNull Charset encoderCharset) {
        if (encodeIdentically(displayCharset, encoderCharset, '\u2192')) {
            return UNICODE;
        }
        if (encodeIdentically(displayCharset, encoderCharset, '\u00B7')
                && encodeIdentically(displayCharset, encoderCharset, '\u00B6')) {
            return LATIN_SAFE;
        }
        return ASCII_SAFE;
    }

    private static boolean encodeIdentically(Charset c1, Charset c2, char ch) {
        if (!c1.newEncoder().canEncode(ch) || !c2.newEncoder().canEncode(ch)) {
            return false;
        }
        ByteBuffer encoded1 = c1.encode(String.valueOf(ch));
        ByteBuffer encoded2 = c2.encode(String.valueOf(ch));
        return encoded1.equals(encoded2);
    }
}

// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.diff;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Controls which symbol set is used to visualize whitespace characters in diff output
 * and to render omission markers in relocation reports.
 *
 * <p>Four styles are available, selected automatically via
 * {@link #forCharsets(Charset, Charset)} based on the display charset (what the console renders)
 * and the encoder charset (what the logging framework writes):
 * <ul>
 *   <li>{@link #UNICODE} — full Unicode markers for whitespace ({@code ·}, {@code →→→→},
 *       {@code ¶}); ASCII {@code ...} as ellipsis; requires both charsets to encode U+2192
 *       (RIGHT ARROW) to identical bytes (e.g. UTF-8/UTF-8)</li>
 *   <li>{@link #EXTENDED_SAFE} — Latin-1 supplement markers (same markers as {@link #LATIN_SAFE});
 *       selected when both charsets encode U+2026 identically but not U+2192 (e.g. CP1252/CP1252);
 *       uses ASCII {@code ...} as ellipsis because byte 0x85 is a C1 control rendered as {@code ?}
 *       by most terminals</li>
 *   <li>{@link #LATIN_SAFE} — Latin-1 supplement markers for spaces and end-of-line,
 *       ASCII {@code --->} for tabs; selected when both charsets encode U+00B7 (·) and U+00B6 (¶)
 *       to identical bytes but cannot encode U+2026 (e.g. ISO-8859-1/ISO-8859-1,
 *       IBM850/IBM850 — byte 0x85 in IBM850 is {@code à}, not {@code …})</li>
 *   <li>{@link #ASCII_SAFE} — pure ASCII markers; used when display and encoder charsets produce
 *       different bytes for the non-ASCII markers, which would cause garbled output
 *       (e.g. CP850 display with CP1252 encoder)</li>
 * </ul>
 *
 * <p>Each constant carries the concrete marker strings ({@link #getSpaceMark()},
 * {@link #getTabMark()}, {@link #getEolMark()}, {@link #getEllipsisMark()},
 * {@link #getChunkOmissionMark()}) so callers can use them directly without further
 * conditional logic.
 */
@Getter
@RequiredArgsConstructor
public enum WhitespaceVisualizationStyle {

    /**
     * Unicode whitespace markers: {@code ·} for spaces, {@code →→→→} for tabs, {@code ¶} for
     * end-of-line, {@code ...} as ellipsis, {@code ¦} (U+00A6) as chunk omission mark.
     * Requires both the display and encoder charsets to encode U+2192 identically (e.g. UTF-8).
     */
    UNICODE("·", "→→→→", "¶", "...", "¦"),

    /**
     * Extended Latin markers: same as {@link #LATIN_SAFE} in every marker, including the
     * ASCII {@code ...} ellipsis.
     * U+2026 (byte {@code 0x85} in Windows-1252) was previously used as the ellipsis here,
     * but byte {@code 0x85} is a C1 control character that most terminals render as {@code ?},
     * so {@code ...} is used instead.
     * Selected when both charsets encode U+2026 identically but not U+2192 (e.g. CP1252/CP1252).
     */
    EXTENDED_SAFE("·", "--->", "¶", "...", "¦"),

    /**
     * Latin-1 supplement markers: {@code ·} for spaces, {@code --->} for tabs, {@code ¶} for
     * end-of-line, {@code ...} as ellipsis (U+2026 is absent from IBM850 and ISO-8859-1),
     * {@code ¦} (U+00A6) as chunk omission mark.
     * Selected when both the display and encoder charsets encode U+00B7 and U+00B6 to the same bytes
     * but cannot encode U+2026 (e.g. ISO-8859-1/ISO-8859-1, IBM850/IBM850).
     */
    LATIN_SAFE("·", "--->", "¶", "...", "¦"),

    /**
     * ASCII-only whitespace markers: {@code .} for spaces, {@code --->} for tabs, no end-of-line
     * marker, {@code ...} as ellipsis, {@code |} as chunk omission mark.
     * Used when the display and encoder charsets produce different byte sequences for non-ASCII
     * markers, which would otherwise cause garbled output (e.g. CP850 display with CP1252 encoder).
     */
    ASCII_SAFE(".", "--->", "", "...", "|");

    /** Symbol used to replace each space character in source lines. */
    private final String spaceMark;

    /** Symbol used to replace each tab character in source lines. */
    private final String tabMark;

    /** Symbol appended at the end of every source line, or empty if not applicable. */
    private final String eolMark;

    /**
     * Single-character ellipsis marker used in omission summaries such as
     * {@code ... and 3 more hunks omitted}.
     * {@code ...} (three ASCII dots) for all styles, ensuring the marker is readable on every
     * terminal regardless of charset.
     */
    private final String ellipsisMark;

    /**
     * Marker used as a visual separator between the first and last members of a truncated
     * relocation chunk, e.g. {@code ¦ (2 members omitted)}.
     * {@code ¦} (U+00A6) for {@link #UNICODE}, {@link #EXTENDED_SAFE}, and {@link #LATIN_SAFE}
     * (present in all charsets at those tiers, including IBM850 and CP1252);
     * {@code |} for {@link #ASCII_SAFE}.
     */
    private final String chunkOmissionMark;

    /**
     * Selects the appropriate style by comparing how the display charset (terminal) and the encoder
     * charset (logging framework) represent each candidate marker character.
     *
     * <p>A marker is safe to use only when both charsets encode it to the <em>same</em> byte
     * sequence, ensuring that what the encoder writes is exactly what the terminal can render.
     *
     * <ul>
     *   <li>Both charsets encode {@code →} (U+2192) identically → {@link #UNICODE}</li>
     *   <li>Both encode {@code …} (U+2026) identically → {@link #EXTENDED_SAFE}
     *       (e.g. CP1252/CP1252; U+2026 is byte 0x85 in Windows-1252, but most terminals
     *       render that byte as {@code ?}, so ASCII {@code ...} is used as the ellipsis)</li>
     *   <li>Both encode {@code ·} (U+00B7) and {@code ¶} (U+00B6) identically → {@link #LATIN_SAFE}
     *       (e.g. ISO-8859-1/ISO-8859-1, IBM850/IBM850)</li>
     *   <li>Otherwise → {@link #ASCII_SAFE} (e.g. CP850 display + CP1252 encoder, or any display
     *       with a UTF-8 encoder on Java 18+ where the ANSI and OEM code pages differ)</li>
     * </ul>
     *
     * @param displayCharset the charset used by the terminal to render output (e.g. from
     *                       {@code stdout.encoding})
     * @param encoderCharset the charset used by {@link System#out} to write log messages
     *                       (obtained via {@link java.io.PrintStream#charset() System.out.charset()})
     * @return the style best suited for the given charset pair
     */
    @NonNull
    static WhitespaceVisualizationStyle forCharsets(@NonNull Charset displayCharset, @NonNull Charset encoderCharset) {
        if (encodeIdentically(displayCharset, encoderCharset, '\u2192')) {
            return UNICODE;
        }
        if (encodeIdentically(displayCharset, encoderCharset, '\u2026')) {
            return EXTENDED_SAFE;
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

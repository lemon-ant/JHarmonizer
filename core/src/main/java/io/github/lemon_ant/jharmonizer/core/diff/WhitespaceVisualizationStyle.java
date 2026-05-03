// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.diff;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Controls which symbol set is used to visualize whitespace characters in diff output.
 *
 * <p>Three styles are available, selected automatically by {@link ConsoleUnicodeDetector}
 * based on what the stdout charset can actually encode:
 * <ul>
 *   <li>{@link #UNICODE} — full Unicode markers; requires a UTF-8 or similarly capable encoding</li>
 *   <li>{@link #LATIN_SAFE} — Latin-1 supplement markers for spaces and end-of-line,
 *       ASCII {@code --->} for tabs; suitable for CP1252, ISO-8859-1 and similar encodings</li>
 *   <li>{@link #ASCII_SAFE} — pure ASCII markers; safe on any encoding including CP850, CP866</li>
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
     * Suitable for CP1252, ISO-8859-1 and similar single-byte encodings that include the Latin-1
     * supplement range (U+0080–U+00FF) but cannot render the Unicode arrow U+2192.
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
}

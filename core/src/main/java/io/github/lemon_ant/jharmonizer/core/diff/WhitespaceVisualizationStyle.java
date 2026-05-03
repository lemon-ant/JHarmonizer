// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.diff;

/**
 * Controls which symbol set is used to visualize whitespace characters in diff output.
 *
 * <p>{@link #UNICODE} uses visually distinctive Unicode characters and is suitable for
 * UTF-8 capable terminals. {@link #ASCII_SAFE} uses plain ASCII characters that render
 * correctly on any terminal, including Windows PowerShell with non-UTF-8 code pages.
 *
 * <p>Each constant carries the concrete marker strings ({@link #spaceMark}, {@link #tabMark},
 * {@link #eolMark}) so callers can use them directly without further conditional logic.
 */
enum WhitespaceVisualizationStyle {

    /**
     * Unicode whitespace markers: {@code ·} for spaces, {@code →→→→} for tabs, {@code ¶} for end-of-line.
     * Requires a terminal that can render these characters (UTF-8 or Latin-1 output encoding).
     */
    UNICODE("·", "→→→→", "¶"),

    /**
     * ASCII-only whitespace markers: {@code .} for spaces, {@code >} for tabs, {@code $} for end-of-line.
     * Works on any terminal regardless of encoding.
     */
    ASCII_SAFE(".", ">", "$");

    /** Symbol used to replace each space character in source lines. */
    final String spaceMark;

    /** Symbol used to replace each tab character in source lines. */
    final String tabMark;

    /** Symbol appended at the end of every source line. */
    final String eolMark;

    WhitespaceVisualizationStyle(String spaceMark, String tabMark, String eolMark) {
        this.spaceMark = spaceMark;
        this.tabMark = tabMark;
        this.eolMark = eolMark;
    }
}

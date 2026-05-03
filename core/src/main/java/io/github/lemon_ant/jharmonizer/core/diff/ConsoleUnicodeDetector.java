// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.diff;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Detects whether the standard output stream can render Unicode whitespace marker characters.
 *
 * <p>Uses the {@code stdout.encoding} JVM system property (available since JDK 17) to determine
 * the effective charset of stdout. Falls back to {@link Charset#defaultCharset()} when the
 * property is absent or names an unrecognised charset. The detection result is computed once
 * and cached for the lifetime of the JVM.
 */
@UtilityClass
class ConsoleUnicodeDetector {

    private static final WhitespaceVisualizationStyle DETECTED_STYLE = detectStyle();

    /**
     * Returns the cached whitespace visualization style for the current JVM stdout.
     *
     * @return {@link WhitespaceVisualizationStyle#UNICODE} when stdout is UTF-8 capable,
     *         {@link WhitespaceVisualizationStyle#ASCII_SAFE} otherwise
     */
    @NonNull
    static WhitespaceVisualizationStyle resolveStyle() {
        return DETECTED_STYLE;
    }

    @NonNull
    private static WhitespaceVisualizationStyle detectStyle() {
        Charset stdoutCharset = resolveStdoutCharset();
        return StandardCharsets.UTF_8.equals(stdoutCharset)
                ? WhitespaceVisualizationStyle.UNICODE
                : WhitespaceVisualizationStyle.ASCII_SAFE;
    }

    @NonNull
    private static Charset resolveStdoutCharset() {
        String encodingProperty = System.getProperty("stdout.encoding");
        if (encodingProperty == null) {
            return Charset.defaultCharset();
        }
        try {
            return Charset.forName(encodingProperty);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException ignored) {
            return Charset.defaultCharset();
        }
    }
}

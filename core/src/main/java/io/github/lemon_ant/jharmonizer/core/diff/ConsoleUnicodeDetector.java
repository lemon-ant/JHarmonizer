// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.diff;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Detects which whitespace visualization style is appropriate for the standard output stream.
 *
 * <p>Uses the {@code stdout.encoding} JVM system property (available since JDK 17) to determine
 * the effective charset of stdout. Falls back to {@link Charset#defaultCharset()} when the
 * property is absent or names an unrecognised charset. The detection result is computed once
 * and cached for the lifetime of the JVM.
 *
 * <p>The style is selected by probing whether the resolved charset can encode the specific
 * marker characters:
 * <ul>
 *   <li>Can encode {@code →} (U+2192) → {@link WhitespaceVisualizationStyle#UNICODE}</li>
 *   <li>Can encode {@code ·} (U+00B7) → {@link WhitespaceVisualizationStyle#LATIN_SAFE}
 *       (e.g. CP1252, ISO-8859-1)</li>
 *   <li>Otherwise → {@link WhitespaceVisualizationStyle#ASCII_SAFE} (e.g. CP850, CP866)</li>
 * </ul>
 */
@UtilityClass
class ConsoleUnicodeDetector {

    private static final WhitespaceVisualizationStyle DETECTED_STYLE = detectStyle();

    /**
     * Returns the cached whitespace visualization style for the current JVM stdout.
     *
     * @return the style that best matches what the stdout encoding can render
     */
    @NonNull
    static WhitespaceVisualizationStyle resolveStyle() {
        return DETECTED_STYLE;
    }

    @NonNull
    private static WhitespaceVisualizationStyle detectStyle() {
        CharsetEncoder encoder = resolveStdoutCharset().newEncoder();
        if (encoder.canEncode('\u2192')) {
            return WhitespaceVisualizationStyle.UNICODE;
        }
        if (encoder.canEncode('\u00B7')) {
            return WhitespaceVisualizationStyle.LATIN_SAFE;
        }
        return WhitespaceVisualizationStyle.ASCII_SAFE;
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

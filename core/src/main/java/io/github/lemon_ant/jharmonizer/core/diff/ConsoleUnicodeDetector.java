// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.diff;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Detects which whitespace visualization style is appropriate for the standard output stream.
 *
 * <p>Uses the {@code stdout.encoding} JVM system property, when present, to determine the
 * effective charset of stdout. Falls back to {@link Charset#defaultCharset()} when the property
 * is absent or names an unrecognised charset; on some supported Java 17 runtimes, this fallback
 * path is the normal behavior because {@code stdout.encoding} is not populated. The detection
 * result is computed once and cached for the lifetime of the JVM.
 *
 * <p>Style selection is delegated to {@link WhitespaceVisualizationStyle#forCharset(Charset)}.
 */
@UtilityClass
class ConsoleUnicodeDetector {

    private static final WhitespaceVisualizationStyle DETECTED_STYLE =
            WhitespaceVisualizationStyle.forCharset(resolveStdoutCharset());

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

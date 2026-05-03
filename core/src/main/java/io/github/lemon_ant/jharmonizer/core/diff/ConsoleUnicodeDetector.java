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
 * <p>Resolves the effective stdout charset (the console display encoding) via a three-step
 * property lookup:
 * <ol>
 *   <li>{@code stdout.encoding} — set by the JVM on Java 18 and later to reflect the actual
 *       I/O encoding of the stdout stream (the console OEM code page on Windows).</li>
 *   <li>{@code native.encoding} — set by the JVM on Java 17 and later to reflect the native
 *       file I/O encoding; on Windows this is typically the ANSI code page, not the console
 *       OEM code page, so it may still be imprecise in locales where the two differ.</li>
 *   <li>{@link Charset#defaultCharset()} — last-resort fallback.</li>
 * </ol>
 *
 * <p>The encoder charset (what the logging framework writes) is always
 * {@link Charset#defaultCharset()}.  Style selection compares the display and encoder charsets
 * byte-by-byte for each candidate marker and delegates to
 * {@link WhitespaceVisualizationStyle#forCharsets(Charset, Charset)}.
 *
 * <p>The detection result is computed once and cached for the lifetime of the JVM.
 */
@UtilityClass
class ConsoleUnicodeDetector {

    private static final WhitespaceVisualizationStyle DETECTED_STYLE =
            WhitespaceVisualizationStyle.forCharsets(resolveStdoutCharset(), Charset.defaultCharset());

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
        String encoding = System.getProperty("stdout.encoding");
        if (encoding == null) {
            encoding = System.getProperty("native.encoding");
        }
        if (encoding == null) {
            return Charset.defaultCharset();
        }
        try {
            return Charset.forName(encoding);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException ignored) {
            return Charset.defaultCharset();
        }
    }
}

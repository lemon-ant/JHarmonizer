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
 * <p>The encoder charset (the charset that the logging framework uses to convert log strings to
 * bytes) is resolved separately: {@code stdout.encoding} if the property is set (Java 18 and
 * later); otherwise {@link Charset#defaultCharset()}, which equals the platform charset derived
 * from {@code file.encoding} (the same charset that Logback's {@code PatternLayoutEncoder} would
 * use when no explicit {@code &lt;charset&gt;} is configured).  The CLI's {@code logback.xml}
 * configures the STDOUT appender encoder with
 * {@code &lt;charset&gt;${stdout.encoding:-${file.encoding}}&lt;/charset&gt;}, so the encoder
 * charset detected here matches what Logback actually writes to the console.
 * Style selection compares the display and encoder charsets byte-by-byte for each candidate
 * marker and delegates to {@link WhitespaceVisualizationStyle#forCharsets(Charset, Charset)}.
 *
 * <p>The detection result is computed once and cached for the lifetime of the JVM.
 */
@UtilityClass
class ConsoleUnicodeDetector {
    private static final WhitespaceVisualizationStyle DETECTED_STYLE =
            WhitespaceVisualizationStyle.forCharsets(resolveStdoutCharset(), resolveEncoderCharset());

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
    private static Charset resolveEncoderCharset() {
        // The CLI's logback.xml configures the STDOUT encoder with
        // <charset>${stdout.encoding:-${file.encoding}}</charset>, so on Java 18+ the encoder
        // uses stdout.encoding.  On Java 17 (where stdout.encoding is not set by the JVM),
        // both the logback.xml fallback (${file.encoding}) and Charset.defaultCharset() resolve
        // to the same platform charset.  native.encoding is intentionally skipped here because
        // it reflects the OS/locale encoding, not the encoding the STDOUT encoder actually uses.
        String encoding = System.getProperty("stdout.encoding");
        if (encoding == null) {
            return Charset.defaultCharset();
        }
        try {
            return Charset.forName(encoding);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException ignored) {
            return Charset.defaultCharset();
        }
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

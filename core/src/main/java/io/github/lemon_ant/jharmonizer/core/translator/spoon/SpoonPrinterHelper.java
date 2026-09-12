// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import lombok.NonNull;
import spoon.compiler.Environment;
import spoon.reflect.visitor.PrinterHelper;

/**
 * Exposes output positions and line termination without copying Spoon's output buffer.
 */
final class SpoonPrinterHelper extends PrinterHelper {

    /**
     * Creates the shared output buffer.
     *
     * @param environment the Spoon printing environment
     */
    SpoonPrinterHelper(@NonNull Environment environment) {
        super(environment);
    }

    /**
     * Returns the current output length in source characters.
     *
     * @return the offset immediately after the last printed character
     */
    int getPrintedLength() {
        return sbf.length();
    }

    /**
     * Terminates the last line if the last printed fragment or comment left it open.
     */
    void terminateLine() {
        if (sbf.isEmpty() || (sbf.charAt(sbf.length() - 1) != '\n' && sbf.charAt(sbf.length() - 1) != '\r')) {
            writeln();
        }
    }
}

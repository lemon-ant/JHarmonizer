// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.cli.command;

import lombok.experimental.UtilityClass;

/**
 * Centralized exit codes returned by the JHarmonizer CLI.
 * <p>
 * Single source of truth used by both production commands and tests.
 * The numeric values are part of the CLI's public contract and are mirrored in
 * {@code cli/README.md} and {@code docs/08-CliRunner.md}.
 */
@UtilityClass
@SuppressWarnings("PMD.DataClass")
public class ExitCodes {

    /**
     * At least one file requires reordering. Returned by both {@code check-all} and
     * {@code check-fast} so that CI gates can match a single value regardless of which
     * check command is invoked.
     */
    public static final int CHECK_FAILED = 3;

    /**
     * Invalid CLI arguments. Picocli's default for usage errors.
     */
    public static final int INVALID_USAGE = 2;

    /**
     * Processing completed successfully and no violations were detected.
     */
    public static final int OK = 0;

    /**
     * Processing error: I/O problem, unexpected exception, invalid {@code --base-dir},
     * invalid {@code --config} path, etc.
     */
    public static final int PROCESSING_ERROR = 1;
}

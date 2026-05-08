// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import lombok.NonNull;
import picocli.CommandLine.Command;

/**
 * CLI command that checks Java source files in fail-fast mode,
 * stopping at the first file that requires reordering and returning a distinct exit code.
 */
@Command(
        name = "check-fast",
        description = "Checks Java source files and stops at the first one that requires reordering (fail-fast mode).",
        mixinStandardHelpOptions = true)
final class CheckFastCommand extends BaseCommand {

    /**
     * Returns the flow type.
     *
     * @return the flow type
     */
    @Override
    @NonNull
    protected FlowType getFlowType() {
        return FlowType.CHECK_FAIL_FAST;
    }

    /**
     * Creates a new {@code check-fast} command.
     */
    CheckFastCommand() {
        super(ExitCodes.CHECK_FAILED);
    }
}

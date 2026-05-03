/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import lombok.NonNull;
import picocli.CommandLine.Command;

/**
 * CLI command that reorders Java source files in-place
 * according to the configured member-ordering and formatting rules.
 */
@Command(
        name = "reorder",
        description = "Reorders Java source files according to the configured ordering rules.",
        mixinStandardHelpOptions = true)
final class ReorderCommand extends BaseCommand {

    /**
     * Creates a new {@code reorder} command.
     */
    ReorderCommand() {
        // Required by Picocli (command instantiation).
    }

    /**
     * Returns the flow type.
     *
     * @return the flow type
     */
    @Override
    @NonNull
    protected FlowType getFlowType() {
        return FlowType.REORDER;
    }
}

package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import picocli.CommandLine.Command;

/**
 * CLI command that restructures Java source files in-place
 * according to the configured member-ordering and formatting rules.
 */
@Command(
        name = "restructure",
        description = "Restructures Java source files according to the configured ordering rules.",
        mixinStandardHelpOptions = true)
final class RestructureCommand extends BaseCommand {

    /**
     * Creates a new {@code restructure} command.
     */
    RestructureCommand() {
        // Required by Picocli (command instantiation).
    }

    /**
     * Returns the flow type.
     *
     * @return the flow type
     */
    @Override
    protected FlowType getFlowType() {
        return FlowType.RESTRUCTURE;
    }
}

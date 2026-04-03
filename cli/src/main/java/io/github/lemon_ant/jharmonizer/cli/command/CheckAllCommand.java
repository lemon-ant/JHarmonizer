package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import lombok.NonNull;
import picocli.CommandLine.Command;

/**
 * CLI command that checks all Java source files and reports which ones require reordering.
 */
@Command(
        name = "check-all",
        description = "Checks all Java source files and reports which ones require reordering.",
        mixinStandardHelpOptions = true)
final class CheckAllCommand extends BaseCommand {

    /**
     * Creates a new {@code check-all} command.
     */
    CheckAllCommand() {
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
        return FlowType.CHECK_ALL;
    }
}

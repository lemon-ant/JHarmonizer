package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import lombok.NonNull;
import picocli.CommandLine.Command;

/**
 * CLI command that checks all Java source files and reports which ones require restructuring.
 */
@Command(
        name = "check-all",
        description = "Checks all Java source files and reports which ones require restructuring.",
        mixinStandardHelpOptions = true)
final class CheckAllCommand extends BaseCommand {

    CheckAllCommand() {
        // Required by Picocli (command instantiation).
    }

    @Override
    @NonNull
    protected FlowType getFlowType() {
        return FlowType.CHECK_ALL;
    }
}

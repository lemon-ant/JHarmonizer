package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import picocli.CommandLine.Command;

@Command(
        name = "check-all",
        description = "Checks all Java source files and reports which ones require restructuring.",
        mixinStandardHelpOptions = true)
final class CheckAllCommand extends BaseCommand {

    CheckAllCommand() {
        // Required for Picocli command instantiation.
    }

    @Override
    protected FlowType getFlowType() {
        return FlowType.CHECK_ALL;
    }
}

package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import picocli.CommandLine.Command;

@Command(
        name = "restructure",
        description = "Restructures Java source files according to the configured ordering rules.",
        mixinStandardHelpOptions = true)
final class RestructureCommand extends BaseCommand {

    // Explicit no-arg constructor is required for Picocli command instantiation.
    RestructureCommand() {}

    @Override
    protected FlowType getFlowType() {
        return FlowType.RESTRUCTURE;
    }
}

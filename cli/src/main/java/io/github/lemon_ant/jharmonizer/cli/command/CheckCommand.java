package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import picocli.CommandLine.Command;

@Command(
        name = "check",
        description = "Checks all Java source files and reports which ones require restructuring.",
        mixinStandardHelpOptions = true)
final class CheckCommand extends BaseCommand {

    CheckCommand() {}

    CheckCommand(SourceProcessor sourceProcessor) {
        super(sourceProcessor);
    }

    @Override
    protected FlowType getFlowType() {
        return FlowType.CHECK_ALL;
    }
}

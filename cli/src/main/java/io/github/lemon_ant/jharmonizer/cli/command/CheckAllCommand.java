package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import picocli.CommandLine.Command;

/**
 * CLI command that checks all Java source files and reports which ones require restructuring.
 */
@Command(
        name = "check-all",
        description = "Checks all Java source files and reports which ones require restructuring.",
        mixinStandardHelpOptions = true)
final class CheckAllCommand extends BaseCommand {

    /**
     * Creates a new CheckAllCommand.
     */
    CheckAllCommand() {}

    /**
     * Creates a new CheckAllCommand.
     * @param sourceProcessor the source processor
     */
    CheckAllCommand(SourceProcessor sourceProcessor) {
        super(sourceProcessor);
    }

    /**
     * Returns the flow type.
     * @return the flow type
     */
    @Override
    protected FlowType getFlowType() {
        return FlowType.CHECK_ALL;
    }
}

package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        name = "check",
        description = "Checks all Java source files and reports which ones require restructuring.",
        mixinStandardHelpOptions = true)
@SuppressWarnings("PMD.GuardLogStatement")
final class CheckCommand extends BaseCommand {

    CheckCommand() {}

    CheckCommand(SourceProcessor sourceProcessor) {
        super(sourceProcessor);
    }

    @Override
    protected int processWithFlow(CommandOptions commandOptions) {
        log.info("Checking sources in: {}", commandOptions.getBaseDir());
        getSourceProcessor()
                .processSources(
                        commandOptions.getBaseDir(),
                        commandOptions.getIncludeGlobs(),
                        commandOptions.getExcludeGlobs(),
                        FlowType.CHECK_ALL);
        log.info("Check completed.");
        return 0;
    }
}

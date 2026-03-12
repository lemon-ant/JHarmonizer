package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        name = "restructure",
        description = "Restructures Java source files according to the configured ordering rules.",
        mixinStandardHelpOptions = true)
@SuppressWarnings("PMD.GuardLogStatement")
final class RestructureCommand extends BaseCommand {

    RestructureCommand() {}

    RestructureCommand(SourceProcessor sourceProcessor) {
        super(sourceProcessor);
    }

    @Override
    protected int processWithFlow(CommandOptions commandOptions) {
        log.info("Processing sources in: {}", commandOptions.getBaseDir());
        getSourceProcessor()
                .processSources(
                        commandOptions.getBaseDir(),
                        commandOptions.getIncludeGlobs(),
                        commandOptions.getExcludeGlobs(),
                        FlowType.RESTRUCTURE);
        return 0;
    }
}

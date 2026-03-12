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
public class RestructureCommand extends BaseCommand {

    public RestructureCommand() {}

    RestructureCommand(SourceProcessor sourceProcessor) {
        super(sourceProcessor);
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    protected int execute(CommandOptions opts) {
        log.info("Processing sources in: {}", opts.getBaseDir());

        try {
            getSourceProcessor()
                    .processSources(
                            opts.getBaseDir(), opts.getIncludeGlobs(), opts.getExcludeGlobs(), FlowType.RESTRUCTURE);
            return 0;
        } catch (RuntimeException e) {
            log.error("Processing failed: {}", e.getMessage());
            return 1;
        }
    }
}

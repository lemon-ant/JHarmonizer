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
public class CheckCommand extends BaseCommand {

    public CheckCommand() {}

    CheckCommand(SourceProcessor sourceProcessor) {
        super(sourceProcessor);
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    protected int execute(CommandOptions opts) {
        log.info("Checking sources in: {}", opts.getBaseDir());

        try {
            sourceProcessor.processSources(
                    opts.getBaseDir(), opts.getIncludeGlobs(), opts.getExcludeGlobs(), FlowType.CHECK_ALL);
            log.info("Check completed.");
            return 0;
        } catch (RuntimeException e) {
            log.error("Processing failed: {}", e.getMessage());
            return 1;
        }
    }
}

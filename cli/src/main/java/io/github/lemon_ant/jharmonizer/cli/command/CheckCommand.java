package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        name = "check",
        description = "Checks all Java source files and reports which ones require restructuring.",
        mixinStandardHelpOptions = true)
@SuppressWarnings("PMD.GuardLogStatement")
public class CheckCommand extends BaseCommand {

    public CheckCommand() {
        this(new SourceProcessor());
    }

    CheckCommand(SourceProcessor sourceProcessor) {
        super(sourceProcessor);
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    protected int execute() {
        log.info("Checking sources in: {}", getBaseDir());

        try {
            sourceProcessor.processSources(
                    getBaseDir().toPath(),
                    Set.copyOf(getIncludeGlobs()),
                    Set.copyOf(getExcludeGlobs()),
                    FlowType.CHECK_ALL);
            log.info("Check completed.");
            return 0;
        } catch (RuntimeException e) {
            log.error("Processing failed: {}", e.getMessage());
            return 1;
        }
    }
}

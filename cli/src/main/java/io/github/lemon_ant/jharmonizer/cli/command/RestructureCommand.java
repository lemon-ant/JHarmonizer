package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        name = "restructure",
        description = "Restructures Java source files according to the configured ordering rules.",
        mixinStandardHelpOptions = true)
@SuppressWarnings("PMD.GuardLogStatement")
public class RestructureCommand extends BaseCommand {

    public RestructureCommand() {
        this(new SourceProcessor());
    }

    RestructureCommand(SourceProcessor sourceProcessor) {
        super(sourceProcessor);
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    protected Integer execute() {
        log.info("Processing sources in: {}", baseDir);

        try {
            sourceProcessor.processSources(
                    baseDir.toPath(), Set.copyOf(includeGlobs), Set.copyOf(excludeGlobs), FlowType.RESTRUCTURE);
            return 0;
        } catch (RuntimeException e) {
            log.error("Processing failed: {}", e.getMessage());
            return 1;
        }
    }
}

package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.flow.NotFormattedException;
import io.github.lemon_ant.jharmonizer.core.flow.NotOrderedException;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        name = "check-fast",
        description =
                "Checks Java source files and stops at the first one that requires restructuring (fail-fast mode).",
        mixinStandardHelpOptions = true)
@SuppressWarnings("PMD.GuardLogStatement")
public class CheckFastCommand extends BaseCommand {

    private static final int EXIT_CODE_CHECK_FAILED = 3;

    public CheckFastCommand() {}

    CheckFastCommand(SourceProcessor sourceProcessor) {
        super(sourceProcessor);
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    protected int execute(CommandOptions opts) {
        log.info("Checking sources (fail-fast) in: {}", opts.getBaseDir());

        try {
            getSourceProcessor()
                    .processSources(
                            opts.getBaseDir(),
                            opts.getIncludeGlobs(),
                            opts.getExcludeGlobs(),
                            FlowType.CHECK_FAIL_FAST);
            log.info("Check passed: no files require restructuring.");
            return 0;
        } catch (NotFormattedException | NotOrderedException e) {
            log.warn("Check failed: file requires restructuring. {}", e.getMessage());
            return EXIT_CODE_CHECK_FAILED;
        } catch (RuntimeException e) {
            log.error("Processing failed: {}", e.getMessage());
            return 1;
        }
    }
}

package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import picocli.CommandLine.Command;

/**
 * CLI command that checks Java source files in fail-fast mode,
 * stopping at the first file that requires restructuring and returning a distinct exit code.
 */
@Command(
        name = "check-fast",
        description =
                "Checks Java source files and stops at the first one that requires restructuring (fail-fast mode).",
        mixinStandardHelpOptions = true)
final class CheckFastCommand extends BaseCommand {

    private static final int EXIT_CODE_CHECK_FAILED = 3;

    /**
     * Creates a new CheckFastCommand.
     */
    CheckFastCommand() {}

    /**
     * Creates a new CheckFastCommand.
     * @param sourceProcessor the source processor
     */
    CheckFastCommand(SourceProcessor sourceProcessor) {
        super(sourceProcessor);
    }

    /**
     * Returns the flow type.
     * @return the flow type
     */
    @Override
    protected FlowType getFlowType() {
        return FlowType.CHECK_FAIL_FAST;
    }

    /**
     * Performs the check failed exit code.
     * @return the result
     */
    @Override
    protected int checkFailedExitCode() {
        return EXIT_CODE_CHECK_FAILED;
    }
}

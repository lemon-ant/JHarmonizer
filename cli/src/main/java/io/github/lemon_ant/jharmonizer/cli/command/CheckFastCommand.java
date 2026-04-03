package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import lombok.NonNull;
import picocli.CommandLine.Command;

/**
 * CLI command that checks Java source files in fail-fast mode,
 * stopping at the first file that requires reordering and returning a distinct exit code.
 */
@Command(
        name = "check-fast",
        description = "Checks Java source files and stops at the first one that requires reordering (fail-fast mode).",
        mixinStandardHelpOptions = true)
final class CheckFastCommand extends BaseCommand {

    private static final int EXIT_CODE_CHECK_FAILED = 3;

    /**
     * Creates a new {@code check-fast} command.
     */
    CheckFastCommand() {
        // Required by Picocli (command instantiation).
    }

    /**
     * Returns the flow type.
     *
     * @return the flow type
     */
    @Override
    @NonNull
    protected FlowType getFlowType() {
        return FlowType.CHECK_FAIL_FAST;
    }

    /**
     * Returns the exit code used when a check detects formatting or ordering changes.
     *
     * @return the fail-fast check exit code
     */
    @Override
    protected int checkFailedExitCode() {
        return EXIT_CODE_CHECK_FAILED;
    }
}

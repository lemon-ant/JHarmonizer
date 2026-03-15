package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import picocli.CommandLine.Command;

@Command(
        name = "check-fast",
        description =
                "Checks Java source files and stops at the first one that requires restructuring (fail-fast mode).",
        mixinStandardHelpOptions = true)
final class CheckFastCommand extends BaseCommand {

    private static final int EXIT_CODE_CHECK_FAILED = 3;

    // Explicit no-arg constructor is required for Picocli command instantiation.
    CheckFastCommand() {}

    @Override
    protected FlowType getFlowType() {
        return FlowType.CHECK_FAIL_FAST;
    }

    @Override
    protected int checkFailedExitCode() {
        return EXIT_CODE_CHECK_FAILED;
    }
}

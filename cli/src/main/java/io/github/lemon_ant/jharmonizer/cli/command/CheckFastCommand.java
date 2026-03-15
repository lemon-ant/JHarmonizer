package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.nio.file.Path;
import lombok.NonNull;
import picocli.CommandLine.Command;

@Command(
        name = "check-fast",
        description =
                "Checks Java source files and stops at the first one that requires restructuring (fail-fast mode).",
        mixinStandardHelpOptions = true)
final class CheckFastCommand extends BaseCommand {

    private static final int EXIT_CODE_CHECK_FAILED = 3;
    private final SourceProcessor sourceProcessor;

    CheckFastCommand() {
        this(null);
    }

    CheckFastCommand(SourceProcessor sourceProcessor) {
        this.sourceProcessor = sourceProcessor;
    }

    @Override
    protected FlowType getFlowType() {
        return FlowType.CHECK_FAIL_FAST;
    }

    @Override
    protected int checkFailedExitCode() {
        return EXIT_CODE_CHECK_FAILED;
    }

    @Override
    @NonNull
    protected SourceProcessor createSourceProcessor(Path configFilePath) {
        return sourceProcessor != null ? sourceProcessor : createDefaultSourceProcessor(configFilePath);
    }
}

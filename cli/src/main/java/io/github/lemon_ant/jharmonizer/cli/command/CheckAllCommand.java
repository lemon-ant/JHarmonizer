package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.nio.file.Path;
import lombok.NonNull;
import picocli.CommandLine.Command;

@Command(
        name = "check-all",
        description = "Checks all Java source files and reports which ones require restructuring.",
        mixinStandardHelpOptions = true)
final class CheckAllCommand extends BaseCommand {

    private final SourceProcessor sourceProcessor;

    CheckAllCommand() {
        this(null);
    }

    CheckAllCommand(SourceProcessor sourceProcessor) {
        this.sourceProcessor = sourceProcessor;
    }

    @Override
    protected FlowType getFlowType() {
        return FlowType.CHECK_ALL;
    }

    @Override
    @NonNull
    protected SourceProcessor createSourceProcessor(Path configFilePath) {
        return sourceProcessor != null ? sourceProcessor : createDefaultSourceProcessor(configFilePath);
    }
}

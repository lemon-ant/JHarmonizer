package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.nio.file.Path;
import lombok.NonNull;
import picocli.CommandLine.Command;

@Command(
        name = "restructure",
        description = "Restructures Java source files according to the configured ordering rules.",
        mixinStandardHelpOptions = true)
final class RestructureCommand extends BaseCommand {

    private final SourceProcessor sourceProcessor;

    RestructureCommand() {
        this(null);
    }

    RestructureCommand(SourceProcessor sourceProcessor) {
        this.sourceProcessor = sourceProcessor;
    }

    @Override
    protected FlowType getFlowType() {
        return FlowType.RESTRUCTURE;
    }

    @Override
    @NonNull
    protected SourceProcessor createSourceProcessor(Path configFilePath) {
        return sourceProcessor != null ? sourceProcessor : createDefaultSourceProcessor(configFilePath);
    }
}

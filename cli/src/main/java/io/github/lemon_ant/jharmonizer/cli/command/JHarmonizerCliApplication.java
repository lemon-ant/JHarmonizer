package io.github.lemon_ant.jharmonizer.cli.command;

import lombok.NonNull;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Entry point for the JHarmonizer command-line application.
 * Registers all sub-commands and delegates execution to Picocli.
 */
@Command(
        name = "jharmonizer",
        description = "JHarmonizer: harmonize Java source file structure.",
        mixinStandardHelpOptions = true,
        subcommands = {ReorderCommand.class, CheckAllCommand.class, CheckFastCommand.class})
public final class JHarmonizerCliApplication {

    private JHarmonizerCliApplication() {}

    /**
     * Starts the CLI application.
     * @param args the command-line arguments
     */
    public static void main(@NonNull String[] args) {
        int exitCode = new CommandLine(JHarmonizerCliApplication.class).execute(args);
        System.exit(exitCode);
    }
}

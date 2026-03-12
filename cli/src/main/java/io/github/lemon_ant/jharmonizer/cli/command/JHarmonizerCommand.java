package io.github.lemon_ant.jharmonizer.cli.command;

import picocli.CommandLine.Command;

@Command(
        name = "jharmonizer",
        description = "JHarmonizer: harmonize Java source file structure.",
        mixinStandardHelpOptions = true,
        subcommands = {RestructureCommand.class, CheckCommand.class, CheckFastCommand.class})
public class JHarmonizerCommand implements Runnable {

    @Override
    public void run() {
        // No-op: subcommands handle execution. Help is shown via mixinStandardHelpOptions.
    }
}

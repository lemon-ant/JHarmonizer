package io.github.lemon_ant.jharmonizer.cli;

import picocli.CommandLine;

public final class JHarmonizerCliApplication {

    private JHarmonizerCliApplication() {}

    public static void main(String[] args) {
        int exitCode = new CommandLine(new JHarmonizerCommand()).execute(args);
        System.exit(exitCode);
    }
}

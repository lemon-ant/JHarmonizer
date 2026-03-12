package io.github.lemon_ant.jharmonizer.cli.command;

import java.nio.file.Path;
import java.util.Set;
import lombok.NonNull;
import lombok.Value;

@Value
public class CommandOptions {
    @NonNull
    Path baseDir;

    @NonNull
    Set<String> includeGlobs;

    @NonNull
    Set<String> excludeGlobs;

    boolean verbose;
}

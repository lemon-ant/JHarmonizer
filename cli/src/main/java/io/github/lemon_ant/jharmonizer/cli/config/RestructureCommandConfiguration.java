package io.github.lemon_ant.jharmonizer.cli.config;

import java.nio.file.Path;
import java.util.Set;
import lombok.Value;

@Value
public class RestructureCommandConfiguration {
    Path baseDir;
    Set<String> includeGlobs;
    Set<String> excludeGlobs;
    boolean dryRun;
    boolean verbose;
}

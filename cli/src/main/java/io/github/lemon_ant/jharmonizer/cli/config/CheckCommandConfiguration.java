package io.github.lemon_ant.jharmonizer.cli.config;

import java.nio.file.Path;
import java.util.Set;
import lombok.Value;

@Value
public class CheckCommandConfiguration {
    Path baseDir;
    Set<String> includeGlobs;
    Set<String> excludeGlobs;
}

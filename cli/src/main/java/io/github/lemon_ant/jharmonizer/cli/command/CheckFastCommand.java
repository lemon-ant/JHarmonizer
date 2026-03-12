package io.github.lemon_ant.jharmonizer.cli.command;

import io.github.lemon_ant.jharmonizer.cli.config.CheckCommandConfiguration;
import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.flow.NotFormattedException;
import io.github.lemon_ant.jharmonizer.core.flow.NotOrderedException;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Command(
        name = "check-fast",
        description =
                "Checks Java source files and stops at the first one that requires restructuring (fail-fast mode).",
        mixinStandardHelpOptions = true)
@SuppressWarnings("PMD.GuardLogStatement")
public class CheckFastCommand implements Callable<Integer> {

    private static final int EXIT_CODE_CHECK_FAILED = 3;

    private final SourceProcessor sourceProcessor;

    @Option(
            names = "--base-dir",
            description = "Base directory containing Java source files to check.",
            required = true)
    private File baseDir;

    @SuppressWarnings("PMD.ImmutableField")
    @Option(names = "--include", description = "Glob patterns for files to include (can be repeated).")
    private Set<String> includeGlobs = new HashSet<>();

    @SuppressWarnings("PMD.ImmutableField")
    @Option(names = "--exclude", description = "Glob patterns for files to exclude (can be repeated).")
    private Set<String> excludeGlobs = new HashSet<>();

    public CheckFastCommand() {
        this(new SourceProcessor());
    }

    CheckFastCommand(SourceProcessor sourceProcessor) {
        this.sourceProcessor = sourceProcessor;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Integer call() {
        CheckCommandConfiguration config =
                new CheckCommandConfiguration(baseDir.toPath(), Set.copyOf(includeGlobs), Set.copyOf(excludeGlobs));

        log.info("Checking sources (fail-fast) in: {}", config.getBaseDir());

        try {
            sourceProcessor.processSources(
                    config.getBaseDir(), config.getIncludeGlobs(), config.getExcludeGlobs(), FlowType.CHECK_FAIL_FAST);
            log.info("Check passed: no files require restructuring.");
            return 0;
        } catch (NotFormattedException | NotOrderedException e) {
            log.warn("Check failed: file requires restructuring. {}", e.getMessage());
            return EXIT_CODE_CHECK_FAILED;
        } catch (RuntimeException e) {
            log.error("Processing failed: {}", e.getMessage());
            return 1;
        }
    }
}

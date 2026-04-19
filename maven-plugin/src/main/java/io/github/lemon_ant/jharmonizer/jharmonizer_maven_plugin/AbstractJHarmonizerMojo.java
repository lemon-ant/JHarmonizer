package io.github.lemon_ant.jharmonizer.jharmonizer_maven_plugin;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.SrcProcessingResult;
import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfigMerger;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Abstract base for all JHarmonizer Maven plugin goals.
 * Handles common parameter binding and delegates flow execution to concrete sub-classes
 * via {@link #getFlowType()}.
 */
abstract class AbstractJHarmonizerMojo extends AbstractMojo {

    /**
     * Directories to scan for Java source files.
     * When not configured, defaults to both the main Java source directory
     * ({@code src/main/java}) and the test Java source directory ({@code src/test/java}),
     * by scanning the project base directory with auto-derived include patterns.
     */
    @Parameter(property = "jharmonizer.baseDirs")
    @Nullable
    private Set<File> baseDirs;

    /**
     * Maven's main Java source directory. Used in the default scan configuration to derive
     * an include pattern pointing at {@code src/main/java} relative to the project root.
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true)
    private File mainSourceDirectory;

    /**
     * Maven's test Java source directory. Used in the default scan configuration to derive
     * an include pattern pointing at {@code src/test/java} relative to the project root.
     */
    @Parameter(defaultValue = "${project.build.testSourceDirectory}", readonly = true)
    private File testSourceDirectory;

    /**
     * The Maven project base directory. When {@code baseDirs} is not explicitly configured,
     * this is used as the single scan root and include patterns for both Java source directories
     * are derived relative to it automatically.
     */
    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File projectBaseDir;

    /**
     * Glob patterns for Java source files to include in processing.
     * When empty, all {@code .java} files under the configured base directories are included.
     */
    @Parameter(property = "jharmonizer.includes")
    private Set<String> includes;

    /**
     * Glob patterns for Java source files to exclude from processing.
     */
    @Parameter(property = "jharmonizer.excludes")
    private Set<String> excludes;

    /**
     * When {@code true}, skips execution of this goal entirely.
     */
    @Parameter(defaultValue = "false", property = "jharmonizer.skip")
    private boolean skip;

    /**
     * Path to a YAML configuration file whose settings are merged over the built-in defaults.
     * Defaults to {@code jharmonizer.yml} in the project root when that file exists.
     * When neither the default file nor an explicitly configured path exists, only the embedded
     * default configuration is used.
     */
    @Parameter(defaultValue = "${project.basedir}/jharmonizer.yml", property = "jharmonizer.configFile")
    @Nullable
    private File configFile;

    /**
     * Overrides the {@code backupsEnabled} setting from the active configuration.
     * When not set, the value from the configuration file is used; the embedded default is {@code true}.
     */
    @Parameter(property = "jharmonizer.backupsEnabled")
    @Nullable
    private Boolean backupsEnabled;

    /**
     * Overrides the {@code printProcessingStatistics} setting from the active configuration.
     * When not set, the value from the configuration file is used; the embedded default is {@code true}.
     */
    @Parameter(property = "jharmonizer.printProcessingStatistics")
    @Nullable
    private Boolean printProcessingStatistics;

    /**
     * When {@code true} and the processing result indicates a violation (e.g. non-conforming files
     * in a check flow), the build is failed with a {@link MojoFailureException}.
     * Set to {@code false} to report violations without breaking the build.
     */
    @Parameter(defaultValue = "true", property = "jharmonizer.failOnViolation")
    private boolean failOnViolation;

    /**
     * Returns the processing flow strategy implemented by the concrete goal.
     *
     * @return the flow type to execute
     */
    @NonNull
    protected abstract FlowType getFlowType();

    /**
     * Validates parameters, builds configuration, and runs the selected JHarmonizer flow.
     *
     * @throws MojoExecutionException when a base directory is invalid or an unexpected error occurs
     * @throws MojoFailureException   when the flow reports violations and {@code failOnViolation} is {@code true}
     */
    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("JHarmonizer: skipping execution (jharmonizer.skip=true).");
            return;
        }

        List<Path> resolvedBaseDirs;
        Set<String> effectiveIncludes;

        if (baseDirs != null && !baseDirs.isEmpty()) {
            resolvedBaseDirs = baseDirs.stream()
                    .map(file -> file.toPath().toAbsolutePath().normalize())
                    .toList();
            for (Path baseDirPath : resolvedBaseDirs) {
                if (!Files.isDirectory(baseDirPath)) {
                    throw new MojoExecutionException(
                            "baseDirs entry does not exist or is not a directory: " + baseDirPath);
                }
            }
            effectiveIncludes = includes != null ? includes : Set.of();
        } else {
            if (projectBaseDir == null) {
                throw new MojoExecutionException("Project base directory (${project.basedir}) is not available."
                        + " Configure <baseDirs> explicitly.");
            }
            Path projectBaseDirPath = projectBaseDir.toPath().toAbsolutePath().normalize();
            if (!Files.isDirectory(projectBaseDirPath)) {
                throw new MojoExecutionException(
                        "Project base directory does not exist or is not a directory: " + projectBaseDirPath);
            }
            resolvedBaseDirs = List.of(projectBaseDirPath);
            try {
                effectiveIncludes = computeDefaultIncludes(projectBaseDirPath);
            } catch (IllegalArgumentException e) {
                throw new MojoExecutionException(
                        "Cannot compute default source include patterns relative to project base directory '"
                                + projectBaseDirPath + "'."
                                + " Configure <baseDirs> explicitly.",
                        e);
            }
        }

        SrcProcessingResult srcProcessingResult = invokeSrcProcessor(resolvedBaseDirs, effectiveIncludes);

        if (!srcProcessingResult.isSuccess() && failOnViolation) {
            long nonConformingCount = srcProcessingResult.getStatistics().computeNonConformingFileCount();
            throw new MojoFailureException("JHarmonizer: "
                    + nonConformingCount
                    + " source file(s) do not conform to the configured ordering."
                    + " To suppress this failure, set -Djharmonizer.failOnViolation=false"
                    + " or configure <failOnViolation>false</failOnViolation> in the plugin configuration.");
        }
    }

    @NonNull
    private Set<String> computeDefaultIncludes(@NonNull Path projectBaseDirPath) {
        Stream<String> srcDirIncludes = Stream.of(mainSourceDirectory, testSourceDirectory)
                .filter(Objects::nonNull)
                .map(srcDir -> projectBaseDirPath
                                .relativize(srcDir.toPath().toAbsolutePath().normalize())
                                .toString()
                                .replace(File.separatorChar, '/')
                        + "/**");
        Stream<String> userIncludes = includes != null ? includes.stream() : Stream.empty();
        return Stream.concat(srcDirIncludes, userIncludes).collect(Collectors.toUnmodifiableSet());
    }

    @NonNull
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private SrcProcessingResult invokeSrcProcessor(
            @NonNull List<Path> baseDirPaths, @NonNull Set<String> effectiveIncludes) throws MojoExecutionException {
        Set<String> effectiveExcludes = excludes != null ? excludes : Set.of();
        try {
            return new SrcProcessor(buildConfig())
                    .processSources(baseDirPaths, effectiveIncludes, effectiveExcludes, getFlowType());
        } catch (RuntimeException e) {
            throw new MojoExecutionException("JHarmonizer processing failed unexpectedly.", e);
        }
    }

    @Nullable
    private FlexibleUnifiedConfig buildConfig() {
        FlexibleUnifiedConfig fileConfig = resolveFileConfig();

        FlexibleUnifiedConfig paramOverrideConfig = (backupsEnabled != null || printProcessingStatistics != null)
                ? FlexibleUnifiedConfig.builder()
                        .backupsEnabled(backupsEnabled)
                        .printProcessingStatistics(printProcessingStatistics)
                        .build()
                : null;

        return mergeFlexibleConfigs(fileConfig, paramOverrideConfig);
    }

    @Nullable
    private FlexibleUnifiedConfig resolveFileConfig() {
        if (configFile == null) {
            return null;
        }
        Path configFilePath = configFile.toPath();
        if (!Files.isRegularFile(configFilePath)) {
            return null;
        }
        return JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromFile(configFilePath);
    }

    @Nullable
    private static FlexibleUnifiedConfig mergeFlexibleConfigs(
            FlexibleUnifiedConfig baselineConfig, FlexibleUnifiedConfig overlayConfig) {
        if (baselineConfig == null) {
            return overlayConfig;
        }
        if (overlayConfig == null) {
            return baselineConfig;
        }
        return UnifiedConfigMerger.merge(baselineConfig, overlayConfig);
    }
}

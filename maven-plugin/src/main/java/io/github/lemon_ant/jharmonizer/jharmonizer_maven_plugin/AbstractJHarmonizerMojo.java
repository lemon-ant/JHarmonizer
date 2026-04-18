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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
     * ({@code src/main/java}) and the test Java source directory ({@code src/test/java}).
     */
    @Parameter(property = "jharmonizer.baseDirs")
    @Nullable
    private List<File> baseDirs;

    /**
     * Maven's main Java source directory, used as a default base directory when
     * {@code baseDirs} is not explicitly configured.
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}", readonly = true)
    private File defaultMainSourceDirectory;

    /**
     * Maven's test Java source directory, used as a default base directory when
     * {@code baseDirs} is not explicitly configured.
     */
    @Parameter(defaultValue = "${project.build.testSourceDirectory}", readonly = true)
    private File defaultTestSourceDirectory;

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

        List<Path> resolvedBaseDirs = resolveBaseDirPaths();
        List<Path> validatedBaseDirs = new ArrayList<>();
        for (Path baseDirPath : resolvedBaseDirs) {
            if (!Files.isDirectory(baseDirPath)) {
                throw new MojoExecutionException("baseDir does not exist or is not a directory: " + baseDirPath);
            }
            validatedBaseDirs.add(baseDirPath);
        }

        SrcProcessingResult srcProcessingResult = invokeSrcProcessor(List.copyOf(validatedBaseDirs));

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
    private List<Path> resolveBaseDirPaths() {
        if (baseDirs != null && !baseDirs.isEmpty()) {
            return baseDirs.stream()
                    .map(File::toPath)
                    .map(path -> path.toAbsolutePath().normalize())
                    .toList();
        }
        List<Path> defaultDirs = new ArrayList<>();
        if (defaultMainSourceDirectory != null) {
            defaultDirs.add(defaultMainSourceDirectory.toPath().toAbsolutePath().normalize());
        }
        if (defaultTestSourceDirectory != null) {
            defaultDirs.add(defaultTestSourceDirectory.toPath().toAbsolutePath().normalize());
        }
        return List.copyOf(defaultDirs);
    }

    @NonNull
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private SrcProcessingResult invokeSrcProcessor(List<Path> baseDirPaths) throws MojoExecutionException {
        Set<String> effectiveIncludes = includes != null ? includes : Set.of();
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

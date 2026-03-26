package io.github.lemon_ant.jharmonizer.core.config;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfigMerger;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Central configuration manager responsible for:
 * 1) Loading the built-in default configuration from resources.
 * 2) Converting the vendor (JHarmonizer) model to the strict Unified model.
 * 3) Compiling the Unified model into the Compiled (runtime) model.
 * <p>
 * This class is intentionally minimal and pure. Future versions can add overlay sources
 * (IDEA/Eclipse exports, project YAML, CLI overrides) and a proper merge pipeline.
 */
@UtilityClass
public class ConfigurationManager {

    /**
     * Load the embedded default configuration, convert it to Unified and compile into Compiled.
     *
     * @return CompiledConfig ready for runtime classification and sorting.
     * @throws IllegalStateException if the resource cannot be found or parsed.
     */
    @NonNull
    public static CompiledConfig loadDefaultConfig() {
        UnifiedConfig dafaultUnifiedConfig = JHarmonizerConfigurationManager.parseUnifiedDefaultConfig();
        return Unified2CompiledModelCompiler.compile(dafaultUnifiedConfig);
    }

    /**
     * Overrides the default config.
     * @param externalConfig the external configuration overrides
     * @return the result
     */
    @NonNull
    public static CompiledConfig overrideDefaultConfig(@Nullable FlexibleUnifiedConfig externalConfig) {
        return overrideDefaultConfig(externalConfig, null);
    }

    /**
     * Overrides the default config with external configuration and optional backups override.
     *
     * @param externalConfig the external configuration overrides
     * @param backupsEnabledOverride optional backups-enabled runtime override
     * @return the merged and compiled runtime configuration
     */
    @NonNull
    public static CompiledConfig overrideDefaultConfig(
            @Nullable FlexibleUnifiedConfig externalConfig, @Nullable Boolean backupsEnabledOverride) {
        FlexibleUnifiedConfig effectiveExternalConfig =
                withBackupsEnabledOverride(externalConfig, backupsEnabledOverride);
        if (effectiveExternalConfig == null) {
            return loadDefaultConfig();
        }
        UnifiedConfig defaultUnifiedConfig = JHarmonizerConfigurationManager.parseUnifiedDefaultConfig();
        UnifiedConfig mergedUnifiedConfig = UnifiedConfigMerger.merge(defaultUnifiedConfig, effectiveExternalConfig);
        return Unified2CompiledModelCompiler.compile(mergedUnifiedConfig);
    }

    /**
     * Returns an external config copy with optional backups override applied.
     *
     * @param externalConfig the external configuration overrides
     * @param backupsEnabledOverride optional backups-enabled runtime override
     * @return adjusted external config; {@code null} when no external config/override is provided
     */
    @Nullable
    public static FlexibleUnifiedConfig withBackupsEnabledOverride(
            @Nullable FlexibleUnifiedConfig externalConfig, @Nullable Boolean backupsEnabledOverride) {
        if (backupsEnabledOverride == null) {
            return externalConfig;
        }
        if (externalConfig == null) {
            return new FlexibleUnifiedConfig(null, null, backupsEnabledOverride, null, null);
        }
        return new FlexibleUnifiedConfig(
                externalConfig.getTopLevelTypesOrdering().orElse(null),
                externalConfig.getFormatting().orElse(null),
                backupsEnabledOverride,
                externalConfig.getHeaderLine().orElse(null),
                externalConfig.getRootMemberGroups().orElse(null));
    }
}

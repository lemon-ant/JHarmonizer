// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfigMerger;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;

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
        if (null == externalConfig) {
            return loadDefaultConfig();
        }

        UnifiedConfig defaultUnifiedConfig = JHarmonizerConfigurationManager.parseUnifiedDefaultConfig();
        UnifiedConfig mergedUnifiedConfig = UnifiedConfigMerger.merge(defaultUnifiedConfig, externalConfig);
        return Unified2CompiledModelCompiler.compile(mergedUnifiedConfig);
    }
}

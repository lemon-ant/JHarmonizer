package io.github.lemon_ant.jharmonizer.core.config;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.UnifiedToEffectiveCompiler;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigLoader;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter.JHarmonizer2UnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Central configuration manager responsible for:
 * 1) Loading the built-in default configuration from resources.
 * 2) Converting the vendor (JHarmonizer) model to the strict Unified model.
 * 3) Compiling the Unified model into the Effective (runtime) model.
 * <p>
 * This class is intentionally minimal and pure. Future versions can add overlay sources
 * (IDEA/Eclipse exports, project YAML, CLI overrides) and a proper merge pipeline.
 */
@UtilityClass
public class ConfigurationManager {

    /**
     * Load the embedded default configuration, convert it to Unified and compile into Effective.
     *
     * @return CompiledConfig ready for runtime classification and sorting.
     * @throws IllegalStateException if the resource cannot be found or parsed.
     */
    @NonNull
    public static CompiledConfig loadDefaultEffectiveConfig() {
        JHarmonizerConfig jHarmonizerConfig = JHarmonizerConfigLoader.loadDefault();
        UnifiedConfig unifiedConfig = JHarmonizer2UnifiedConverter.convert2Unified(jHarmonizerConfig);
        return UnifiedToEffectiveCompiler.compile(unifiedConfig);
    }
}

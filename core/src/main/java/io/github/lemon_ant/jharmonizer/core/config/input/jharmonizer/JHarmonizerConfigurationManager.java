package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter.JHarmonizer2UnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import java.net.URL;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

// TODO Merge with JHarmonizerConfigLoader
/**
 * Facade for loading and converting JHarmonizer YAML configs into the
 * {@link io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig} model.
 */
@UtilityClass
public class JHarmonizerConfigurationManager {
    @NonNull
    public static UnifiedConfig parseUnifiedDefaultConfig() {
        JHarmonizerConfig defaultJHarmonizerConfig = JHarmonizerConfigLoader.loadDefault();
        return JHarmonizer2UnifiedConverter.convert2Unified(defaultJHarmonizerConfig);
    }

    @NonNull
    public static UnifiedConfig parseUnifiedConfigFromClasspathResource(@NonNull URL classpathResource) {
        JHarmonizerConfig loadedConfig = JHarmonizerConfigLoader.loadFromClasspathResource(classpathResource);
        return JHarmonizer2UnifiedConverter.convert2Unified(loadedConfig);
    }
}

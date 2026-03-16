package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter.JHarmonizer2UnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import java.net.URL;
import lombok.experimental.UtilityClass;

// TODO Merge with JHarmonizerConfigLoader
/**
 * Facade for loading and converting JHarmonizer YAML configs into the
 * {@link io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig} model.
 */
@UtilityClass
public class JHarmonizerConfigurationManager {
    /**
     * Parses the unified default config.
     * @return the unified default config
     */
    public static UnifiedConfig parseUnifiedDefaultConfig() {
        JHarmonizerConfig defaultJHarmonizerConfig = JHarmonizerConfigLoader.loadDefault();
        return JHarmonizer2UnifiedConverter.convert2Unified(defaultJHarmonizerConfig);
    }

    /**
     * Parses the unified config from classpath resource.
     * @param classpathResource the classpath resource to read
     * @return the unified config from classpath resource
     */
    public static UnifiedConfig parseUnifiedConfigFromClasspathResource(URL classpathResource) {
        JHarmonizerConfig loadedConfig = JHarmonizerConfigLoader.loadFromClasspathResource(classpathResource);
        return JHarmonizer2UnifiedConverter.convert2Unified(loadedConfig);
    }
}

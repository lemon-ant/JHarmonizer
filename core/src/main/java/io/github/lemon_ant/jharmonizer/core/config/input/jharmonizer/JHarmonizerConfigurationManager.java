package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter.JHarmonizer2FlexibleUnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter.JHarmonizer2UnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerFlexibleConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import java.net.URL;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

// TODO Merge with JHarmonizerConfigLoader
@UtilityClass
public class JHarmonizerConfigurationManager {
    public static UnifiedConfig parseUnifiedDefaultConfig() {
        JHarmonizerConfig defaultJHarmonizerConfig = JHarmonizerConfigLoader.loadDefault();
        return JHarmonizer2UnifiedConverter.convert2Unified(defaultJHarmonizerConfig);
    }

    public static UnifiedConfig parseUnifiedConfigFromClasspathResource(URL classpathResource) {
        JHarmonizerConfig loadedConfig = JHarmonizerConfigLoader.loadFromClasspathResource(classpathResource);
        return JHarmonizer2UnifiedConverter.convert2Unified(loadedConfig);
    }

    public static FlexibleUnifiedConfig parseFlexibleUnifiedConfigFromClasspathResource(
            @NonNull URL classpathResource) {
        JHarmonizerFlexibleConfig flexibleConfig =
                JHarmonizerConfigLoader.loadFlexibleFromClasspathResource(classpathResource);
        return JHarmonizer2FlexibleUnifiedConverter.convert2Flexible(flexibleConfig);
    }

    public static FlexibleUnifiedConfig parseFlexibleUnifiedConfigFromFile(@NonNull Path configFilePath) {
        JHarmonizerFlexibleConfig flexibleConfig = JHarmonizerConfigLoader.loadFlexibleFrom(configFilePath.toFile());
        return JHarmonizer2FlexibleUnifiedConverter.convert2Flexible(flexibleConfig);
    }
}

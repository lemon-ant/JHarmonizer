// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter.JHarmonizer2UnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter.JHarmonizerFlexible2FlexibleUnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerFlexibleConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import java.net.URL;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

// TODO Merge with JHarmonizerConfigLoader
/**
 * Facade for loading and converting JHarmonizer YAML configs into the
 * {@link io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig} model.
 */
@UtilityClass
public class JHarmonizerConfigurationManager {
    /**
     * Parses the embedded default configuration into the unified model.
     *
     * @return the unified default configuration
     */
    @NonNull
    public static UnifiedConfig parseUnifiedDefaultConfig() {
        JHarmonizerConfig defaultJHarmonizerConfig = JHarmonizerConfigLoader.loadDefault();
        return JHarmonizer2UnifiedConverter.convert2Unified(defaultJHarmonizerConfig);
    }

    /**
     * Parses a classpath configuration resource into the unified model.
     *
     * @param classpathResource the classpath resource to read
     * @return the unified configuration loaded from the resource
     */
    @NonNull
    public static UnifiedConfig parseUnifiedConfigFromClasspathResource(@NonNull URL classpathResource) {
        JHarmonizerConfig loadedConfig = JHarmonizerConfigLoader.loadFromClasspathResource(classpathResource);
        return JHarmonizer2UnifiedConverter.convert2Unified(loadedConfig);
    }

    /**
     * Parses a classpath configuration resource into the flexible unified model.
     *
     * @param classpathResource the classpath resource to read
     * @return the flexible unified configuration loaded from the resource
     */
    @NonNull
    public static FlexibleUnifiedConfig parseFlexibleUnifiedConfigFromClasspathResource(
            @NonNull URL classpathResource) {
        JHarmonizerFlexibleConfig flexibleConfig =
                JHarmonizerConfigLoader.loadFlexibleFromClasspathResource(classpathResource);
        return JHarmonizerFlexible2FlexibleUnifiedConverter.convert2FlexibleUnified(flexibleConfig);
    }

    /**
     * Parses a filesystem configuration file into the flexible unified model.
     *
     * @param configFilePath the configuration file path to read
     * @return the flexible unified configuration loaded from the file
     */
    @NonNull
    public static FlexibleUnifiedConfig parseFlexibleUnifiedConfigFromFile(@NonNull Path configFilePath) {
        JHarmonizerFlexibleConfig flexibleConfig = JHarmonizerConfigLoader.loadFlexibleFrom(configFilePath.toFile());
        return JHarmonizerFlexible2FlexibleUnifiedConverter.convert2FlexibleUnified(flexibleConfig);
    }
}

package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerFlexibleConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Loads {@link JHarmonizerConfig} instances from YAML sources:
 * the embedded default config, an {@link InputStream}, a classpath URL, or a file on disk.
 */
@UtilityClass
public class JHarmonizerConfigLoader {
    private static final String DEFAULT_CONFIG_RESOURCE_PATH = "/default-config.yml";
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    @NonNull
    static JHarmonizerConfig loadDefault() {
        try (InputStream configYaml =
                requireNonNull(JHarmonizerConfigLoader.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE_PATH))) {
            return loadFrom(configYaml);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read embedded default configuration: " + DEFAULT_CONFIG_RESOURCE_PATH, e);
        }
    }

    @NonNull
    static JHarmonizerConfig loadFrom(@NonNull InputStream configInput) throws IOException {
        return YAML_MAPPER.readValue(configInput, JHarmonizerConfig.class);
    }

    @NonNull
    static JHarmonizerConfig loadFromClasspathResource(@NonNull URL classpathResource) {
        try (InputStream inputStream = classpathResource.openStream()) {
            return JHarmonizerConfigLoader.loadFrom(inputStream);
        } catch (IOException ioException) {
            throw new UncheckedIOException(
                    "Failed to load JHarmonizer config from classpath URL: " + classpathResource, ioException);
        }
    }

    @NonNull
    static JHarmonizerConfig loadFrom(@NonNull File yamlFile) {
        try (InputStream configYaml = Files.newInputStream(yamlFile.toPath())) {
            return loadFrom(configYaml);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load JHarmonizer config from file: " + yamlFile, e);
        }
    }

    @NonNull
    static JHarmonizerFlexibleConfig loadFlexibleFromClasspathResource(@NonNull URL classpathResource) {
        try (InputStream inputStream = classpathResource.openStream()) {
            return JHarmonizerConfigLoader.loadFlexibleFrom(inputStream);
        } catch (IOException ioException) {
            throw new UncheckedIOException(
                    "Failed to load flexible JHarmonizer config from classpath URL: " + classpathResource, ioException);
        }
    }

    @NonNull
    static JHarmonizerFlexibleConfig loadFlexibleFrom(@NonNull File yamlFile) {
        try (InputStream configYaml = Files.newInputStream(yamlFile.toPath())) {
            return loadFlexibleFrom(configYaml);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load flexible JHarmonizer config from file: " + yamlFile, e);
        }
    }

    @NonNull
    static JHarmonizerFlexibleConfig loadFlexibleFrom(@NonNull InputStream configInput) throws IOException {
        return YAML_MAPPER.readValue(configInput, JHarmonizerFlexibleConfig.class);
    }
}

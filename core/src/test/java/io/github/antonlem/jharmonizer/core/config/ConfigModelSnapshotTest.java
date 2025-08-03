package io.github.antonlem.jharmonizer.core.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ConfigModelSnapshotTest {

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void configModel_serializationMatchesSnapshot() throws Exception {
        // given
        ConfigRoot config = ConfigLoader.loadFrom(
                new File(getClass().getResource("/default-config.yml").toURI()));

        // when
        String actualJson = mapper.writeValueAsString(config);

        // then
        InputStream expectedJsonStream = getClass().getResourceAsStream("/test-cases/core/config/expected-config.json");
        String expectedJson = new String(expectedJsonStream.readAllBytes(), StandardCharsets.UTF_8);

        assertThat(actualJson).isEqualToNormalizingNewlines(expectedJson);
    }

    @Test
    @Disabled("It's not a test but utility. Use it to regenerate expected-config.json")
    void regenerateSnapshot() throws Exception {
        ConfigRoot config = ConfigLoader.loadFrom(
                new File(getClass().getResource("/default-config.yml").toURI()));

        String newSnapshot = mapper.writeValueAsString(config);
        Files.writeString(
                Path.of("src/test/resources/test-cases/config/expected-config.json"),
                newSnapshot,
                StandardCharsets.UTF_8);
    }
}

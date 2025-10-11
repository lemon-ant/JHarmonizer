package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

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

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void configModel_serializationMatchesSnapshot() throws Exception {
        // given
        File defaultConfigFile =
                new File(getClass().getResource("/default-config.yml").toURI());
        JHarmonizerConfig JHarmonizerConfig = JHarmonizerConfigLoader.loadFrom(defaultConfigFile);

        // when
        String actualJson = MAPPER.writeValueAsString(JHarmonizerConfig);

        // then
        try (InputStream expectedJsonStream =
                getClass().getResourceAsStream("/test-cases/core/config/expected-config.json")) {

            // Defensive: make missing snapshot an explicit test failure with instructions.
            assertThat(expectedJsonStream)
                    .as("Missing snapshot file: /test-cases/core/config/expected-config.json")
                    .isNotNull();

            String expectedJson = new String(expectedJsonStream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(actualJson)
                    .as(
                            """
                        Config snapshot mismatch.

                        If you intentionally changed the default configuration (default-config.yml) or the config model,
                        you must refresh the JSON snapshot:

                          1) Run ConfigModelSnapshotTest.regenerateSnapshot() — it will rewrite:
                             src/test/resources/test-cases/core/config/expected-config.json
                          2) Re-run this test.

                        IMPORTANT BEFORE COMMIT:
                          • Verify that the diff in expected-config.json EXACTLY reflects your YAML/model changes.
                          • Make sure nothing accidental was lost or reordered.
                          • Commit both the YAML change and the updated snapshot together.
                        """)
                    .isEqualToNormalizingNewlines(expectedJson);
        }
    }

    @Test
    @Disabled("Utility only. Temporarily enable and run to regenerate expected-config.json")
    void regenerateSnapshot() throws Exception {
        // Load current default config
        File defaultConfigFile =
                new File(getClass().getResource("/default-config.yml").toURI());
        JHarmonizerConfig JHarmonizerConfig = JHarmonizerConfigLoader.loadFrom(defaultConfigFile);

        // Serialize and overwrite snapshot
        String newSnapshot = MAPPER.writeValueAsString(JHarmonizerConfig);
        Path snapshotPath = Path.of("src/test/resources/test-cases/core/config/expected-config.json");
        Files.writeString(snapshotPath, newSnapshot, StandardCharsets.UTF_8);
    }
}

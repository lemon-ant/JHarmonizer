package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigLoader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ConfigModelSnapshotTest {

    private static final String CLASS_PATH_TO_SNAPSHOT =
            "/test-cases/core/config/input/jharmonizer/expected-default-jharmonizer-config.json";
    private static final String FILE_PATH_TO_SNAPSHOT = "src/test/resources" + CLASS_PATH_TO_SNAPSHOT;
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void configModel_serializationMatchesSnapshot() throws Exception {
        // given
        JHarmonizerConfig JHarmonizerConfig = JHarmonizerConfigLoader.loadDefault();

        // when
        String actualJson = MAPPER.writeValueAsString(JHarmonizerConfig);

        // then
        try (InputStream expectedJsonStream = getClass().getResourceAsStream(CLASS_PATH_TO_SNAPSHOT)) {

            // Defensive: make missing snapshot an explicit test failure with instructions.
            assertThat(expectedJsonStream)
                    .as("Missing snapshot file: " + CLASS_PATH_TO_SNAPSHOT)
                    .isNotNull();

            String expectedJson = new String(expectedJsonStream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(actualJson)
                    .as(
                            """
                        Config snapshot mismatch.

                        If you intentionally changed the default configuration (default-config.yml) or the config model,
                        you must refresh the JSON snapshot:

                          1) Run ConfigModelSnapshotTest.regenerateSnapshot() — it will rewrite:
                             %s
                          2) Re-run this test.

                        IMPORTANT BEFORE COMMIT:
                          • Verify that the diff in expected-default-jharmonizer-config.json EXACTLY reflects your YAML/model changes.
                          • Make sure nothing accidental was lost or reordered.
                          • Commit both the YAML change and the updated snapshot together.
                        """,
                            FILE_PATH_TO_SNAPSHOT)
                    .isEqualToNormalizingNewlines(expectedJson);
        }
    }

    @Test
    @Disabled("Utility only. Run to regenerate expected-default-jharmonizer-config.json")
    void regenerateSnapshot() throws Exception {
        // Load current default config
        JHarmonizerConfig jHarmonizerConfig = JHarmonizerConfigLoader.loadDefault();

        // Serialize and overwrite snapshot
        String newSnapshot = MAPPER.writeValueAsString(jHarmonizerConfig);
        Path snapshotPath = Path.of(FILE_PATH_TO_SNAPSHOT);
        Files.writeString(snapshotPath, newSnapshot, StandardCharsets.UTF_8);
        // quick sanity: snapshot should be readable right away
        assertThat(Files.readString(snapshotPath, StandardCharsets.UTF_8))
                .as("Snapshot file should be readable immediately after write")
                .isEqualTo(newSnapshot);
    }
}

package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigLoaderHelper.DEFAULT_JHARMONIZER_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ConfigModelSnapshotTest {

    private static final String CLASS_PATH_TO_SNAPSHOT =
            "/test-cases/core/config/input/jharmonizer/expected-default-jharmonizer-config.json";
    private static final URL SNAPSHOT_RESOURCE_URL = ConfigModelSnapshotTest.class.getResource(CLASS_PATH_TO_SNAPSHOT);
    private static final String FILE_PATH_TO_SNAPSHOT = "src/test/resources" + CLASS_PATH_TO_SNAPSHOT;
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void serializeDefaultJHarmonizerConfig_whenSerialized_shouldMatchSnapshot() throws Exception {
        // When
        String actualJson = MAPPER.writeValueAsString(DEFAULT_JHARMONIZER_CONFIG);

        // Then
        String expectedJson = TestCaseResourceUtils.readClasspathResourceAsString(SNAPSHOT_RESOURCE_URL);
        assertThat(actualJson).as("""
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
            """, FILE_PATH_TO_SNAPSHOT).isEqualToNormalizingNewlines(expectedJson);
    }

    @Test
    @Disabled("Utility only. Run to regenerate expected-default-jharmonizer-config.json")
    void regenerateSnapshot() throws Exception {
        // Given
        String newSnapshot = MAPPER.writeValueAsString(DEFAULT_JHARMONIZER_CONFIG);
        Path snapshotPath = Path.of(FILE_PATH_TO_SNAPSHOT);

        // When
        Files.writeString(snapshotPath, newSnapshot, StandardCharsets.UTF_8);

        // Then
        assertThat(Files.readString(snapshotPath, StandardCharsets.UTF_8))
                .as("Snapshot file should be readable immediately after write")
                .isEqualTo(newSnapshot);
    }
}

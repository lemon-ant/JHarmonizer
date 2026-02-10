package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigLoaderHelper.DEFAULT_JHARMONIZER_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Snapshot test: default vendor YAML -> UnifiedConfig JSON equals expected snapshot (raw JSON compare).
 * <p>
 * If the test fails after intentional changes, run the disabled generator below to refresh:
 * JHarmonizer2UnifiedConverterSnapshotTest#regenerateSnapshot_whenRun_overwritesSnapshotFile
 */
class JHarmonizer2UnifiedConverterSnapshotTest {

    private static final String CLASS_PATH_TO_SNAPSHOT =
            "/test-cases/core/config/input/jharmonizer/expected-default-unified-config.json";
    private static final URL SNAPSHOT_RESOURCE_URL =
            JHarmonizer2UnifiedConverterSnapshotTest.class.getResource(CLASS_PATH_TO_SNAPSHOT);
    private static final String FILE_PATH_TO_SNAPSHOT = "src/test/resources" + CLASS_PATH_TO_SNAPSHOT;
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    @DisplayName("configUnified_serializationMatchesSnapshot")
    void configUnified_serializationMatchesSnapshot() throws Exception {
        // Given
        JHarmonizerConfig vendorConfig = DEFAULT_JHARMONIZER_CONFIG;
        assertThat(vendorConfig)
                .as("Default vendor config must be loadable via JHarmonizerConfigLoader.loadDefault()")
                .isNotNull();

        // When
        UnifiedConfig unifiedConfig = JHarmonizer2UnifiedConverter.convert2Unified(vendorConfig);
        String actualJson = MAPPER.writeValueAsString(unifiedConfig);

        // Then
        String expectedJson = TestCaseResourceUtils.readClasspathResourceAsString(SNAPSHOT_RESOURCE_URL);
        assertThat(actualJson).as("""
            UnifiedConfig snapshot mismatch.

            If you intentionally changed the default vendor YAML, the converter, or the unified model,
            regenerate the snapshot locally:

              1) Run: UnifiedConfigSnapshotTest#regenerateSnapshot_whenRun_overwritesSnapshotFile
              2) Re-run this test.

            IMPORTANT BEFORE COMMIT:
              • Review the diff in expected-unified.json to ensure it exactly reflects your intended changes.
              • Commit the YAML/model changes together with the updated snapshot.
              • Actual JSON dumped to: %s
            """, FILE_PATH_TO_SNAPSHOT).isEqualToNormalizingNewlines(expectedJson);
    }

    @Test
    @Disabled("Click to (re)generate snapshot after intentional conversion/model changes")
    @DisplayName("regenerateSnapshot_whenRun_overwritesSnapshotFile")
    void regenerateSnapshot_whenRun_overwritesSnapshotFile() throws Exception {
        // Given
        JHarmonizerConfig vendorConfig = DEFAULT_JHARMONIZER_CONFIG;
        assertThat(vendorConfig).as("Default vendor config must be loadable").isNotNull();

        // When
        UnifiedConfig unifiedConfig = JHarmonizer2UnifiedConverter.convert2Unified(vendorConfig);
        String expectedJson = MAPPER.writeValueAsString(unifiedConfig);

        // Then
        Path pathToSnapshot = Path.of(FILE_PATH_TO_SNAPSHOT);
        Files.writeString(pathToSnapshot, expectedJson, StandardCharsets.UTF_8);
        assertThat(Files.readString(pathToSnapshot, StandardCharsets.UTF_8))
                .as("Snapshot file should be readable immediately after write")
                .isEqualTo(expectedJson);
    }
}

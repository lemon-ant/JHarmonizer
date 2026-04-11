package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigLoaderHelper.DEFAULT_JHARMONIZER_CONFIG;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.TEST_CASES_DIR;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ConfigModelSnapshotTest {

    private static final String CLASS_PATH_TO_SNAPSHOT =
            "/" + TEST_CASES_DIR + "/core/config/input/jharmonizer/expected-default-jharmonizer-config.json";
    private static final URL SNAPSHOT_RESOURCE_URL =
            TestCaseResourceUtils.requireClasspathResourceUrl(CLASS_PATH_TO_SNAPSHOT);
    private static final String FILE_PATH_TO_SNAPSHOT = "src/test/resources" + CLASS_PATH_TO_SNAPSHOT;
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final Pattern SELECTOR_BLOCK_PATTERN = Pattern.compile(
            "(?m)^(\\s+)\"(includes|excludes)\"\\s*:\\s*\\[(\\s*\\[[^\\n\\]]*\\](?:,\\s*\\[[^\\n\\]]*\\])*)\\s*\\](,?)$");
    private static final Pattern SELECTOR_LINE_PATTERN = Pattern.compile("\\[[^\\]]*\\]");

    @Test
    void serializeDefaultJHarmonizerConfig_serializedDefaultConfig_matchesSnapshot() throws Exception {
        // When
        String actualJson = formatNestedSelectorLines(MAPPER.writeValueAsString(DEFAULT_JHARMONIZER_CONFIG));

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
        String newSnapshot = formatNestedSelectorLines(MAPPER.writeValueAsString(DEFAULT_JHARMONIZER_CONFIG));
        Path snapshotPath = Path.of(FILE_PATH_TO_SNAPSHOT);

        // When
        Files.writeString(snapshotPath, newSnapshot, StandardCharsets.UTF_8);

        // Then
        assertThat(Files.readString(snapshotPath, StandardCharsets.UTF_8))
                .as("Snapshot file should be readable immediately after write")
                .isEqualTo(newSnapshot);
    }

    @NonNull
    private static String formatNestedSelectorLines(String json) {
        Matcher selectorBlockMatcher = SELECTOR_BLOCK_PATTERN.matcher(json);
        StringBuffer reformattedJson = new StringBuffer();
        while (selectorBlockMatcher.find()) {
            String baseIndent = selectorBlockMatcher.group(1);
            String selectorField = selectorBlockMatcher.group(2);
            String selectorItems = selectorBlockMatcher.group(3);
            String trailingComma = selectorBlockMatcher.group(4);
            String replacement = buildFormattedSelectorBlock(baseIndent, selectorField, selectorItems, trailingComma);
            selectorBlockMatcher.appendReplacement(reformattedJson, Matcher.quoteReplacement(replacement));
        }
        selectorBlockMatcher.appendTail(reformattedJson);
        return reformattedJson.toString();
    }

    @NonNull
    private static String buildFormattedSelectorBlock(
            String baseIndent, String selectorField, String selectorItems, String trailingComma) {
        String itemIndent = baseIndent + "  ";
        List<String> selectorLines = new ArrayList<>();
        Matcher selectorLineMatcher = SELECTOR_LINE_PATTERN.matcher(selectorItems);
        while (selectorLineMatcher.find()) {
            selectorLines.add(selectorLineMatcher.group());
        }
        StringBuilder formattedBlock = new StringBuilder();
        formattedBlock.append(baseIndent).append('"').append(selectorField).append("\" : [\n");
        for (int i = 0; i < selectorLines.size(); i++) {
            formattedBlock.append(itemIndent).append(selectorLines.get(i));
            if (i + 1 < selectorLines.size()) {
                formattedBlock.append(",\n");
            }
        }
        formattedBlock.append('\n').append(baseIndent).append(']').append(trailingComma);
        return formattedBlock.toString();
    }
}

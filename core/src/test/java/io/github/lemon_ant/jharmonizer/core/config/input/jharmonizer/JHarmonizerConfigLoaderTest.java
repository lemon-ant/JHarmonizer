package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.FormatterStyle;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerOrderingRule;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// TODO Refactor
class JHarmonizerConfigLoaderTest {

    @Test
    void loadFrom_emptyFile_throwsException(@TempDir Path tempDir) throws IOException {
        // Given
        File empty = tempDir.resolve("empty.yml").toFile();
        assertThat(empty.createNewFile()).isTrue();
        try (InputStream configYaml = Files.newInputStream(empty.toPath())) {

            // When / Then
            assertThatThrownBy(() -> JHarmonizerConfigLoader.loadFrom(configYaml))
                    .isInstanceOf(MismatchedInputException.class);
        }
    }

    @Test
    void loadFrom_invalidIncludesInTypeMembers_throwsValidationError() throws IOException {
        // Given
        try (InputStream configYaml = TestCaseResourceUtils.openClasspathResourceStream(
                JHarmonizerConfigLoaderTest.class,
                "/test-cases/core/config/input/jharmonizer/invalid-config-duplicate-types.yml")) {

            // When / Then
            assertThatThrownBy(() -> JHarmonizerConfigLoader.loadFrom(configYaml))
                    .isInstanceOf(ValueInstantiationException.class)
                    .hasMessageContaining("Duplicate", "found");
        }
    }

    @Test
    void loadFrom_missingRequiredField_throwsException(@TempDir Path tempDir) throws IOException {
        // Given
        File badFile = tempDir.resolve("bad.yml").toFile();
        try (FileWriter writer = new FileWriter(badFile)) {
            writer.write("top-level-types-ordering:\n main-type-first: true\n"); // Missing required "type-order"
        }
        try (InputStream configYaml = Files.newInputStream(badFile.toPath())) {

            // When / Then
            assertThatThrownBy(() -> JHarmonizerConfigLoader.loadFrom(configYaml))
                    .isInstanceOf(MismatchedInputException.class);
        }
    }

    @Test
    void loadFrom_simpleWorkingConfigFile_doesNotThrow() throws IOException {
        // Given
        try (InputStream configYaml = TestCaseResourceUtils.openClasspathResourceStream(
                JHarmonizerConfigLoaderTest.class,
                "/test-cases/core/config/input/jharmonizer/simplest-working-config.yml")) {

            // When / Then
            assertThatCode(() -> JHarmonizerConfigLoader.loadFrom(configYaml)).doesNotThrowAnyException();
        }
    }

    @Test
    void loadFrom_validDefaultConfig_returnsParsedConfigRoot() {
        // When
        JHarmonizerConfig jharmonizerConfig = JHarmonizerConfigLoader.loadDefault();

        // Then
        assertThat(jharmonizerConfig).isNotNull();
        JHarmonizerTopLevelTypesOrdering topLevelTypesOrdering = jharmonizerConfig.getTopLevelTypesOrdering();
        assertThat(topLevelTypesOrdering).isNotNull();
        assertThat(topLevelTypesOrdering.isMainTypeFirst()).isTrue();
        assertThat(topLevelTypesOrdering.getTopLevelTypeSelectors()).isNotEmpty();
        topLevelTypesOrdering.getTopLevelTypeSelectors().forEach(entry -> assertThat(entry.getTypeKinds())
                .isNotEmpty());
        assertThat(topLevelTypesOrdering.getOrderingRules())
                .containsExactly(JHarmonizerOrderingRule.VISIBILITY_ASC, JHarmonizerOrderingRule.ALPHA);
        assertThat(jharmonizerConfig.getFormatting().isFixImports()).isTrue();
        assertThat(jharmonizerConfig.getFormatting().getFormatterStyle()).isEqualTo(FormatterStyle.PALANTIR);
    }
}

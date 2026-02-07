package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.FormatterStyle;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerSortKey;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerTopLevelTypesOrdering;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JHarmonizerConfigLoaderTest {

    @Test
    void loadFrom_emptyFile_throwsException(@TempDir Path tempDir) throws IOException {
        // Given
        File empty = tempDir.resolve("empty.yml").toFile();
        assertThat(empty.createNewFile()).isTrue();
        try (InputStream configYaml = Files.newInputStream(empty.toPath())) {

            // When/Then
            assertThatThrownBy(() -> JHarmonizerConfigLoader.loadFrom(configYaml))
                    .isInstanceOf(MismatchedInputException.class);
        }
    }

    @Test
    void loadFrom_invalidIncludesInTypeMembers_throwsValidationError() {
        // Given
        InputStream config = Objects.requireNonNull(getClass()
                .getResourceAsStream("/test-cases/core/config/input/jharmonizer/invalid-config-duplicate-types.yml"));

        // When / Then
        assertThatThrownBy(() -> JHarmonizerConfigLoader.loadFrom(config))
                .isInstanceOf(ValueInstantiationException.class)
                .hasMessageContaining("Duplicate", "found"); // уточнение, если проверка сообщает контекст
    }

    @Test
    void loadFrom_missingRequiredField_throwsException(@TempDir Path tempDir) throws IOException {
        // Given
        File badFile = tempDir.resolve("bad.yml").toFile();
        try (FileWriter writer = new FileWriter(badFile)) {
            writer.write("top-level-types-ordering:\n main-type-first: true\n"); // type-order отсутствует
        }
        try (InputStream configYaml = Files.newInputStream(badFile.toPath())) {

            // When/Then
            assertThatThrownBy(() -> JHarmonizerConfigLoader.loadFrom(configYaml))
                    .isInstanceOf(MismatchedInputException.class);
        }
    }

    @Test
    void loadFrom_simpleWorkingConfigFile_doesNotThrow() throws Exception {
        // Given
        try (InputStream stream = getClass()
                .getResourceAsStream("/test-cases/core/config/input/jharmonizer/simplest-working-config.yml")) {

            // When / Then
            assertThatCode(() -> {
                        Assertions.assertNotNull(stream);
                        JHarmonizerConfigLoader.loadFrom(stream);
                    })
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void loadFrom_validDefaultConfig_returnsParsedConfigRoot() throws IOException {
        // When
        JHarmonizerConfig JHarmonizerConfig = JHarmonizerConfigLoader.loadDefault();

        // Then
        assertThat(JHarmonizerConfig).isNotNull();
        JHarmonizerTopLevelTypesOrdering topLevelTypesOrdering = JHarmonizerConfig.getTopLevelTypesOrdering();
        assertThat(topLevelTypesOrdering).isNotNull();
        assertThat(topLevelTypesOrdering.isMainTypeFirst()).isTrue();
        assertThat(topLevelTypesOrdering.getTopLevelTypeSelectors()).isNotEmpty();
        topLevelTypesOrdering.getTopLevelTypeSelectors().forEach(entry -> assertThat(entry.getTypeKinds())
                .isNotEmpty());
        assertThat(topLevelTypesOrdering.getSortKeys())
                .containsExactly(JHarmonizerSortKey.VISIBILITY_ASC, JHarmonizerSortKey.ALPHA);
        assertThat(JHarmonizerConfig.getFormatting().isFixImports()).isTrue();
        assertThat(JHarmonizerConfig.getFormatting().getFormatterStyle()).isEqualTo(FormatterStyle.PALANTIR);
    }
}

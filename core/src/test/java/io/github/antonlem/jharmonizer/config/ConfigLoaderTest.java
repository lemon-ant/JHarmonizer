package io.github.antonlem.jharmonizer.config;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {

    @Test
    void loadFrom_validDefaultConfig_returnsParsedConfigRoot() throws IOException {
        // given
        InputStream stream = getClass().getResourceAsStream("/default-config.yml");

        // when
        ConfigRoot configRoot = ConfigLoader.loadFrom(stream);

        // then
        assertThat(configRoot).isNotNull();
        TopLevelTypesOrdering topLevelTypesOrdering = configRoot.getTopLevelTypesOrdering();
        assertThat(topLevelTypesOrdering).isNotNull();
        assertThat(topLevelTypesOrdering.isMainTypeFirst()).isTrue();
        assertThat(topLevelTypesOrdering.getTypeGroups()).isNotEmpty();
        topLevelTypesOrdering.getTypeGroups().forEach(entry -> assertThat(entry.getTypeKinds())
                .isNotEmpty());
        assertThat(topLevelTypesOrdering.getSortKeys()).containsExactly(SortKey.VISIBILITY_DESC, SortKey.ALPHA);
        assertThat(configRoot.isFixImports()).isTrue();
        assertThat(configRoot.getFormatterStyle()).isEqualTo(FormatterStyle.PALANTIR);
    }

    @Test
    void loadFrom_missingRequiredField_throwsException(@TempDir Path tempDir) throws IOException {
        // given
        File badFile = tempDir.resolve("bad.yml").toFile();
        try (FileWriter writer = new FileWriter(badFile)) {
            writer.write("top-level-types-ordering:\n  main-type-first: true\n"); // type-order отсутствует
        }

        // when/then
        assertThatThrownBy(() -> ConfigLoader.loadFrom(badFile)).isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void loadFrom_emptyFile_throwsException(@TempDir Path tempDir) throws IOException {
        // given
        File empty = tempDir.resolve("empty.yml").toFile();
        assertThat(empty.createNewFile()).isTrue();

        // when/then
        assertThatThrownBy(() -> ConfigLoader.loadFrom(empty)).isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void loadFrom_simpleWorkingConfigFile_doesNotThrow() {
        // given
        InputStream stream = getClass().getResourceAsStream("/test-cases/config/simplest-working-config.yml");

        // when / then
        assertThatCode(() -> ConfigLoader.loadFrom(stream)).doesNotThrowAnyException();
    }

    @Test
    void loadFrom_invalidIncludesInTypeMembers_throwsValidationError() {
        // given
        InputStream config = Objects.requireNonNull(
                getClass().getResourceAsStream("/test-cases/config/invalid-config-duplicate-types.yml"));

        // when / then
        assertThatThrownBy(() -> ConfigLoader.loadFrom(config))
                .isInstanceOf(ValueInstantiationException.class)
                .hasMessageContaining("Duplicate", "found"); // уточнение, если проверка сообщает контекст
    }
}

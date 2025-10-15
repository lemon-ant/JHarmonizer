package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.FormatterStyle;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.SortKey;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.TopLevelTypesOrdering;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JHarmonizerConfigLoaderTest {

    @Test
    void loadFrom_emptyFile_throwsException(@TempDir Path tempDir) throws IOException {
        // given
        File empty = tempDir.resolve("empty.yml").toFile();
        assertThat(empty.createNewFile()).isTrue();

        // when/then
        assertThatThrownBy(() -> JHarmonizerConfigLoader.loadFrom(empty)).isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void loadFrom_invalidIncludesInTypeMembers_throwsValidationError() {
        // given
        InputStream config = Objects.requireNonNull(
                getClass().getResourceAsStream("/test-cases/core/config/invalid-config-duplicate-types.yml"));

        // when / then
        assertThatThrownBy(() -> JHarmonizerConfigLoader.loadFrom(config))
                .isInstanceOf(ValueInstantiationException.class)
                .hasMessageContaining("Duplicate", "found"); // уточнение, если проверка сообщает контекст
    }

    @Test
    void loadFrom_missingRequiredField_throwsException(@TempDir Path tempDir) throws IOException {
        // given
        File badFile = tempDir.resolve("bad.yml").toFile();
        try (FileWriter writer = new FileWriter(badFile)) {
            writer.write("top-level-types-ordering:\n main-type-first: true\n"); // type-order отсутствует
        }

        // when/then
        assertThatThrownBy(() -> JHarmonizerConfigLoader.loadFrom(badFile))
                .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void loadFrom_simpleWorkingConfigFile_doesNotThrow() {
        // given
        InputStream stream = getClass().getResourceAsStream("/test-cases/core/config/simplest-working-config.yml");

        // when / then
        assertThatCode(() -> JHarmonizerConfigLoader.loadFrom(stream)).doesNotThrowAnyException();
    }

    @Test
    void loadFrom_validDefaultConfig_returnsParsedConfigRoot() throws IOException {
        // given
        InputStream stream = getClass().getResourceAsStream("/default-config.yml");

        // when
        JHarmonizerConfig JHarmonizerConfig = JHarmonizerConfigLoader.loadFrom(stream);

        // then
        assertThat(JHarmonizerConfig).isNotNull();
        TopLevelTypesOrdering topLevelTypesOrdering = JHarmonizerConfig.getTopLevelTypesOrdering();
        assertThat(topLevelTypesOrdering).isNotNull();
        assertThat(topLevelTypesOrdering.isMainTypeFirst()).isTrue();
        assertThat(topLevelTypesOrdering.getTypeGroups()).isNotEmpty();
        topLevelTypesOrdering.getTypeGroups().forEach(entry -> assertThat(entry.getTypeKinds())
                .isNotEmpty());
        assertThat(topLevelTypesOrdering.getSortKeys()).containsExactly(SortKey.VISIBILITY_DESC, SortKey.ALPHA);
        assertThat(JHarmonizerConfig.isFixImports()).isTrue();
        assertThat(JHarmonizerConfig.getFormatterStyle()).isEqualTo(FormatterStyle.PALANTIR);
    }
}

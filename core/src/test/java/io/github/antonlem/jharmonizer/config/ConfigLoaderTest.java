package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class ConfigLoaderTest {

    @Test
    void loadFrom_validDefaultConfig_returnsParsedConfigRoot() throws IOException {
        // given
        File yaml = new File("src/main/resources/default-config.yml");

        // when
        ConfigRoot root = ConfigLoader.loadFrom(yaml);

        // then
        assertThat(root).isNotNull();
        assertThat(root.getJavaFile()).isNotNull();
        assertThat(root.getJavaFile().isMainTypeFirst()).isTrue();
        assertThat(root.getJavaFile().getTypeOrderEntries()).isNotEmpty();
        root.getJavaFile().getTypeOrderEntries().forEach(entry ->
            assertThat(entry.getKinds()).isNotEmpty()
        );
    }

    @Test
    void loadFrom_missingRequiredField_throwsException(@TempDir Path tempDir) throws IOException {
        // given
        File badFile = tempDir.resolve("bad.yml").toFile();
        try (FileWriter writer = new FileWriter(badFile)) {
            writer.write("java-file:\n  main-type-first: true\n"); // type-order отсутствует
        }

        // when/then
        assertThatThrownBy(() -> ConfigLoader.loadFrom(badFile))
            .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void loadFrom_emptyFile_throwsException(@TempDir Path tempDir) throws IOException {
        // given
        File empty = tempDir.resolve("empty.yml").toFile();
        assertThat(empty.createNewFile()).isTrue();

        // when/then
        assertThatThrownBy(() -> ConfigLoader.loadFrom(empty))
            .isInstanceOf(MismatchedInputException.class);
    }
}

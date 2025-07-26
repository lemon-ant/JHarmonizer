package io.github.antonlem.jharmonizer.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {

    @Test
    @Disabled
    void loadFrom_validDefaultConfig_returnsParsedJavaFileEntry() throws IOException {
        File yaml = new File("src/main/resources/default-config.yml");
        JavaFileEntry config = ConfigLoader.loadFrom(yaml);

        assertNotNull(config);
        assertTrue(config.isMainTypeFirst());
        assertNotNull(config.getTypeOrderEntries());
        assertFalse(config.getTypeOrderEntries().isEmpty());

        config.getTypeOrderEntries().forEach(entry -> assertNotNull(entry.getKinds()));
    }

    @Test
    void loadFrom_missingRequiredField_throwsException(@TempDir Path tempDir) throws IOException {
        File invalidFile = tempDir.resolve("invalid.yml").toFile();
        try (FileWriter writer = new FileWriter(invalidFile)) {
            writer.write("main-type-first: true"); // no "type-order"
        }

        assertThrows(Exception.class, () -> ConfigLoader.loadFrom(invalidFile));
    }

    @Test
    void loadFrom_emptyFile_throwsException(@TempDir Path tempDir) throws IOException {
        File emptyFile = tempDir.resolve("empty.yml").toFile();
        assertTrue(emptyFile.createNewFile());

        assertThrows(Exception.class, () -> ConfigLoader.loadFrom(emptyFile));
    }

    @Test
    void loadFrom_malformedYaml_throwsException(@TempDir Path tempDir) throws IOException {
        File badFile = tempDir.resolve("bad.yml").toFile();
        try (FileWriter writer = new FileWriter(badFile)) {
            writer.write("this: is: not: valid: yaml:");
        }

        assertThrows(Exception.class, () -> ConfigLoader.loadFrom(badFile));
    }
}

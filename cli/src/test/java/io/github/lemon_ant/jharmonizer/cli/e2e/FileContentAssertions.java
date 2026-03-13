package io.github.lemon_ant.jharmonizer.cli.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;

@UtilityClass
class FileContentAssertions {

    static void assertFileChanged(Path expectedProjectDirectory, Path actualProjectDirectory, String relativePath) {
        assertThat(readFile(actualProjectDirectory.resolve(relativePath)))
                .as("Expected file to be modified: %s", relativePath)
                .isNotEqualTo(readFile(expectedProjectDirectory.resolve(relativePath)));
    }

    static void assertFileUnchanged(Path expectedProjectDirectory, Path actualProjectDirectory, String relativePath) {
        assertThat(readFile(actualProjectDirectory.resolve(relativePath)))
                .as("Expected file to remain unchanged: %s", relativePath)
                .isEqualTo(readFile(expectedProjectDirectory.resolve(relativePath)));
    }

    static String readFile(Path filePath) {
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read test file: " + filePath, exception);
        }
    }
}

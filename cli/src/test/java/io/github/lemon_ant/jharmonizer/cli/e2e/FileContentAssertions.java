package io.github.lemon_ant.jharmonizer.cli.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.experimental.UtilityClass;

@UtilityClass
class FileContentAssertions {

    static void assertFileChanged(
            DirectorySnapshot beforeSnapshot, DirectorySnapshot afterSnapshot, String relativePath) {
        assertThat(afterSnapshot.content(relativePath))
                .as("Expected file to be modified: %s", relativePath)
                .isNotEqualTo(beforeSnapshot.content(relativePath));
    }

    static void assertFileUnchanged(
            DirectorySnapshot beforeSnapshot, DirectorySnapshot afterSnapshot, String relativePath) {
        assertThat(afterSnapshot.content(relativePath))
                .as("Expected file to remain unchanged: %s", relativePath)
                .isEqualTo(beforeSnapshot.content(relativePath));
    }
}

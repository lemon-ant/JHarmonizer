package io.github.lemon_ant.jharmonizer.cli.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.cli.command.JHarmonizerCliApplication;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class JHarmonizerCliE2ETest {

    private static final String INPUT_RESOURCE = "/test-cases/cli/e2e/CliE2ESample.java";

    @TempDir
    Path temporaryDirectory;

    @Test
    void checkFastCommand_unformattedFile_returnsExitCode3() throws IOException, URISyntaxException {
        // Given
        copyResourceToTempDir(INPUT_RESOURCE, temporaryDirectory);

        // When
        int exitCode = new CommandLine(JHarmonizerCliApplication.class)
                .execute("check-fast", "--base-dir", temporaryDirectory.toString(), "--include", "*.java");

        // Then
        assertThat(exitCode).isEqualTo(3);
    }

    @Test
    void restructureCommand_unformattedFile_reformatsFileAndReturnsExitCode0() throws IOException, URISyntaxException {
        // Given
        Path sourceFile = copyResourceToTempDir(INPUT_RESOURCE, temporaryDirectory);
        String originalContent = Files.readString(sourceFile, StandardCharsets.UTF_8);

        // When
        int exitCode = new CommandLine(JHarmonizerCliApplication.class)
                .execute("restructure", "--base-dir", temporaryDirectory.toString(), "--include", "*.java");

        // Then
        assertThat(exitCode).isZero();
        assertThat(Files.readString(sourceFile, StandardCharsets.UTF_8)).isNotEqualTo(originalContent);
    }

    @Test
    void checkFastCommand_restructuredFile_returnsExitCode0() throws IOException, URISyntaxException {
        // Given
        copyResourceToTempDir(INPUT_RESOURCE, temporaryDirectory);
        new CommandLine(JHarmonizerCliApplication.class)
                .execute("restructure", "--base-dir", temporaryDirectory.toString(), "--include", "*.java");

        // When
        int exitCode = new CommandLine(JHarmonizerCliApplication.class)
                .execute("check-fast", "--base-dir", temporaryDirectory.toString(), "--include", "*.java");

        // Then
        assertThat(exitCode).isZero();
    }

    private Path copyResourceToTempDir(String resourcePath, Path targetDir) throws IOException, URISyntaxException {
        URL resource = getClass().getResource(resourcePath);
        assertThat(resource).as("Test resource not found: %s", resourcePath).isNotNull();
        Path source = Path.of(resource.toURI());
        Path target = targetDir.resolve(source.getFileName());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }
}

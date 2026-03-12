package io.github.lemon_ant.jharmonizer.cli.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
class TemporaryProjectCopier {

    static Path copyProject(@NonNull String resourceRoot, @NonNull Path targetDirectory)
            throws IOException, URISyntaxException {
        URL resource = TemporaryProjectCopier.class.getClassLoader().getResource(resourceRoot);
        assertThat(resource)
                .as("Test resource directory not found: %s", resourceRoot)
                .isNotNull();

        Path sourceDirectory = Path.of(resource.toURI());
        Files.createDirectories(targetDirectory);
        try (var paths = Files.walk(sourceDirectory)) {
            paths.forEach(sourcePath -> copyPath(sourceDirectory, sourcePath, targetDirectory));
        }
        return targetDirectory;
    }

    private static void copyPath(Path sourceDirectory, Path sourcePath, Path targetDirectory) {
        try {
            Path relativePath = sourceDirectory.relativize(sourcePath);
            Path targetPath = targetDirectory.resolve(relativePath);
            if (Files.isDirectory(sourcePath)) {
                Files.createDirectories(targetPath);
                return;
            }
            Files.createDirectories(targetPath.getParent());
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to copy test project resource: " + sourcePath, exception);
        }
    }
}

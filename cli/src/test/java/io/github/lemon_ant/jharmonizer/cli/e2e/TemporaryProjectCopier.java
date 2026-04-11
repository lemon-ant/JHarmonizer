package io.github.lemon_ant.jharmonizer.cli.e2e;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
class TemporaryProjectCopier {

    static Path copyProject(@NonNull String resourceRoot, @NonNull Path targetDirectory) throws IOException {
        Path srcDirectory = locateOriginalTestResource(resourceRoot);
        Files.createDirectories(targetDirectory);
        copyDirectoryRecursively(srcDirectory, targetDirectory);
        return targetDirectory;
    }

    static Path locateOriginalTestResource(@NonNull String resourceRoot) {
        URL resource = TemporaryProjectCopier.class.getClassLoader().getResource(resourceRoot);
        if (resource == null) {
            throw new IllegalStateException("Test resource directory not found: " + resourceRoot);
        }
        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Failed to locate original test resource: " + resourceRoot, exception);
        }
    }

    private static void copyDirectoryRecursively(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(srcPath -> {
                Path destPath = target.resolve(source.relativize(srcPath));
                try {
                    if (Files.isDirectory(srcPath)) {
                        Files.createDirectories(destPath);
                    } else {
                        Files.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        }
    }
}

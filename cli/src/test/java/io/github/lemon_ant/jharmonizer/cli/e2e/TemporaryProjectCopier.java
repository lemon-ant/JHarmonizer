package io.github.lemon_ant.jharmonizer.cli.e2e;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;

@UtilityClass
class TemporaryProjectCopier {

    static Path copyProject(@NonNull String resourceRoot, @NonNull Path targetDirectory)
            throws IOException, URISyntaxException {
        Path sourceDirectory = locateProject(resourceRoot);
        Files.createDirectories(targetDirectory);
        FileUtils.copyDirectory(sourceDirectory.toFile(), targetDirectory.toFile());
        return targetDirectory;
    }

    static Path locateProject(@NonNull String resourceRoot) throws URISyntaxException {
        URL resource = TemporaryProjectCopier.class.getClassLoader().getResource(resourceRoot);
        if (resource == null) {
            throw new IllegalStateException("Test resource directory not found: " + resourceRoot);
        }
        return Path.of(resource.toURI());
    }
}

package io.github.lemon_ant.jharmonizer.core.e2e;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
class E2EFileUtils {

    static void requireRegularFile(@NonNull Path sourceFilePath, @NonNull String messagePrefix) {
        if (!Files.isRegularFile(sourceFilePath)) {
            throw new IllegalArgumentException(messagePrefix + sourceFilePath);
        }
    }

    static URL toUrl(@NonNull Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Cannot convert path to URL: " + path, exception);
        }
    }
}

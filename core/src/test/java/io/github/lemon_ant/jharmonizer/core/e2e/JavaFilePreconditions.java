package io.github.lemon_ant.jharmonizer.core.e2e;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
class JavaFilePreconditions {

    static void requireRegularFile(@NonNull Path sourceFilePath, @NonNull String messagePrefix) {
        if (!Files.isRegularFile(sourceFilePath)) {
            throw new IllegalArgumentException(messagePrefix + sourceFilePath);
        }
    }
}

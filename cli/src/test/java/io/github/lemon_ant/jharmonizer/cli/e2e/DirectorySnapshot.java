package io.github.lemon_ant.jharmonizer.cli.e2e;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

record DirectorySnapshot(Map<String, String> fileContents) {

    static DirectorySnapshot capture(Path rootDirectory) throws IOException {
        Map<String, String> contents = new LinkedHashMap<>();
        try (var paths = Files.walk(rootDirectory)) {
            paths.filter(Files::isRegularFile)
                    .sorted()
                    .forEach(path -> contents.put(
                            rootDirectory.relativize(path).toString().replace('\\', '/'), readString(path)));
        }
        return new DirectorySnapshot(Map.copyOf(contents));
    }

    String content(String relativePath) {
        return fileContents.get(relativePath);
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read file for snapshot: " + path, exception);
        }
    }
}

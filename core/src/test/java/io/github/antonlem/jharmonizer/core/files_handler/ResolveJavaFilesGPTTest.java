package io.github.antonlem.jharmonizer.core.files_handler;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.antonlem.jharmonizer.core.files_handler.SourceFilesHandler.resolveJavaFiles;
import static org.assertj.core.api.Assertions.assertThat;

class ResolveJavaFilesGPTTest {

    @TempDir
    Path tempDir;

    Path write(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content);
    }

    @BeforeEach
    void setupTestFiles() throws IOException {
        write("src/main/java/com/example/App.java", "class App {}");
        write("src/main/java/com/example/Helper.java", "class Helper {}");
        write("src/main/resources/config.yaml", "key: value");
        write("src/test/java/com/example/AppTest.java", "class AppTest {}");
        write("build/generated/Generated.java", "class Gen {}");
        write("README.md", "# Readme");
        write("scripts/gen.sh", "#!/bin/bash");
        write("nested/absolute/Deep.java", "class Deep {}");
        write("nested/absolute/ignore.txt", "no");
    }

    @Test
    void includesAllJavaFiles() {
        List<String> includes = List.of(tempGlob("**/*.java"));
        List<String> excludes = List.of();

        Set<String> result = call(includes, excludes);
        assertThat(result).containsExactlyInAnyOrder(
            "src/main/java/com/example/App.java",
            "src/main/java/com/example/Helper.java",
            "src/test/java/com/example/AppTest.java",
            "build/generated/Generated.java",
            "nested/absolute/Deep.java"
        );
    }

    @Test
    void excludesTestAndGenerated() {
        List<String> includes = List.of(tempGlob("**/*.java"));
        List<String> excludes = List.of("**/test/**", "**/generated/**");

        Set<String> result = call(includes, excludes);
        assertThat(result).containsExactlyInAnyOrder(
            "src/main/java/com/example/App.java",
            "src/main/java/com/example/Helper.java",
            "nested/absolute/Deep.java"
        );
    }

    @Test
    void onlyAbsoluteGlob() {
        Path deep = tempDir.resolve("nested/absolute/Deep.java");
        String absGlob = deep.getParent().toAbsolutePath().toString().replace("\\", "/") + "/**/*.java";

        Set<String> result = call(List.of(absGlob), List.of());
        assertThat(result).containsExactly("nested/absolute/Deep.java");
    }

    @Test
    void excludesEverything() {
        List<String> includes = List.of(tempGlob("**/*.java"));
        List<String> excludes = List.of("**/*.java");

        Set<String> result = call(includes, excludes);
        assertThat(result).isEmpty();
    }

    @Test
    void noJavaFilesMatched() {
        List<String> includes = List.of(tempGlob("**/*.kt"));
        List<String> excludes = List.of();

        Set<String> result = call(includes, excludes);
        assertThat(result).isEmpty();
    }

    private Set<String> call(List<String> includeGlobs, List<String> excludeGlobs) {
        try (Stream<Path> stream = resolveJavaFiles(includeGlobs, excludeGlobs)) {
            return stream
                .map(path -> tempDir.relativize(path.normalize()))
                .map(Path::toString)
                .collect(Collectors.toSet());
        }
    }

    private String tempGlob(String relativeGlob) {
        return tempDir.resolve(relativeGlob.replace("/", FileSystems.getDefault().getSeparator()))
            .toString()
            .replace("\\", "/");
    }
}

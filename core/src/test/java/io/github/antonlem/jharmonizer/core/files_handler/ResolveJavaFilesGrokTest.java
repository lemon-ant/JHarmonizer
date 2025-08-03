package io.github.antonlem.jharmonizer.core.files_handler;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static io.github.antonlem.jharmonizer.core.files_handler.SourceFilesHandler.resolveJavaFiles;
import static org.assertj.core.api.Assertions.*;
import static java.util.Collections.*;

public class ResolveJavaFilesGrokTest {

    private static final String JAVA_FILE = "Test.java";
    private static final String NON_JAVA_FILE = "Test.txt";

    @TempDir
    Path tempRoot;

    @BeforeEach
    void setup() throws IOException {
        Path src = tempRoot.resolve("src/main/java");
        Files.createDirectories(src);
        Files.createFile(src.resolve(JAVA_FILE));
        Files.createFile(src.resolve(NON_JAVA_FILE));

        Path deep = tempRoot.resolve("deep/level1/level2");
        Files.createDirectories(deep);
        Files.createFile(deep.resolve(JAVA_FILE));

        Path target = tempRoot.resolve("target");
        Files.createDirectories(target);
        Files.createFile(target.resolve(JAVA_FILE));

        Files.createFile(tempRoot.resolve("single.java"));
    }

    @Test
    void resolveJavaFiles_emptyIncludes_returnsEmptyStream() {
        Collection<String> includes = emptyList();
        Collection<String> excludes = emptyList();
        Stream<Path> result = resolveJavaFiles(includes, excludes);
        assertThat(result).isEmpty();
    }

    @Test
    void resolveJavaFiles_nullIncludes_throwsNullPointerException() {
        assertThatThrownBy(() -> resolveJavaFiles(null, emptyList()))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void resolveJavaFiles_relativeGlob_findsFilesInCwd() {
        String relGlobAsAbs = tempRoot.toString() + "/src/**.java";
        Collection<String> includes = singleton(relGlobAsAbs);
        Collection<String> excludes = emptyList();
        Stream<Path> result = resolveJavaFiles(includes, excludes);
        assertThat(result).hasSize(1)
            .containsExactly(tempRoot.resolve("src/main/java/" + JAVA_FILE).normalize().toAbsolutePath());
    }

    @Test
    void resolveJavaFiles_absoluteGlob_findsFilesFromAbsPath() {
        String absGlob = tempRoot.toString() + "/src/**.java";
        Collection<String> includes = singleton(absGlob);
        Collection<String> excludes = emptyList();
        Stream<Path> result = resolveJavaFiles(includes, excludes);
        assertThat(result).hasSize(1)
            .containsExactly(tempRoot.resolve("src/main/java/" + JAVA_FILE).normalize().toAbsolutePath());
    }

    @Test
    void resolveJavaFiles_nonExistentBaseDir_skipsWithWarning() {
        String nonExistentGlob = tempRoot.toString() + "/nonexistent/**.java";
        Collection<String> includes = singleton(nonExistentGlob);
        Collection<String> excludes = emptyList();
        Stream<Path> result = resolveJavaFiles(includes, excludes);
        assertThat(result).isEmpty();
    }

    @Test
    void resolveJavaFiles_overlappingGlobs_removesDuplicates() {
        String glob1 = tempRoot.toString() + "/src/**.java";
        String glob2 = tempRoot.toString() + "/src/main/**.java";
        Collection<String> includes = Arrays.asList(glob1, glob2);
        Collection<String> excludes = emptyList();
        Stream<Path> result = resolveJavaFiles(includes, excludes);
        assertThat(result).hasSize(1);
    }

    @Test
    void resolveJavaFiles_singleFileGlobWithoutWildcard_findsExactFile() {
        String singleFileGlob = tempRoot.resolve("single.java").toString();
        Collection<String> includes = singleton(singleFileGlob);
        Collection<String> excludes = emptyList();
        Stream<Path> result = resolveJavaFiles(includes, excludes);
        assertThat(result).hasSize(1).containsExactly(tempRoot.resolve("single.java").normalize().toAbsolutePath());
    }

    @Test
    void resolveJavaFiles_relativeExclude_filtersOutFiles() {
        String absInclude = tempRoot.toString() + "/**.java";
        Collection<String> includes = singleton(absInclude);
        Collection<String> excludes = singleton("target/**");
        Stream<Path> result = resolveJavaFiles(includes, excludes);
        assertThat(result)
            .doesNotContain(tempRoot.resolve("target/" + JAVA_FILE).normalize().toAbsolutePath())
            .hasSize(3); // src, deep, single
    }

    @Test
    void resolveJavaFiles_absoluteExclude_filtersOutAbsFiles() {
        String absInclude = tempRoot.toString() + "/**.java";
        Collection<String> includes = singleton(absInclude);
        String absExclude = tempRoot.toString() + "/target/**";
        Collection<String> excludes = singleton(absExclude);
        Stream<Path> result = resolveJavaFiles(includes, excludes);
        assertThat(result)
            .doesNotContain(tempRoot.resolve("target/" + JAVA_FILE).normalize().toAbsolutePath())
            .hasSize(3); // src, deep, single
    }

    @Test
    void resolveJavaFiles_windowsStylePaths_handlesBackslashes() {
        String winGlob = (tempRoot.toString() + "/src/main/java/*.java").replace('/', '\\');
        Collection<String> includes = singleton(winGlob);
        Collection<String> excludes = emptyList();
        Stream<Path> result = resolveJavaFiles(includes, excludes);
        assertThat(result).hasSize(1);
    }

    @Test
    void resolveJavaFiles_deepDir_respectsDepthLimit() {
        String deepGlob = tempRoot.toString() + "/deep/**.java";
        Collection<String> includes = singleton(deepGlob);
        Collection<String> excludes = emptyList();
        Stream<Path> result = resolveJavaFiles(includes, excludes);
        assertThat(result).hasSize(1);
    }

    @ParameterizedTest
    @MethodSource("invalidGlobProvider")
    void resolveJavaFiles_invalidGlob_throwsException(String invalidGlob) {
        Collection<String> includes = singleton(invalidGlob);
        Collection<String> excludes = emptyList();
        assertThatThrownBy(() -> resolveJavaFiles(includes, excludes).count())
            .isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<Arguments> invalidGlobProvider() {
        return Stream.of(
            Arguments.of("**.java["),
            Arguments.of("*:bad")
        );
    }

    @Test
    void resolveJavaFiles_noJavaFiles_returnsEmpty() {
        String txtGlob = tempRoot.toString() + "/**.txt";
        Collection<String> includes = singleton(txtGlob);
        Collection<String> excludes = emptyList();
        Stream<Path> result = resolveJavaFiles(includes, excludes);
        assertThat(result).isEmpty();
    }

    @Test
    void resolveJavaFiles_emptyDir_returnsEmpty() throws IOException {
        Path emptyDir = tempRoot.resolve("empty");
        Files.createDirectory(emptyDir);
        String emptyGlob = emptyDir.toString() + "/**.java";
        Collection<String> includes = singleton(emptyGlob);
        Collection<String> excludes = emptyList();
        Stream<Path> result = resolveJavaFiles(includes, excludes);
        assertThat(result).isEmpty();
    }
}

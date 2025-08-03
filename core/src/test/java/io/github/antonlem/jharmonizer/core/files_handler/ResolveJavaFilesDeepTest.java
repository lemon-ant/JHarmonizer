package io.github.antonlem.jharmonizer.core.files_handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.io.IOException;
import java.util.stream.Collectors;

import static io.github.antonlem.jharmonizer.core.files_handler.SourceFilesHandler.resolveJavaFiles;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ResolveJavaFilesDeepTest {

    // Основной метод для проверки всех corner cases
    @ParameterizedTest
    @MethodSource("provideCornerCases")
    void testResolveJavaFilesWithAllCornerCases(
        String testName,
        Map<String, String> fileStructure,
        List<String> includeGlobs,
        List<String> excludeGlobs,
        Set<String> expectedFiles
    ) throws IOException {

        // 1. Создаем временную файловую структуру
        Path tempRoot = createTempFileStructure(fileStructure);

        // 2. Преобразуем пути в абсолютные
        Set<Path> expectedPaths = expectedFiles.stream()
            .map(p -> tempRoot.resolve(p).normalize())
            .collect(Collectors.toSet());

        // 3. Создаем resolver и выполняем метод
        Set<Path> actualPaths = resolveJavaFiles(
                includeGlobs.stream()
                    .map(g -> g.replace("{root}", tempRoot.toString()))
                    .collect(Collectors.toList()),
                excludeGlobs.stream()
                    .map(g -> g.replace("{root}", tempRoot.toString()))
                    .collect(Collectors.toList())
            )
            .collect(Collectors.toSet());

        // 4. Проверяем результаты
        assertEquals(expectedPaths, actualPaths, "Failed test: " + testName);
    }

    // Создание временной файловой структуры
    private Path createTempFileStructure(Map<String, String> structure) throws IOException {
        Path root = Files.createTempDirectory("fileResolverTest");

        for (Map.Entry<String, String> entry : structure.entrySet()) {
            Path path = root.resolve(entry.getKey());
            if (entry.getValue().equals("DIR")) {
                Files.createDirectories(path);
            } else if (entry.getValue().startsWith("SYMLINK:")) {
                Path target = root.resolve(entry.getValue().substring(8));
                Files.createSymbolicLink(path, target);
            } else if (entry.getValue().equals("JAVA_FILE")) {
                Files.createDirectories(path.getParent());
                Files.createFile(path);
            } else {
                Files.createDirectories(path.getParent());
                Files.write(path, entry.getValue().getBytes());
            }
        }

        return root;
    }

    // Источник данных для параметризованного теста
    private static Stream<Arguments> provideCornerCases() {
        return Stream.of(
            // Тест 1: Простое включение
            Arguments.of(
                "Simple include",
                Map.of(
                    "src/main/java/App.java", "JAVA_FILE",
                    "src/test/java/Test.java", "JAVA_FILE"
                ),
                List.of("{root}/src/main/**/*.java"),
                List.of(),
                Set.of("src/main/java/App.java")
            ),

            // Тест 2: Исключение файлов
            Arguments.of(
                "Exclude pattern",
                Map.of(
                    "src/A.java", "JAVA_FILE",
                    "src/B.java", "JAVA_FILE",
                    "src/exclude/C.java", "JAVA_FILE"
                ),
                List.of("{root}/src/**/*.java"),
                List.of("**/exclude/*.java"),
                Set.of("src/A.java", "src/B.java")
            ),

            // Тест 3: Глубокая вложенность (больше 100 уровней)
            Arguments.of(
                "Deep nesting",
                generateDeepStructure(150),
                List.of("{root}/**/*.java"),
                List.of(),
                Set.of("deep/" + "d/".repeat(149) + "Deep.java")
            ),

            // Тест 4: Символические ссылки
            Arguments.of(
                "Symbolic links",
                Map.of(
                    "real/src/Main.java", "JAVA_FILE",
                    "link", "SYMLINK:real"
                ),
                List.of("{root}/link/**/*.java"),
                List.of(),
                Set.of("real/src/Main.java") // Ожидаем оригинальный путь
            ),

            // Тест 5: Циклические ссылки
            Arguments.of(
                "Cyclic links",
                Map.of(
                    "loop", "SYMLINK:loop",
                    "loop/File.java", "JAVA_FILE"
                ),
                List.of("{root}/**/*.java"),
                List.of(),
                Set.of() // Не должно найти файл в цикле
            ),

            // Тест 6: Абсолютные и относительные пути
            Arguments.of(
                "Mixed path types",
                Map.of(
                    "project/src/App.java", "JAVA_FILE",
                    "project/test/Test.java", "JAVA_FILE"
                ),
                List.of(
                    "{root}/project/src/*.java", // Абсолютный
                    "project/test/*.java"        // Относительный
                ),
                List.of(),
                Set.of("project/src/App.java", "project/test/Test.java")
            ),

            // Тест 7: Wildcards и спецсимволы
            Arguments.of(
                "Special characters",
                Map.of(
                    "src/a$b/Main.java", "JAVA_FILE",
                    "src/a?b/Util.java", "JAVA_FILE",
                    "src/[ab]/Test.java", "JAVA_FILE"
                ),
                List.of(
                    "{root}/src/a\\$b/*.java",
                    "{root}/src/a?b/*.java",
                    "{root}/src/\\[ab\\]/*.java"
                ),
                List.of(),
                Set.of("src/a$b/Main.java", "src/a?b/Util.java", "src/[ab]/Test.java")
            ),

            // Тест 8: Граничные случаи
            Arguments.of(
                "Edge cases",
                Map.of(
                    "empty.java", "", // Пустой файл
                    "not_java.txt", "TEXT_FILE",
                    "no_ext", "JAVA_FILE"
                ),
                List.of("{root}/*.java"),
                List.of(),
                Set.of("empty.java") // Только файл с расширением .java
            ),

            // Тест 9: Пустые входные данные
            Arguments.of(
                "Empty inputs",
                Map.of("src/App.java", "JAVA_FILE"),
                List.of(),
                List.of(),
                Set.of()
            ),

            // Тест 10: Несуществующие директории
            Arguments.of(
                "Non-existent directories",
                Map.of(),
                List.of("{root}/missing/**/*.java"),
                List.of(),
                Set.of()
            ),

            // Тест 11: Комплексный сценарий
            Arguments.of(
                "Complex scenario",
                Map.of(
                    "src/main/App.java", "JAVA_FILE",
                    "src/main/Util.java", "JAVA_FILE",
                    "src/test/Test.java", "JAVA_FILE",
                    "lib/Old.java", "JAVA_FILE"
                ),
                List.of(
                    "{root}/src/**/*.java",
                    "{root}/lib/*.java"
                ),
                List.of(
                    "**/test/*.java",
                    "**/Util.java"
                ),
                Set.of("src/main/App.java", "lib/Old.java")
            )
        );
    }

    // Генератор глубокой структуры
    private static Object[] generateDeepStructure(int depth) {
        StringBuilder path = new StringBuilder("deep/");
        for (int i = 0; i < depth; i++) {
            path.append("d/");
        }
        path.append("Deep.java");

        return new Object[] {
            Map.of(path.toString(), "JAVA_FILE"),
            List.of("{root}/deep/**/*.java"),
            List.of(),
            Set.of(path.toString())
        };
    }

    // Дополнительный тест для Windows-специфичных путей
    @Test
    void testWindowsPaths(@TempDir Path tempDir) throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("dos"));

        // Создаем структуру с Windows-стилем путей
        Path winDir = tempDir.resolve("win");
        Files.createDirectories(winDir);

        Path javaFile = winDir.resolve("App.java");
        Files.createFile(javaFile);

        Set<Path> result = resolveJavaFiles(
                List.of(winDir.toString().replace("\\", "\\\\") + "\\\\*.java"),
                List.of()
            )
            .collect(Collectors.toSet());

        assertEquals(Set.of(javaFile.toAbsolutePath().normalize()), result);
    }
}

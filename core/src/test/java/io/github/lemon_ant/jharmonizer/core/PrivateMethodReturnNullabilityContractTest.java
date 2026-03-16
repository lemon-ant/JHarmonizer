package io.github.lemon_ant.jharmonizer.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class PrivateMethodReturnNullabilityContractTest {
    private static final Set<String> JAVA_SOURCE_ROOTS =
            Set.of("core/src/main/java", "core/src/test/java", "cli/src/main/java", "cli/src/test/java");
    private static final Set<String> NON_METHOD_KEYWORDS = Set.of("class", "record", "interface", "enum");
    private static final Set<String> PRIMITIVE_RETURN_TYPES =
            Set.of("void", "boolean", "byte", "short", "int", "long", "float", "double", "char");
    private static final Pattern PRIVATE_METHOD_SIGNATURE_PATTERN = Pattern.compile(
            "^\\s*private\\s+(?:static\\s+)?(?:final\\s+)?(?:synchronized\\s+)?(?:<[^>]+>\\s+)?(?<returnType>[\\w.$<>?, \\[\\]]+)\\s+(?<methodName>\\w+)\\s*\\([^;=]*$");
    private static final Pattern NULLABILITY_ANNOTATION_PATTERN = Pattern.compile("@(?:NonNull|Nullable)\\b");

    @Test
    void privateReferenceReturnMethods_repositorySources_haveExplicitNullabilityAnnotations() throws IOException {
        // Given
        Path repositoryRoot = resolveRepositoryRoot();

        // When
        List<String> missingAnnotations = findMissingReturnNullabilityAnnotations(repositoryRoot);

        // Then
        assertThat(missingAnnotations).isEmpty();
    }

    @NonNull
    private static Path resolveRepositoryRoot() {
        String repositoryRootProperty = System.getProperty("maven.multiModuleProjectDirectory");
        if (repositoryRootProperty != null && !repositoryRootProperty.isBlank()) {
            return Path.of(repositoryRootProperty).toAbsolutePath().normalize();
        }

        Path currentDirectory = Path.of("").toAbsolutePath().normalize();
        for (Path candidate = currentDirectory; candidate != null; candidate = candidate.getParent()) {
            if (Files.exists(candidate.resolve("core/pom.xml")) && Files.exists(candidate.resolve("cli/pom.xml"))) {
                return candidate;
            }
        }

        throw new IllegalStateException("Could not locate the multi-module repository root from " + currentDirectory);
    }

    @NonNull
    private static List<String> findMissingReturnNullabilityAnnotations(Path repositoryRoot) throws IOException {
        List<String> missingAnnotations = new ArrayList<>();
        for (String sourceRoot : JAVA_SOURCE_ROOTS) {
            Path sourceRootPath = repositoryRoot.resolve(sourceRoot);
            if (!Files.exists(sourceRootPath)) {
                continue;
            }
            try (Stream<Path> javaFiles = Files.walk(sourceRootPath)) {
                List<Path> javaFilePaths = javaFiles
                        .filter(path -> path.toString().endsWith(".java"))
                        .toList();
                for (Path javaFilePath : javaFilePaths) {
                    missingAnnotations.addAll(scanJavaSourceFile(repositoryRoot, javaFilePath));
                }
            }
        }
        return missingAnnotations;
    }

    @NonNull
    private static List<String> scanJavaSourceFile(Path repositoryRoot, Path javaFilePath) throws IOException {
        List<String> lines = Files.readAllLines(javaFilePath);
        List<String> violations = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            Matcher privateMethodMatcher = PRIVATE_METHOD_SIGNATURE_PATTERN.matcher(lines.get(lineIndex));
            if (!privateMethodMatcher.matches()) {
                continue;
            }

            String returnType = privateMethodMatcher.group("returnType").trim();
            if (!isReferenceReturnType(returnType)) {
                continue;
            }
            if (hasExplicitReturnNullabilityAnnotation(lines, lineIndex)) {
                continue;
            }

            String methodName = privateMethodMatcher.group("methodName");
            Path relativeJavaFilePath = repositoryRoot.relativize(javaFilePath);
            violations.add(relativeJavaFilePath + ":" + (lineIndex + 1) + " " + methodName);
        }
        return violations;
    }

    private static boolean isReferenceReturnType(String returnType) {
        String normalizedReturnType = returnType.split("<", 2)[0].trim();
        return !PRIMITIVE_RETURN_TYPES.contains(normalizedReturnType)
                && !NON_METHOD_KEYWORDS.contains(normalizedReturnType);
    }

    private static boolean hasExplicitReturnNullabilityAnnotation(List<String> lines, int signatureLineIndex) {
        for (int lineIndex = signatureLineIndex - 1; lineIndex >= 0; lineIndex--) {
            String currentLine = lines.get(lineIndex).trim();
            if (currentLine.isEmpty()) {
                break;
            }
            if (NULLABILITY_ANNOTATION_PATTERN.matcher(currentLine).find()) {
                return true;
            }
            if (currentLine.startsWith("@")
                    || currentLine.startsWith("/**")
                    || currentLine.startsWith("*/")
                    || currentLine.startsWith("*")) {
                continue;
            }
            break;
        }
        return false;
    }
}

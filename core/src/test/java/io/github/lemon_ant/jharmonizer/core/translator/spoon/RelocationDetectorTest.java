package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.indexElementsByOrder;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SrcAstTranslator;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;

class RelocationDetectorTest {

    private static final CompiledConfig DEFAULT_CONFIG = ConfigurationManager.loadDefaultConfig();
    private static final PrinterConfig DEFAULT_PRINTER_CONFIG = new PrinterConfig(
            DEFAULT_CONFIG.getFormatting().isBlankLineAfterTypeHeader(),
            DEFAULT_CONFIG.getFormatting().isBlankLineBeforeComment(),
            DEFAULT_CONFIG.getFormatting().isBlankLineBetweenFields());

    @Test
    void findRelocations_noChanges_returnsEmptyList() {
        // Given
        SrcFile srcFile = createSrcFile(
                "public class Sample {\n    public void a() {}\n\n    public void b() {}\n}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        Map<SourcePosition, Integer> originalOrderIndices = indexElementsByOrder(spoonAstModel.getCompilationUnit());

        // When
        List<MemberRelocation> relocations = findRelocations(originalOrderIndices, spoonAstModel.getCompilationUnit());

        // Then
        assertThat(relocations).isEmpty();
    }

    @Test
    void findRelocations_withEmptyOriginalIndices_returnsEmptyList() {
        // Given
        SrcFile srcFile = createSrcFile("public class Sample {\n    public void a() {}\n}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();

        // When
        List<MemberRelocation> relocations = findRelocations(Map.of(), spoonAstModel.getCompilationUnit());

        // Then
        assertThat(relocations).isEmpty();
    }

    @Test
    void isRelocated_noChanges_returnsFalse() {
        // Given
        SrcFile srcFile = createSrcFile(
                "public class Sample {\n    public void a() {}\n\n    public void b() {}\n}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        Map<SourcePosition, Integer> originalOrderIndices = indexElementsByOrder(spoonAstModel.getCompilationUnit());

        // When
        boolean isRelocated = RelocationDetector.isRelocated(originalOrderIndices, spoonAstModel.getCompilationUnit());

        // Then
        assertThat(isRelocated).isFalse();
    }

    @Test
    void printRelocations_noRelocations_returnsMessageWithFileName() {
        // When
        String printedRelocations = RelocationDetector.printRelocations(Path.of("dir/Sample.java"), List.of());

        // Then
        assertThat(printedRelocations).contains("Detected member ordering violations in:");
        assertThat(printedRelocations).contains("Sample.java");
    }

    @Test
    void printRelocations_withTopLevelTypeRelocation_showsFileRootAsTypeName() {
        // Given
        SrcFile srcFile = createSrcFile("class Alpha {}\nclass Beta {}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        List<CtElement> topLevelTypes = spoonAstModel.getCompilationUnit().getDeclaredTypes().stream()
                .map(CtElement.class::cast)
                .toList();
        List<MemberRelocation> relocations =
                List.of(new MemberRelocation(topLevelTypes.get(0), null, topLevelTypes.get(1), 1));

        // When
        String printedRelocations = RelocationDetector.printRelocations(Path.of("Sample.java"), relocations);

        // Then
        assertThat(printedRelocations).contains("[1] <file root>:");
    }

    @Test
    void printRelocations_withMethodRelocation_showsDeclarationHeaderSnippet() {
        // Given
        SrcFile srcFile = createSrcFile(
                "public class Sample {\n    public void a() {}\n\n    public void b() {}\n}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        List<MemberRelocation> fakeRelocations = buildMethodRelocationsWithFakeNeighbors(spoonAstModel);

        // When
        String printedRelocations = RelocationDetector.printRelocations(Path.of("Sample.java"), fakeRelocations);

        // Then
        assertThat(printedRelocations).contains("Detected member ordering violations in:");
        assertThat(printedRelocations).contains("  Sample.java");
        assertThat(printedRelocations).doesNotContain("Ordering violation in");
        assertThat(printedRelocations).contains("    --> public void a() { ... }");
        assertThat(printedRelocations).contains("        public void b() { ... }");
    }

    @Test
    void printRelocations_moreViolationsThanLimit_showsFooterWithTotalCount() {
        // Given
        SrcFile srcFile = createSrcFile(
                "public class Sample {\n    public void a() {}\n\n    public void b() {}\n}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        List<CtElement> methods = spoonAstModel.getCompilationUnit().getDeclaredTypes().stream()
                .flatMap(ctType -> ctType.getMethods().stream())
                .map(CtElement.class::cast)
                .toList();
        CtElement firstMethod = methods.get(0);
        List<MemberRelocation> relocationsExceedingLimit = List.of(
                new MemberRelocation(firstMethod, null, null, 1),
                new MemberRelocation(firstMethod, null, null, 2),
                new MemberRelocation(firstMethod, null, null, 3),
                new MemberRelocation(firstMethod, null, null, 4),
                new MemberRelocation(firstMethod, null, null, 5),
                new MemberRelocation(firstMethod, null, null, 6));

        // When
        String printedRelocations =
                RelocationDetector.printRelocations(Path.of("Sample.java"), relocationsExceedingLimit);

        // Then
        assertThat(printedRelocations).contains("  [5]");
        assertThat(printedRelocations).doesNotContain("  [6]");
        assertThat(printedRelocations).contains("  ... 6 violations total");
    }

    @NonNull
    private static List<MemberRelocation> buildMethodRelocationsWithFakeNeighbors(
            @NonNull SpoonAstModel spoonAstModel) {
        List<CtElement> methods = spoonAstModel.getCompilationUnit().getDeclaredTypes().stream()
                .flatMap(ctType -> ctType.getMethods().stream())
                .sorted(Comparator.comparing(CtMethod::getSimpleName))
                .map(CtElement.class::cast)
                .toList();
        if (methods.size() < 2) {
            return methods.stream()
                    .map(method -> new MemberRelocation(method, null, null, 1))
                    .toList();
        }
        return List.of(new MemberRelocation(methods.get(0), null, methods.get(1), 1));
    }
}

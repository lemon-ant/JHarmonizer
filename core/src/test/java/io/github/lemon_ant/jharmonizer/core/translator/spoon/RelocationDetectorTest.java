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
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;

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
        List<Pair<CtElement, Integer>> relocations =
                findRelocations(originalOrderIndices, spoonAstModel.getCompilationUnit());

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
        List<Pair<CtElement, Integer>> relocations = findRelocations(Map.of(), spoonAstModel.getCompilationUnit());

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
        assertThat(printedRelocations).contains("Sample.java");
    }

    @Test
    void printRelocations_withMethodRelocation_includesMethodInOutput() {
        // Given
        SrcFile srcFile = createSrcFile(
                "public class Sample {\n    public void a() {}\n\n    public void b() {}\n}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        List<Pair<CtElement, Integer>> relocations = findRelocations(
                indexElementsByOrder(spoonAstModel.getCompilationUnit()), spoonAstModel.getCompilationUnit());
        List<Pair<CtElement, Integer>> fakeRelocations = buildMethodsWithFakeOffset(spoonAstModel, 1);

        // When
        String printedRelocations = RelocationDetector.printRelocations(Path.of("Sample.java"), fakeRelocations);

        // Then
        assertThat(printedRelocations).contains("Sample.java");
        assertThat(printedRelocations).containsAnyOf("DOWN", "UP");
    }

    @NonNull
    private static List<Pair<CtElement, Integer>> buildMethodsWithFakeOffset(
            @NonNull SpoonAstModel spoonAstModel, int fakeOffset) {
        return spoonAstModel.getCompilationUnit().getDeclaredTypes().stream()
                .flatMap(ctType -> ctType.getMethods().stream())
                .map(method -> Pair.<CtElement, Integer>of(method, fakeOffset))
                .toList();
    }
}

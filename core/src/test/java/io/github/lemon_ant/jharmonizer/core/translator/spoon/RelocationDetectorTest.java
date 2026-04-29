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
        assertThat(printedRelocations).contains("Sample.java");
    }

    @Test
    void printRelocations_withMethodRelocation_includesNeighborContext() {
        // Given
        SrcFile srcFile = createSrcFile(
                "public class Sample {\n    public void a() {}\n\n    public void b() {}\n}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        List<MemberRelocation> fakeRelocations = buildMethodRelocationsWithFakeNeighbors(spoonAstModel);

        // When
        String printedRelocations = RelocationDetector.printRelocations(Path.of("Sample.java"), fakeRelocations);

        // Then
        assertThat(printedRelocations).contains("Sample.java");
        assertThat(printedRelocations).containsAnyOf("should be between", "should be before", "should be after");
    }

    @NonNull
    private static List<MemberRelocation> buildMethodRelocationsWithFakeNeighbors(
            @NonNull SpoonAstModel spoonAstModel) {
        List<CtElement> methods = spoonAstModel.getCompilationUnit().getDeclaredTypes().stream()
                .flatMap(ctType -> ctType.getMethods().stream())
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

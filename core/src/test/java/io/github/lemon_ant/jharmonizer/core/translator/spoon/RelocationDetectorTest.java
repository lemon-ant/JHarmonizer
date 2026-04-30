package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.snapshotOriginalSuccessors;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SrcAstTranslator;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;

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
        Map<SourcePosition, SourcePosition> originalSuccessors =
                snapshotOriginalSuccessors(spoonAstModel.getCompilationUnit());

        // When
        List<MemberRelocation> relocations = findRelocations(originalSuccessors, spoonAstModel.getCompilationUnit());

        // Then
        assertThat(relocations).isEmpty();
    }

    @Test
    void findRelocations_withEmptyOriginalSuccessors_returnsEmptyList() {
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
        Map<SourcePosition, SourcePosition> originalSuccessors =
                snapshotOriginalSuccessors(spoonAstModel.getCompilationUnit());

        // When
        boolean isRelocated = RelocationDetector.isRelocated(originalSuccessors, spoonAstModel.getCompilationUnit());

        // Then
        assertThat(isRelocated).isFalse();
    }

    @Test
    void snapshotOriginalSuccessors_multiRootTypeFile_recordsNextSiblingPositions() {
        // Given
        SrcFile srcFile =
                createSrcFile("class Alpha { static String label() {} }\nclass Beta {}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        CtType<?> alpha = spoonAstModel.getCompilationUnit().getDeclaredTypes().get(0);
        CtType<?> beta = spoonAstModel.getCompilationUnit().getDeclaredTypes().get(1);
        CtMethod<?> labelMethod = alpha.getMethods().iterator().next();

        // When
        Map<SourcePosition, SourcePosition> successors = snapshotOriginalSuccessors(spoonAstModel.getCompilationUnit());

        // Then
        assertThat(successors.get(alpha.getPosition())).isEqualTo(beta.getPosition());
        assertThat(successors).doesNotContainKey(beta.getPosition());
        assertThat(successors).doesNotContainKey(labelMethod.getPosition());
    }

    @Test
    void findRelocations_memberBelongingToRelocatedType_notFlaggedAsMemberViolation() {
        // Given
        SrcFile srcFile =
                createSrcFile("class Alpha { static String label() {} }\nclass Beta {}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        CtType<?> alpha = spoonAstModel.getCompilationUnit().getDeclaredTypes().get(0);
        CtType<?> beta = spoonAstModel.getCompilationUnit().getDeclaredTypes().get(1);
        CtMethod<?> labelMethod = alpha.getMethods().iterator().next();
        // Simulate original order: Beta first, Alpha second; label stays as Alpha's only member.
        Map<SourcePosition, SourcePosition> simulatedOriginalSuccessors = new HashMap<>();
        simulatedOriginalSuccessors.put(beta.getPosition(), alpha.getPosition());

        // When
        List<MemberRelocation> relocations =
                findRelocations(simulatedOriginalSuccessors, spoonAstModel.getCompilationUnit());

        // Then
        assertThat(relocations)
                .noneMatch(relocation -> relocation.getViolatingElement().equals(labelMethod));
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
                List.of(new MemberRelocation(topLevelTypes.get(0), null, topLevelTypes.get(1)));

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
                new MemberRelocation(firstMethod, null, null),
                new MemberRelocation(firstMethod, null, null),
                new MemberRelocation(firstMethod, null, null),
                new MemberRelocation(firstMethod, null, null),
                new MemberRelocation(firstMethod, null, null),
                new MemberRelocation(firstMethod, null, null));

        // When
        String printedRelocations =
                RelocationDetector.printRelocations(Path.of("Sample.java"), relocationsExceedingLimit);

        // Then
        assertThat(printedRelocations).contains("  [5]");
        assertThat(printedRelocations).doesNotContain("  [6]");
        assertThat(printedRelocations).contains("  ... 6 violations total");
    }

    @Test
    void findRelocations_oneMemberMovedFromLastToFirst_reportsSingleBreak() {
        // Given — compilation unit has the "sorted" order [d, a, b, c] (d was last, now first)
        SrcFile srcFile = createSrcFile(
                "public class Sample {\n"
                        + "    public void d() {}\n\n"
                        + "    public void a() {}\n\n"
                        + "    public void b() {}\n\n"
                        + "    public void c() {}\n"
                        + "}\n",
                Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        CtType<?> sampleType =
                spoonAstModel.getCompilationUnit().getDeclaredTypes().get(0);
        CtMethod<?> methodD = requireMethodByName(sampleType, "d");
        CtMethod<?> methodA = requireMethodByName(sampleType, "a");
        CtMethod<?> methodB = requireMethodByName(sampleType, "b");
        CtMethod<?> methodC = requireMethodByName(sampleType, "c");
        // Simulate original order: [a, b, c, d] — d was last, c→d successor, d has no successor
        Map<SourcePosition, SourcePosition> originalSuccessors = new HashMap<>();
        originalSuccessors.put(methodA.getPosition(), methodB.getPosition()); // a→b
        originalSuccessors.put(methodB.getPosition(), methodC.getPosition()); // b→c
        originalSuccessors.put(methodC.getPosition(), methodD.getPosition()); // c→d

        // When
        List<MemberRelocation> relocations = findRelocations(originalSuccessors, spoonAstModel.getCompilationUnit());

        // Then — only 1 break even though 4 members exist; a follows d unexpectedly
        assertThat(relocations).hasSize(1);
        assertThat(relocations.get(0).getViolatingElement()).isEqualTo(methodA);
        assertThat(relocations.get(0).getSortedPredecessor()).isEqualTo(methodD);
        assertThat(relocations.get(0).getSortedSuccessor()).isEqualTo(methodB);
    }

    @NonNull
    private static CtMethod<?> requireMethodByName(CtType<?> type, String name) {
        return type.getMethods().stream()
                .filter(m -> m.getSimpleName().equals(name))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("No method named '" + name + "' in " + type.getSimpleName()));
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
                    .map(method -> new MemberRelocation(method, null, null))
                    .toList();
        }
        return List.of(new MemberRelocation(methods.get(0), null, methods.get(1)));
    }
}

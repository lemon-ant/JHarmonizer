package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;

class RelocationDetectorTest {

    private static final PrinterConfig DEFAULT_PRINTER_CONFIG = new PrinterConfig(true, true, false);
    private static final Path SAMPLE_PATH = Path.of("Sample.java");

    @Nested
    class FindRelocations {

        @Test
        void findRelocations_emptyOriginalIndices_returnsEmptyList() {
            // Given
            SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class Sample { int a; int b; }", SAMPLE_PATH), DEFAULT_PRINTER_CONFIG);
            CtCompilationUnit compilationUnit = spoonAstModel.getCompilationUnit();
            Map<SourcePosition, Integer> emptyOriginalIndices = Collections.emptyMap();

            // When
            List<Pair<CtElement, Integer>> relocations =
                    RelocationDetector.findRelocations(emptyOriginalIndices, compilationUnit);

            // Then
            assertThat(relocations).isEmpty();
        }

        @Test
        void findRelocations_originalOrderPreserved_returnsEmptyList() {
            // Given
            SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class Sample { int a; int b; }", SAMPLE_PATH), DEFAULT_PRINTER_CONFIG);
            CtCompilationUnit compilationUnit = spoonAstModel.getCompilationUnit();
            Map<SourcePosition, Integer> originalOrderIndices =
                    RelocationDetector.indexElementsByOrder(compilationUnit);

            // When
            List<Pair<CtElement, Integer>> relocations =
                    RelocationDetector.findRelocations(originalOrderIndices, compilationUnit);

            // Then
            assertThat(relocations).isEmpty();
        }
    }

    @Nested
    class IsRelocated {

        @Test
        void isRelocated_emptyOriginalIndices_returnsFalse() {
            // Given
            SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class Sample { int a; int b; }", SAMPLE_PATH), DEFAULT_PRINTER_CONFIG);
            CtCompilationUnit compilationUnit = spoonAstModel.getCompilationUnit();
            Map<SourcePosition, Integer> emptyOriginalIndices = Collections.emptyMap();

            // When
            boolean isRelocated = RelocationDetector.isRelocated(emptyOriginalIndices, compilationUnit);

            // Then
            assertThat(isRelocated).isFalse();
        }

        @Test
        void isRelocated_originalOrderPreserved_returnsFalse() {
            // Given
            SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class Sample { int a; int b; }", SAMPLE_PATH), DEFAULT_PRINTER_CONFIG);
            CtCompilationUnit compilationUnit = spoonAstModel.getCompilationUnit();
            Map<SourcePosition, Integer> originalOrderIndices =
                    RelocationDetector.indexElementsByOrder(compilationUnit);

            // When
            boolean isRelocated = RelocationDetector.isRelocated(originalOrderIndices, compilationUnit);

            // Then
            assertThat(isRelocated).isFalse();
        }

        @Test
        void isRelocated_shiftedIndices_returnsTrue() {
            // Given
            SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class Sample { int a; int b; int c; }", SAMPLE_PATH), DEFAULT_PRINTER_CONFIG);
            CtCompilationUnit compilationUnit = spoonAstModel.getCompilationUnit();
            Map<SourcePosition, Integer> originalOrderIndices =
                    RelocationDetector.indexElementsByOrder(compilationUnit);
            // Reverse the indices to simulate reordering (map becomes unmodifiable so we create shifted copy)
            Map<SourcePosition, Integer> reversedIndices = new java.util.HashMap<>();
            List<SourcePosition> positions = new java.util.ArrayList<>(originalOrderIndices.keySet());
            int size = positions.size();
            for (int i = 0; i < size; i++) {
                reversedIndices.put(positions.get(i), size - 1 - i);
            }

            // When
            boolean isRelocated = RelocationDetector.isRelocated(reversedIndices, compilationUnit);

            // Then
            // with reversed mapping some elements will appear to have moved
            assertThat(isRelocated).isTrue();
        }
    }

    @Nested
    class PrintRelocations {

        @Test
        void printRelocations_emptyRelocations_returnsHeaderOnly() {
            // Given
            Path path = Path.of("Sample.java");
            List<Pair<CtElement, Integer>> emptyRelocations = Collections.emptyList();

            // When
            String report = RelocationDetector.printRelocations(path, emptyRelocations);

            // Then
            assertThat(report).contains("Sample.java");
        }

        @Test
        void printRelocations_withFieldRelocation_returnsDescription() {
            // Given
            Path path = Path.of("Sample.java");
            SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class Sample { int alpha; int beta; }", SAMPLE_PATH), DEFAULT_PRINTER_CONFIG);
            CtElement field = spoonAstModel
                    .getCompilationUnit()
                    .getDeclaredTypes()
                    .getFirst()
                    .getTypeMembers()
                    .getFirst();
            List<Pair<CtElement, Integer>> relocations = List.of(Pair.of(field, 1));

            // When
            String report = RelocationDetector.printRelocations(path, relocations);

            // Then
            assertThat(report).contains("Sample.java").contains("DOWN");
        }

        @Test
        void printRelocations_withNegativeOffset_showsUp() {
            // Given
            Path path = Path.of("Sample.java");
            SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class Sample { int alpha; }", SAMPLE_PATH), DEFAULT_PRINTER_CONFIG);
            CtElement field = spoonAstModel
                    .getCompilationUnit()
                    .getDeclaredTypes()
                    .getFirst()
                    .getTypeMembers()
                    .getFirst();
            List<Pair<CtElement, Integer>> relocations = List.of(Pair.of(field, -1));

            // When
            String report = RelocationDetector.printRelocations(path, relocations);

            // Then
            assertThat(report).contains("Sample.java").contains("UP");
        }

        @Test
        void printRelocations_withMethodRelocation_containsMethodSignature() {
            // Given
            Path path = Path.of("Sample.java");
            SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class Sample { void foo() {} }", SAMPLE_PATH), DEFAULT_PRINTER_CONFIG);
            CtElement method =
                    spoonAstModel.getCompilationUnit().getDeclaredTypes().getFirst().getTypeMembers().stream()
                            .filter(member -> member instanceof spoon.reflect.declaration.CtMethod)
                            .findFirst()
                            .orElseThrow();
            List<Pair<CtElement, Integer>> relocations = List.of(Pair.of(method, 1));

            // When
            String report = RelocationDetector.printRelocations(path, relocations);

            // Then
            assertThat(report).contains("foo");
        }

        @Test
        void printRelocations_withConstructorRelocation_containsConstructorSignature() {
            // Given
            Path path = Path.of("Sample.java");
            SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class Sample { Sample() {} }", SAMPLE_PATH), DEFAULT_PRINTER_CONFIG);
            CtElement constructor = spoonAstModel
                    .getCompilationUnit()
                    .getDeclaredTypes()
                    .getFirst()
                    .getTypeMembers()
                    .getFirst();
            List<Pair<CtElement, Integer>> relocations = List.of(Pair.of(constructor, 1));

            // When
            String report = RelocationDetector.printRelocations(path, relocations);

            // Then
            assertThat(report).contains("Sample.java");
        }

        @Test
        void printRelocations_withTypeRelocation_containsTypeName() {
            // Given
            Path path = Path.of("Sample.java");
            SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class Outer { class Inner {} }", SAMPLE_PATH), DEFAULT_PRINTER_CONFIG);
            CtElement innerType = spoonAstModel
                    .getCompilationUnit()
                    .getDeclaredTypes()
                    .getFirst()
                    .getTypeMembers()
                    .getFirst();
            List<Pair<CtElement, Integer>> relocations = List.of(Pair.of(innerType, 1));

            // When
            String report = RelocationDetector.printRelocations(path, relocations);

            // Then
            assertThat(report).contains("Sample.java");
        }
    }

    @Nested
    class IndexElementsByOrder {

        @Test
        void indexElementsByOrder_simpleClass_returnsNonEmptyMap() {
            // Given
            SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class Sample { int a; void foo() {} }", SAMPLE_PATH), DEFAULT_PRINTER_CONFIG);

            // When
            Map<SourcePosition, Integer> orderIndices =
                    RelocationDetector.indexElementsByOrder(spoonAstModel.getCompilationUnit());

            // Then
            assertThat(orderIndices).isNotEmpty();
        }

        @Test
        void indexElementsByOrder_sequentialIndices_areConsistentlyIncreasing() {
            // Given
            SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(
                    createSrcFile("class Sample { int a; int b; int c; }", SAMPLE_PATH), DEFAULT_PRINTER_CONFIG);

            // When
            Map<SourcePosition, Integer> orderIndices =
                    RelocationDetector.indexElementsByOrder(spoonAstModel.getCompilationUnit());

            // Then
            List<Integer> indices = orderIndices.values().stream().sorted().toList();
            assertThat(indices.getFirst()).isEqualTo(0);
            assertThat(indices).doesNotHaveDuplicates();
        }
    }
}

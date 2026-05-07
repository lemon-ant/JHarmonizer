// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders dependent static fields in test classes;
// remove this directive once jharmonizer is upgraded to a version that respects field initialization order.
import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.diff.DiffReporter;
import io.github.lemon_ant.jharmonizer.core.diff.WhitespaceVisualizationStyle;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SrcAstTranslator;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class MemberRelocationPrinterTest {

    private static final CompiledConfig DEFAULT_CONFIG = ConfigurationManager.loadDefaultConfig();
    private static final PrinterConfig DEFAULT_PRINTER_CONFIG = new PrinterConfig(
            DEFAULT_CONFIG.getFormatting().isBlankLineAfterTypeHeader(),
            DEFAULT_CONFIG.getFormatting().isBlankLineBeforeComment(),
            DEFAULT_CONFIG.getFormatting().isBlankLineBetweenFields());
    private static final WhitespaceVisualizationStyle STYLE = DiffReporter.resolveStyle();

    @Test
    void printRelocations_noRelocations_returnsMessageWithFileName() {
        // When
        String output = MemberRelocationPrinter.printRelocations(Path.of("dir/Sample.java"), List.of());

        // Then
        assertThat(output).contains("Detected member ordering violations in:");
        assertThat(output).contains("Sample.java");
    }

    @Test
    void printRelocations_withTopLevelTypeRelocation_showsFileRootAsTypeName() {
        // Given
        SrcFile srcFile = createSrcFile("class Alpha {}\nclass Beta {}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        List<CtTypeMember> topLevelTypes = spoonAstModel.getCompilationUnit().getDeclaredTypes().stream()
                .map(CtTypeMember.class::cast)
                .toList();
        List<MemberRelocation> relocations =
                List.of(new MemberRelocation(List.of(topLevelTypes.get(0)), null, topLevelTypes.get(1)));

        // When
        String output = MemberRelocationPrinter.printRelocations(Path.of("Sample.java"), relocations);

        // Then
        assertThat(output).contains("[1] <file root>:");
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
        String output = MemberRelocationPrinter.printRelocations(Path.of("Sample.java"), fakeRelocations);

        // Then
        assertThat(output).contains("Detected member ordering violations in:");
        assertThat(output).contains("  Sample.java");
        assertThat(output).doesNotContain("Ordering violation in");
        assertThat(output).contains("    --> public void a() { " + STYLE.getEllipsisMark() + " }");
        assertThat(output).contains("        public void b() { " + STYLE.getEllipsisMark() + " }");
    }

    @Test
    void printRelocations_moreViolationsThanLimit_showsFooterWithTotalCount() {
        // Given
        SrcFile srcFile = createSrcFile(
                "public class Sample {\n    public void a() {}\n\n    public void b() {}\n}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        List<CtTypeMember> methods = spoonAstModel.getCompilationUnit().getDeclaredTypes().stream()
                .flatMap(ctType -> ctType.getMethods().stream())
                .map(CtTypeMember.class::cast)
                .toList();
        CtTypeMember firstMethod = methods.get(0);
        List<MemberRelocation> relocationsExceedingLimit = List.of(
                new MemberRelocation(List.of(firstMethod), null, null),
                new MemberRelocation(List.of(firstMethod), null, null),
                new MemberRelocation(List.of(firstMethod), null, null),
                new MemberRelocation(List.of(firstMethod), null, null),
                new MemberRelocation(List.of(firstMethod), null, null),
                new MemberRelocation(List.of(firstMethod), null, null));

        // When
        String output = MemberRelocationPrinter.printRelocations(Path.of("Sample.java"), relocationsExceedingLimit);

        // Then
        assertThat(output).contains("  [5]");
        assertThat(output).doesNotContain("  [6]");
        assertThat(output).contains("  " + STYLE.getEllipsisMark() + " 6 violations total");
    }

    @Test
    void printRelocations_chunkOfFourOrMoreMembers_showsFirstOmissionMarkerAndLast() {
        // Given
        SrcFile srcFile = createSrcFile(
                "public class Sample {\n"
                        + "    public void a() {}\n\n"
                        + "    public void b() {}\n\n"
                        + "    public void c() {}\n\n"
                        + "    public void d() {}\n"
                        + "}\n",
                Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        CtType<?> sampleType =
                spoonAstModel.getCompilationUnit().getDeclaredTypes().get(0);
        CtMethod<?> methodA = requireMethodByName(sampleType, "a");
        CtMethod<?> methodB = requireMethodByName(sampleType, "b");
        CtMethod<?> methodC = requireMethodByName(sampleType, "c");
        CtMethod<?> methodD = requireMethodByName(sampleType, "d");
        List<MemberRelocation> relocation =
                List.of(new MemberRelocation(List.of(methodA, methodB, methodC, methodD), null, null));

        // When
        String output = MemberRelocationPrinter.printRelocations(Path.of("Sample.java"), relocation);

        // Then — first and last are shown with -->, hidden middle elements indicated by count
        assertThat(output).contains("    --> public void a() { " + STYLE.getEllipsisMark() + " }");
        assertThat(output).contains("      " + STYLE.getChunkOmissionMark() + " (2 members omitted)");
        assertThat(output).contains("    --> public void d() { " + STYLE.getEllipsisMark() + " }");
        assertThat(output).doesNotContain("    --> public void b()");
        assertThat(output).doesNotContain("    --> public void c()");
    }

    @NonNull
    private static CtMethod<?> requireMethodByName(CtType<?> type, String name) {
        return type.getMethods().stream()
                .filter(method -> method.getSimpleName().equals(name))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("No method named '" + name + "' in " + type.getSimpleName()));
    }

    @NonNull
    private static List<MemberRelocation> buildMethodRelocationsWithFakeNeighbors(SpoonAstModel spoonAstModel) {
        List<CtTypeMember> methods = spoonAstModel.getCompilationUnit().getDeclaredTypes().stream()
                .flatMap(ctType -> ctType.getMethods().stream())
                .sorted(Comparator.comparing(CtMethod::getSimpleName))
                .map(CtTypeMember.class::cast)
                .toList();
        if (methods.size() < 2) {
            return methods.stream()
                    .map(method -> new MemberRelocation(List.of(method), null, null))
                    .toList();
        }
        return List.of(new MemberRelocation(List.of(methods.get(0)), null, methods.get(1)));
    }
}

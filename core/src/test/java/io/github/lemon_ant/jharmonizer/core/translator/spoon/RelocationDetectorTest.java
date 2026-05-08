// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.findRelocations;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.snapshotOriginalMemberOrder;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.translator.ParsingResult;
import io.github.lemon_ant.jharmonizer.core.translator.SrcAstTranslator;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

class RelocationDetectorTest {
    private static final CompiledConfig DEFAULT_CONFIG = ConfigurationManager.loadDefaultConfig();
    private static final PrinterConfig DEFAULT_PRINTER_CONFIG = new PrinterConfig(
            DEFAULT_CONFIG.getFormatting().isBlankLineAfterTypeHeader(),
            DEFAULT_CONFIG.getFormatting().isBlankLineBeforeComment(),
            DEFAULT_CONFIG.getFormatting().isBlankLineBetweenFields());

    @Test
    void findRelocations_contiguousChunkMoved_reportsSingleRelocationWithMinimalMovedChunk() {
        // Given — sorted order is [a, b, c, d] but original was [c, d, a, b]
        // (chunk [a, b] moved to the front; equivalently [c, d] moved to the back)
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
        // Simulate original order: [c, d, a, b]
        List<CtTypeMember> simulatedOriginalOrder = List.of(methodC, methodD, methodA, methodB);

        // When
        List<MemberRelocation> relocations =
                findRelocations(simulatedOriginalOrder, spoonAstModel.getCompilationUnit());

        // Then — patience-sort LIS keeps the latest-finishing increasing run [c, d] stable;
        // [a, b] is reported as the single moved chunk inserted before c
        assertThat(relocations).hasSize(1);
        assertThat(relocations.get(0).getRelocatedMembers()).containsExactly(methodA, methodB);
        assertThat(relocations.get(0).getSortedPredecessor()).isNull();
        assertThat(relocations.get(0).getSortedSuccessor()).isEqualTo(methodC);
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
        CtTypeMember labelMethod = alpha.getMethods().iterator().next();
        // Simulate original order: Beta first, Alpha second; label stays as Alpha's only member.
        List<CtTypeMember> simulatedOriginalOrder = List.of(beta, alpha, labelMethod);

        // When
        List<MemberRelocation> relocations =
                findRelocations(simulatedOriginalOrder, spoonAstModel.getCompilationUnit());

        // Then
        assertThat(relocations)
                .noneMatch(relocation -> relocation.getRelocatedMembers().contains(labelMethod));
    }

    @Test
    void findRelocations_noChanges_returnsEmptyList() {
        // Given
        SrcFile srcFile = createSrcFile(
                "public class Sample {\n    public void a() {}\n\n    public void b() {}\n}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        List<CtTypeMember> originalMemberOrder = snapshotOriginalMemberOrder(spoonAstModel.getCompilationUnit());

        // When
        List<MemberRelocation> relocations = findRelocations(originalMemberOrder, spoonAstModel.getCompilationUnit());

        // Then
        assertThat(relocations).isEmpty();
    }

    @Test
    void findRelocations_oneMemberMovedFromLastToFirst_reportsSingleRelocationForMovedMember() {
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
        // Simulate original order: [a, b, c, d] — d was last
        List<CtTypeMember> simulatedOriginalOrder = List.of(methodA, methodB, methodC, methodD);

        // When
        List<MemberRelocation> relocations =
                findRelocations(simulatedOriginalOrder, spoonAstModel.getCompilationUnit());

        // Then — minimal moved set is just {d}; [a, b, c] remain stable as the longest increasing subsequence
        assertThat(relocations).hasSize(1);
        assertThat(relocations.get(0).getRelocatedMembers()).containsExactly(methodD);
        assertThat(relocations.get(0).getSortedPredecessor()).isNull();
        assertThat(relocations.get(0).getSortedSuccessor()).isEqualTo(methodA);
    }

    @Test
    void findRelocations_withEmptyOriginalMemberOrder_returnsEmptyList() {
        // Given
        SrcFile srcFile = createSrcFile("public class Sample {\n    public void a() {}\n}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();

        // When
        List<MemberRelocation> relocations = findRelocations(List.of(), spoonAstModel.getCompilationUnit());

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
        List<CtTypeMember> originalMemberOrder = snapshotOriginalMemberOrder(spoonAstModel.getCompilationUnit());

        // When
        boolean isRelocated = RelocationDetector.isRelocated(originalMemberOrder, spoonAstModel.getCompilationUnit());

        // Then
        assertThat(isRelocated).isFalse();
    }

    @Test
    void snapshotOriginalMemberOrder_multiRootTypeFile_collectsMembersInSourceOrder() {
        // Given
        SrcFile srcFile =
                createSrcFile("class Alpha { static String label() {} }\nclass Beta {}\n", Path.of("Sample.java"));
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        CtType<?> alpha = spoonAstModel.getCompilationUnit().getDeclaredTypes().get(0);
        CtType<?> beta = spoonAstModel.getCompilationUnit().getDeclaredTypes().get(1);
        CtTypeMember labelMethod = alpha.getMethods().iterator().next();

        // When
        List<CtTypeMember> memberOrder = snapshotOriginalMemberOrder(spoonAstModel.getCompilationUnit());

        // Then — DFS source order: Alpha, Alpha.label, Beta
        assertThat(memberOrder).containsExactly(alpha, labelMethod, beta);
    }

    @NonNull
    private static CtMethod<?> requireMethodByName(CtType<?> type, String name) {
        return type.getMethods().stream()
                .filter(method -> method.getSimpleName().equals(name))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("No method named '" + name + "' in " + type.getSimpleName()));
    }
}

// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.readClasspathResourceAsString;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinter.serializeCompilationUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.translator.SerializedSrcWithSkippedTypeRanges;
import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import spoon.reflect.declaration.CtType;

class SpoonSrcPrinterTest {

    @NonNull
    private final PrinterConfig printerConfig = new PrinterConfig(true, true, false);

    @TempDir
    private Path temporaryDirectory;

    @Test
    void serializeCompilationUnit_interleavedSourcesAndConfigurations_keepsResultsIndependent() {
        // Given
        String srcCode = readClasspathResourceAsString(
                "/test-cases/core/e2e/printer/scenarios/05-combined-boundaries/input/BoundarySample.java");
        SpoonAstModel model =
                SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("BoundarySample.java")), printerConfig);
        String otherSrcCode = "class Other {\r\n    int value;\r\n}\r\n";
        PrinterConfig otherPrinterConfig = new PrinterConfig(false, false, false);
        SpoonAstModel otherModel =
                SpoonParser.parseJavaSrcFile(createSrcFile(otherSrcCode, Path.of("Other.java")), otherPrinterConfig);
        SerializedSrcWithSkippedTypeRanges firstResult = serializeCompilationUnit(
                model.getCompilationUnit(), srcCode, model.getOptOuts().getSortingSkippedTypes(), printerConfig);
        Map<CtType<?>, SrcCharacterRange> originalRanges = Map.copyOf(firstResult.getSortingSkippedTypeRanges());
        SerializedSrcWithSkippedTypeRanges otherResult = serializeCompilationUnit(
                otherModel.getCompilationUnit(),
                otherSrcCode,
                otherModel.getOptOuts().getSortingSkippedTypes(),
                otherPrinterConfig);

        // When
        SerializedSrcWithSkippedTypeRanges repeatedResult = serializeCompilationUnit(
                model.getCompilationUnit(), srcCode, model.getOptOuts().getSortingSkippedTypes(), printerConfig);

        // Then
        assertThat(firstResult.getSortingSkippedTypeRanges())
                .hasSize(1)
                .containsExactlyInAnyOrderEntriesOf(originalRanges);
        assertThat(repeatedResult.getSerializedSrcCode()).isEqualTo(firstResult.getSerializedSrcCode());
        assertThat(repeatedResult.getSortingSkippedTypeRanges()).containsExactlyInAnyOrderEntriesOf(originalRanges);
        assertThat(otherResult.getSerializedSrcCode()).isEqualTo(otherSrcCode);
        assertThat(otherResult.getSortingSkippedTypeRanges()).isEmpty();
        assertThatThrownBy(() -> firstResult.getSortingSkippedTypeRanges().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest(name = "{0}, trailing terminators: {1}")
    @MethodSource("provideVirtualFooters")
    void serializeCompilationUnit_virtualFooterComment_preservesFooterAcrossRepeatedSerialization(
            @NonNull String fileName, int trailingTerminators) {
        // Given
        String fixtureRoot = "/test-cases/core/e2e/printer/scenarios/08-footer-comments/";
        String inputSrcCode =
                readClasspathResourceAsString(fixtureRoot + "input/" + fileName).stripTrailing()
                        + "\n".repeat(trailingTerminators);
        String expectedSrcCode = readClasspathResourceAsString(fixtureRoot + "expected/" + fileName)
                        .stripTrailing()
                + "\n".repeat(trailingTerminators);
        Path srcPath = temporaryDirectory.resolve(fileName);
        SpoonAstModel model = SpoonParser.parseJavaSrcFile(createSrcFile(inputSrcCode, srcPath), printerConfig);
        String printedSrcCode = model.getSerializedSrcCode().get().getSerializedSrcCode();
        SpoonAstModel repeatedModel =
                SpoonParser.parseJavaSrcFile(createSrcFile(printedSrcCode, srcPath), printerConfig);

        // When
        String repeatedSrcCode = repeatedModel.getSerializedSrcCode().get().getSerializedSrcCode();

        // Then
        assertThat(printedSrcCode).isEqualTo(expectedSrcCode);
        assertThat(repeatedSrcCode).isEqualTo(printedSrcCode);
    }

    @NonNull
    private static Stream<Arguments> provideVirtualFooters() {
        return Stream.of(
                        "InlineFooter",
                        "BlockFooter",
                        "MultilineFooter",
                        "JavaDocFooter",
                        "EmptyJavaDocFooter",
                        "EmptyInlineFooter",
                        "EmptyBlockFooter",
                        "TripleSlashFooter",
                        "MultipleFooters")
                .flatMap(name -> Stream.of(0, 1, 3).map(terminators -> Arguments.of(name + ".java", terminators)));
    }
}

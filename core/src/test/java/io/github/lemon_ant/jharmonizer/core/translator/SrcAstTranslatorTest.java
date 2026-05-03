/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.translator;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFilesHandler;
import io.github.lemon_ant.jharmonizer.core.optout.OptOutFormattingRangeResolver;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SrcAstTranslatorTest {

    private static final PrinterConfig DEFAULT_PRINTER_CONFIG = new PrinterConfig(true, true, false);

    SrcFilesHandler srcFilesHandler;

    @TempDir
    Path tempDir;

    @Test
    void parseSrcFile_validJavaSrc_returnParsingResult() throws Exception {
        // Given
        Path file = Files.writeString(tempDir.resolve("TestClass.java"), "class TestClass { int value = 42; }");
        SrcFile srcFile = createSrcFile(Files.readString(file, StandardCharsets.UTF_8), file);

        // When
        ParsingResult result = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSpoonAstModel()).isNotNull();
        assertThat(result.getParsingStatistic().getParsingTimeInNanos()).isGreaterThan(0);
        assertThat(result.getParsingStatistic().getParsedRootTypesCount()).isEqualTo(1);
        assertThat(result.getParsingStatistic().getParsedTypesTotalCount()).isEqualTo(1);
        assertThat(result.getParsingStatistic().getParsedMembersCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void serialize_validSpoonAstModel_returnSerializedCode() {
        // Given: simple source code
        String src = "class Demo { void m() {} }";
        SrcFile srcFile = createSrcFile(src, Path.of("Demo.java"));
        SpoonAstModel model =
                SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG).getSpoonAstModel();

        // When
        SerializationResult result = SrcAstTranslator.serialize(model);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSerializedSrcWithSkippedTypeRanges().getSerializedSrcCode())
                .contains("class Demo");
        assertThat(result.getSerializationStatistic().getSerializedCodeLength()).isGreaterThan(0);
        assertThat(result.getSerializationStatistic().getProcessingTimeInNanos())
                .isGreaterThan(0);
    }

    @Test
    void serialize_sortOffAndFullyOffTypes_returnFormattingSkippedRangesOnlyForFullyOffTypes() {
        // Given
        String sortOffFragment = """
                // @jharmonizer:sort-off
                class Beta{int z;  int a;}
                """;
        String fullyOffFragment = """
                // @jharmonizer:fully-off
                class Gamma{int y;  int x;}
                """;
        String srcCode = """
                class Alpha {}

                %s

                %s
                """.formatted(sortOffFragment.stripTrailing(), fullyOffFragment.stripTrailing());
        SrcFile srcFile = createSrcFile(srcCode, Path.of("Sample.java"));
        SpoonAstModel spoonAstModel =
                SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG).getSpoonAstModel();

        // When
        SerializationResult result = SrcAstTranslator.serialize(spoonAstModel);
        List<SrcCharacterRange> formattingSkippedRanges = OptOutFormattingRangeResolver.resolveFormattingSkippedRanges(
                spoonAstModel.getOptOuts(), result.getSerializedSrcWithSkippedTypeRanges());
        String serializedSrcCode =
                result.getSerializedSrcWithSkippedTypeRanges().getSerializedSrcCode();

        // Then
        assertThat(serializedSrcCode).contains(sortOffFragment).contains(fullyOffFragment);
        assertThat(formattingSkippedRanges)
                .singleElement()
                .satisfies(range -> assertThat(
                                serializedSrcCode.substring(range.getStartInclusive(), range.getEndExclusive()))
                        .isEqualTo(fullyOffFragment));
    }

    @Test
    void serializedSrcWithSkippedTypeRanges_mutableInputMap_returnsUnmodifiableMap() {
        // Given
        String srcCode = "class Gamma {}";
        SrcFile srcFile = createSrcFile(srcCode, Path.of("Gamma.java"));
        SpoonAstModel spoonAstModel =
                SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG).getSpoonAstModel();
        SerializedSrcWithSkippedTypeRanges serializedSrcWithSkippedTypeRanges = new SerializedSrcWithSkippedTypeRanges(
                srcCode,
                new HashMap<>(
                        Map.of(spoonAstModel.getMainType().orElseThrow(), new SrcCharacterRange(0, srcCode.length()))));

        // When
        Map<?, ?> sortingSkippedTypeRanges = serializedSrcWithSkippedTypeRanges.getSortingSkippedTypeRanges();

        // Then
        assertThat(sortingSkippedTypeRanges).hasSize(1);
        assertThatThrownBy(sortingSkippedTypeRanges::clear).isInstanceOf(UnsupportedOperationException.class);
    }
}

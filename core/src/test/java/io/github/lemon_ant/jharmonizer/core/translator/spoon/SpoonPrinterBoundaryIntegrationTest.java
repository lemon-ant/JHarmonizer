// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.requireTypeMemberBySimpleName;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.readClasspathResourceAsString;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.GROUP_HEADER_METADATA;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.translator.SerializedSrcWithSkippedTypeRanges;
import io.github.lemon_ant.jharmonizer.core.translator.SrcAstTranslator;
import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
import java.nio.file.Path;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtType;

@SuppressWarnings("NotNullFieldNotInitialized")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpoonPrinterBoundaryIntegrationTest {

    @NonNull
    private String expectedSrcCode;

    @NonNull
    private String inputSrcCode;

    @NonNull
    private final PrinterConfig printerConfig = new PrinterConfig(true, true, true);

    @BeforeAll
    void setUp() {
        String fixtureRoot = "/test-cases/core/translator/spoon-printer-boundaries/";
        inputSrcCode = readClasspathResourceAsString(fixtureRoot + "valid/BoundarySample.java");
        expectedSrcCode = readClasspathResourceAsString(fixtureRoot + "expected/BoundarySample.java");
    }

    @ParameterizedTest(name = "{0}, original trailing line terminators: {2}")
    @MethodSource("provideLineEndings")
    void serialize_combinedSeparatorsAndOptOut_preservesExactSourceLayout(
            @NonNull String style, @NonNull String lineSeparator, int trailingLineTerminators) {
        // Given
        String srcCode = inputSrcCode.stripTrailing().replace("\n", lineSeparator)
                + lineSeparator.repeat(trailingLineTerminators);
        SpoonAstModel model =
                SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("BoundarySample.java")), printerConfig);
        CtType<?> mainType = model.getMainType().orElseThrow();
        requireTypeMemberBySimpleName(mainType.getTypeMembers(), "first").putMetadata(GROUP_HEADER_METADATA, "Fields");
        requireTypeMemberBySimpleName(mainType.getTypeMembers(), "Nested")
                .putMetadata(GROUP_HEADER_METADATA, "Nested types");
        SourcePosition preservedPosition = requireTypeMemberBySimpleName(mainType.getTypeMembers(), "Preserved")
                .getPosition();
        String preservedFragment =
                srcCode.substring(preservedPosition.getSourceStart(), preservedPosition.getSourceEnd() + 1)
                        + lineSeparator;

        // When
        SerializedSrcWithSkippedTypeRanges result =
                SrcAstTranslator.serialize(model).getSerializedSrcWithSkippedTypeRanges();

        // Then
        assertThat(result.getSerializedSrcCode()).isEqualTo(expectedSrcCode.replace("\n", lineSeparator));
        assertThat(result.getSortingSkippedTypeRanges()).hasSize(1);
        SrcCharacterRange preservedRange =
                result.getSortingSkippedTypeRanges().values().iterator().next();
        assertThat(result.getSerializedSrcCode()
                        .substring(preservedRange.getStartInclusive(), preservedRange.getEndExclusive()))
                .isEqualTo("    " + preservedFragment);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void serialize_firstNestedType_keepsSingleHeaderSeparator(boolean blankLineAfterTypeHeader) {
        // Given
        String srcCode = "class Outer {\n    static class Inner {}\n}\n";
        PrinterConfig config = new PrinterConfig(blankLineAfterTypeHeader, false, false);
        SpoonAstModel model = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Outer.java")), config);

        // When
        String printedSrcCode = SrcAstTranslator.serialize(model)
                .getSerializedSrcWithSkippedTypeRanges()
                .getSerializedSrcCode();

        // Then
        assertThat(printedSrcCode).isEqualTo("class Outer {\n\n    static class Inner {}\n}\n");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n"})
    void serialize_typeWithoutPreamble_hasOnlyFinalLineTerminator(@NonNull String lineSeparator) {
        // Given
        String srcCode = "class Empty {}" + lineSeparator.repeat(3);
        SpoonAstModel model =
                SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Empty.java")), printerConfig);

        // When
        String printedSrcCode = SrcAstTranslator.serialize(model)
                .getSerializedSrcWithSkippedTypeRanges()
                .getSerializedSrcCode();

        // Then
        assertThat(printedSrcCode).isEqualTo("class Empty {}" + lineSeparator);
    }

    @NonNull
    private static Stream<Arguments> provideLineEndings() {
        return Stream.of(
                Arguments.of("LF", "\n", 0),
                Arguments.of("LF", "\n", 3),
                Arguments.of("CRLF", "\r\n", 0),
                Arguments.of("CRLF", "\r\n", 3));
    }
}

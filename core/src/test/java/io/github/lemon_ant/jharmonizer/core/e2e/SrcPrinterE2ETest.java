// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import static io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromClasspathResource;
import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle.NONE;
import static io.github.lemon_ant.jharmonizer.core.e2e.JavaCompileTestUtils.compileJavaSrcWithRelease21;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.readClasspathResourceAsString;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.SrcProcessingResult;
import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.e2e.JavaCompileTestUtils.CompileResult;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings("NotNullFieldNotInitialized")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SrcPrinterE2ETest {
    private static final String FIXTURES = "/test-cases/core/e2e/printer/";

    @NonNull
    private String boundaryExpectedSrcCode;

    @NonNull
    private String boundaryInputSrcCode;

    @NonNull
    private SrcProcessor boundaryProcessor;

    @NonNull
    private SrcProcessor commentHeaderProcessor;

    @NonNull
    private String commentPlacementSrcCode;

    @NonNull
    private String enumWhitespaceSrcCode;

    @NonNull
    private SrcProcessor fieldGroupProcessor;

    @NonNull
    private String fieldGroupsSrcCode;

    @NonNull
    private String lineSeparatorsSrcCode;

    @NonNull
    private SrcProcessor srcProcessor;

    @TempDir
    private Path temporaryDirectory;

    @NonNull
    private String topLevelTypesExpectedSrcCode;

    @NonNull
    private String topLevelTypesInputSrcCode;

    @NonNull
    private String typeDeclarationsExpectedSrcCode;

    @NonNull
    private String typeDeclarationsInputSrcCode;

    @BeforeAll
    void setUp() {
        srcProcessor = createSrcProcessor(true);
        fieldGroupProcessor = createSrcProcessor(false);
        boundaryProcessor = new SrcProcessor(parseFlexibleUnifiedConfigFromClasspathResource(
                requireClasspathResourceUrl(FIXTURES + "combined-boundaries.yml")));
        commentHeaderProcessor = new SrcProcessor(parseFlexibleUnifiedConfigFromClasspathResource(
                requireClasspathResourceUrl(FIXTURES + "comment-headers.yml")));
        String boundaryFixtures = "/test-cases/core/translator/spoon-printer-boundaries/";
        boundaryInputSrcCode = readNormalizedFixture(boundaryFixtures + "valid/BoundarySample.java");
        boundaryExpectedSrcCode = readNormalizedFixture(boundaryFixtures + "expected/BoundarySample.java");
        commentPlacementSrcCode = readNormalizedFixture(FIXTURES + "valid/input/CommentPlacementScenario.java");
        enumWhitespaceSrcCode = readNormalizedFixture(FIXTURES + "valid/input/EnumWhitespaceScenario.java");
        fieldGroupsSrcCode = readNormalizedFixture(FIXTURES + "valid/input/FieldGroupsScenario.java");
        lineSeparatorsSrcCode = readNormalizedFixture(FIXTURES + "valid/input/LineSeparatorsScenario.java");
        String topLevelFixtures = "/test-cases/core/e2e/reorder/18-top-level-types-default-groups-ordering/";
        topLevelTypesInputSrcCode = readNormalizedFixture(topLevelFixtures + "input/TopLevelTypesOrderingFixture.java");
        topLevelTypesExpectedSrcCode =
                readNormalizedFixture(topLevelFixtures + "expected/TopLevelTypesOrderingFixture.java");
        typeDeclarationsInputSrcCode = readNormalizedFixture(FIXTURES + "valid/input/TypeDeclarationsScenario.java");
        typeDeclarationsExpectedSrcCode = readNormalizedFixture(FIXTURES + "expected/TypeDeclarationsScenario.java");
    }

    @ParameterizedTest(name = "{0}, trailing line terminators: {2}")
    @MethodSource("provideBoundaryVariants")
    void reorder_combinedSeparatorsAndOptOut_preservesExactBoundaries(
            @NonNull String scenario,
            @NonNull String lineSeparator,
            int trailingLineTerminators,
            @NonNull String indentation)
            throws Exception {
        // Given
        String inputSrcCode = boundaryInputSrcCode
                        .stripTrailing()
                        .replace("    ", indentation)
                        .replace("\n", lineSeparator)
                + lineSeparator.repeat(trailingLineTerminators);
        Path srcFile = Files.writeString(temporaryDirectory.resolve("BoundarySample.java"), inputSrcCode);

        // When
        SrcProcessingResult result = processSrc(srcFile, boundaryProcessor);
        String printedSrcCode = Files.readString(srcFile);
        SrcProcessingResult repeatedResult = processSrc(srcFile, boundaryProcessor);
        String repeatedSrcCode = Files.readString(srcFile);
        CompileResult compilation = compileJavaSrcWithRelease21(srcFile, temporaryDirectory.resolve("classes"));

        // Then
        assertThat(result.isSuccess()).as(scenario).isTrue();
        assertThat(repeatedResult.isSuccess()).as(scenario).isTrue();
        assertThat(printedSrcCode)
                .isEqualTo(boundaryExpectedSrcCode.replace("    ", indentation).replace("\n", lineSeparator));
        assertThat(repeatedSrcCode).isEqualTo(printedSrcCode);
        assertThat(compilation.getExitCode()).as(compilation.getOutput()).isZero();
    }

    @Test
    void reorder_commentsSharingDeclarationLines_preservesCommentsAndStableGroupHeader() throws Exception {
        // Given
        Path srcFile =
                Files.writeString(temporaryDirectory.resolve("CommentPlacementScenario.java"), commentPlacementSrcCode);

        // When
        SrcProcessingResult result = processSrc(srcFile, commentHeaderProcessor);
        String printedSrc = Files.readString(srcFile);
        SrcProcessingResult repeatedResult = processSrc(srcFile, commentHeaderProcessor);
        String repeatedSrc = Files.readString(srcFile);
        CompileResult compilation = compileJavaSrcWithRelease21(srcFile, temporaryDirectory.resolve("classes"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(repeatedResult.isSuccess()).isTrue();
        assertThat(repeatedSrc).isEqualTo(printedSrc);
        assertThat(printedSrc)
                .containsOnlyOnce("// Fields\n")
                .containsOnlyOnce("/* Fields */ int alpha;")
                .containsOnlyOnce("int alpha;\n    /* Keep this multi-line\n       field comment. */ int beta;")
                .containsSubsequence("int alpha;", "int beta;", "int zebra;");
        assertThat(compilation.getExitCode()).as(compilation.getOutput()).isZero();
    }

    @Test
    void reorder_distinctFieldGroups_preservesSingleGroupSeparator() throws Exception {
        // Given
        Path srcFile = Files.writeString(temporaryDirectory.resolve("FieldGroupsScenario.java"), fieldGroupsSrcCode);

        // When
        SrcProcessingResult result = processSrc(srcFile, fieldGroupProcessor);
        String printedSrcCode = Files.readString(srcFile);
        CompileResult compilation = compileJavaSrcWithRelease21(srcFile, temporaryDirectory.resolve("classes"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(printedSrcCode.lines()).containsSequence("    static int shared;", "", "    int instance;", "}");
        assertThat(compilation.getExitCode()).as(compilation.getOutput()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"  ", "\u2003", "\u2003\u2003"})
    void reorder_enumCommentWhitespace_preservesCommentsAndMemberContents(@NonNull String commentWhitespace)
            throws Exception {
        // Given
        String inputSrc = enumWhitespaceSrcCode
                .replace("the  double", "the" + commentWhitespace + "double")
                .replace("<trailing-whitespace>", commentWhitespace);
        Path srcFile = Files.writeString(temporaryDirectory.resolve("EnumWhitespaceScenario.java"), inputSrc);

        // When
        SrcProcessingResult result = processSrc(srcFile, srcProcessor);
        String printedSrc = Files.readString(srcFile);
        CompileResult compilation = compileJavaSrcWithRelease21(srcFile, temporaryDirectory.resolve("classes"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(printedSrc)
                .contains("// Keep the" + commentWhitespace + "double spacing." + commentWhitespace + "\n")
                .contains("\"two  spaces\"", "return \"three   spaces\";")
                .containsSubsequence("String alpha()", "String zebra()");
        assertThat(compilation.getExitCode()).as(compilation.getOutput()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideNestedTypeProcessors")
    void reorder_firstNestedType_insertsOneHeaderSeparator(@NonNull String scenario, @NonNull SrcProcessor processor)
            throws Exception {
        // Given
        Path srcFile = Files.writeString(
                temporaryDirectory.resolve("NestedOnly.java"), "class NestedOnly {\n    static class Inner {}\n}\n");

        // When
        SrcProcessingResult result = processSrc(srcFile, processor);
        String printedSrcCode = Files.readString(srcFile);
        CompileResult compilation = compileJavaSrcWithRelease21(srcFile, temporaryDirectory.resolve("classes"));

        // Then
        assertThat(result.isSuccess()).as(scenario).isTrue();
        assertThat(printedSrcCode).isEqualTo("class NestedOnly {\n\n    static class Inner {}\n}\n");
        assertThat(compilation.getExitCode()).as(compilation.getOutput()).isZero();
    }

    @Test
    void reorder_headerlessSingleLineUnit_printsSortedMembersAndFinalNewline() throws Exception {
        // Given
        Path srcFile = Files.writeString(
                temporaryDirectory.resolve("HeaderlessScenario.java"),
                "class HeaderlessScenario{int zebra;int alpha;}");
        String lineSeparator = System.lineSeparator();

        // When
        SrcProcessingResult result = processSrc(srcFile, srcProcessor);
        String printedSrc = Files.readString(srcFile);
        CompileResult compilation = compileJavaSrcWithRelease21(srcFile, temporaryDirectory.resolve("classes"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(printedSrc)
                .isEqualTo("class HeaderlessScenario{\nint alpha;\nint zebra;\n}\n".replace("\n", lineSeparator));
        assertThat(compilation.getExitCode()).as(compilation.getOutput()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideLineSeparators")
    void reorder_lineSeparatorVariants_preservesFragmentsAndUsesDominantSeparator(
            @NonNull String scenarioName, @NonNull String inputSeparator, @NonNull String expectedSeparator)
            throws Exception {
        // Given
        String inputSrc = lineSeparatorsSrcCode.replace("\n", inputSeparator);
        Path srcFile = Files.writeString(temporaryDirectory.resolve("LineSeparatorsScenario.java"), inputSrc);
        String originalHeader = inputSrc.substring(0, inputSrc.indexOf("class LineSeparatorsScenario"))
                .stripTrailing();

        // When
        SrcProcessingResult result = processSrc(srcFile, srcProcessor);
        String printedSrc = Files.readString(srcFile);
        CompileResult compilation = compileJavaSrcWithRelease21(srcFile, temporaryDirectory.resolve("classes"));

        // Then
        assertThat(result.isSuccess()).as(scenarioName).isTrue();
        // Original fragments retain their own separators; newly inserted lines use the dominant separator.
        assertThat(printedSrc)
                .startsWith(originalHeader + expectedSeparator + expectedSeparator)
                .contains("    int alpha;" + expectedSeparator + "    int zebra;" + expectedSeparator)
                .endsWith(expectedSeparator + "}" + expectedSeparator);
        assertThat(compilation.getExitCode()).as(compilation.getOutput()).isZero();
    }

    @Test
    void reorder_topLevelKinds_sortsAndSeparatesEveryDeclarationKind() throws Exception {
        // Given
        Path srcFile = Files.writeString(
                temporaryDirectory.resolve("TopLevelTypesOrderingFixture.java"), topLevelTypesInputSrcCode);

        // When
        SrcProcessingResult result = processSrc(srcFile, srcProcessor);
        String printedSrcCode = Files.readString(srcFile);
        CompileResult compilation = compileJavaSrcWithRelease21(srcFile, temporaryDirectory.resolve("classes"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(printedSrcCode).isEqualTo(topLevelTypesExpectedSrcCode);
        assertThat(compilation.getExitCode()).as(compilation.getOutput()).isZero();
    }

    @Test
    void reorder_typeDeclarationsAndComments_preservesUnformattedPrinterOutput() throws Exception {
        // Given
        Path srcFile = Files.writeString(
                temporaryDirectory.resolve("TypeDeclarationsScenario.java"), typeDeclarationsInputSrcCode);

        // When
        SrcProcessingResult result = processSrc(srcFile, srcProcessor);
        String printedSrc = Files.readString(srcFile);
        CompileResult compilation = compileJavaSrcWithRelease21(srcFile, temporaryDirectory.resolve("classes"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(printedSrc).isEqualTo(typeDeclarationsExpectedSrcCode);
        assertThat(compilation.getExitCode()).as(compilation.getOutput()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideWhitespacePreambles")
    void reorder_whitespaceOnlyPreamble_omitsEmptyHeader(
            @NonNull String scenario, @NonNull String preamble, @NonNull String lineSeparator) throws Exception {
        // Given
        Path srcFile = Files.writeString(
                temporaryDirectory.resolve("EmptyPreamble.java"), preamble + "class EmptyPreamble {}");

        // When
        SrcProcessingResult result = processSrc(srcFile, srcProcessor);
        String printedSrcCode = Files.readString(srcFile);
        CompileResult compilation = compileJavaSrcWithRelease21(srcFile, temporaryDirectory.resolve("classes"));

        // Then
        assertThat(result.isSuccess()).as(scenario).isTrue();
        assertThat(printedSrcCode).isEqualTo("class EmptyPreamble {}" + lineSeparator);
        assertThat(compilation.getExitCode()).as(compilation.getOutput()).isZero();
    }

    @NonNull
    private static SrcProcessor createSrcProcessor(boolean blankLineBeforeComment) {
        return new SrcProcessor(FlexibleUnifiedConfig.builder()
                .formatting(FlexibleUnifiedFormatting.builder()
                        .formatterStyle(NONE)
                        .fixImports(false)
                        .blankLineBeforeComment(blankLineBeforeComment)
                        .build())
                .backupsEnabled(false)
                .processingStatisticsMode(ProcessingStatisticsMode.DISABLED)
                .build());
    }

    @NonNull
    private static Stream<Arguments> provideBoundaryVariants() {
        return Stream.of(
                Arguments.of("LF", "\n", 0, "    "),
                Arguments.of("LF", "\n", 3, "    "),
                Arguments.of("CRLF", "\r\n", 0, "    "),
                Arguments.of("CRLF", "\r\n", 3, "    "),
                Arguments.of("CR", "\r", 0, "    "),
                Arguments.of("CR", "\r", 3, "    "),
                Arguments.of("LF with tabs", "\n", 3, "\t"));
    }

    @NonNull
    private static Stream<Arguments> provideLineSeparators() {
        return Stream.of(
                Arguments.of("LF", "\n", "\n"),
                Arguments.of("CRLF", "\r\n", "\r\n"),
                Arguments.of("CR including final standalone CR", "\r", "\r"),
                Arguments.of("CRLF dominates LF", "\r\n\r\n\n", "\r\n"),
                Arguments.of("LF dominates CRLF", "\r\n\n\n", "\n"),
                Arguments.of("CR dominates CRLF", "\r\n\r\r", "\r"),
                Arguments.of("CRLF wins tie with LF", "\r\n\n", "\r\n"),
                Arguments.of("CRLF wins tie with CR", "\r\n\r", "\r\n"),
                Arguments.of("LF wins tie with CR", "\n\r", "\n"));
    }

    @NonNull
    private static Stream<Arguments> provideWhitespacePreambles() {
        return Stream.of(
                Arguments.of("LF", "\n\n", "\n"),
                Arguments.of("CRLF", "\r\n\r\n", "\r\n"),
                Arguments.of("spaces, tab and form feed", " \t\f\n", "\n"));
    }

    @NonNull
    private static String readNormalizedFixture(String resourcePath) {
        return readClasspathResourceAsString(resourcePath).replace("\r\n", "\n");
    }

    @NonNull
    private SrcProcessingResult processSrc(Path srcFile, SrcProcessor processor) {
        return processor.processSources(
                temporaryDirectory, List.of(srcFile.getFileName().toString()), List.of(), FlowType.REORDER);
    }

    @NonNull
    private Stream<Arguments> provideNestedTypeProcessors() {
        return Stream.of(
                Arguments.of("header separator disabled", srcProcessor),
                Arguments.of("header separator enabled", boundaryProcessor));
    }
}

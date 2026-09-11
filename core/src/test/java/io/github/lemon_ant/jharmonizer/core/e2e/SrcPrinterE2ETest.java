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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SrcPrinterE2ETest {
    private static final String FIXTURES = "/test-cases/core/e2e/printer/";
    private static final String COMMENT_PLACEMENT_INPUT = FIXTURES + "valid/input/CommentPlacementScenario.java";
    private static final String ENUM_WHITESPACE_INPUT = FIXTURES + "valid/input/EnumWhitespaceScenario.java";
    private static final String LINE_SEPARATORS_INPUT = FIXTURES + "valid/input/LineSeparatorsScenario.java";
    private static final String TYPE_DECLARATIONS_EXPECTED = FIXTURES + "expected/TypeDeclarationsScenario.java";
    private static final String TYPE_DECLARATIONS_INPUT = FIXTURES + "valid/input/TypeDeclarationsScenario.java";

    private final SrcProcessor srcProcessor = new SrcProcessor(FlexibleUnifiedConfig.builder()
            .formatting(FlexibleUnifiedFormatting.builder()
                    .formatterStyle(NONE)
                    .fixImports(false)
                    .build())
            .backupsEnabled(false)
            .processingStatisticsMode(ProcessingStatisticsMode.DISABLED)
            .build());

    @TempDir
    private Path temporaryDirectory;

    @Test
    void reorder_commentsSharingDeclarationLines_preservesCommentsAndAddsGroupHeader() throws Exception {
        // Given
        SrcProcessor commentHeaderProcessor = new SrcProcessor(parseFlexibleUnifiedConfigFromClasspathResource(
                requireClasspathResourceUrl(FIXTURES + "comment-headers.yml")));
        String inputSrc = readClasspathResourceAsString(COMMENT_PLACEMENT_INPUT).replace("\r\n", "\n");
        Path srcFile = Files.writeString(temporaryDirectory.resolve("CommentPlacementScenario.java"), inputSrc);

        // When
        SrcProcessingResult result = processSrc(srcFile, commentHeaderProcessor);
        String printedSrc = Files.readString(srcFile);
        CompileResult compilation = compileJavaSrcWithRelease21(srcFile, temporaryDirectory.resolve("classes"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(printedSrc)
                .containsOnlyOnce("// Fields\n")
                .containsOnlyOnce("/* Fields */ int alpha;")
                .containsOnlyOnce("int alpha;\n    /* Keep this multi-line\n       field comment. */ int beta;")
                .containsSubsequence("int alpha;", "int beta;", "int zebra;");
        assertThat(compilation.getExitCode()).as(compilation.getOutput()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"  ", "\u2003", "\u2003\u2003"})
    void reorder_enumCommentWhitespace_preservesCommentsAndMemberContents(String commentWhitespace) throws Exception {
        // Given
        String inputSrc = readClasspathResourceAsString(ENUM_WHITESPACE_INPUT)
                .replace("\r\n", "\n")
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
                .isEqualTo("\nclass HeaderlessScenario{\nint alpha;\nint zebra;\n}\n".replace("\n", lineSeparator));
        assertThat(compilation.getExitCode()).as(compilation.getOutput()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lineSeparators")
    void reorder_lineSeparatorVariants_preservesFragmentsAndUsesDominantSeparator(
            String scenarioName, String inputSeparator, String expectedSeparator) throws Exception {
        // Given
        String inputSrc = readClasspathResourceAsString(LINE_SEPARATORS_INPUT)
                .replace("\r\n", "\n")
                .replace("\n", inputSeparator);
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
                .endsWith(inputSeparator + "}" + expectedSeparator);
        assertThat(compilation.getExitCode()).as(compilation.getOutput()).isZero();
    }

    @Test
    void reorder_typeDeclarationsAndComments_preservesUnformattedPrinterOutput() throws Exception {
        // Given
        String inputSrc = readClasspathResourceAsString(TYPE_DECLARATIONS_INPUT).replace("\r\n", "\n");
        String expectedSrc =
                readClasspathResourceAsString(TYPE_DECLARATIONS_EXPECTED).replace("\r\n", "\n");
        Path srcFile = Files.writeString(temporaryDirectory.resolve("TypeDeclarationsScenario.java"), inputSrc);

        // When
        SrcProcessingResult result = processSrc(srcFile, srcProcessor);
        String printedSrc = Files.readString(srcFile);
        CompileResult compilation = compileJavaSrcWithRelease21(srcFile, temporaryDirectory.resolve("classes"));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(printedSrc).isEqualTo(expectedSrc);
        assertThat(compilation.getExitCode()).as(compilation.getOutput()).isZero();
    }

    @NonNull
    private static Stream<Arguments> lineSeparators() {
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
    private SrcProcessingResult processSrc(Path srcFile, SrcProcessor processor) {
        return processor.processSources(
                temporaryDirectory, List.of(srcFile.getFileName().toString()), List.of(), FlowType.REORDER);
    }
}

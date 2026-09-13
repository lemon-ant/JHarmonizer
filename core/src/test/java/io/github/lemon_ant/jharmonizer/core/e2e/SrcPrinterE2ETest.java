// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.readClasspathResourceAsString;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathDirectoryUrl;

import java.net.URI;
import java.nio.file.Path;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SrcPrinterE2ETest extends AbstractCompilableSrcProcessorE2ETest {
    private static final String FIXTURES = "/test-cases/core/e2e/printer/scenarios/";

    @NonNull
    private static final Path FIXTURES_ROOT =
            Path.of(URI.create(requireClasspathDirectoryUrl(FIXTURES).toExternalForm()));

    @ParameterizedTest(name = "{0}, trailing line terminators: {2}")
    @MethodSource("provideBoundaryVariants")
    void reorder_combinedSeparatorsAndOptOut_preservesExactBoundaries(
            @NonNull String scenario,
            @NonNull String lineSeparator,
            int trailingLineTerminators,
            @NonNull String indentation)
            throws Exception {
        // When / Then
        processFixtureVariant(
                "05-combined-boundaries",
                "BoundarySample.java",
                srcCode -> srcCode.stripTrailing().replace("    ", indentation).replace("\n", lineSeparator)
                        + lineSeparator.repeat(trailingLineTerminators),
                srcCode -> srcCode.replace("    ", indentation).replace("\n", lineSeparator));
    }

    @ParameterizedTest
    @MethodSource("provideEmptyPreambles")
    void reorder_emptyPreamble_hasOnlyFinalLineTerminator(
            @NonNull String preamble, @NonNull String trailingTerminators, @NonNull String expectedSeparator)
            throws Exception {
        // When / Then
        processVariant(
                "07-line-separators",
                "EmptyPreamble.java",
                preamble + "class EmptyPreamble {}" + trailingTerminators,
                "class EmptyPreamble {}" + expectedSeparator);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\u2003", "\u2003\u2003"})
    void reorder_enumCommentWhitespace_preservesCommentsAndMemberContents(@NonNull String commentWhitespace)
            throws Exception {
        // Given
        UnaryOperator<String> replaceWhitespace =
                srcCode -> srcCode.replace("the  double", "the" + commentWhitespace + "double")
                        .replace("spacing.  \n", "spacing." + commentWhitespace + "\n");

        // When / Then
        processFixtureVariant(
                "06-enum-whitespace", "EnumWhitespaceScenario.java", replaceWhitespace, replaceWhitespace);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void reorder_footerJavaDocWithTags_preservesWhitespaceAndLineSeparators(@NonNull String lineSeparator)
            throws Exception {
        // Given
        UnaryOperator<String> replaceWhitespace =
                srcCode -> srcCode.replace("First description line.", "First description line.  ")
                        .replace("\n", lineSeparator);

        // When / Then
        processFixtureVariant("09-footer-javadoc", "FooterJavaDocScenario.java", replaceWhitespace, replaceWhitespace);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("footerInputFiles")
    void reorder_footerWithoutFinalTerminator_preservesFooter(@NonNull Path scenarioDir, @NonNull Path srcFile)
            throws Exception {
        // When / Then
        processFixtureVariant(scenarioDir.toString(), srcFile.toString(), String::stripTrailing, String::stripTrailing);
    }

    @Test
    void reorder_headerlessSingleLineUnit_printsSortedMembersAndFinalNewline() throws Exception {
        // When / Then
        processVariant(
                "07-line-separators",
                "HeaderlessScenario.java",
                "class HeaderlessScenario{int zebra;int alpha;}",
                "class HeaderlessScenario{\nint alpha;\nint zebra;\n}\n".replace("\n", System.lineSeparator()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideLineSeparators")
    void reorder_lineSeparatorVariants_preservesFragmentsAndUsesDominantSeparator(
            @NonNull String scenarioName, @NonNull String inputSeparator, @NonNull String expectedSeparator)
            throws Exception {
        // Given
        String inputSrcCode = readFixture("07-line-separators/input/LineSeparatorsScenario.java")
                .replace("\n", inputSeparator);
        String expectedSrcCode = readFixture("07-line-separators/expected/LineSeparatorsScenario.java");
        String typeHeader = "class LineSeparatorsScenario";
        String originalHeader =
                inputSrcCode.substring(0, inputSrcCode.indexOf(typeHeader)).stripTrailing();
        // Retained fragments keep their separators; inserted lines use the dominant separator.
        String expectedOutput = originalHeader
                + expectedSeparator.repeat(2)
                + expectedSrcCode.substring(expectedSrcCode.indexOf(typeHeader)).replace("\n", expectedSeparator);

        // When / Then
        processVariant("07-line-separators", "LineSeparatorsScenario.java", inputSrcCode, expectedOutput);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideRawTailVariants")
    void reorder_rawTailVariants_preservesTextAndEndOfFile(
            @NonNull String scenario, @NonNull String lineSeparator, @NonNull String fileEnding) throws Exception {
        // Given
        UnaryOperator<String> replaceWhitespace =
                srcCode -> srcCode.stripTrailing().replace("\n", lineSeparator) + fileEnding;

        // When / Then
        processFixtureVariant("16-raw-source-tail", "IndentedTail.java", replaceWhitespace, replaceWhitespace);
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
    private static Stream<Arguments> provideEmptyPreambles() {
        return Stream.of(
                Arguments.of("\n\n", "", "\n"),
                Arguments.of("\r\n\r\n", "", "\r\n"),
                Arguments.of(" \t\f\n", "", "\n"),
                Arguments.of("", "\n\n\n", "\n"),
                Arguments.of("", "\r\n\r\n\r\n", "\r\n"));
    }

    @NonNull
    private static Stream<Arguments> provideLineSeparators() {
        return Stream.of(
                Arguments.of("LF", "\n", "\n"),
                Arguments.of("CRLF", "\r\n", "\r\n"),
                Arguments.of("CR", "\r", "\r"),
                Arguments.of("CRLF dominates LF", "\r\n\r\n\n", "\r\n"),
                Arguments.of("LF dominates CRLF", "\r\n\n\n", "\n"),
                Arguments.of("CR dominates CRLF", "\r\n\r\r", "\r"),
                Arguments.of("CRLF wins an LF tie", "\r\n\n", "\r\n"),
                Arguments.of("CRLF wins a CR tie", "\r\n\r", "\r\n"),
                Arguments.of("LF wins a CR tie", "\n\r", "\n"));
    }

    @NonNull
    private static Stream<Arguments> provideRawTailVariants() {
        return Stream.of(
                Arguments.of("LF without final terminator", "\n", ""),
                Arguments.of("CRLF without final terminator", "\r\n", ""),
                Arguments.of("CR without final terminator", "\r", ""),
                Arguments.of("LF with trailing whitespace", "\n", " \t\n\n"),
                Arguments.of("CRLF with trailing whitespace", "\r\n", " \t\r\n\r\n"),
                Arguments.of("CR with trailing whitespace", "\r", " \t\r\r"),
                Arguments.of("ASCII SUB at end of file", "\n", "\n\032"),
                Arguments.of("escaped SUB at end of file", "\n", "\n\\u001a"));
    }

    @NonNull
    private static String readFixture(String relativePath) {
        return readClasspathResourceAsString(FIXTURES + relativePath);
    }

    @NonNull
    private Stream<Arguments> footerInputFiles() {
        return fixtureInputFiles()
                .filter(arguments -> arguments.get()[0].toString().equals("08-footer-comments"));
    }

    @NonNull
    @Override
    protected Path getFixturesRoot() {
        return FIXTURES_ROOT;
    }

    private void processFixtureVariant(
            String scenario,
            String fileName,
            UnaryOperator<String> transformInput,
            UnaryOperator<String> transformExpected)
            throws Exception {
        processVariant(
                scenario,
                fileName,
                transformInput.apply(readFixture(scenario + "/input/" + fileName)),
                transformExpected.apply(readFixture(scenario + "/expected/" + fileName)));
    }
}

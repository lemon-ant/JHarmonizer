// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.readClasspathResourceAsString;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.detectDominantLineSeparator;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.SrcAstTranslator;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// JUnit initializes the shared output in @BeforeAll before tests and instance @MethodSource providers run.
// The IDE's constructor-based nullability check cannot see this lifecycle initialization.
@SuppressWarnings("NotNullFieldNotInitialized")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpoonPrinterBlankLinesIntegrationTest {

    @NonNull
    private static final String E2E_ROOT = "/test-cases/core/e2e/reorder/";

    @NonNull
    private String complexTypesSrcCode;

    @NonNull
    private String disabledFlagsSrcCode;

    @NonNull
    private String fieldSeparatorsSrcCode;

    @NonNull
    private String groupSeparatorsSrcCode;

    @NonNull
    private String headerSeparatorSrcCode;

    @NonNull
    private String topLevelTypesSrcCode;

    @BeforeAll
    void setUp() {
        CompiledConfig defaultConfig = ConfigurationManager.loadDefaultConfig();
        complexTypesSrcCode = printSortedFixture(
                "16-default-config-complex-non-class-types/", "DefaultConfigComplexTypesScenario.java", defaultConfig);
        disabledFlagsSrcCode = printSortedFixtureWithScenarioConfig(
                "23-printer-config-blank-line-flags-disabled/", "PrinterConfigBlankLineFlagsDisabledScenario.java");
        fieldSeparatorsSrcCode = printSortedFixtureWithScenarioConfig(
                "25-printer-config-blank-line-between-fields-enabled/",
                "PrinterConfigBlankLineBetweenFieldsEnabledScenario.java");
        groupSeparatorsSrcCode =
                printSortedFixtureWithScenarioConfig("08-separator-variants/", "NewLineSeparatorBehaviorSample.java");
        headerSeparatorSrcCode = printSortedFixtureWithScenarioConfig(
                "24-printer-config-blank-line-after-type-header-enabled/",
                "PrinterConfigBlankLineAfterTypeHeaderEnabledScenario.java");
        topLevelTypesSrcCode = printSortedFixture(
                "18-top-level-types-default-groups-ordering/", "TopLevelTypesOrderingFixture.java", defaultConfig);
    }

    @Test
    void serialize_emptyLastTopLevelType_hasNoTrailingBlankLine() {
        // When
        // String.lines() ignores the required final line terminator, but retains any additional blank line at EOF.
        List<String> printedLines =
                topLevelTypesSrcCode.lines().map(String::strip).toList();

        // Then
        assertThat(printedLines)
                .as("An empty final annotation type must not leave a blank line at EOF")
                .endsWith("@interface ZetaAnnotation {}");
    }

    @Test
    void serialize_finalNestedType_hasNoBlankLineBeforeOuterClosingBrace() {
        // When
        List<String> printedLines =
                disabledFlagsSrcCode.lines().map(String::strip).toList();

        // Then
        assertThat(printedLines)
                .as("The final nested enum and its enclosing class must close without an extra blank line")
                .endsWith("}", "}");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({"provideClosingTypeBoundaries", "provideNeighboringTypeBoundaries", "provideRequiredSeparators"})
    void serialize_sortedDeclarations_preservesOnlyRequiredBlankLines(
            @NonNull String boundary, @NonNull String printedSrcCode, @NonNull List<String> expectedLines) {
        // When
        List<String> printedLines = printedSrcCode.lines().map(String::strip).toList();

        // Then
        assertThat(printedLines).as("%s before formatting", boundary).containsSequence(expectedLines);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("providePrintedFiles")
    void serialize_sortedFile_hasFinalLineSeparator(@NonNull String scenario, @NonNull String printedSrcCode) {
        // When
        String lineSeparator = detectDominantLineSeparator(printedSrcCode);

        // Then
        // Check the raw text: String.lines() cannot distinguish a terminated last line from an unterminated one.
        assertThat(printedSrcCode)
                .as("%s must end with a line separator before formatting", scenario)
                .endsWith(lineSeparator);
    }

    @NonNull
    private static String printSortedFixture(String scenario, String fileName, CompiledConfig config) {
        URL inputResource = requireClasspathResourceUrl(E2E_ROOT + scenario + "input/" + fileName);
        String srcCode = readClasspathResourceAsString(inputResource);
        UnifiedFormatting formatting = config.getFormatting();
        PrinterConfig printerConfig = new PrinterConfig(
                formatting.isBlankLineAfterTypeHeader(),
                formatting.isBlankLineBeforeComment(),
                formatting.isBlankLineBetweenFields());
        SpoonAstModel parsedModel = SrcAstTranslator.parse(createSrcFile(srcCode, Path.of(fileName)), printerConfig)
                .getSpoonAstModel();
        SpoonAstModel sortedModel = new Sorter(config).sort(parsedModel).getSortedSpoonAstModel();
        // Stop before Formatter: the E2E expectations hide redundant separators removed by Palantir.
        // Assertions strip indentation only, retaining every empty line at the selected declaration boundaries.
        return SrcAstTranslator.serialize(sortedModel)
                .getSerializedSrcWithSkippedTypeRanges()
                .getSerializedSrcCode();
    }

    @NonNull
    private static String printSortedFixtureWithScenarioConfig(String scenario, String fileName) {
        URL configResource = requireClasspathResourceUrl(E2E_ROOT + scenario + "config.yml");
        CompiledConfig config = ConfigurationManager.overrideDefaultConfig(
                JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromClasspathResource(configResource));
        return printSortedFixture(scenario, fileName, config);
    }

    @NonNull
    private Stream<Arguments> provideClosingTypeBoundaries() {
        return Stream.of(
                Arguments.of("class ending in a method", groupSeparatorsSrcCode, List.of("void aMethod() {}", "}")),
                Arguments.of("nested enum ending in a method", disabledFlagsSrcCode, List.of("void beta() {}", "}")),
                Arguments.of(
                        "nested annotation ending in a member", complexTypesSrcCode, List.of("String name();", "}")),
                Arguments.of(
                        "nested interface ending in a method", complexTypesSrcCode, List.of("String value();", "}")),
                Arguments.of(
                        "class ending in an empty record",
                        complexTypesSrcCode,
                        List.of("private record PrivateRecord(String value) {}", "}")),
                Arguments.of(
                        "top-level annotation ending in a member",
                        complexTypesSrcCode,
                        List.of("int zeta() default 7;", "}")));
    }

    @NonNull
    private Stream<Arguments> provideNeighboringTypeBoundaries() {
        return Stream.of(
                Arguments.of(
                        "method followed by a nested enum with flags disabled",
                        disabledFlagsSrcCode,
                        List.of("}", "", "enum Status {")),
                Arguments.of(
                        "method followed by a nested enum with header spacing enabled",
                        headerSeparatorSrcCode,
                        List.of("}", "", "enum Status {")),
                Arguments.of(
                        "nested enum followed by a nested class",
                        headerSeparatorSrcCode,
                        List.of("}", "", "static class Nested {")),
                Arguments.of(
                        "method followed by a nested annotation",
                        complexTypesSrcCode,
                        List.of("}", "", "private @interface PrivateAnnotation {")),
                Arguments.of(
                        "nested annotation followed by a nested interface",
                        complexTypesSrcCode,
                        List.of("}", "", "private interface PrivateInterface {")),
                Arguments.of(
                        "nested interface followed by a constants-only enum",
                        complexTypesSrcCode,
                        List.of("}", "", "private enum PrivateEnum {")),
                Arguments.of(
                        "constants-only enum followed by an empty nested record",
                        complexTypesSrcCode,
                        List.of("}", "", "private record PrivateRecord(String value) {}")),
                Arguments.of(
                        "empty top-level record followed by a class",
                        topLevelTypesSrcCode,
                        List.of("record BetaRecord(int value) {}", "", "class BetaUtility {")),
                Arguments.of(
                        "constants-only top-level enum followed by another enum",
                        topLevelTypesSrcCode,
                        List.of("}", "", "enum ZetaKind {")),
                Arguments.of(
                        "constants-only top-level enum followed by an annotation",
                        topLevelTypesSrcCode,
                        List.of("}", "", "@interface AlphaAnnotation {}")),
                Arguments.of(
                        "consecutive empty top-level annotations",
                        topLevelTypesSrcCode,
                        List.of("@interface AlphaAnnotation {}", "", "@interface ZetaAnnotation {}")));
    }

    @NonNull
    private Stream<Arguments> providePrintedFiles() {
        return Stream.of(
                Arguments.of("complex type declarations", complexTypesSrcCode),
                Arguments.of("blank-line flags disabled", disabledFlagsSrcCode),
                Arguments.of("field separators enabled", fieldSeparatorsSrcCode),
                Arguments.of("member group separators", groupSeparatorsSrcCode),
                Arguments.of("type header separator enabled", headerSeparatorSrcCode),
                Arguments.of("empty final top-level annotation", topLevelTypesSrcCode));
    }

    @NonNull
    private Stream<Arguments> provideRequiredSeparators() {
        return Stream.of(
                Arguments.of(
                        "one separator between methods",
                        disabledFlagsSrcCode,
                        List.of("void alpha() {}", "", "@SuppressWarnings(\"unused\")", "void beta() {}")),
                Arguments.of(
                        "one separator before a method comment",
                        disabledFlagsSrcCode,
                        List.of("void beta() {}", "", "// utility comment", "void gamma() {}")),
                Arguments.of(
                        "one separator after enum constants",
                        disabledFlagsSrcCode,
                        List.of("INACTIVE;", "", "void alpha() {}")),
                Arguments.of(
                        "no separator after a type header with flags disabled",
                        disabledFlagsSrcCode,
                        List.of("public class PrinterConfigBlankLineFlagsDisabledScenario {", "int alpha = 1;")),
                Arguments.of(
                        "one configured separator after a type header",
                        headerSeparatorSrcCode,
                        List.of(
                                "public class PrinterConfigBlankLineAfterTypeHeaderEnabledScenario {",
                                "",
                                "int alpha = 1;")),
                Arguments.of(
                        "one configured separator after a nested type header",
                        headerSeparatorSrcCode,
                        List.of("static class Nested {", "", "int alpha = 1;")),
                Arguments.of(
                        "no separator between ordinary fields with field spacing disabled",
                        headerSeparatorSrcCode,
                        List.of("int alpha = 1;", "int beta = 2;")),
                Arguments.of(
                        "one configured separator between fields",
                        fieldSeparatorsSrcCode,
                        List.of("int beta = 2;", "", "int gamma = 3;")),
                Arguments.of(
                        "one separator between member groups",
                        groupSeparatorsSrcCode,
                        List.of("int b;", "", "void aMethod() {}")),
                Arguments.of(
                        "group header adjacent to its first field",
                        groupSeparatorsSrcCode,
                        List.of("// Header fields", "int a;", "int b;")),
                Arguments.of(
                        "one separator between non-empty top-level classes",
                        topLevelTypesSrcCode,
                        List.of("}", "", "class AlphaUtility {")));
    }
}

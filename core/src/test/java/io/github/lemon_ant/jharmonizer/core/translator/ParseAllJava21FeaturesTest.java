package io.github.lemon_ant.jharmonizer.core.translator;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.TEST_CASES_DIR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ParseAllJava21FeaturesTest {

    private static final PrinterConfig DEFAULT_PRINTER_CONFIG = new PrinterConfig(true, true);
    private static final int ORIGINAL_SOURCE_CODE_LENGTH = 10053;
    private static final String SAMPLE_ALL_JAVA21_RESOURCE_PATH =
            "/" + TEST_CASES_DIR + "/core/translator/valid/SampleAllJava21FeaturesList.java";
    private static final String SAMPLE_ALL_JAVA21_SOURCE_CODE =
            TestCaseResourceUtils.readClasspathResourceAsString(SAMPLE_ALL_JAVA21_RESOURCE_PATH);
    private static final Path SAMPLE_ALL_JAVA21_PSEUDO_SOURCE_PATH = Path.of("SampleAllJava21FeaturesList.java");

    @Test
    void parseSrcFile_validSampleAllJava21FeaturesList_returnExpectedParsingResult() {
        // Given
        SrcFile srcFile = createSrcFile(SAMPLE_ALL_JAVA21_SOURCE_CODE, SAMPLE_ALL_JAVA21_PSEUDO_SOURCE_PATH);

        // When
        ParsingResult parsingResult = SrcAstTranslator.parse(srcFile, DEFAULT_PRINTER_CONFIG);
        ParsingStatistic parsingStatistic = parsingResult.getParsingStatistic();

        // Then
        assertThat(parsingResult).isNotNull();
        assertThat(parsingResult.getSpoonAstModel()).isNotNull();
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        assertThat(spoonAstModel.getMainType()).isNotNull();
        assertThat(spoonAstModel.getCompilationUnit()).isNotNull();
        assertThat(parsingStatistic).isNotNull();
        assertThat(parsingStatistic.getOriginalSrcCodeLength())
                .isCloseTo(ORIGINAL_SOURCE_CODE_LENGTH, withPercentage(10));
        assertThat(parsingStatistic.getParsedRootTypesCount()).isEqualTo(8);
        assertThat(parsingStatistic.getParsedTypesTotalCount()).isEqualTo(19);
        assertThat(parsingStatistic.getParsedMembersCount()).isEqualTo(129);
        assertThat(parsingStatistic.getParsingTimeInNanos()).isGreaterThan(100000000);
    }

    @Test
    void serialize_validSpoonASTModelWithAllJava21Features_returnExpectedSrcCode() {
        // Given
        SpoonAstModel spoonASTModel = SpoonParser.parseJavaSrcFile(
                createSrcFile(SAMPLE_ALL_JAVA21_SOURCE_CODE, SAMPLE_ALL_JAVA21_PSEUDO_SOURCE_PATH),
                DEFAULT_PRINTER_CONFIG);

        // When
        SerializationResult serializationResult = SrcAstTranslator.serialize(spoonASTModel);

        // Then
        assertThat(serializationResult).isNotNull();
        assertThat(serializationResult.getSerializedSrcWithSkippedTypeRanges().getSerializedSrcCode())
                .contains("SampleAllJava21FeaturesList");
        assertThat(serializationResult.getSerializationStatistic()).isNotNull();
        SerializationStatistic serializationStatistic = serializationResult.getSerializationStatistic();
        // Use withPercentage because the serializer may add or remove a few line separators.
        assertThat(serializationStatistic.getSerializedCodeLength())
                .isCloseTo(ORIGINAL_SOURCE_CODE_LENGTH, withPercentage(10));
        assertThat(serializationStatistic.getProcessingTimeInNanos()).isGreaterThan(1000000);
    }
}

package io.github.lemon_ant.jharmonizer.core.translator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import io.github.lemon_ant.jharmonizer.core.common.SrcFile;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ParseAllJava21FeaturesTest {

    private static final int ORIGINAL_SOURCE_CODE_LENGTH = 10053;
    private static final String SAMPLE_ALL_JAVA21_RESOURCE_PATH =
            "/test-cases/core/translator/valid/SampleAllJava21FeaturesList.java";
    private static final String SAMPLE_ALL_JAVA21_SOURCE_CODE =
            TestCaseResourceUtils.readClasspathResourceAsString(SAMPLE_ALL_JAVA21_RESOURCE_PATH);
    private static final Path SAMPLE_ALL_JAVA21_PSEUDO_SOURCE_PATH = Path.of("SampleAllJava21FeaturesList.java");

    @Test
    void parseSourceFile_validSampleAllJava21FeaturesList_returnExpectedParsingResult() {
        // Given
        SrcFile srcFile = new SrcFile(SAMPLE_ALL_JAVA21_SOURCE_CODE, SAMPLE_ALL_JAVA21_PSEUDO_SOURCE_PATH);

        // When
        ParsingResult parsingResult = SourceAstTranslator.parse(srcFile);
        ParsingStatistic parsingStatistic = parsingResult.getParsingStatistic();

        // Then
        assertThat(parsingResult).isNotNull();
        assertThat(parsingResult.getSpoonAstModel()).isNotNull();
        SpoonAstModel spoonAstModel = parsingResult.getSpoonAstModel();
        assertThat(spoonAstModel.getMainType()).isNotNull();
        assertThat(spoonAstModel.getCompilationUnit()).isNotNull();
        assertThat(parsingStatistic).isNotNull();
        assertThat(parsingStatistic.getOriginalSourceCodeLength())
                .isCloseTo(ORIGINAL_SOURCE_CODE_LENGTH, withPercentage(10));
        assertThat(parsingStatistic.getParsedRootTypesCount()).isEqualTo(8);
        assertThat(parsingStatistic.getParsedTypesTotalCount()).isEqualTo(19);
        assertThat(parsingStatistic.getParsedMembersCount()).isEqualTo(129);
        assertThat(parsingStatistic.getParsingTimeInNanos()).isGreaterThan(100000000);
    }

    @Test
    void serialize_validSpoonASTModelWithAllJava21Features_returnExpectedSourceCode() {
        // Given
        SpoonAstModel spoonASTModel = SpoonParser.parseJavaSrcFile(
                new SrcFile(SAMPLE_ALL_JAVA21_SOURCE_CODE, SAMPLE_ALL_JAVA21_PSEUDO_SOURCE_PATH));

        // When
        SerializationResult serializationResult = SourceAstTranslator.serialize(spoonASTModel);

        // Then
        assertThat(serializationResult).isNotNull();
        assertThat(serializationResult
                        .getSerializedSourceWithSkippedTypeRanges()
                        .getSerializedSrcCode())
                .contains("SampleAllJava21FeaturesList");
        assertThat(serializationResult.getSerializationStatistic()).isNotNull();
        SerializationStatistic serializationStatistic = serializationResult.getSerializationStatistic();
        // Use withPercentage because the serializer may add or remove a few line separators.
        assertThat(serializationStatistic.getSerializedCodeLength())
                .isCloseTo(ORIGINAL_SOURCE_CODE_LENGTH, withPercentage(10));
        assertThat(serializationStatistic.getProcessingTimeInNanos()).isGreaterThan(1000000);
    }
}

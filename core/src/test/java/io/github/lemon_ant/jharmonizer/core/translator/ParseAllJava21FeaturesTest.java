package io.github.lemon_ant.jharmonizer.core.translator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ParseAllJava21FeaturesTest {
    private static final int ORIGINAL_SOURCE_CODE_LENGTH = 10053;
    private static final String SAMPLE_ALL_JAVA21_RESOURCE_PATH =
            "/test-cases/core/translator/valid/SampleAllJava21FeaturesList.java";
    private static final Path SAMPLE_ALL_JAVA21_PSEUDO_SOURCE_PATH = Path.of(SAMPLE_ALL_JAVA21_RESOURCE_PATH);

    @Test
    void parseSourceFile_validSampleAllJava21FeaturesList_returnExpectedParsingResult() throws Exception {
        // Given
        String sampleSourceCode = TestCaseResourceUtils.readClasspathResourceAsString(SAMPLE_ALL_JAVA21_RESOURCE_PATH);

        // When
        SrcFile srcFile = new SrcFile(sampleSourceCode, SAMPLE_ALL_JAVA21_PSEUDO_SOURCE_PATH);
        ParsingResult parsingResult = SourceAstTranslator.parseSourceFile(srcFile);
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
    void serialize_validSpoonASTModelWithAllJava21Features_returnExpectedSourceCode() throws Exception {
        // Given
        String sampleSourceCode = TestCaseResourceUtils.readClasspathResourceAsString(SAMPLE_ALL_JAVA21_RESOURCE_PATH);
        SpoonAstModel spoonASTModel =
                SpoonParser.parseJavaSourceResource(SAMPLE_ALL_JAVA21_PSEUDO_SOURCE_PATH, sampleSourceCode);

        // When
        SerializationResult serializationResult = SourceAstTranslator.serialize(spoonASTModel);

        // Then
        assertThat(serializationResult).isNotNull();
        assertThat(serializationResult.getSerializedSrcCode()).contains("SampleAllJava21FeaturesList");
        assertThat(serializationResult.getSerializationStatistic()).isNotNull();
        SerializationStatistic serializationStatistic = serializationResult.getSerializationStatistic();
        // Use withPercentage because serializator can add or remove additional new line separators
        assertThat(serializationStatistic.getSerializedCodeLength())
                .isCloseTo(ORIGINAL_SOURCE_CODE_LENGTH, withPercentage(10));
        assertThat(serializationStatistic.getProcessingTimeInNanos()).isGreaterThan(1000000);
    }
}

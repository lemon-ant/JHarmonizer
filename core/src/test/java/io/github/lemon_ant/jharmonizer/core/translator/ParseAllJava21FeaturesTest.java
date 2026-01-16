package io.github.lemon_ant.jharmonizer.core.translator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Percentage.withPercentage;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class ParseAllJava21FeaturesTest {
    private static final int ORIGINAL_SOURCE_CODE_LENGTH = 10053;
    private static final URL VALID_SAMPLE_SOURCE_CODE = Objects.requireNonNull(ParseAllJava21FeaturesTest.class
            .getClassLoader()
            .getResource("test-cases/core/parser/SampleAllJava21FeaturesList.java"));

    private SourceFilesHandler sourceFilesHandler;

    @Test
    void parseSourceFile_validSampleAllJava21FeaturesList_returnExpectedParsingResult() throws Exception {
        // When
        @NonNull Path file = Path.of(VALID_SAMPLE_SOURCE_CODE.toURI());

        SrcFile srcFile = new SrcFile(Files.readString(file, StandardCharsets.UTF_8), file);
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
        SpoonAstModel spoonASTModel = SpoonParser.parseJavaSourceResource(Path.of(VALID_SAMPLE_SOURCE_CODE.toURI()));

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

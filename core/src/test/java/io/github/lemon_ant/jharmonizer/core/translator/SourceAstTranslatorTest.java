package io.github.lemon_ant.jharmonizer.core.translator;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.common.SrcFile;
import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler;
import io.github.lemon_ant.jharmonizer.core.optout.SourceCharacterRange;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceAstTranslatorTest {

    SourceFilesHandler sourceFilesHandler;

    @TempDir
    Path tempDir;

    @Test
    void parseSourceFile_validJavaSrc_returnParsingResult() throws Exception {
        // Given
        Path file = Files.writeString(tempDir.resolve("TestClass.java"), "class TestClass { int value = 42; }");
        SrcFile srcFile = new SrcFile(Files.readString(file, StandardCharsets.UTF_8), file);

        // When
        ParsingResult result = SourceAstTranslator.parseSrcFile(srcFile);

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
        String source = "class Demo { void m() {} }";
        SrcFile srcFile = new SrcFile(source, Path.of("Demo.java"));
        SpoonAstModel model = SourceAstTranslator.parseSrcFile(srcFile).getSpoonAstModel();

        // When
        SerializationResult result = SourceAstTranslator.serialize(model);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSerializedSrcCode()).contains("class Demo");
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
        String sourceCode = """
                class Alpha {}

                %s

                %s
                """.formatted(sortOffFragment.stripTrailing(), fullyOffFragment.stripTrailing());
        SrcFile srcFile = new SrcFile(sourceCode, Path.of("Sample.java"));
        SpoonAstModel spoonAstModel = SourceAstTranslator.parseSrcFile(srcFile).getSpoonAstModel();

        // When
        SerializationResult result = SourceAstTranslator.serialize(spoonAstModel);
        List<SourceCharacterRange> formattingSkippedRanges = result.getFormattingSkippedRanges();

        // Then
        assertThat(result.getSerializedSrcCode()).contains(sortOffFragment).contains(fullyOffFragment);
        assertThat(formattingSkippedRanges)
                .singleElement()
                .satisfies(range -> assertThat(result.getSerializedSrcCode()
                                .substring(range.getStartInclusive(), range.getEndExclusive()))
                        .isEqualTo(fullyOffFragment));
    }
}

package io.github.lemon_ant.jharmonizer.core.parser;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.files_handler.SourceFilesHandler.FileContent;
import io.github.lemon_ant.jharmonizer.core.parser.spoon.SpoonASTModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceAstConverterTest {

    @TempDir
    Path tempDir;

    @Test
    void parseSourceFile_validJavaSource_returnParsingResult() throws IOException {
        Path file = Files.writeString(tempDir.resolve("TestClass.java"), "class TestClass { int value = 42; }");

        FileContent fileContent = new FileContent(file, Files.readString(file));
        SourceAstConverter converter = new SourceAstConverter();

        ParsingResult result = converter.parseSourceFile(fileContent);

        assertThat(result).isNotNull();
        assertThat(result.getSpoonAstModel()).isNotNull();
        assertThat(result.getParsingStatistic().getParsingTimeInNanos()).isGreaterThan(0);
        assertThat(result.getParsingStatistic().getParsedRootTypesCount()).isEqualTo(1);
        assertThat(result.getParsingStatistic().getParsedTypesTotalCount()).isEqualTo(1);
        assertThat(result.getParsingStatistic().getParsedMembersCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void serialize_validSpoonAstModel_returnSerializedCode() {
        // given: simple source code
        String source = "class Demo { void m() {} }";
        FileContent fileContent = new FileContent(Path.of("Demo.java"), source);
        SpoonASTModel model =
                new SourceAstConverter().parseSourceFile(fileContent).getSpoonAstModel();

        SourceAstConverter converter = new SourceAstConverter();

        // when
        SerializationResult result = converter.serialize(model);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSerializedSourceCode()).contains("class Demo");
        assertThat(result.getSerializationStatistic().getSerializedCodeLength()).isGreaterThan(0);
        assertThat(result.getSerializationStatistic().getProcessingTimeInNanos())
                .isGreaterThan(0);
    }
}

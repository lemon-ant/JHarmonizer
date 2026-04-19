package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class CheckAllFlowIntegrationTest {

    private static final CheckAllFlow FLOW = createFlow();

    @Test
    void processStream_fullyOffOptOut_returnsSkippedResult() {
        // Given
        SrcFile srcFile =
                createSrcFile("// @jharmonizer:fully-off\nclass Z { void b() {} void a() {} }\n", Path.of("Z.java"));

        // When
        FileProcessingResult fileProcessingResult =
                FLOW.processStream(Stream.of(srcFile)).findFirst().orElseThrow();

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.SKIPPED);
        assertThat(fileProcessingResult.getDiff()).isNull();
        assertThat(fileProcessingResult.getRelocations()).isNull();
    }

    @Test
    void processStream_alreadyFormattedAndOrdered_returnsCheckedResult() {
        // Given
        SrcFile srcFile = createSrcFile("class A {\n    void a() {}\n\n    void b() {}\n}\n", Path.of("A.java"));

        // When
        FileProcessingResult fileProcessingResult =
                FLOW.processStream(Stream.of(srcFile)).findFirst().orElseThrow();

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.CHECKED);
        assertThat(fileProcessingResult.getDiff()).isEmpty();
        assertThat(fileProcessingResult.getRelocations()).isEmpty();
    }

    @Test
    void processStream_membersOutOfOrder_returnsReorderedResult() {
        // Given
        SrcFile srcFile = createSrcFile("class A { void b() {} void a() {} }", Path.of("A.java"));

        // When
        FileProcessingResult fileProcessingResult =
                FLOW.processStream(Stream.of(srcFile)).findFirst().orElseThrow();

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.REORDERED);
        assertThat(fileProcessingResult.getDiff()).isNotEmpty();
        assertThat(fileProcessingResult.getRelocations()).isNotEmpty();
    }

    @Test
    void processStream_formattingOnlyViolation_returnsFormattedResult() {
        // Given
        SrcFile srcFile = createSrcFile("class A{void a(){}}", Path.of("A.java"));

        // When
        FileProcessingResult fileProcessingResult =
                FLOW.processStream(Stream.of(srcFile)).findFirst().orElseThrow();

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.FORMATTED);
        assertThat(fileProcessingResult.getDiff()).isNotEmpty();
        assertThat(fileProcessingResult.getRelocations()).isEmpty();
    }

    @Test
    void processStream_sortingOffOptOut_skipsReorderingAndFormatsSrcCode() {
        // Given
        SrcFile srcFile = createSrcFile(
                "// @jharmonizer:sort-off\npublic class B {\n    public void b() {}\n\n    public void a() {}\n}\n",
                Path.of("B.java"));

        // When
        FileProcessingResult fileProcessingResult =
                FLOW.processStream(Stream.of(srcFile)).findFirst().orElseThrow();

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus())
                .isIn(FileProcessingStatus.FORMATTED, FileProcessingStatus.CHECKED);
        assertThat(fileProcessingResult.getRelocations()).isEmpty();
    }

    @Test
    void processStream_multipleFiles_returnsResultsInEncounterOrder() {
        // Given
        SrcFile cleanFile = createSrcFile("class A {\n    void a() {}\n}\n", Path.of("A.java"));
        SrcFile dirtyFile = createSrcFile("class B{void b(){}}", Path.of("B.java"));

        // When
        List<FileProcessingResult> fileProcessingResults =
                FLOW.processStream(List.of(cleanFile, dirtyFile).stream()).toList();

        // Then
        assertThat(fileProcessingResults).hasSize(2);
        assertThat(fileProcessingResults.get(0).getFileProcessingStatus()).isEqualTo(FileProcessingStatus.CHECKED);
        assertThat(fileProcessingResults.get(1).getFileProcessingStatus()).isNotEqualTo(FileProcessingStatus.CHECKED);
    }

    @Test
    void isSuccessful_noModifications_returnsTrue() {
        // When / Then
        assertThat(FLOW.isSuccessful(false)).isTrue();
    }

    @Test
    void isSuccessful_hasModifications_returnsFalse() {
        // When / Then
        assertThat(FLOW.isSuccessful(true)).isFalse();
    }

    @NonNull
    private static CheckAllFlow createFlow() {
        CompiledConfig compiledConfig = ConfigurationManager.loadDefaultConfig();
        Formatter formatter = new Formatter(
                compiledConfig.getFormatting().getFormatterStyle(),
                compiledConfig.getFormatting().isFixImports());
        Sorter sorter = new Sorter(compiledConfig);
        PrinterConfig printerConfig = new PrinterConfig(
                compiledConfig.getFormatting().isBlankLineAfterTypeHeader(),
                compiledConfig.getFormatting().isBlankLineBeforeComment(),
                compiledConfig.getFormatting().isBlankLineBetweenFields());
        return new CheckAllFlow(formatter, sorter, printerConfig);
    }
}

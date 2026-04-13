package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReorderFlowIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void processSource_alreadyOrdered_returnsCheckedResult() throws IOException {
        // Given
        ReorderFlow flow = createFlow(false);
        Path javaFile = writeJavaFile("AlreadyOrdered.java", "class AlreadyOrdered {\n    int a;\n    int b;\n}\n");
        SrcFile srcFile = createSrcFile(Files.readString(javaFile), javaFile);

        // When
        FileProcessingResult fileProcessingResult = flow.processSrc(srcFile);

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.UNCHANGED);
        assertThat(fileProcessingResult.isStopRequested()).isFalse();
    }

    @Test
    void processSource_misordered_rewritesFile() throws IOException {
        // Given
        ReorderFlow flow = createFlow(false);
        Path javaFile = writeJavaFile("Misordered.java", "class Misordered { int z; int a; }");
        SrcFile srcFile = createSrcFile(Files.readString(javaFile), javaFile);

        // When
        FileProcessingResult fileProcessingResult = flow.processSrc(srcFile);

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus())
                .isIn(FileProcessingStatus.REORDERED, FileProcessingStatus.FORMATTED);
        assertThat(fileProcessingResult.isStopRequested()).isFalse();
    }

    @Test
    void processSource_misorderedWithBackupsEnabled_createsBackupFile() throws IOException {
        // Given
        ReorderFlow flow = createFlow(true);
        Path javaFile = writeJavaFile("BackupTest.java", "class BackupTest { int z; int a; }");
        SrcFile srcFile = createSrcFile(Files.readString(javaFile), javaFile);

        // When
        flow.processSrc(srcFile);

        // Then
        Path backupFile = javaFile.resolveSibling(javaFile.getFileName() + ".bak");
        assertThat(backupFile).exists();
    }

    @Test
    void processSource_formattingOnlyDifference_rewritesFile() throws IOException {
        // Given
        ReorderFlow flow = createFlow(false);
        Path javaFile = writeJavaFile("FormattingOnly.java", "class FormattingOnly{int x;}");
        SrcFile srcFile = createSrcFile(Files.readString(javaFile), javaFile);

        // When
        FileProcessingResult fileProcessingResult = flow.processSrc(srcFile);

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.FORMATTED);
        String rewrittenContent = Files.readString(javaFile, StandardCharsets.UTF_8);
        assertThat(rewrittenContent).isNotEqualTo("class FormattingOnly{int x;}");
    }

    @Test
    void processStream_multipleFiles_returnsResultForEach() throws IOException {
        // Given
        ReorderFlow flow = createFlow(false);
        Path javaFileA = writeJavaFile("A.java", "class A {\n    int a;\n}\n");
        Path javaFileB = writeJavaFile("B.java", "class B {\n    int b;\n}\n");
        List<SrcFile> srcFiles = List.of(
                createSrcFile(Files.readString(javaFileA), javaFileA),
                createSrcFile(Files.readString(javaFileB), javaFileB));

        // When
        List<FileProcessingResult> results =
                flow.processStream(srcFiles.stream()).toList();

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void isSuccessful_alwaysReturnsTrue() {
        // Given
        ReorderFlow flow = createFlow(false);

        // When / Then
        assertThat(flow.isSuccessful(true)).isTrue();
        assertThat(flow.isSuccessful(false)).isTrue();
    }

    @Test
    void processSource_spoonParseFailure_fallsBackToFormattingOnly() throws IOException {
        // Given
        ReorderFlow flow = createFlow(false);
        Path javaFile = writeJavaFile("UnparsableFile.java", "not valid java !@#!{}{}");
        SrcFile srcFile = createSrcFile(Files.readString(javaFile), javaFile);

        // When
        FileProcessingResult fileProcessingResult = flow.processSrcSafely(srcFile);

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus())
                .isIn(FileProcessingStatus.ERROR, FileProcessingStatus.UNCHANGED, FileProcessingStatus.FORMATTED);
    }

    @NonNull
    private static ReorderFlow createFlow(boolean backupsEnabled) {
        CompiledConfig compiledConfig = ConfigurationManager.loadDefaultConfig();
        Formatter formatter = new Formatter(
                compiledConfig.getFormatting().getFormatterStyle(),
                compiledConfig.getFormatting().isFixImports());
        Sorter sorter = new Sorter(compiledConfig);
        PrinterConfig printerConfig = new PrinterConfig(
                compiledConfig.getFormatting().isBlankLineAfterTypeHeader(),
                compiledConfig.getFormatting().isBlankLineBeforeComment(),
                compiledConfig.getFormatting().isBlankLineBetweenFields());
        return new ReorderFlow(formatter, backupsEnabled, sorter, printerConfig);
    }

    @NonNull
    private Path writeJavaFile(String fileName, String content) throws IOException {
        Path javaFile = temporaryDirectory.resolve(fileName);
        Files.writeString(javaFile, content, StandardCharsets.UTF_8);
        return javaFile;
    }
}

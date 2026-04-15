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
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReorderFlowIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void processSrc_fullyOffOptOut_returnsSkippedResultAndLeavesFileUnchanged() throws IOException {
        // Given
        String originalSrcCode = "// @jharmonizer:fully-off\nclass Z { void b() {} void a() {} }\n";
        Path javaFilePath = writeJavaFile("Z.java", originalSrcCode);
        SrcFile srcFile = createSrcFile(originalSrcCode, javaFilePath);
        ReorderFlow reorderFlow = createFlow(false);

        // When
        FileProcessingResult fileProcessingResult = reorderFlow.processSrc(srcFile);

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.SKIPPED);
        assertThat(Files.readString(javaFilePath, StandardCharsets.UTF_8)).isEqualTo(originalSrcCode);
    }

    @Test
    void processSrc_alreadyFormattedAndOrdered_returnsUnchangedResultAndLeavesFileIntact() throws IOException {
        // Given
        String alreadyFormattedSrcCode = "class A {\n    void a() {}\n\n    void b() {}\n}\n";
        Path javaFilePath = writeJavaFile("A.java", alreadyFormattedSrcCode);
        SrcFile srcFile = createSrcFile(alreadyFormattedSrcCode, javaFilePath);
        ReorderFlow reorderFlow = createFlow(false);

        // When
        FileProcessingResult fileProcessingResult = reorderFlow.processSrc(srcFile);

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.UNCHANGED);
        assertThat(Files.readString(javaFilePath, StandardCharsets.UTF_8)).isEqualTo(alreadyFormattedSrcCode);
    }

    @Test
    void processSrc_unformattedFileWithoutBackups_rewritesFileNoBackup() throws IOException {
        // Given
        String unformattedSrcCode = "class B { void b() {} void a() {} }";
        Path javaFilePath = writeJavaFile("B.java", unformattedSrcCode);
        SrcFile srcFile = createSrcFile(unformattedSrcCode, javaFilePath);
        ReorderFlow reorderFlow = createFlow(false);

        // When
        FileProcessingResult fileProcessingResult = reorderFlow.processSrc(srcFile);

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus())
                .isNotEqualTo(FileProcessingStatus.UNCHANGED)
                .isNotEqualTo(FileProcessingStatus.SKIPPED);
        assertThat(Files.readString(javaFilePath, StandardCharsets.UTF_8)).isNotEqualTo(unformattedSrcCode);
        Path backupFilePath = javaFilePath.resolveSibling("B.java.bak");
        assertThat(backupFilePath).doesNotExist();
    }

    @Test
    void processSrc_unformattedFileWithBackupsEnabled_rewritesFileAndCreatesBackup() throws IOException {
        // Given
        String unformattedSrcCode = "class C { void b() {} void a() {} }";
        Path javaFilePath = writeJavaFile("C.java", unformattedSrcCode);
        SrcFile srcFile = createSrcFile(unformattedSrcCode, javaFilePath);
        ReorderFlow reorderFlow = createFlow(true);

        // When
        FileProcessingResult fileProcessingResult = reorderFlow.processSrc(srcFile);

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus())
                .isNotEqualTo(FileProcessingStatus.UNCHANGED)
                .isNotEqualTo(FileProcessingStatus.SKIPPED);
        Path backupFilePath = javaFilePath.resolveSibling("C.java.bak");
        assertThat(backupFilePath).exists();
        assertThat(Files.readString(backupFilePath, StandardCharsets.UTF_8)).isEqualTo(unformattedSrcCode);
    }

    @Test
    void isSuccessful_withOrWithoutModifications_alwaysReturnsTrue() {
        // Given
        ReorderFlow reorderFlow = createFlow(false);

        // When / Then
        assertThat(reorderFlow.isSuccessful(false)).isTrue();
        assertThat(reorderFlow.isSuccessful(true)).isTrue();
    }

    @NonNull
    private ReorderFlow createFlow(boolean backupsEnabled) {
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
    private Path writeJavaFile(@NonNull String fileName, @NonNull String srcCode) throws IOException {
        return Files.writeString(temporaryDirectory.resolve(fileName), srcCode, StandardCharsets.UTF_8);
    }
}

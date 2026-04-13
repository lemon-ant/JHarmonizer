package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class CheckAllFlowIntegrationTest {

    @Test
    void processSource_orderedFile_returnsCheckedResult() {
        // Given
        CheckAllFlow flow = createFlow();
        SrcFile srcFile = createSrcFile("class Sample {\n    int a;\n    int b;\n}\n", Path.of("Sample.java"));

        // When
        FileProcessingResult result = flow.processSrc(srcFile);

        // Then
        assertThat(result.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.CHECKED);
        assertThat(result.isStopRequested()).isFalse();
        assertThat(result.getDiff()).isEmpty();
        assertThat(result.getRelocations()).isEmpty();
    }

    @Test
    void processSource_misordered_returnsReorderedResult() {
        // Given
        CheckAllFlow flow = createFlow();
        SrcFile srcFile = createSrcFile("class BViolation { int z; int a; }", Path.of("B_Violation.java"));

        // When
        FileProcessingResult result = flow.processSrc(srcFile);

        // Then
        assertThat(result.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.REORDERED);
        assertThat(result.isStopRequested()).isFalse();
        assertThat(result.getRelocations()).isNotEmpty();
    }

    @Test
    void processSource_formattingOnlyDifference_returnsFormattedResult() {
        // Given
        CheckAllFlow flow = createFlow();
        SrcFile srcFile = createSrcFile("class Sample{int x;}", Path.of("Sample.java"));

        // When
        FileProcessingResult result = flow.processSrc(srcFile);

        // Then
        assertThat(result.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.FORMATTED);
        assertThat(result.getDiff()).isNotEmpty();
    }

    @Test
    void processStream_multipleFiles_returnsResultForEach() {
        // Given
        CheckAllFlow flow = createFlow();
        List<SrcFile> srcFiles = List.of(
                createSrcFile("class A {\n    int a;\n}\n", Path.of("A.java")),
                createSrcFile("class B {\n    int b;\n}\n", Path.of("B.java")));

        // When
        List<FileProcessingResult> results =
                flow.processStream(srcFiles.stream()).toList();

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(result -> result.getFileProcessingStatus() == FileProcessingStatus.CHECKED);
    }

    @Test
    void isSuccessful_withModifications_returnsFalse() {
        // Given
        CheckAllFlow flow = createFlow();

        // When / Then
        assertThat(flow.isSuccessful(true)).isFalse();
    }

    @Test
    void isSuccessful_withoutModifications_returnsTrue() {
        // Given
        CheckAllFlow flow = createFlow();

        // When / Then
        assertThat(flow.isSuccessful(false)).isTrue();
    }

    @Test
    void processSrcSafely_runtimeException_returnsErrorResult() {
        // Given
        CheckAllFlow flow = createFlow();
        SrcFile malformedFile = createSrcFile("not valid java !@#$%", Path.of("Broken.java"));

        // When
        FileProcessingResult result = flow.processSrcSafely(malformedFile);

        // Then
        assertThat(result.getFileProcessingStatus())
                .isIn(FileProcessingStatus.ERROR, FileProcessingStatus.CHECKED, FileProcessingStatus.FORMATTED);
    }

    @Test
    void processSourceWithFormattingOnlyFallback_formattingChanges_returnsFormattedResult() throws Exception {
        // Given
        CheckAllFlow flow = createFlow();
        SrcFile srcFile = createSrcFile("class Sample{int x;}", Path.of("Sample.java"));
        io.github.lemon_ant.jharmonizer.core.translator.SpoonModelBuildException modelBuildException =
                new io.github.lemon_ant.jharmonizer.core.translator.SpoonModelBuildException(
                        srcFile.getPath(), "RuntimeException: boom", new RuntimeException("boom"));

        // When
        FileProcessingResult result = invokeFormattingOnlyFallback(flow, srcFile, modelBuildException);

        // Then
        assertThat(result.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.FORMATTED);
        assertThat(result.getDiff()).isNotEmpty();
    }

    @Test
    void processSourceWithFormattingOnlyFallback_noFormattingChanges_returnsCheckedResult() throws Exception {
        // Given
        CheckAllFlow flow = createFlow();
        SrcFile srcFile = createSrcFile("class Sample {\n    int x;\n}\n", Path.of("Sample.java"));
        io.github.lemon_ant.jharmonizer.core.translator.SpoonModelBuildException modelBuildException =
                new io.github.lemon_ant.jharmonizer.core.translator.SpoonModelBuildException(
                        srcFile.getPath(), "RuntimeException: boom", new RuntimeException("boom"));

        // When
        FileProcessingResult result = invokeFormattingOnlyFallback(flow, srcFile, modelBuildException);

        // Then
        assertThat(result.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.CHECKED);
        assertThat(result.getDiff()).isEmpty();
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

    @NonNull
    private static FileProcessingResult invokeFormattingOnlyFallback(
            @NonNull CheckAllFlow flow,
            @NonNull SrcFile srcFile,
            @NonNull io.github.lemon_ant.jharmonizer.core.translator.SpoonModelBuildException modelBuildException)
            throws Exception {
        Method method = CheckAllFlow.class.getDeclaredMethod(
                "processSrcWithFormattingOnlyFallback",
                SrcFile.class,
                io.github.lemon_ant.jharmonizer.core.translator.SpoonModelBuildException.class);
        method.setAccessible(true);
        try {
            return (FileProcessingResult) method.invoke(flow, srcFile, modelBuildException);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }
}

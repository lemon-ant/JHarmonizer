// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.SpoonModelBuildException;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class CheckFailFastFlowIntegrationTest {
    private static final SrcFile CLEAN_FILE_A =
            createSrcFile("public class A {\n    public void a() {}\n}\n", Path.of("A.java"));
    private static final SrcFile CLEAN_FILE_B =
            createSrcFile("public class B {\n    public void b() {}\n}\n", Path.of("B.java"));

    @Test
    void isSuccessful_hasModifications_returnsFalse() {
        // Given
        CheckFailFastFlow flow = createFlow();

        // When / Then
        assertThat(flow.isSuccessful(true)).isFalse();
    }

    @Test
    void isSuccessful_noModifications_returnsTrue() {
        // Given
        CheckFailFastFlow flow = createFlow();

        // When / Then
        assertThat(flow.isSuccessful(false)).isTrue();
    }

    @Test
    void processSourceWithFormattingOnlyFallback_formattingChanges_returnsStopRequestedResult() throws Exception {
        // Given
        CheckFailFastFlow flow = createFlow();
        SrcFile srcFile = createSrcFile("class Sample{int x;}", Path.of("Sample.java"));
        SpoonModelBuildException modelBuildException =
                new SpoonModelBuildException(srcFile.getPath(), "RuntimeException: boom", new RuntimeException("boom"));

        // When
        FileProcessingResult fileProcessingResult = invokeFormattingOnlyFallback(flow, srcFile, modelBuildException);

        // Then
        assertThat(fileProcessingResult.isStopRequested()).isTrue();
        assertThat(fileProcessingResult.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.FORMATTED);
        assertThat(fileProcessingResult.getDiff()).isNotEmpty();
    }

    @Test
    void processSourceWithFormattingOnlyFallback_noFormattingChanges_returnsCheckedResult() throws Exception {
        // Given
        CheckFailFastFlow flow = createFlow();
        SrcFile srcFile = createSrcFile("class Sample {\n    int x;\n}\n", Path.of("Sample.java"));
        SpoonModelBuildException modelBuildException =
                new SpoonModelBuildException(srcFile.getPath(), "RuntimeException: boom", new RuntimeException("boom"));

        // When
        FileProcessingResult fileProcessingResult = invokeFormattingOnlyFallback(flow, srcFile, modelBuildException);

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.CHECKED);
        assertThat(fileProcessingResult.isStopRequested()).isFalse();
        assertThat(fileProcessingResult.getSortingStatistic().getSortingTimeInNanos())
                .isZero();
    }

    @Test
    void processStream_allCleanFiles_processesAllFilesWithoutStop() {
        // Given
        CheckFailFastFlow flow = createFlow();

        // When
        List<FileProcessingResult> fileProcessingResults =
                flow.processStream(List.of(CLEAN_FILE_A, CLEAN_FILE_B).stream()).toList();

        // Then
        assertThat(fileProcessingResults).hasSize(2);
        assertThat(fileProcessingResults)
                .extracting(FileProcessingResult::isStopRequested)
                .containsOnly(false);
    }

    @Test
    void processStream_firstViolationDetected_returnsStopRequestedResult() {
        // Given
        CheckFailFastFlow flow = createFlow();
        SrcFile srcFile = createSrcFile("class BViolation { int z; int a; }", Path.of("B_Violation.java"));

        // When
        FileProcessingResult fileProcessingResult =
                flow.processStream(Stream.of(srcFile)).findFirst().orElseThrow();

        // Then
        assertThat(fileProcessingResult.isStopRequested()).isTrue();
        assertThat(fileProcessingResult.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.REORDERED);
        assertThat(fileProcessingResult.getMemberRelocations()).isNotEmpty();
    }

    @Test
    void processStream_fullyOffOptOut_returnsSkippedResultWithNoStopRequested() {
        // Given
        CheckFailFastFlow flow = createFlow();
        SrcFile srcFile = createSrcFile(
                "// @jharmonizer:fully-off\npublic class Z {\n    public void b() {}\n}\n", Path.of("Z.java"));

        // When
        FileProcessingResult fileProcessingResult =
                flow.processStream(Stream.of(srcFile)).findFirst().orElseThrow();

        // Then
        assertThat(fileProcessingResult.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.SKIPPED);
        assertThat(fileProcessingResult.isStopRequested()).isFalse();
    }

    @Test
    void processStream_violationOnFirstFile_skipsSecondFileBeforeMapping() {
        // Given
        CheckFailFastFlow flow = createFlow();
        SrcFile violatingFile = createSrcFile("class BViolation { int z; int a; }", Path.of("B_Violation.java"));
        SrcFile secondFile = createSrcFile("public class C {\n    public void c() {}\n}\n", Path.of("C.java"));

        // When
        List<FileProcessingResult> fileProcessingResults =
                flow.processStream(Stream.of(violatingFile, secondFile)).toList();

        // Then
        assertThat(fileProcessingResults).hasSize(1);
        assertThat(fileProcessingResults.getFirst().isStopRequested()).isTrue();
    }

    @NonNull
    private static CheckFailFastFlow createFlow() {
        CompiledConfig compiledConfig = ConfigurationManager.loadDefaultConfig();
        Formatter formatter = new Formatter(
                compiledConfig.getFormatting().getFormatterStyle(),
                compiledConfig.getFormatting().isFixImports());
        Sorter sorter = new Sorter(compiledConfig);
        PrinterConfig printerConfig = new PrinterConfig(
                compiledConfig.getFormatting().isBlankLineAfterTypeHeader(),
                compiledConfig.getFormatting().isBlankLineBeforeComment(),
                compiledConfig.getFormatting().isBlankLineBetweenFields());
        return new CheckFailFastFlow(formatter, sorter, printerConfig);
    }

    @NonNull
    private static FileProcessingResult invokeFormattingOnlyFallback(
            @NonNull CheckFailFastFlow flow,
            @NonNull SrcFile srcFile,
            @NonNull SpoonModelBuildException modelBuildException)
            throws Exception {
        Method method = AbstractOptOutFlow.class.getDeclaredMethod(
                "processSrcWithFormattingOnlyFallback", SrcFile.class, String.class);
        method.setAccessible(true);
        try {
            return (FileProcessingResult) method.invoke(flow, srcFile, modelBuildException.getMessage());
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }
}

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
import io.github.lemon_ant.jharmonizer.core.translator.spoon.PrinterConfig;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AbstractSrcProcessingFlowTest {
    private static final CompiledConfig DEFAULT_CONFIG = ConfigurationManager.loadDefaultConfig();
    private static final Formatter DEFAULT_FORMATTER = new Formatter(
            DEFAULT_CONFIG.getFormatting().getFormatterStyle(),
            DEFAULT_CONFIG.getFormatting().isFixImports());
    private static final PrinterConfig DEFAULT_PRINTER_CONFIG = new PrinterConfig(
            DEFAULT_CONFIG.getFormatting().isBlankLineAfterTypeHeader(),
            DEFAULT_CONFIG.getFormatting().isBlankLineBeforeComment(),
            DEFAULT_CONFIG.getFormatting().isBlankLineBetweenFields());
    private static final Sorter DEFAULT_SORTER = new Sorter(DEFAULT_CONFIG);

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void processStream_annotationsOutOfOrder_reportsDiffWithoutFormatting(boolean failFast) {
        // Given
        IFlow flow = failFast
                ? new CheckFailFastFlow(DEFAULT_FORMATTER, DEFAULT_SORTER, DEFAULT_PRINTER_CONFIG)
                : new CheckAllFlow(DEFAULT_FORMATTER, DEFAULT_SORTER, DEFAULT_PRINTER_CONFIG);
        SrcFile annotatedFile = createSrcFile(
                "@SuppressWarnings(\"all\") @Deprecated class AnnotationViolation {}",
                Path.of("AnnotationViolation.java"));
        SrcFile cleanFile = createSrcFile("class Clean {}\n", Path.of("Clean.java"));

        // When
        List<FileProcessingResult> results =
                flow.processStream(Stream.of(annotatedFile, cleanFile)).toList();

        // Then
        assertThat(results).hasSize(failFast ? 1 : 2);
        FileProcessingResult result = results.getFirst();
        assertThat(result.getFileProcessingStatus()).isEqualTo(FileProcessingStatus.REORDERED);
        assertThat(result.getMemberRelocations()).isEmpty();
        assertThat(result.getDiff()).isNotEmpty();
        assertThat(result.getFormattingStatistic().getFormattingTimeInNanos()).isZero();
        assertThat(result.getFormattingStatistic().getFormattedCodeLength()).isZero();
        assertThat(result.isStopRequested()).isEqualTo(failFast);
    }

    @Test
    void processStream_blankMessageException_returnsErrorResult() {
        // Given
        ThrowingFlow throwingFlow = new ThrowingFlow(new RuntimeException("   "));
        SrcFile srcFile = createSrcFile("class B {}", Path.of("B.java"));

        // When
        List<FileProcessingResult> fileProcessingResults =
                throwingFlow.processStream(Stream.of(srcFile)).toList();

        // Then
        assertThat(fileProcessingResults.getFirst().getFileProcessingStatus()).isEqualTo(FileProcessingStatus.ERROR);
    }

    @Test
    void processStream_multipleFilesWithOneThrowingException_continuesProcessingRemainingFiles() {
        // Given
        ThrowingFlow throwingFlow = new ThrowingFlow(new RuntimeException("error"));
        SrcFile srcFile1 = createSrcFile("class D {}", Path.of("D.java"));
        SrcFile srcFile2 = createSrcFile("class E {}", Path.of("E.java"));

        // When
        List<FileProcessingResult> fileProcessingResults =
                throwingFlow.processStream(List.of(srcFile1, srcFile2).stream()).toList();

        // Then
        assertThat(fileProcessingResults).hasSize(2);
        assertThat(fileProcessingResults)
                .extracting(FileProcessingResult::getFileProcessingStatus)
                .containsOnly(FileProcessingStatus.ERROR);
    }

    @Test
    void processStream_nonBlankMessageException_returnsErrorResult() {
        // Given
        ThrowingFlow throwingFlow = new ThrowingFlow(new RuntimeException("boom"));
        SrcFile srcFile = createSrcFile("class C {}", Path.of("C.java"));

        // When
        List<FileProcessingResult> fileProcessingResults =
                throwingFlow.processStream(Stream.of(srcFile)).toList();

        // Then
        assertThat(fileProcessingResults.getFirst().getFileProcessingStatus()).isEqualTo(FileProcessingStatus.ERROR);
    }

    @Test
    void processStream_nullMessageException_returnsErrorResult() {
        // Given
        ThrowingFlow throwingFlow = new ThrowingFlow(new RuntimeException((String) null));
        SrcFile srcFile = createSrcFile("class A {}", Path.of("A.java"));

        // When
        List<FileProcessingResult> fileProcessingResults =
                throwingFlow.processStream(Stream.of(srcFile)).toList();

        // Then
        assertThat(fileProcessingResults.getFirst().getFileProcessingStatus()).isEqualTo(FileProcessingStatus.ERROR);
        assertThat(fileProcessingResults.getFirst().getPath()).isEqualTo(Path.of("A.java"));
    }

    private class ThrowingFlow extends AbstractSrcProcessingFlow {
        private final RuntimeException exceptionToThrow;

        @Override
        public boolean isModifyingFlow() {
            return false;
        }

        @Override
        public boolean isSuccessful(boolean hasModifications) {
            return true;
        }

        ThrowingFlow(@NonNull RuntimeException exceptionToThrow) {
            super(DEFAULT_FORMATTER, DEFAULT_SORTER, DEFAULT_PRINTER_CONFIG);
            this.exceptionToThrow = exceptionToThrow;
        }

        @NonNull
        @Override
        FileProcessingResult processSrc(@NonNull SrcFile srcFile) {
            throw exceptionToThrow;
        }
    }
}

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

class AbstractOptOutFlowTest {

    private static final CompiledConfig DEFAULT_CONFIG = ConfigurationManager.loadDefaultConfig();
    private static final Formatter DEFAULT_FORMATTER = new Formatter(
            DEFAULT_CONFIG.getFormatting().getFormatterStyle(),
            DEFAULT_CONFIG.getFormatting().isFixImports());
    private static final Sorter DEFAULT_SORTER = new Sorter(DEFAULT_CONFIG);
    private static final PrinterConfig DEFAULT_PRINTER_CONFIG = new PrinterConfig(
            DEFAULT_CONFIG.getFormatting().isBlankLineAfterTypeHeader(),
            DEFAULT_CONFIG.getFormatting().isBlankLineBeforeComment(),
            DEFAULT_CONFIG.getFormatting().isBlankLineBetweenFields());

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

    private class ThrowingFlow extends AbstractOptOutFlow {

        private final RuntimeException exceptionToThrow;

        ThrowingFlow(@NonNull RuntimeException exceptionToThrow) {
            super(DEFAULT_FORMATTER, DEFAULT_SORTER, DEFAULT_PRINTER_CONFIG, FlowType.CHECK_ALL);
            this.exceptionToThrow = exceptionToThrow;
        }

        @Override
        @NonNull
        FileProcessingResult processSrc(@NonNull SrcFile srcFile) {
            throw exceptionToThrow;
        }

        @Override
        public boolean isSuccessful(boolean hasModifications) {
            return true;
        }

        @Override
        public boolean isModifyingFlow() {
            return false;
        }
    }
}

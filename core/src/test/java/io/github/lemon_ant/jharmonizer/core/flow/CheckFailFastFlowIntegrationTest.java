package io.github.lemon_ant.jharmonizer.core.flow;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.translator.SpoonModelBuildException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class CheckFailFastFlowIntegrationTest {

    @Test
    void processSource_firstViolationDetected_returnsStopRequestedResult() {
        // Given
        CheckFailFastFlow flow = createFlow();
        SrcFile srcFile = createSrcFile("class BViolation { int z; int a; }", Path.of("B_Violation.java"));

        // When
        FlowProcessingResult result = flow.processSrc(srcFile);

        // Then
        assertThat(result.isStopRequested()).isTrue();
        assertThat(result.getFlowProcessingStatus()).isEqualTo(FlowProcessingStatus.REORDERED);
        assertThat(result.getRelocations()).isNotEmpty();
    }

    @Test
    void processSource_firstViolationWithStopFlag_stopsSequentialProcessing() {
        // Given
        CheckFailFastFlow flow = createFlow();
        List<SrcFile> srcFiles = List.of(
                createSrcFile("class BViolation { int z; int a; }", Path.of("B_Violation.java")),
                createSrcFile("class CFormatted{int a;}", Path.of("C_Formatted.java")));
        AtomicInteger processedSources = new AtomicInteger();

        // When
        List<FlowProcessingResult> results = srcFiles.stream()
                .map(srcFile -> {
                    processedSources.incrementAndGet();
                    return flow.processSrc(srcFile);
                })
                .takeWhile(result -> !result.isStopRequested())
                .toList();

        // Then
        assertThat(processedSources).hasValue(1);
        assertThat(results).isEmpty();
    }

    @Test
    void processSourceWithFormattingOnlyFallback_formattingChanges_returnsStopRequestedResult() throws Exception {
        // Given
        CheckFailFastFlow flow = createFlow();
        SrcFile srcFile = createSrcFile("class Sample{int x;}", Path.of("Sample.java"));
        SpoonModelBuildException modelBuildException =
                new SpoonModelBuildException(srcFile.getPath(), "RuntimeException: boom", new RuntimeException("boom"));

        // When
        FlowProcessingResult result = invokeFormattingOnlyFallback(flow, srcFile, modelBuildException);

        // Then
        assertThat(result.isStopRequested()).isTrue();
        assertThat(result.getFlowProcessingStatus()).isEqualTo(FlowProcessingStatus.FORMATTED);
        assertThat(result.getDiff()).isNotEmpty();
    }

    @Test
    void processSourceWithFormattingOnlyFallback_noFormattingChanges_returnsCheckedResult() throws Exception {
        // Given
        CheckFailFastFlow flow = createFlow();
        SrcFile srcFile = createSrcFile("class Sample {\n    int x;\n}\n", Path.of("Sample.java"));
        SpoonModelBuildException modelBuildException =
                new SpoonModelBuildException(srcFile.getPath(), "RuntimeException: boom", new RuntimeException("boom"));

        // When
        FlowProcessingResult result = invokeFormattingOnlyFallback(flow, srcFile, modelBuildException);

        // Then
        assertThat(result.getFlowProcessingStatus()).isEqualTo(FlowProcessingStatus.CHECKED);
        assertThat(result.isStopRequested()).isFalse();
        assertThat(result.getSortingStatistic().getSortingTimeInNanos()).isZero();
    }

    @NonNull
    private static CheckFailFastFlow createFlow() {
        CompiledConfig compiledConfig = ConfigurationManager.loadDefaultConfig();
        Formatter formatter = new Formatter(
                compiledConfig.getFormatting().getFormatterStyle(),
                compiledConfig.getFormatting().isFixImports());
        Sorter sorter = new Sorter(compiledConfig);
        return new CheckFailFastFlow(formatter, sorter);
    }

    @NonNull
    private static FlowProcessingResult invokeFormattingOnlyFallback(
            @NonNull CheckFailFastFlow flow,
            @NonNull SrcFile srcFile,
            @NonNull SpoonModelBuildException modelBuildException)
            throws Exception {
        Method method = CheckFailFastFlow.class.getDeclaredMethod(
                "processSrcWithFormattingOnlyFallback", SrcFile.class, SpoonModelBuildException.class);
        method.setAccessible(true);
        try {
            return (FlowProcessingResult) method.invoke(flow, srcFile, modelBuildException);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }
}

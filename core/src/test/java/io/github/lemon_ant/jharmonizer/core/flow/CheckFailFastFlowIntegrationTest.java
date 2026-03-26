package io.github.lemon_ant.jharmonizer.core.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.config.ConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileTestFactory;
import io.github.lemon_ant.jharmonizer.core.formatter.Formatter;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class CheckFailFastFlowIntegrationTest {

    @Test
    void processSource_firstViolationDetected_stopsSequentialProcessing() {
        // Given
        CheckFailFastFlow flow = createFlow();
        List<SrcFile> sourceFiles = List.of(
                SrcFileTestFactory.createSrcFile("class BViolation { int z; int a; }", Path.of("B_Violation.java")),
                SrcFileTestFactory.createSrcFile("class CFormatted{int a;}", Path.of("C_Formatted.java")));
        AtomicInteger processedSources = new AtomicInteger();

        // When / Then
        assertThatThrownBy(() -> sourceFiles.stream()
                        .map(sourceFile -> {
                            processedSources.incrementAndGet();
                            return flow.processSource(sourceFile);
                        })
                        .toList())
                .isInstanceOf(NotOrderedException.class)
                .hasMessageContaining("BViolation");

        // Then
        assertThat(processedSources).hasValue(1);
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
}

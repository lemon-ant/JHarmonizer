package io.github.lemon_ant.jharmonizer.cli.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SourceProcessingStats.AggregatedProcessingStatistic;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import picocli.CommandLine;

class RestructureCommandTest {

    private static final String SOURCE_WITH_MULTIPLE_TOP_LEVEL_TYPES = """
            package demo;
            public class Sample {}
            interface Alpha {}
            """;
    private static final String PARTIAL_TOP_LEVEL_TYPES_CONFIG = """
            top-level-types-ordering:
              main-type-first: false
              type-groups:
                - [ interface ]
                - [ class ]
              ordering-rules: [ alpha ]
            """;

    @TempDir
    Path temporaryDirectory;

    @Test
    void restructureCommand_baseDirOption_invokesProcessorWithCorrectBaseDir() {
        // Given
        CommandLine cmd = new CommandLine(new RestructureCommand());

        // When
        int exitCode;
        SourceProcessor constructedProcessor;
        try (MockedConstruction<SourceProcessor> sourceProcessorMocks = mockSuccessfulProcessorConstruction()) {
            exitCode = cmd.execute("--base-dir", "src/main/java");
            constructedProcessor = sourceProcessorMocks.constructed().getFirst();
        }

        // Then
        assertThat(exitCode).isZero();
        verify(constructedProcessor)
                .processSources(eq(Path.of("src/main/java")), any(), any(), eq(FlowType.RESTRUCTURE));
    }

    @Test
    void restructureCommand_includeOption_parsesIncludePatternCorrectly() {
        // Given
        CommandLine cmd = new CommandLine(new RestructureCommand());

        // When
        int exitCode;
        SourceProcessor constructedProcessor;
        try (MockedConstruction<SourceProcessor> sourceProcessorMocks = mockSuccessfulProcessorConstruction()) {
            exitCode = cmd.execute("--base-dir", "src", "--include", "**/*.java");
            constructedProcessor = sourceProcessorMocks.constructed().getFirst();
        }

        // Then
        assertThat(exitCode).isZero();
        verify(constructedProcessor)
                .processSources(any(Path.class), eq(java.util.Set.of("**/*.java")), any(), eq(FlowType.RESTRUCTURE));
    }

    @Test
    void restructureCommand_mixedCollectionOptions_combineAllValuesCorrectly() {
        // Given
        CommandLine cmd = new CommandLine(new RestructureCommand());

        // When
        int exitCode;
        SourceProcessor constructedProcessor;
        try (MockedConstruction<SourceProcessor> sourceProcessorMocks = mockSuccessfulProcessorConstruction()) {
            exitCode = cmd.execute(
                    "-b",
                    "src",
                    "-i",
                    "src/main/java/**/*.java,src/test/java/**/*.java",
                    "--include",
                    "src/integrationTest/java/**/*.java",
                    "-e",
                    "**/internal/**",
                    "--exclude",
                    "**/excluded/**,**/*Test.java");
            constructedProcessor = sourceProcessorMocks.constructed().getFirst();
        }

        // Then
        assertThat(exitCode).isZero();
        verify(constructedProcessor)
                .processSources(
                        eq(Path.of("src")),
                        eq(Set.of(
                                "src/main/java/**/*.java",
                                "src/test/java/**/*.java",
                                "src/integrationTest/java/**/*.java")),
                        eq(Set.of("**/internal/**", "**/excluded/**", "**/*Test.java")),
                        eq(FlowType.RESTRUCTURE));
    }

    @Test
    void restructureCommand_helpUsage_describeOptionAliasesAndCollectionFormats() {
        // Given
        CommandLine cmd = new CommandLine(new RestructureCommand());
        StringWriter usage = new StringWriter();

        // When
        cmd.usage(new PrintWriter(usage), CommandLine.Help.Ansi.OFF);

        // Then
        assertThat(usage.toString())
                .contains("-b, --base-dir")
                .contains("-c, --config")
                .contains("-i, --include")
                .contains("-e, --exclude")
                .contains("Repeat this option or pass multiple patterns as a")
                .contains("comma-separated list.");
    }

    @Test
    void restructureCommand_configOptionSupportsPartialConfig_reordersUsingMergedDefaults() throws Exception {
        // Given
        Path javaFilePath = Files.writeString(
                temporaryDirectory.resolve("Sample.java"),
                SOURCE_WITH_MULTIPLE_TOP_LEVEL_TYPES,
                StandardCharsets.UTF_8);
        Path configFilePath = writeConfigFile(temporaryDirectory.resolve("custom-config.yml"));
        CommandLine cmd = new CommandLine(new RestructureCommand());

        // When
        int exitCode = cmd.execute("--base-dir", temporaryDirectory.toString(), "--config", configFilePath.toString());

        // Then
        assertThat(exitCode).isZero();
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        assertThat(processedSourceCode.indexOf("interface Alpha"))
                .isLessThan(processedSourceCode.indexOf("class Sample"));
    }

    @Test
    void restructureCommand_processorThrowsRuntimeException_returnsExitCode1() {
        // Given
        CommandLine cmd = new CommandLine(new RestructureCommand());

        // When
        int exitCode;
        try (MockedConstruction<SourceProcessor> ignored = mockConstruction(SourceProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any()))
                    .thenThrow(new RuntimeException("Unexpected error"));
        })) {
            exitCode = cmd.execute("--base-dir", "src");
        }

        // Then
        assertThat(exitCode).isEqualTo(1);
    }

    private static Path writeConfigFile(Path configFilePath) throws Exception {
        return Files.writeString(configFilePath, PARTIAL_TOP_LEVEL_TYPES_CONFIG, StandardCharsets.UTF_8);
    }

    private static MockedConstruction<SourceProcessor> mockSuccessfulProcessorConstruction() {
        return mockConstruction(SourceProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any()))
                    .thenReturn(new AggregatedProcessingStatistic(0, 0, 0, null, null));
        });
    }
}

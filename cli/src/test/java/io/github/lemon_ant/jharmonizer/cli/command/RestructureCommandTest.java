package io.github.lemon_ant.jharmonizer.cli.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import picocli.CommandLine;

class RestructureCommandTest {

    private CommandLine commandLine;

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

    @BeforeEach
    void setUp() {
        commandLine = new CommandLine(new RestructureCommand());
    }

    @Test
    void restructureCommand_invoked_usesRestructureFlow() {
        // When
        int exitCode;
        SourceProcessor constructedProcessor;
        try (MockedConstruction<SourceProcessor> sourceProcessorMocks =
                CommandTestUtils.mockSuccessfulProcessorConstruction()) {
            exitCode = commandLine.execute("--base-dir", "src/main/java");
            constructedProcessor = sourceProcessorMocks.constructed().getFirst();
        }

        // Then
        assertThat(exitCode).isZero();
        verify(constructedProcessor)
                .processSources(
                        eq(Path.of("src/main/java").toAbsolutePath().normalize()),
                        any(),
                        any(),
                        eq(FlowType.RESTRUCTURE));
    }

    @Test
    void restructureCommand_helpUsage_describeOptionAliasesAndCollectionFormats() {
        // Given
        StringWriter usage = new StringWriter();

        // When
        commandLine.usage(new PrintWriter(usage), CommandLine.Help.Ansi.OFF);

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

        // When
        int exitCode = commandLine.execute("--base-dir", temporaryDirectory.toString(), "--config", configFilePath.toString());

        // Then
        assertThat(exitCode).isZero();
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        assertThat(processedSourceCode.indexOf("interface Alpha"))
                .isLessThan(processedSourceCode.indexOf("class Sample"));
    }

    @Test
    void restructureCommand_processorThrowsRuntimeException_returnsExitCode1() {
        // When
        int exitCode;
        try (MockedConstruction<SourceProcessor> ignored = mockConstruction(SourceProcessor.class, (mock, context) -> {
            when(mock.processSources(any(Path.class), any(), any(), any()))
                    .thenThrow(new RuntimeException("Unexpected error"));
        })) {
            exitCode = commandLine.execute("--base-dir", "src");
        }

        // Then
        assertThat(exitCode).isEqualTo(1);
    }

    @NonNull
    private static Path writeConfigFile(Path configFilePath) throws Exception {
        return Files.writeString(configFilePath, PARTIAL_TOP_LEVEL_TYPES_CONFIG, StandardCharsets.UTF_8);
    }
}

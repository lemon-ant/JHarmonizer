package io.github.lemon_ant.jharmonizer.cli.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import picocli.CommandLine;

class ReorderCommandTest {

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
        commandLine = new CommandLine(new ReorderCommand());
    }

    @Test
    void reorderCommand_invoked_usesReorderFlow() {
        // When
        int exitCode;
        SrcProcessor constructedProcessor;
        try (MockedConstruction<SrcProcessor> srcProcessorMocks =
                CommandTestUtils.mockSuccessfulProcessorConstruction()) {
            exitCode = commandLine.execute("--base-dir", "src/main/java");
            constructedProcessor = srcProcessorMocks.constructed().getFirst();
        }

        // Then
        assertThat(exitCode).isZero();
        verify(constructedProcessor)
                .processSources(
                        eq(Set.of(Path.of("src/main/java").toAbsolutePath().normalize())),
                        any(),
                        any(),
                        eq(FlowType.REORDER));
    }

    @Test
    void reorderCommand_helpUsage_describeOptionAliasesAndCollectionFormats() {
        // Given
        StringWriter usage = new StringWriter();

        // When
        commandLine.usage(new PrintWriter(usage), CommandLine.Help.Ansi.OFF);

        // Then
        assertThat(usage.toString())
                .contains("-b, --base-dir")
                .contains("-c, --config")
                .contains("-B, --no-backup")
                .contains("-S, --no-statistics")
                .contains("-i, --include")
                .contains("-e, --exclude")
                .contains("Repeat this option or pass multiple patterns as a")
                .contains("comma-separated list.");
    }

    @Test
    void reorderCommand_configOptionSupportsPartialConfig_reordersUsingMergedDefaults() throws Exception {
        // Given
        Path javaFilePath = Files.writeString(
                temporaryDirectory.resolve("Sample.java"),
                SOURCE_WITH_MULTIPLE_TOP_LEVEL_TYPES,
                StandardCharsets.UTF_8);
        Path configFilePath = writeConfigFile(temporaryDirectory.resolve("custom-config.yml"));

        // When
        int exitCode =
                commandLine.execute("--base-dir", temporaryDirectory.toString(), "--config", configFilePath.toString());

        // Then
        assertThat(exitCode).isZero();
        String processedSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);
        assertThat(processedSrcCode.indexOf("interface Alpha")).isLessThan(processedSrcCode.indexOf("class Sample"));
    }

    @Test
    void reorderCommand_processorThrowsRuntimeException_returnsExitCode1() throws Exception {
        // When
        int exitCode;
        try (AutoCloseable ignoredLogs = CommandTestUtils.suppressBaseCommandLogs();
                MockedConstruction<SrcProcessor> ignored = mockConstruction(SrcProcessor.class, (mock, context) -> {
                    when(mock.processSources(any(Collection.class), any(), any(), any()))
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

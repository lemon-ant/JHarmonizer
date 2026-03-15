package io.github.lemon_ant.jharmonizer.cli.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.lemon_ant.jharmonizer.core.SourceProcessor;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.processing_stat.SourceProcessingStats.AggregatedProcessingStatistic;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class RestructureCommandTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void restructureCommand_baseDirOption_invokesProcessorWithCorrectBaseDir() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        AggregatedProcessingStatistic stats = new AggregatedProcessingStatistic(0, 0, 0, null, null);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any())).thenReturn(stats);
        CommandLine cmd = new CommandLine(new RestructureCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src/main/java");

        // Then
        assertThat(exitCode).isZero();
        verify(mockProcessor).processSources(eq(Path.of("src/main/java")), any(), any(), eq(FlowType.RESTRUCTURE));
    }

    @Test
    void restructureCommand_includeOption_parsesIncludePatternCorrectly() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        AggregatedProcessingStatistic stats = new AggregatedProcessingStatistic(0, 0, 0, null, null);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any())).thenReturn(stats);
        CommandLine cmd = new CommandLine(new RestructureCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src", "--include", "**/*.java");

        // Then
        assertThat(exitCode).isZero();
        verify(mockProcessor)
                .processSources(any(Path.class), eq(java.util.Set.of("**/*.java")), any(), eq(FlowType.RESTRUCTURE));
    }

    @Test
    void restructureCommand_mixedCollectionOptions_combineAllValuesCorrectly() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        AggregatedProcessingStatistic stats = new AggregatedProcessingStatistic(0, 0, 0, null, null);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any())).thenReturn(stats);
        CommandLine cmd = new CommandLine(new RestructureCommand(mockProcessor));

        // When
        int exitCode = cmd.execute(
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

        // Then
        assertThat(exitCode).isZero();
        verify(mockProcessor)
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
        CommandLine cmd = new CommandLine(new RestructureCommand(mock(SourceProcessor.class)));
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
    void restructureCommand_configOptionDisablesBackups_skipBackupFileCreation() throws Exception {
        // Given
        Path javaFilePath = Files.writeString(
                temporaryDirectory.resolve("Sample.java"),
                "package demo; public class Sample {private int x;}",
                StandardCharsets.UTF_8);
        Path configFilePath = writeCustomConfigFile(temporaryDirectory.resolve("custom-config.yml"));
        CommandLine cmd = new CommandLine(new RestructureCommand());

        // When
        int exitCode = cmd.execute("--base-dir", temporaryDirectory.toString(), "--config", configFilePath.toString());

        // Then
        assertThat(exitCode).isZero();
        assertThat(Files.readString(javaFilePath, StandardCharsets.UTF_8))
                .isNotEqualTo("package demo; public class Sample {private int x;}");
        assertThat(temporaryDirectory.resolve("Sample.java.bak")).doesNotExist();
    }

    @Test
    void restructureCommand_processorThrowsRuntimeException_returnsExitCode1() {
        // Given
        SourceProcessor mockProcessor = mock(SourceProcessor.class);
        when(mockProcessor.processSources(any(Path.class), any(), any(), any()))
                .thenThrow(new RuntimeException("Unexpected error"));
        CommandLine cmd = new CommandLine(new RestructureCommand(mockProcessor));

        // When
        int exitCode = cmd.execute("--base-dir", "src");

        // Then
        assertThat(exitCode).isEqualTo(1);
    }

    private static Path writeCustomConfigFile(Path configFilePath) throws IOException {
        String defaultConfig = readDefaultConfig();
        String customConfig = replaceBackupsEnabled(defaultConfig, false);
        return Files.writeString(configFilePath, customConfig, StandardCharsets.UTF_8);
    }

    private static String readDefaultConfig() {
        try (InputStream inputStream = SourceProcessor.class.getResourceAsStream("/default-config.yml")) {
            if (inputStream == null) {
                throw new IllegalStateException("Classpath resource not found: /default-config.yml");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read classpath resource: /default-config.yml", exception);
        }
    }

    private static String replaceBackupsEnabled(String configContent, boolean backupsEnabled) {
        String originalLine = "backups-enabled: true";
        String updatedLine = "backups-enabled: " + backupsEnabled;
        if (!configContent.contains(originalLine)) {
            throw new IllegalStateException("Expected default config to contain line: " + originalLine);
        }
        return configContent.replace(originalLine, updatedLine);
    }
}

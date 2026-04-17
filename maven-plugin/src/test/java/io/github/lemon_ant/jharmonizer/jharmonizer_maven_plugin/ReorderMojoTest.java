package io.github.lemon_ant.jharmonizer.jharmonizer_maven_plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReorderMojoTest {

    @TempDir
    private Path tempDir;

    @Test
    void execute_nonConformingInput_reordersMembers() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/reorder-basic/input", tempDir);
        ReorderMojo reorderMojo = new ReorderMojo();
        MojoTestUtils.injectField(reorderMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        String inputContent =
                MojoTestUtils.readResourceAsString("/test-cases/reorder-basic/input/NonConformingSample.java");
        String expectedContent =
                MojoTestUtils.readResourceAsString("/test-cases/reorder-basic/expected/NonConformingSample.java");

        // When
        reorderMojo.execute();

        // Then
        String reorderedContent = Files.readString(tempDir.resolve("NonConformingSample.java"));
        assertThat(reorderedContent).isNotEqualTo(inputContent);
        assertThat(reorderedContent).isEqualTo(expectedContent);
    }

    @Test
    void execute_skipTrue_leavesFilesUnchanged() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/reorder-basic/input", tempDir);
        ReorderMojo reorderMojo = new ReorderMojo();
        MojoTestUtils.injectField(reorderMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(reorderMojo, "skip", true);
        String inputContent = Files.readString(tempDir.resolve("NonConformingSample.java"));

        // When
        reorderMojo.execute();

        // Then
        String contentAfterSkip = Files.readString(tempDir.resolve("NonConformingSample.java"));
        assertThat(contentAfterSkip).isEqualTo(inputContent);
    }

    @Test
    void execute_baseDirDoesNotExist_throwsMojoExecutionException() {
        // Given
        Path nonExistentDir = tempDir.resolve("does-not-exist");
        ReorderMojo reorderMojo = new ReorderMojo();
        MojoTestUtils.injectField(reorderMojo, "baseDir", MojoTestUtils.toFile(nonExistentDir));

        // When
        Throwable thrown = catchThrowable(reorderMojo::execute);

        // Then
        assertThat(thrown)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("does not exist or is not a directory");
    }

    @Test
    void execute_baseDirIsFile_throwsMojoExecutionException() throws Exception {
        // Given
        Path fileInsteadOfDir = tempDir.resolve("NotADirectory.txt");
        Files.createFile(fileInsteadOfDir);
        ReorderMojo reorderMojo = new ReorderMojo();
        MojoTestUtils.injectField(reorderMojo, "baseDir", MojoTestUtils.toFile(fileInsteadOfDir));

        // When
        Throwable thrown = catchThrowable(reorderMojo::execute);

        // Then
        assertThat(thrown)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("does not exist or is not a directory");
    }

    @Test
    void execute_emptyDirectory_completesWithoutException() throws Exception {
        // Given
        ReorderMojo reorderMojo = new ReorderMojo();
        MojoTestUtils.injectField(reorderMojo, "baseDir", MojoTestUtils.toFile(tempDir));

        // When
        Throwable thrown = catchThrowable(reorderMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_withBackupsDisabledOverride_processesSuccessfully() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/reorder-basic/input", tempDir);
        ReorderMojo reorderMojo = new ReorderMojo();
        MojoTestUtils.injectField(reorderMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(reorderMojo, "backupsEnabled", Boolean.FALSE);

        // When
        Throwable thrown = catchThrowable(reorderMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_withConfigFile_processesSuccessfully() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/reorder-basic/input", tempDir);
        Path configFile = MojoTestUtils.extractResourceToTemp(
                "/test-cases/config-override/custom-config.yml", tempDir.resolve("custom-config.yml"));
        ReorderMojo reorderMojo = new ReorderMojo();
        MojoTestUtils.injectField(reorderMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(reorderMojo, "configFile", MojoTestUtils.toFile(configFile));

        // When
        Throwable thrown = catchThrowable(reorderMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_withConfigFileAndBackupsOverride_mergesConfigAndProcessesSuccessfully() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/reorder-basic/input", tempDir);
        Path configFile = MojoTestUtils.extractResourceToTemp(
                "/test-cases/config-override/custom-config.yml", tempDir.resolve("custom-config.yml"));
        ReorderMojo reorderMojo = new ReorderMojo();
        MojoTestUtils.injectField(reorderMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(reorderMojo, "configFile", MojoTestUtils.toFile(configFile));
        MojoTestUtils.injectField(reorderMojo, "backupsEnabled", Boolean.FALSE);

        // When
        Throwable thrown = catchThrowable(reorderMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }
}

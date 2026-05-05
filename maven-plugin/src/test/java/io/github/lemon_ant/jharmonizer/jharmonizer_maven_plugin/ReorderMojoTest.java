// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
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
    void execute_noBaseDirWithProjectBaseDir_processesSuccessfully() throws Exception {
        // Given
        Path srcMainJava = tempDir.resolve("src/main/java");
        Files.createDirectories(srcMainJava);
        MojoTestUtils.copyResourceDirectory("/test-cases/reorder-basic/input", srcMainJava);
        ReorderMojo reorderMojo = new ReorderMojo();
        MojoTestUtils.injectField(reorderMojo, "projectBaseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(reorderMojo, "mainSourceDirectory", MojoTestUtils.toFile(srcMainJava));

        // When
        Throwable thrown = catchThrowable(reorderMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_testSourceDirectoryConfiguredButAbsent_processesMainDirectoryOnly() throws Exception {
        // Given – src/main/java exists with a Java file; src/test/java is configured but absent.
        Path srcMainJava = tempDir.resolve("src/main/java");
        Path srcTestJava = tempDir.resolve("src/test/java");
        Files.createDirectories(srcMainJava);
        MojoTestUtils.copyResourceDirectory("/test-cases/reorder-basic/input", srcMainJava);
        ReorderMojo reorderMojo = new ReorderMojo();
        MojoTestUtils.injectField(reorderMojo, "projectBaseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(reorderMojo, "mainSourceDirectory", MojoTestUtils.toFile(srcMainJava));
        MojoTestUtils.injectField(reorderMojo, "testSourceDirectory", MojoTestUtils.toFile(srcTestJava));

        // When
        Throwable thrown = catchThrowable(reorderMojo::execute);

        // Then – mojo runs to completion; the non-existent test source directory pattern is safely ignored
        assertThat(thrown).isNull();
    }

    @Test
    void execute_nonExistentSourceDirectories_skipsExecutionAndLeavesFilesUntouched() throws Exception {
        // Given - the project base dir exists but neither src/main/java nor src/test/java is present
        // (typical for a parent-only POM module in a multi-module build).
        // A Java file placed directly under the project root should NOT be processed.
        Path srcMainJava = tempDir.resolve("src/main/java");
        Path srcTestJava = tempDir.resolve("src/test/java");
        MojoTestUtils.copyResourceDirectory("/test-cases/reorder-basic/input", tempDir);
        ReorderMojo reorderMojo = new ReorderMojo();
        MojoTestUtils.injectField(reorderMojo, "projectBaseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(reorderMojo, "mainSourceDirectory", MojoTestUtils.toFile(srcMainJava));
        MojoTestUtils.injectField(reorderMojo, "testSourceDirectory", MojoTestUtils.toFile(srcTestJava));
        String contentBefore =
                MojoTestUtils.readResourceAsString("/test-cases/reorder-basic/input/NonConformingSample.java");

        // When
        Throwable thrown = catchThrowable(reorderMojo::execute);

        // Then – plugin skips gracefully and leaves the file untouched
        assertThat(thrown).isNull();
        String contentAfter = Files.readString(tempDir.resolve("NonConformingSample.java"));
        assertThat(contentAfter).isEqualTo(contentBefore);
    }

    @Test
    void execute_projectBaseDirNull_throwsMojoExecutionException() {
        // Given
        ReorderMojo reorderMojo = new ReorderMojo();

        // When
        Throwable thrown = catchThrowable(reorderMojo::execute);

        // Then
        assertThat(thrown).isInstanceOf(MojoExecutionException.class).hasMessageContaining("Project base directory");
    }

    @Test
    void execute_projectBaseDirIsFile_throwsMojoExecutionException() throws Exception {
        // Given
        Path fileInsteadOfDir = tempDir.resolve("NotADirectory.txt");
        Files.createFile(fileInsteadOfDir);
        ReorderMojo reorderMojo = new ReorderMojo();
        MojoTestUtils.injectField(reorderMojo, "projectBaseDir", MojoTestUtils.toFile(fileInsteadOfDir));

        // When
        Throwable thrown = catchThrowable(reorderMojo::execute);

        // Then
        assertThat(thrown)
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("does not exist or is not a directory");
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

    @Test
    void execute_autoDiscoveredConfigFilePresent_loadsConfigSuccessfully() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/reorder-basic/input", tempDir);
        Path autoDiscoveredConfigFile = MojoTestUtils.extractResourceToTemp(
                "/test-cases/config-override/custom-config.yml", tempDir.resolve("jharmonizer.yml"));
        ReorderMojo reorderMojo = new ReorderMojo();
        MojoTestUtils.injectField(reorderMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(reorderMojo, "configFile", MojoTestUtils.toFile(autoDiscoveredConfigFile));

        // When
        Throwable thrown = catchThrowable(reorderMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_configFilePointsToNonExistentPath_skipsFileConfigAndSucceeds() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/reorder-basic/input", tempDir);
        ReorderMojo reorderMojo = new ReorderMojo();
        MojoTestUtils.injectField(reorderMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(reorderMojo, "configFile", MojoTestUtils.toFile(tempDir.resolve("jharmonizer.yml")));

        // When
        Throwable thrown = catchThrowable(reorderMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }
}

/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.jharmonizer_maven_plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckFastMojoTest {

    @TempDir
    private Path tempDir;

    @Test
    void execute_conformingFiles_completesWithoutException() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-conforming", tempDir);
        CheckFastMojo checkFastMojo = new CheckFastMojo();
        MojoTestUtils.injectField(checkFastMojo, "baseDir", MojoTestUtils.toFile(tempDir));

        // When
        Throwable thrown = catchThrowable(checkFastMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_nonConformingFiles_throwsMojoFailureException() {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-non-conforming", tempDir);
        CheckFastMojo checkFastMojo = new CheckFastMojo();
        MojoTestUtils.injectField(checkFastMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(checkFastMojo, "failOnViolation", true);

        // When
        Throwable thrown = catchThrowable(checkFastMojo::execute);

        // Then
        assertThat(thrown)
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("do not conform to the configured ordering");
    }

    @Test
    void execute_nonConformingFilesAndFailOnViolationFalse_completesWithoutException() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-non-conforming", tempDir);
        CheckFastMojo checkFastMojo = new CheckFastMojo();
        MojoTestUtils.injectField(checkFastMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(checkFastMojo, "failOnViolation", false);

        // When
        Throwable thrown = catchThrowable(checkFastMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_skipTrue_skipsExecutionWithoutException() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-non-conforming", tempDir);
        CheckFastMojo checkFastMojo = new CheckFastMojo();
        MojoTestUtils.injectField(checkFastMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(checkFastMojo, "skip", true);

        // When
        Throwable thrown = catchThrowable(checkFastMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_nonConformingFiles_logsReorderFixCommand() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-non-conforming", tempDir);
        CheckFastMojo checkFastMojo = new CheckFastMojo();
        MojoTestUtils.injectField(checkFastMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(checkFastMojo, "failOnViolation", false);
        Log mockLog = mock(Log.class);
        checkFastMojo.setLog(mockLog);

        // When
        checkFastMojo.execute();

        // Then
        verify(mockLog).warn(argThat((CharSequence msg) -> msg.toString().contains("jharmonizer:reorder")));
    }
}

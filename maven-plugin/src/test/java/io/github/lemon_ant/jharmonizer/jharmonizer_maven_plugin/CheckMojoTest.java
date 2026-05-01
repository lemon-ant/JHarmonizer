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

class CheckMojoTest {

    @TempDir
    private Path tempDir;

    @Test
    void execute_conformingFiles_completesWithoutException() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-conforming", tempDir);
        CheckMojo checkMojo = new CheckMojo();
        MojoTestUtils.injectField(checkMojo, "baseDir", MojoTestUtils.toFile(tempDir));

        // When
        Throwable thrown = catchThrowable(checkMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_nonConformingFiles_throwsMojoFailureException() {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-non-conforming", tempDir);
        CheckMojo checkMojo = new CheckMojo();
        MojoTestUtils.injectField(checkMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(checkMojo, "failOnViolation", true);

        // When
        Throwable thrown = catchThrowable(checkMojo::execute);

        // Then
        assertThat(thrown)
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("do not conform to the configured ordering");
    }

    @Test
    void execute_nonConformingFilesAndFailOnViolationFalse_completesWithoutException() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-non-conforming", tempDir);
        CheckMojo checkMojo = new CheckMojo();
        MojoTestUtils.injectField(checkMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(checkMojo, "failOnViolation", false);

        // When
        Throwable thrown = catchThrowable(checkMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_skipTrue_skipsExecutionWithoutException() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-non-conforming", tempDir);
        CheckMojo checkMojo = new CheckMojo();
        MojoTestUtils.injectField(checkMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(checkMojo, "skip", true);

        // When
        Throwable thrown = catchThrowable(checkMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_emptyDirectory_completesWithoutException() throws Exception {
        // Given
        CheckMojo checkMojo = new CheckMojo();
        MojoTestUtils.injectField(checkMojo, "baseDir", MojoTestUtils.toFile(tempDir));

        // When
        Throwable thrown = catchThrowable(checkMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_nonConformingFiles_logsReorderFixCommand() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-non-conforming", tempDir);
        CheckMojo checkMojo = new CheckMojo();
        MojoTestUtils.injectField(checkMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(checkMojo, "failOnViolation", false);
        Log mockLog = mock(Log.class);
        checkMojo.setLog(mockLog);

        // When
        checkMojo.execute();

        // Then
        verify(mockLog).warn(argThat((CharSequence msg) -> msg.toString().contains("jharmonizer:reorder")));
    }
}

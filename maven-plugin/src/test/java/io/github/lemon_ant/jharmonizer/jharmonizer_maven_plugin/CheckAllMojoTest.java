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

class CheckAllMojoTest {

    @TempDir
    private Path tempDir;

    @Test
    void execute_conformingFiles_completesWithoutException() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-conforming", tempDir);
        CheckAllMojo checkAllMojo = new CheckAllMojo();
        MojoTestUtils.injectField(checkAllMojo, "baseDir", MojoTestUtils.toFile(tempDir));

        // When
        Throwable thrown = catchThrowable(checkAllMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_nonConformingFiles_throwsMojoFailureException() {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-non-conforming", tempDir);
        CheckAllMojo checkAllMojo = new CheckAllMojo();
        MojoTestUtils.injectField(checkAllMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(checkAllMojo, "failOnViolation", true);

        // When
        Throwable thrown = catchThrowable(checkAllMojo::execute);

        // Then
        assertThat(thrown)
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("do not conform to the configured ordering");
    }

    @Test
    void execute_nonConformingFilesAndFailOnViolationFalse_completesWithoutException() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-non-conforming", tempDir);
        CheckAllMojo checkAllMojo = new CheckAllMojo();
        MojoTestUtils.injectField(checkAllMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(checkAllMojo, "failOnViolation", false);

        // When
        Throwable thrown = catchThrowable(checkAllMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_skipTrue_skipsExecutionWithoutException() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-non-conforming", tempDir);
        CheckAllMojo checkAllMojo = new CheckAllMojo();
        MojoTestUtils.injectField(checkAllMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(checkAllMojo, "skip", true);

        // When
        Throwable thrown = catchThrowable(checkAllMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_emptyDirectory_completesWithoutException() throws Exception {
        // Given
        CheckAllMojo checkAllMojo = new CheckAllMojo();
        MojoTestUtils.injectField(checkAllMojo, "baseDir", MojoTestUtils.toFile(tempDir));

        // When
        Throwable thrown = catchThrowable(checkAllMojo::execute);

        // Then
        assertThat(thrown).isNull();
    }

    @Test
    void execute_nonConformingFiles_logsReorderFixCommand() throws Exception {
        // Given
        MojoTestUtils.copyResourceDirectory("/test-cases/check-non-conforming", tempDir);
        CheckAllMojo checkAllMojo = new CheckAllMojo();
        MojoTestUtils.injectField(checkAllMojo, "baseDir", MojoTestUtils.toFile(tempDir));
        MojoTestUtils.injectField(checkAllMojo, "failOnViolation", false);
        Log mockLog = mock(Log.class);
        checkAllMojo.setLog(mockLog);

        // When
        checkAllMojo.execute();

        // Then
        verify(mockLog).warn(argThat((CharSequence msg) -> msg.toString().contains("jharmonizer:reorder")));
    }
}

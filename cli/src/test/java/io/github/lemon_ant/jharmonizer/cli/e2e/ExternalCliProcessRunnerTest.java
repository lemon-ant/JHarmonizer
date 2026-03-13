package io.github.lemon_ant.jharmonizer.cli.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExternalCliProcessRunnerTest {

    @Test
    void normalizeErrorOutput_javaToolOptionsLauncherMessageProvided_removeOnlyLauncherNoise() {
        // Given
        String stderr =
                "Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=UTF-8" + System.lineSeparator() + "real stderr line";

        // When
        String normalizedStderr = ExternalCliProcessRunner.normalizeErrorOutput(stderr);

        // Then
        assertThat(normalizedStderr).isEqualTo("real stderr line");
    }

    @Test
    void normalizeErrorOutput_applicationErrorProvided_preserveOriginalContent() {
        // Given
        String stderr = "first line" + System.lineSeparator() + "second line";

        // When
        String normalizedStderr = ExternalCliProcessRunner.normalizeErrorOutput(stderr);

        // Then
        assertThat(normalizedStderr).isEqualTo(stderr);
    }
}

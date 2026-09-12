// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import lombok.NonNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import spoon.support.StandardEnvironment;

class SpoonPrinterHelperTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideLineTerminationCases")
    void terminateLine_variedBufferEndings_addsOnlyMissingLineTerminator(
            @NonNull String scenario,
            @NonNull String input,
            @NonNull String lineSeparator,
            @NonNull String expectedOutput) {
        // Given
        SpoonPrinterHelper printerHelper = new SpoonPrinterHelper(new StandardEnvironment());
        printerHelper.setLineSeparator(lineSeparator);
        printerHelper.write(input);

        // When
        printerHelper.terminateLine();
        printerHelper.terminateLine();

        // Then
        assertThat(printerHelper.toString()).as(scenario).isEqualTo(expectedOutput);
    }

    @NonNull
    private static Stream<Arguments> provideLineTerminationCases() {
        return Stream.of(
                Arguments.of("empty buffer", "", "\n", "\n"),
                Arguments.of("open line with LF", "text", "\n", "text\n"),
                Arguments.of("open line with CRLF", "text", "\r\n", "text\r\n"),
                Arguments.of("LF already present", "text\n", "\r\n", "text\n"),
                Arguments.of("CR already present", "text\r", "\r\n", "text\r"),
                Arguments.of("CRLF already present", "text\r\n", "\n", "text\r\n"));
    }
}

// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core;

import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.junit.jupiter.api.Test;

class GenerateExpectedIT {
    @Test
    void generate() throws Exception {
        Path inputDir = Path.of(
                "src/test/resources/test-cases/core/e2e/regression/12-test-class-it-and-test-suffix-matching/input");
        Path outputDir = Path.of("/tmp/regression-12-output");
        Files.createDirectories(outputDir);

        SrcProcessor processor = new SrcProcessor(
                FlexibleUnifiedConfig.builder().printProcessingStatistics(false).build());

        for (String file : List.of("AbstractConnectionIT.java", "AbstractConnectionTest.java")) {
            Path workFile = outputDir.resolve(file);
            Files.copy(inputDir.resolve(file), workFile, StandardCopyOption.REPLACE_EXISTING);

            SrcProcessingResult result =
                    processor.processSources(outputDir, List.of(file), List.of(), FlowType.REORDER);
            System.err.println("=== " + file + " Success: " + result.isSuccess() + " ===");
            System.err.println(Files.readString(workFile, StandardCharsets.UTF_8));
            System.err.println("===END===");
        }
    }
}

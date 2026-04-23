// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core;

import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MinimizeTest {

    @TempDir
    Path tempDir;

    @Test
    void showMinimalBug() throws IOException {
        String src = "// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>\n"
                + "// SPDX-License-Identifier: Apache-2.0\n"
                + "package io.github.lemon_ant.jharmonizer.core.e2e;\n\n"
                + "public class Foo {\n\n"
                + "    private volatile int v;  // comment\n"
                + "}\n";

        Path file = tempDir.resolve("Foo.java");
        Files.writeString(file, src);

        SrcProcessor processor = new SrcProcessor(
                FlexibleUnifiedConfig.builder().printProcessingStatistics(false).build());
        processor.processSources(tempDir, List.of("Foo.java"), List.of(), FlowType.REORDER);
        String afterFirst = Files.readString(file);
        System.out.println("=== AFTER FIRST ===");
        System.out.println(afterFirst);

        SrcProcessor checkProcessor = new SrcProcessor(
                FlexibleUnifiedConfig.builder().printProcessingStatistics(false).build());
        var result = checkProcessor.processSources(tempDir, List.of("Foo.java"), List.of(), FlowType.CHECK_FAIL_FAST);
        System.out.println("CHECK: " + (result.isSuccess() ? "OK" : "BUG"));

        if (!result.isSuccess()) {
            processor.processSources(tempDir, List.of("Foo.java"), List.of(), FlowType.REORDER);
            String afterSecond = Files.readString(file);
            System.out.println("=== AFTER SECOND ===");
            System.out.println(afterSecond);
        }
    }
}

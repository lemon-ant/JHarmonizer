// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.e2e;

import io.github.lemon_ant.jharmonizer.core.SrcProcessingResult;
import io.github.lemon_ant.jharmonizer.core.SrcProcessor;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BugDiagnosticTest {

    private static final String INPUT = "// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>\n"
            + "// SPDX-License-Identifier: Apache-2.0\n"
            + "package io.github.lemon_ant.jharmonizer.core.e2e;\n"
            + "\n"
            + "import java.io.Closeable;\n"
            + "\n"
            + "public interface RegistryService extends Closeable {\n"
            + "\n"
            + "    /**\n"
            + "     * @return the token client\n"
            + "     */\n"
            + "    Object getTokenClient();\n"
            + "\n"
            + "    //-------------------------------------------------------------------------------------------\n"
            + "\n"
            + "    /**\n"
            + "     * @return the bucket reader\n"
            + "     */\n"
            + "    Object getBucketReader();\n"
            + "\n"
            + "    /**\n"
            + "     * @return the bucket reader with options\n"
            + "     */\n"
            + "    Object getBucketReader(Object options);\n"
            + "\n"
            + "    //-------------------------------------------------------------------------------------------\n"
            + "\n"
            + "    /**\n"
            + "     * The builder.\n"
            + "     */\n"
            + "    interface Builder {\n"
            + "\n"
            + "        Builder withOptions(Object options);\n"
            + "\n"
            + "        RegistryService build();\n"
            + "\n"
            + "        Object getOptions();\n"
            + "\n"
            + "    }\n"
            + "\n"
            + "}\n";

    @TempDir
    Path tempDir;

    @Test
    void diagnosticTest() throws Exception {
        Path inputFile = tempDir.resolve("RegistryService.java");
        Files.writeString(inputFile, INPUT, StandardCharsets.UTF_8);

        SrcProcessor processor1 = new SrcProcessor(
                FlexibleUnifiedConfig.builder().printProcessingStatistics(false).build());
        processor1.processSources(tempDir, List.of("RegistryService.java"), List.of(), FlowType.REORDER);
        String firstPass = Files.readString(inputFile, StandardCharsets.UTF_8);
        System.out.println("=== FIRST PASS OUTPUT ===");
        System.out.println(firstPass);
        System.out.println("=== END FIRST PASS ===");

        SrcProcessingResult checkResult = new SrcProcessor(FlexibleUnifiedConfig.builder()
                        .printProcessingStatistics(false)
                        .build())
                .processSources(tempDir, List.of("RegistryService.java"), List.of(), FlowType.CHECK_FAIL_FAST);
        System.out.println("=== IDEMPOTENT: " + checkResult.isSuccess() + " ===");

        if (!checkResult.isSuccess()) {
            new SrcProcessor(FlexibleUnifiedConfig.builder()
                            .printProcessingStatistics(false)
                            .build())
                    .processSources(tempDir, List.of("RegistryService.java"), List.of(), FlowType.REORDER);
            String secondPass = Files.readString(inputFile, StandardCharsets.UTF_8);
            System.out.println("=== SECOND PASS OUTPUT ===");
            System.out.println(secondPass);
            System.out.println("=== END SECOND PASS ===");
        }
    }
}

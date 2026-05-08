// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedHeaderLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedOrderingRule;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypeSelector;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTypeKind;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OptOutSrcProcessorIntegrationTest {
    private static final FlexibleUnifiedConfig OPT_OUT_TEST_CONFIG = createOptOutTestConfig();

    @TempDir
    Path temporaryDirectory;

    @Test
    void processSources_duplicateFileScopeOptOut_usesLastDirectiveAndProcesses() throws Exception {
        // Given
        // Two file-scope opt-out comments: the second one (FULLY_OFF) should win.
        String srcCodeWithDuplicateOptOut = """
            // @jharmonizer:sort-off
            // @jharmonizer:fully-off
            public class DuplicateOptOut {
                public void b() {}
                public void a() {}
            }
            """;
        Path javaFilePath = writeJavaFile("DuplicateOptOut.java", srcCodeWithDuplicateOptOut);
        SrcProcessor srcProcessor = new SrcProcessor(OPT_OUT_TEST_CONFIG);

        // When
        srcProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.REORDER);

        // Then
        assertThat(Files.readString(javaFilePath, StandardCharsets.UTF_8)).isEqualTo(srcCodeWithDuplicateOptOut);
    }

    @Test
    void processSources_fileFullyOffWithNestedOverrides_keepOriginalSrc() throws Exception {
        // Given
        // Keep the nested declarations compact and intentionally out of order so byte-for-byte preservation
        // proves that file-level fully-off ignores nested override directives and extra declarations.
        String originalSrcCode = """
            // @jharmonizer:fully-off
            class ZuluHelper{static String label(){return "zulu";}}
            public class Sample{int zebra;
                static class LaterOuter{static String label(){return "later";}}
                int ant;
                // @jharmonizer:sort-off
                static class InnerSortOff{int zebra; static class BetaNested{static String describe(){return "beta";}} int ant; String describe(){return BetaNested.describe()+new AlphaNested().describe()+zebra+ant;} static class AlphaNested{static String describe(){return "alpha";}}}
                // @jharmonizer:fully-off
                static class InnerFullyOff{int zebra; static class LaterNested{static String describe(){return "later";}} int ant; String describe(){return LaterNested.describe()+zebra+ant;}}
                public static void main(String[] args){
                    System.out.println(new Sample().describe());
                }
                String describe(){
                    return new InnerSortOff().describe()+new InnerFullyOff().describe()+LaterOuter.label()+zebra+ant+ZuluHelper.label()+AlphaHelper.label();
                }
            }
            class AlphaHelper{static String label(){return "alpha";}}
            """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor(OPT_OUT_TEST_CONFIG);

        // When
        srcProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.REORDER);

        // Then
        assertThat(Files.readString(javaFilePath, StandardCharsets.UTF_8)).isEqualTo(originalSrcCode);
    }

    @Test
    void processSources_fileMixedCaseSortOffDirective_formatWithoutSortingTopLevelTypes() throws Exception {
        // Given
        String originalSrcCode = """
            // @JHarmonizer:SoRt-OfF
            import java.util.List;
            class Z{int b;int a;}
            class A{}
            """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor(OPT_OUT_TEST_CONFIG);

        // When
        srcProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.REORDER);
        String processedSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSrcCode).doesNotContain("import java.util.List;");
        assertThat(processedSrcCode).containsSubsequence("class Z", "class A");
        assertThat(processedSrcCode).contains("int a;").contains("int b;");
    }

    @Test
    void processSources_fileOptOutOff_keepOriginalSrc() throws Exception {
        // Given
        String originalSrcCode = """
            // @jharmonizer:fully-off
            import java.util.List;
            class Z{int b;int a;}
            class A{}
            """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor(OPT_OUT_TEST_CONFIG);

        // When
        srcProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.REORDER);

        // Then
        assertThat(Files.readString(javaFilePath, StandardCharsets.UTF_8)).isEqualTo(originalSrcCode);
    }

    @Test
    void processSources_fileOptOutSortOff_formatWithoutSortingTopLevelTypes() throws Exception {
        // Given
        String originalSrcCode = """
            // @jharmonizer:sort-off
            import java.util.List;
            class Z{int b;int a;}
            class A{}
            """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor(OPT_OUT_TEST_CONFIG);

        // When
        srcProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.REORDER);
        String processedSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSrcCode).doesNotContain("import java.util.List;");
        assertThat(processedSrcCode).containsSubsequence("class Z", "class A");
        assertThat(processedSrcCode).contains("int a;").contains("int b;");
    }

    @Test
    void processSources_fileSortOffAndNestedFullyOff_preserveFullyOffFragmentWhileFormattingRestOfFile()
            throws Exception {
        // Given
        String expectedFullyOffFragment = """
                // @jharmonizer:fully-off
                static class Inner{int z;  int a;}
            """.stripTrailing();
        String originalSrcCode = """
            // @jharmonizer:sort-off
            class Outer{int b;int a;
            %s
            }
            """.formatted(expectedFullyOffFragment);
        Path javaFilePath = writeJavaFile("Sample.java", originalSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor(OPT_OUT_TEST_CONFIG);

        // When
        srcProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.REORDER);
        String processedSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSrcCode).contains(expectedFullyOffFragment);
        assertThat(processedSrcCode).contains("static class Inner{int z;  int a;}");
        assertThat(processedSrcCode).contains("class Outer {");
    }

    @Test
    void processSources_nestedTypeOptOutOff_failFastReportsParentLevelRelocation() throws Exception {
        // Given
        String originalSrcCode = """
            class Outer {
                int a;
                int b;

                // @jharmonizer:fully-off
                static class Inner{int z;int a;}
            }
            """;
        writeJavaFile("Sample.java", originalSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor(OPT_OUT_TEST_CONFIG);

        // When
        SrcProcessingResult result =
                srcProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.CHECK_FAIL_FAST);

        // Then
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void processSources_nestedTypeOptOutOff_preserveExactFragmentAndKeepImports() throws Exception {
        // Given
        String ignoredFragment = """
                /* @jharmonizer:fully-off */
                static class Inner {
                    java.util.List<String> values;
                    int z;   int a;
                }
            """;
        String originalSrcCode = """
            class Outer {
                int b;
                int a;

                /* @jharmonizer:fully-off */
                static class Inner {
                    java.util.List<String> values;
                    int z;   int a;
                }
            }
            """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor(OPT_OUT_TEST_CONFIG);

        // When
        srcProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.REORDER);
        String processedSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSrcCode).contains(ignoredFragment);
        assertThat(processedSrcCode).containsSubsequence("int a;", "int b;").contains("java.util.List<String> values;");
    }

    @Test
    void processSources_nestedTypeOptOutSortOff_keepNestedMemberOrderAndFormatType() throws Exception {
        // Given
        String originalSrcCode = """
            class Outer {
                int b;
                int a;

                // @jharmonizer:sort-off
                static class Inner{int z;int a;}
            }
            """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor(OPT_OUT_TEST_CONFIG);

        // When
        srcProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.REORDER);
        String processedSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSrcCode)
                .containsSubsequence("int a;", "int b;")
                .contains("static class Inner {\n        int z;\n        int a;\n    }")
                .doesNotContain("static class Inner {\n        int a;\n        int z;\n    }");
    }

    @Test
    void processSources_nestedTypeOptOutSortOff_moveNestedTypeWithinParentOrdering() throws Exception {
        // Given
        String originalSrcCode = """
            class Outer {
                int z;

                // @jharmonizer:sort-off
                static class Inner {int z;int a;}

                int a;
            }
            """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor(OPT_OUT_TEST_CONFIG);

        // When
        srcProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.REORDER);
        String processedSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSrcCode)
                .containsSubsequence("static class Inner", "int a;", "int z;")
                .contains("static class Inner {\n        int z;\n        int a;\n    }");
    }

    @Test
    void processSources_packageDeclarationFileWithOptOut_appliesRawSrcFallback() throws Exception {
        // Given
        String packageSrcCode = "// @jharmonizer:fully-off\npackage com.example;\n";
        Path javaFilePath = writeJavaFile("package-info.java", packageSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor(OPT_OUT_TEST_CONFIG);

        // When
        srcProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.REORDER);

        // Then
        assertThat(Files.readString(javaFilePath, StandardCharsets.UTF_8)).isEqualTo(packageSrcCode);
    }

    @Test
    void processSources_topLevelTypeOptOutOff_preserveExactFragmentAndSortRemainingTypes() throws Exception {
        // Given
        String ignoredFragment = """
            /* @jharmonizer:fully-off */
            class Beta {
                int z;   int a;
            }
            """;
        String originalSrcCode = """
            class Gamma {}

            /* @jharmonizer:fully-off */
            class Beta {
                int z;   int a;
            }

            class Delta {}
            class Alpha {}
            """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor(OPT_OUT_TEST_CONFIG);

        // When
        srcProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.REORDER);
        String processedSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSrcCode).contains(ignoredFragment);
        assertThat(processedSrcCode).containsSubsequence("class Alpha", "class Beta", "class Delta", "class Gamma");
    }

    @Test
    void processSources_topLevelTypeOptOutSortOff_keepTypeBodyOrderButFormatType() throws Exception {
        // Given
        String originalSrcCode = """
            class Gamma {}

            // @jharmonizer:sort-off
            class Beta{int z;int a;}

            class Delta {}
            class Alpha {}
            """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSrcCode);
        SrcProcessor srcProcessor = new SrcProcessor(OPT_OUT_TEST_CONFIG);

        // When
        srcProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.REORDER);
        String processedSrcCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSrcCode).containsSubsequence("class Alpha", "class Beta", "class Delta", "class Gamma");
        assertThat(processedSrcCode)
                .contains("class Beta {\n    int z;\n    int a;\n}")
                .doesNotContain("class Beta {\n    int a;\n    int z;\n}");
    }

    private static FlexibleUnifiedConfig createOptOutTestConfig() {
        UnifiedMemberGroup rootMemberGroup = UnifiedMemberGroup.builder()
                .groupName("Root")
                .selectorBlock(UnifiedMemberGroupSelectorBlock.builder().build())
                .separator(UnifiedSeparator.NONE)
                .orderingRules(List.of(UnifiedOrderingRule.ALPHA))
                .build();
        UnifiedTopLevelTypesOrdering topLevelTypesOrdering = UnifiedTopLevelTypesOrdering.builder()
                .mainTypeFirst(false)
                .topLevelTypeSelectors(List.of(new UnifiedTopLevelTypeSelector(Set.of(
                        UnifiedTypeKind.CLASS,
                        UnifiedTypeKind.RECORD,
                        UnifiedTypeKind.INTERFACE,
                        UnifiedTypeKind.ENUM,
                        UnifiedTypeKind.ANNOTATION))))
                .orderingRules(List.of(UnifiedOrderingRule.ALPHA))
                .build();
        return FlexibleUnifiedConfig.builder()
                .topLevelTypesOrdering(topLevelTypesOrdering)
                .formatting(FlexibleUnifiedFormatting.builder()
                        .fixImports(true)
                        .formatterStyle(UnifiedFormatterStyle.PALANTIR)
                        .blankLineAfterTypeHeader(true)
                        .blankLineBeforeComment(true)
                        .blankLineBetweenFields(false)
                        .build())
                .backupsEnabled(false)
                .headerLine(new UnifiedHeaderLine('-', 0))
                .rootMemberGroups(List.of(rootMemberGroup))
                .build();
    }

    private Path writeJavaFile(String fileName, String fileContent) throws Exception {
        Path javaFilePath = temporaryDirectory.resolve(fileName);
        return Files.writeString(javaFilePath, fileContent, StandardCharsets.UTF_8);
    }
}

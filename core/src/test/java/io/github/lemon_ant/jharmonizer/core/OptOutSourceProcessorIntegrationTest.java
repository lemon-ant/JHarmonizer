package io.github.lemon_ant.jharmonizer.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedHeaderLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedOrderingRule;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypeSelector;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTypeKind;
import io.github.lemon_ant.jharmonizer.core.flow.FlowType;
import io.github.lemon_ant.jharmonizer.core.flow.NotOrderedException;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OptOutSourceProcessorIntegrationTest {
    private static final FlexibleUnifiedConfig OPT_OUT_TEST_CONFIG = createOptOutTestConfig();

    @TempDir
    Path temporaryDirectory;

    @Test
    void processSources_fileOptOutOff_keepOriginalSource() throws Exception {
        // Given
        String originalSourceCode = """
                // @jharmonizer:fully-off
                import java.util.List;
                class Z{int b;int a;}
                class A{}
                """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSourceCode);
        SourceProcessor sourceProcessor = new SourceProcessor(OPT_OUT_TEST_CONFIG);

        // When
        sourceProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.RESTRUCTURE);

        // Then
        assertThat(Files.readString(javaFilePath, StandardCharsets.UTF_8)).isEqualTo(originalSourceCode);
    }

    @Test
    void processSources_fileOptOutSortOff_formatWithoutSortingTopLevelTypes() throws Exception {
        // Given
        String originalSourceCode = """
                // @jharmonizer:sort-off
                import java.util.List;
                class Z{int b;int a;}
                class A{}
                """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSourceCode);
        SourceProcessor sourceProcessor = new SourceProcessor(OPT_OUT_TEST_CONFIG);

        // When
        sourceProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.RESTRUCTURE);
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSourceCode).doesNotContain("import java.util.List;");
        assertThat(processedSourceCode).containsSubsequence("class Z", "class A");
        assertThat(processedSourceCode).contains("int a;").contains("int b;");
    }

    @Test
    void processSources_fileMixedCaseSortOffDirective_formatWithoutSortingTopLevelTypes() throws Exception {
        // Given
        String originalSourceCode = """
                // @JHarmonizer:SoRt-OfF
                import java.util.List;
                class Z{int b;int a;}
                class A{}
                """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSourceCode);
        SourceProcessor sourceProcessor = new SourceProcessor(OPT_OUT_TEST_CONFIG);

        // When
        sourceProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.RESTRUCTURE);
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSourceCode).doesNotContain("import java.util.List;");
        assertThat(processedSourceCode).containsSubsequence("class Z", "class A");
        assertThat(processedSourceCode).contains("int a;").contains("int b;");
    }

    @Test
    void processSources_fileSortOffAndNestedFullyOff_preserveFullyOffFragmentWhileFormattingRestOfFile()
            throws Exception {
        // Given
        String expectedFullyOffFragment = """
                    // @jharmonizer:fully-off
                    static class Inner{int z;  int a;}
                """.stripTrailing();
        String originalSourceCode = """
                // @jharmonizer:sort-off
                class Outer{int b;int a;
                %s
                }
                """.formatted(expectedFullyOffFragment);
        Path javaFilePath = writeJavaFile("Sample.java", originalSourceCode);
        SourceProcessor sourceProcessor = new SourceProcessor(OPT_OUT_TEST_CONFIG);

        // When
        sourceProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.RESTRUCTURE);
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSourceCode).contains(expectedFullyOffFragment);
        assertThat(processedSourceCode).contains("static class Inner{int z;  int a;}");
        assertThat(processedSourceCode).contains("class Outer {");
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
        String originalSourceCode = """
                class Gamma {}

                /* @jharmonizer:fully-off */
                class Beta {
                    int z;   int a;
                }

                class Delta {}
                class Alpha {}
                """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSourceCode);
        SourceProcessor sourceProcessor = new SourceProcessor(OPT_OUT_TEST_CONFIG);

        // When
        sourceProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.RESTRUCTURE);
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSourceCode).contains(ignoredFragment);
        assertThat(processedSourceCode).containsSubsequence("class Alpha", "class Beta", "class Delta", "class Gamma");
    }

    @Test
    void processSources_topLevelTypeOptOutSortOff_keepTypeBodyOrderButFormatType() throws Exception {
        // Given
        String originalSourceCode = """
                class Gamma {}

                // @jharmonizer:sort-off
                class Beta{int z;int a;}

                class Delta {}
                class Alpha {}
                """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSourceCode);
        SourceProcessor sourceProcessor = new SourceProcessor(OPT_OUT_TEST_CONFIG);

        // When
        sourceProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.RESTRUCTURE);
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSourceCode).containsSubsequence("class Alpha", "class Beta", "class Delta", "class Gamma");
        assertThat(processedSourceCode)
                .contains("class Beta {\n    int z;\n    int a;\n}")
                .doesNotContain("class Beta {\n    int a;\n    int z;\n}");
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
        String originalSourceCode = """
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
        Path javaFilePath = writeJavaFile("Sample.java", originalSourceCode);
        SourceProcessor sourceProcessor = new SourceProcessor(OPT_OUT_TEST_CONFIG);

        // When
        sourceProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.RESTRUCTURE);
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSourceCode).contains(ignoredFragment);
        assertThat(processedSourceCode)
                .containsSubsequence("int a;", "int b;")
                .contains("java.util.List<String> values;");
    }

    @Test
    void processSources_nestedTypeOptOutSortOff_keepNestedMemberOrderAndFormatType() throws Exception {
        // Given
        String originalSourceCode = """
                class Outer {
                    int b;
                    int a;

                    // @jharmonizer:sort-off
                    static class Inner{int z;int a;}
                }
                """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSourceCode);
        SourceProcessor sourceProcessor = new SourceProcessor(OPT_OUT_TEST_CONFIG);

        // When
        sourceProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.RESTRUCTURE);
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSourceCode)
                .containsSubsequence("int a;", "int b;")
                .contains("static class Inner {\n        int z;\n        int a;\n    }")
                .doesNotContain("static class Inner {\n        int a;\n        int z;\n    }");
    }

    @Test
    void processSources_nestedTypeOptOutSortOff_moveNestedTypeWithinParentOrdering() throws Exception {
        // Given
        String originalSourceCode = """
                class Outer {
                    int z;

                    // @jharmonizer:sort-off
                    static class Inner {int z;int a;}

                    int a;
                }
                """;
        Path javaFilePath = writeJavaFile("Sample.java", originalSourceCode);
        SourceProcessor sourceProcessor = new SourceProcessor(OPT_OUT_TEST_CONFIG);

        // When
        sourceProcessor.processSources(temporaryDirectory, List.of("*.java"), List.of(), FlowType.RESTRUCTURE);
        String processedSourceCode = Files.readString(javaFilePath, StandardCharsets.UTF_8);

        // Then
        assertThat(processedSourceCode)
                .containsSubsequence("static class Inner", "int a;", "int z;")
                .contains("static class Inner {\n        int z;\n        int a;\n    }");
    }

    @Test
    void processSources_nestedTypeOptOutOff_failFastReportsParentLevelRelocation() throws Exception {
        // Given
        String originalSourceCode = """
                class Outer {
                    int a;
                    int b;

                    // @jharmonizer:fully-off
                    static class Inner{int z;int a;}
                }
                """;
        writeJavaFile("Sample.java", originalSourceCode);
        SourceProcessor sourceProcessor = new SourceProcessor(OPT_OUT_TEST_CONFIG);

        // When / Then
        assertThatThrownBy(() -> sourceProcessor.processSources(
                        temporaryDirectory, List.of("*.java"), List.of(), FlowType.CHECK_FAIL_FAST))
                .isInstanceOf(NotOrderedException.class)
                .hasMessageContaining("Outer$Inner expected to relocate UP")
                .hasMessageContaining("Outer$a expected to relocate DOWN");
    }

    @Test
    void processSources_optOutCombinationMatrix_keepFullyOffFilesUnchanged() throws Exception {
        // Given
        copyScenarioInputFiles();
        SourceProcessor sourceProcessor = new SourceProcessor(OPT_OUT_TEST_CONFIG);

        // When
        sourceProcessor.processSources(temporaryDirectory, List.of("**/*.java"), List.of(), FlowType.RESTRUCTURE);
        String fullyOffFile = readProcessedFile(Constants.DEEP_FULLY_OFF_FILE);

        // Then
        assertThat(fullyOffFile).isEqualTo(readScenarioInputFile(Constants.DEEP_FULLY_OFF_FILE));
    }

    @Test
    void processSources_fileSortOffWithNestedFullyOff_preserveBothAnnotations() throws Exception {
        // Given
        copyScenarioInputFiles();
        SourceProcessor sourceProcessor = new SourceProcessor(OPT_OUT_TEST_CONFIG);

        // When
        sourceProcessor.processSources(temporaryDirectory, List.of("**/*.java"), List.of(), FlowType.RESTRUCTURE);
        String fileSortOffFile = readProcessedFile(Constants.FILE_SORT_OFF_WITH_NESTED_FULLY_OFF_FILE);

        // Then
        assertThat(fileSortOffFile)
                .contains(Constants.FILE_SORT_OFF_NESTED_FULLY_OFF_FRAGMENT)
                .containsSubsequence("class Zeta", "class Alpha")
                .containsSubsequence("int walrus;", "int aardvark;", "static class Inner");
    }

    @Test
    void processSources_nestedSortOffWithDeepFullyOff_preserveInnerAnnotations() throws Exception {
        // Given
        copyScenarioInputFiles();
        SourceProcessor sourceProcessor = new SourceProcessor(OPT_OUT_TEST_CONFIG);

        // When
        sourceProcessor.processSources(temporaryDirectory, List.of("**/*.java"), List.of(), FlowType.RESTRUCTURE);
        String nestedCombinationFile = readProcessedFile(Constants.NESTED_OPT_OUT_COMBINATION_FILE);

        // Then
        assertThat(nestedCombinationFile)
                .contains(Constants.TOP_LEVEL_FULLY_OFF_FRAGMENT)
                .contains(Constants.DEEP_NESTED_FULLY_OFF_FRAGMENT)
                .contains("class Gamma {\n    int gammaFirst;\n    int gammaLast;\n}")
                .containsSubsequence("class Alpha", "class Beta", "class Gamma")
                .containsSubsequence("static class Inner", "int zebra;", "int ant;", "static class DeepInner")
                .containsSubsequence("static class Inner", "int aardvark;", "int walrus;");
    }

    private static final class Constants {
        private static final String SCENARIO_INPUT_RESOURCE_ROOT =
                "/test-cases/core/e2e/optout/01-combination-matrix/input/";
        private static final String DEEP_FULLY_OFF_FILE = "deep/skip/DeepFileFullyOff.java";
        private static final String FILE_SORT_OFF_WITH_NESTED_FULLY_OFF_FILE =
                "sortoff/FileSortOffWithNestedFullyOff.java";
        private static final String NESTED_OPT_OUT_COMBINATION_FILE = "nested/deeper/NestedOptOutCombination.java";
        private static final List<String> SCENARIO_RELATIVE_PATHS =
                List.of(DEEP_FULLY_OFF_FILE, FILE_SORT_OFF_WITH_NESTED_FULLY_OFF_FILE, NESTED_OPT_OUT_COMBINATION_FILE);
        private static final String FILE_SORT_OFF_NESTED_FULLY_OFF_FRAGMENT = """
                    // @jharmonizer:fully-off
                    static class Inner{int zebra;  int ant;}
                """.stripTrailing();
        private static final String TOP_LEVEL_FULLY_OFF_FRAGMENT = """
                /* @jharmonizer:fully-off */
                class Beta {
                    int betaLast;   int betaFirst;
                }
                """.stripTrailing();
        private static final String DEEP_NESTED_FULLY_OFF_FRAGMENT = """
                        // @jharmonizer:fully-off
                        static class DeepInner{int later;  int earlier;}
                """.stripTrailing();
    }

    @NonNull
    private Path writeJavaFile(String fileName, String fileContent) throws Exception {
        Path javaFilePath = temporaryDirectory.resolve(fileName);
        return Files.writeString(javaFilePath, fileContent, StandardCharsets.UTF_8);
    }

    private void copyScenarioInputFiles() throws Exception {
        for (String relativePath : Constants.SCENARIO_RELATIVE_PATHS) {
            Path targetPath = temporaryDirectory.resolve(relativePath);
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, readScenarioInputFile(relativePath), StandardCharsets.UTF_8);
        }
    }

    @NonNull
    private String readProcessedFile(String relativePath) throws Exception {
        return Files.readString(temporaryDirectory.resolve(relativePath), StandardCharsets.UTF_8);
    }

    @NonNull
    private String readScenarioInputFile(String relativePath) {
        return TestCaseResourceUtils.readClasspathResourceAsString(
                Constants.SCENARIO_INPUT_RESOURCE_ROOT + relativePath);
    }

    private static FlexibleUnifiedConfig createOptOutTestConfig() {
        UnifiedMemberGroup rootMemberGroup = UnifiedMemberGroup.builder()
                .groupName("Root")
                .selectorBlock(UnifiedMemberGroupSelectorBlock.builder().build())
                .separator(UnifiedSeparator.NONE)
                .orderingRule(UnifiedOrderingRule.ALPHA)
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
        return new FlexibleUnifiedConfig(
                topLevelTypesOrdering,
                new UnifiedFormatting(true, UnifiedFormatterStyle.PALANTIR),
                false,
                new UnifiedHeaderLine('-', 0),
                List.of(rootMemberGroup));
    }
}

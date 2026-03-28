package io.github.lemon_ant.jharmonizer.core.optout;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;

class JHarmonizerOptOutResolverTest {

    @Test
    void parseJavaSrcResource_fileOptOutBeforePackage_resolveFileOptOutOff() {
        // Given
        String srcCode = """
                // @jharmonizer:fully-off
                package demo;

                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Sample.java")));

        // Then
        assertThat(spoonAstModel.getOptOuts().getFileOptOutMode()).contains(JHarmonizerOptOutMode.FULLY_OFF);
    }

    @Test
    void parseJavaSrcResource_fileOptOutBetweenPackageAndImport_resolveFileSortOffOptOut() {
        // Given
        String srcCode = """
                package demo;

                /* @jharmonizer:sort-off */
                import java.util.List;

                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Sample.java")));

        // Then
        assertThat(spoonAstModel.getOptOuts().getFileOptOutMode()).contains(JHarmonizerOptOutMode.SORTING_OFF);
    }

    @Test
    void parseJavaSrcResource_typeOptOutBeforeAnnotatedTopLevelType_resolveTypeOptOut() {
        // Given
        String srcCode = """
                package demo;

                // @jharmonizer:fully-off
                @Deprecated
                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Sample.java")));
        CtType<?> sampleType =
                spoonAstModel.getCompilationUnit().getDeclaredTypes().getFirst();

        // Then
        assertThat(spoonAstModel.getOptOuts().findTypeOptOutMode(sampleType)).contains(JHarmonizerOptOutMode.FULLY_OFF);
    }

    @Test
    void parseJavaSrcResource_typeOptOutBeforeNestedType_resolveNestedTypeOptOut() {
        // Given
        String srcCode = """
                package demo;

                class Outer {
                    /* @jharmonizer:sort-off */
                    class Inner {}
                }
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Outer.java")));
        CtCompilationUnit compilationUnit = spoonAstModel.getCompilationUnit();
        CtType<?> outerType = compilationUnit.getDeclaredTypes().getFirst();
        CtType<?> nestedType = outerType.getNestedTypes().stream().findFirst().orElseThrow();

        // Then
        assertThat(spoonAstModel.getOptOuts().findTypeOptOutMode(nestedType))
                .contains(JHarmonizerOptOutMode.SORTING_OFF);
    }

    @Test
    void parseJavaSrcResource_packageInfoFullyOffBeforeJavadoc_resolveFileOptOutOff() {
        // Given
        String srcCode = """
                // @jharmonizer:fully-off
                /**
                 * Package docs.
                 */
                @Deprecated
                package demo;
                """;

        // When
        SpoonAstModel spoonAstModel =
                SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("package-info.java")));

        // Then
        assertThat(spoonAstModel.getOptOuts().getFileOptOutMode()).contains(JHarmonizerOptOutMode.FULLY_OFF);
    }

    @Test
    void parseJavaSrcResource_packageInfoFullyOffThenSortOff_resolveFileFullyOff() {
        // Given
        String srcCode = """
                // @jharmonizer:fully-off
                // @jharmonizer:sort-off
                /**
                 * Package docs.
                 */
                @Deprecated
                package demo;
                """;

        // When
        SpoonAstModel spoonAstModel =
                SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("package-info.java")));

        // Then
        assertThat(spoonAstModel.getOptOuts().getFileOptOutMode()).contains(JHarmonizerOptOutMode.FULLY_OFF);
    }

    @Test
    void parseJavaSrcResource_moduleInfoFullyOffThenSortOff_resolveFileFullyOff() {
        // Given
        String srcCode = """
                // @jharmonizer:fully-off
                // @jharmonizer:sort-off
                @Deprecated
                module demo.module {
                    requires java.base;
                }
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("module-info.java")));

        // Then
        assertThat(spoonAstModel.getOptOuts().getFileOptOutMode()).contains(JHarmonizerOptOutMode.FULLY_OFF);
    }

    @Test
    void parseJavaSrcResource_commentOnlyFullyOff_resolveFileFullyOff() {
        // Given
        String srcCode = """
                // @jharmonizer:fully-off
                    // Intentionally comment-only source.
                /*    No package, imports, module, or type declarations.    */
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("CommentOnly.java")));

        // Then
        assertThat(spoonAstModel.getOptOuts().getFileOptOutMode()).contains(JHarmonizerOptOutMode.FULLY_OFF);
    }

    @Test
    void parseJavaSrcResource_commentOnlyIndentedLineDirectiveFullyOff_resolveFileFullyOff() {
        // Given
        String srcCode = """
                //     @jharmonizer:fully-off
                // Comment-only fixture with indented token after line-comment prefix.
                /*    Keeps intentionally odd spacing to validate tolerant parsing.    */
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("CommentOnly.java")));

        // Then
        assertThat(spoonAstModel.getOptOuts().getFileOptOutMode()).contains(JHarmonizerOptOutMode.FULLY_OFF);
    }

    @Test
    void parseJavaSrcResource_blockDirectiveWithLeadingNewline_resolveFileSortOff() {
        // Given
        String srcCode = """
                package demo;

                /*
                   @jharmonizer:sort-off
                */
                import java.util.List;
                import java.util.ArrayList;

                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Sample.java")));

        // Then
        assertThat(spoonAstModel.getOptOuts().getFileOptOutMode()).contains(JHarmonizerOptOutMode.SORTING_OFF);
    }

    @Test
    void parseJavaSrcResource_memberOptOutBeforeField_ignoreInvalidOptOut() {
        // Given
        String srcCode = """
                package demo;

                class Sample {
                    // @jharmonizer:fully-off
                    int value;
                }
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Sample.java")));

        // Then
        assertThat(spoonAstModel.getOptOuts().isEmpty()).isTrue();
    }

    @Test
    void parseJavaSrcResource_unsupportedOptOutToken_ignoreInvalidOptOut() {
        // Given
        String srcCode = """
                package demo;

                // @jharmonizer:format-off
                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Sample.java")));

        // Then
        assertThat(spoonAstModel.getOptOuts().isEmpty()).isTrue();
    }

    @Test
    void parseJavaSrcResource_mixedCaseOptOutToken_resolveIgnoringCase() {
        // Given
        String srcCode = """
                // @JHarmonizer:SoRt-OfF
                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Sample.java")));

        // Then
        assertThat(spoonAstModel.getOptOuts().getFileOptOutMode()).contains(JHarmonizerOptOutMode.SORTING_OFF);
    }

    @Test
    void resolveFileOptOutMode_sortOffThenFullyOff_resolvesFullyOff() {
        // Given
        String srcCode = """
                // @jharmonizer:sort-off
                // @jharmonizer:fully-off
                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Sample.java")));

        // Then
        assertThat(spoonAstModel.getOptOuts().getFileOptOutMode()).contains(JHarmonizerOptOutMode.FULLY_OFF);
    }

    @Test
    void resolveTypeOptOutMode_sortOffThenFullyOff_resolvesFullyOff() {
        // Given
        String srcCode = """
                class Sample {
                    // @jharmonizer:sort-off
                    // @jharmonizer:fully-off
                    class Nested {}
                }
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Sample.java")));
        CtType<?> nestedType =
                spoonAstModel.getCompilationUnit().getDeclaredTypes().getFirst().getNestedTypes().stream()
                        .findFirst()
                        .orElseThrow();

        // Then
        assertThat(spoonAstModel.getOptOuts().findTypeOptOutMode(nestedType)).contains(JHarmonizerOptOutMode.FULLY_OFF);
    }

    @Test
    void getSortingSkippedTypes_nestedSortOffWithinSortOffParent_skipOnlyParentType() {
        // Given
        String srcCode = """
                class Sample {
                    // @jharmonizer:sort-off
                    class Outer {
                        // @jharmonizer:sort-off
                        class Inner {}
                    }
                }
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Sample.java")));
        CtType<?> outerType = spoonAstModel.getCompilationUnit().getDeclaredTypes().getFirst().getNestedTypes().stream()
                .findFirst()
                .orElseThrow();
        CtType<?> innerType = outerType.getNestedTypes().stream().findFirst().orElseThrow();

        // Then
        assertThat(spoonAstModel.getOptOuts().getSortingSkippedTypes())
                .contains(outerType)
                .doesNotContain(innerType);
        assertThat(spoonAstModel.getOptOuts().findTypeOptOutMode(innerType)).isEmpty();
    }

    @Test
    void parseJavaSrcResource_legacyOffAlias_ignoreInvalidOptOut() {
        // Given
        String srcCode = """
                // @jharmonizer:off
                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSrcFile(createSrcFile(srcCode, Path.of("Sample.java")));

        // Then
        assertThat(spoonAstModel.getOptOuts().isEmpty()).isTrue();
    }
}

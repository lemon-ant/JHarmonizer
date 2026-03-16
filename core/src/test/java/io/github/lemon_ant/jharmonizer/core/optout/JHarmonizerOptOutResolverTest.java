package io.github.lemon_ant.jharmonizer.core.optout;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;

class JHarmonizerOptOutResolverTest {

    @Test
    void parseJavaSourceResource_fileOptOutBeforePackage_resolveFileOptOutOff() {
        // Given
        String sourceCode = """
                // @jharmonizer:fully-off
                package demo;

                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of("Sample.java"), sourceCode);

        // Then
        assertThat(spoonAstModel.getOptOuts().getFileOptOut())
                .get()
                .extracting(ResolvedJHarmonizerOptOut::getMode, ResolvedJHarmonizerOptOut::getScope)
                .containsExactly(JHarmonizerOptOutMode.FULLY_OFF, JHarmonizerOptOutScope.FILE_SCOPE);
    }

    @Test
    void parseJavaSourceResource_fileOptOutBetweenPackageAndImport_resolveFileSortOffOptOut() {
        // Given
        String sourceCode = """
                package demo;

                /* @jharmonizer:sort-off */
                import java.util.List;

                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of("Sample.java"), sourceCode);

        // Then
        assertThat(spoonAstModel.getOptOuts().getFileOptOut())
                .get()
                .extracting(ResolvedJHarmonizerOptOut::getMode, ResolvedJHarmonizerOptOut::getScope)
                .containsExactly(JHarmonizerOptOutMode.SORTING_OFF, JHarmonizerOptOutScope.FILE_SCOPE);
    }

    @Test
    void parseJavaSourceResource_typeOptOutBeforeAnnotatedTopLevelType_resolveTypeOptOut() {
        // Given
        String sourceCode = """
                package demo;

                // @jharmonizer:fully-off
                @Deprecated
                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of("Sample.java"), sourceCode);
        CtType<?> sampleType =
                spoonAstModel.getCompilationUnit().getDeclaredTypes().getFirst();

        // Then
        assertThat(spoonAstModel.getOptOuts().findTypeOptOut(sampleType))
                .get()
                .extracting(ResolvedJHarmonizerOptOut::getMode, optOut -> optOut.getTargetType()
                        .map(CtType::getSimpleName)
                        .orElseThrow())
                .containsExactly(JHarmonizerOptOutMode.FULLY_OFF, "Sample");
    }

    @Test
    void parseJavaSourceResource_typeOptOutBeforeNestedType_resolveNestedTypeOptOut() {
        // Given
        String sourceCode = """
                package demo;

                class Outer {
                    /* @jharmonizer:sort-off */
                    class Inner {}
                }
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of("Outer.java"), sourceCode);
        CtCompilationUnit compilationUnit = spoonAstModel.getCompilationUnit();
        CtType<?> outerType = compilationUnit.getDeclaredTypes().getFirst();
        CtType<?> nestedType = outerType.getNestedTypes().stream().findFirst().orElseThrow();

        // Then
        assertThat(spoonAstModel.getOptOuts().findTypeOptOut(nestedType))
                .get()
                .extracting(ResolvedJHarmonizerOptOut::getMode, ResolvedJHarmonizerOptOut::getScope)
                .containsExactly(JHarmonizerOptOutMode.SORTING_OFF, JHarmonizerOptOutScope.TYPE_SCOPE);
    }

    @Test
    void parseJavaSourceResource_memberOptOutBeforeField_ignoreInvalidOptOut() {
        // Given
        String sourceCode = """
                package demo;

                class Sample {
                    // @jharmonizer:fully-off
                    int value;
                }
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of("Sample.java"), sourceCode);

        // Then
        assertThat(spoonAstModel.getOptOuts().isEmpty()).isTrue();
    }

    @Test
    void parseJavaSourceResource_unsupportedOptOutToken_ignoreInvalidOptOut() {
        // Given
        String sourceCode = """
                package demo;

                // @jharmonizer:format-off
                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of("Sample.java"), sourceCode);

        // Then
        assertThat(spoonAstModel.getOptOuts().isEmpty()).isTrue();
    }

    @Test
    void parseJavaSourceResource_mixedCaseOptOutToken_resolveIgnoringCase() {
        // Given
        String sourceCode = """
                // @JHarmonizer:SoRt-OfF
                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of("Sample.java"), sourceCode);

        // Then
        assertThat(spoonAstModel.getOptOuts().getFileOptOut())
                .get()
                .extracting(ResolvedJHarmonizerOptOut::getMode)
                .isEqualTo(JHarmonizerOptOutMode.SORTING_OFF);
    }

    @Test
    void parseJavaSourceResource_legacyOffAlias_ignoreInvalidOptOut() {
        // Given
        String srcCode = """
                // @jharmonizer:off
                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of("Sample.java"), srcCode);

        // Then
        assertThat(spoonAstModel.getOptOuts().isEmpty()).isTrue();
    }
}

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
    void parseJavaSourceResource_fileOptOutBeforePackage_resolveFileOffOptOut() {
        // Given
        String sourceCode = """
                // @jharmonizer:off
                package demo;

                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of("Sample.java"), sourceCode);

        // Then
        assertThat(spoonAstModel.getOptOuts().getFileOptOut())
                .get()
                .extracting(ResolvedJHarmonizerOptOut::getMode, ResolvedJHarmonizerOptOut::getScope)
                .containsExactly(JHarmonizerOptOutMode.OFF, JHarmonizerOptOutScope.FILE);
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
                .containsExactly(JHarmonizerOptOutMode.SORT_OFF, JHarmonizerOptOutScope.FILE);
    }

    @Test
    void parseJavaSourceResource_typeOptOutBeforeAnnotatedTopLevelType_resolveTypeOptOut() {
        // Given
        String sourceCode = """
                package demo;

                // @jharmonizer:off
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
                        .map(ResolvedOptOutTargetType::getSimpleName)
                        .orElseThrow())
                .containsExactly(JHarmonizerOptOutMode.OFF, "Sample");
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
                .containsExactly(JHarmonizerOptOutMode.SORT_OFF, JHarmonizerOptOutScope.TYPE);
    }

    @Test
    void parseJavaSourceResource_memberOptOutBeforeField_ignoreInvalidOptOut() {
        // Given
        String sourceCode = """
                package demo;

                class Sample {
                    // @jharmonizer:off
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
                .isEqualTo(JHarmonizerOptOutMode.SORT_OFF);
    }
}

package io.github.lemon_ant.jharmonizer.core.directive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonParser;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;

class JHarmonizerDirectiveResolverTest {

    @Test
    void parseJavaSourceResource_fileDirectiveBeforePackage_resolveFileOffDirective() {
        // Given
        String sourceCode = """
                // @jharmonizer:off
                package demo;

                class Sample {}
                """;

        // When
        SpoonAstModel spoonAstModel = SpoonParser.parseJavaSourceResource(Path.of("Sample.java"), sourceCode);

        // Then
        assertThat(spoonAstModel.getDirectives().getFileDirective())
                .get()
                .extracting(ResolvedJHarmonizerDirective::getMode, ResolvedJHarmonizerDirective::getScope)
                .containsExactly(JHarmonizerDirectiveMode.OFF, JHarmonizerDirectiveScope.FILE);
    }

    @Test
    void parseJavaSourceResource_fileDirectiveBetweenPackageAndImport_resolveFileSortOffDirective() {
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
        assertThat(spoonAstModel.getDirectives().getFileDirective())
                .get()
                .extracting(ResolvedJHarmonizerDirective::getMode, ResolvedJHarmonizerDirective::getScope)
                .containsExactly(JHarmonizerDirectiveMode.SORT_OFF, JHarmonizerDirectiveScope.FILE);
    }

    @Test
    void parseJavaSourceResource_typeDirectiveBeforeAnnotatedTopLevelType_resolveTypeDirective() {
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
        assertThat(spoonAstModel.getDirectives().findTypeDirective(sampleType))
                .get()
                .extracting(ResolvedJHarmonizerDirective::getMode, directive -> directive
                        .getTargetType()
                        .map(ResolvedDirectiveTargetType::getSimpleName)
                        .orElseThrow())
                .containsExactly(JHarmonizerDirectiveMode.OFF, "Sample");
    }

    @Test
    void parseJavaSourceResource_typeDirectiveBeforeNestedType_resolveNestedTypeDirective() {
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
        assertThat(spoonAstModel.getDirectives().findTypeDirective(nestedType))
                .get()
                .extracting(ResolvedJHarmonizerDirective::getMode, ResolvedJHarmonizerDirective::getScope)
                .containsExactly(JHarmonizerDirectiveMode.SORT_OFF, JHarmonizerDirectiveScope.TYPE);
    }

    @Test
    void parseJavaSourceResource_memberDirectiveBeforeField_throwException() {
        // Given
        String sourceCode = """
                package demo;

                class Sample {
                    // @jharmonizer:off
                    int value;
                }
                """;

        // When / Then
        assertThatThrownBy(() -> SpoonParser.parseJavaSourceResource(Path.of("Sample.java"), sourceCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Member-level JHarmonizer directives are not supported")
                .hasMessageContaining("Sample.java");
    }

    @Test
    void parseJavaSourceResource_unsupportedDirectiveToken_throwException() {
        // Given
        String sourceCode = """
                package demo;

                // @jharmonizer:format-off
                class Sample {}
                """;

        // When / Then
        assertThatThrownBy(() -> SpoonParser.parseJavaSourceResource(Path.of("Sample.java"), sourceCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported JHarmonizer directive token")
                .hasMessageContaining("Sample.java");
    }
}

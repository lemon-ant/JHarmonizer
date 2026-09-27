// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.files_handler.SrcFileCreator.createSrcFile;
import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseAstModelFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.readClasspathResourceAsString;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup;
import io.github.lemon_ant.jharmonizer.core.translator.SpoonModelBuildException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import spoon.Launcher;

class SpoonParserTest {
    @NonNull
    private static final URL TYPE_USE_ANNOTATIONS = requireClasspathResourceUrl(
            "/test-cases/core/e2e/printer/scenarios/18-annotation-language-constructs/input/TypeUseAnnotationOrdering.java");

    @Test
    void buildSpoonAstModel_launcherBuildFails_wrapsWithSpoonModelBuildException() throws Exception {
        // Given
        RuntimeException launcherFailure = new RuntimeException("boom");
        Launcher launcher = mock(Launcher.class);
        doThrow(launcherFailure).when(launcher).buildModel();
        SrcFile srcFile = createSrcFile("class Sample {}", Path.of("Sample.java"));

        // When
        SpoonModelBuildException thrown =
                catchThrowableOfType(SpoonModelBuildException.class, () -> invokeBuildSpoonAstModel(srcFile, launcher));

        // Then
        assertThat(thrown.getSrcPath()).isEqualTo(Path.of("Sample.java"));
        assertThat(thrown)
                .hasMessageContaining("RuntimeException")
                .hasMessageContaining("boom")
                .hasCause(launcherFailure);
    }

    @Test
    void parseJavaSrcFile_arrayAndReceiverAnnotations_preservesLexicalSourceGroups() {
        // Given
        String srcCode = readClasspathResourceAsString(TYPE_USE_ANNOTATIONS);
        int firstDimensionStart = srcCode.indexOf("@Z @A []");
        int secondDimensionStart = srcCode.indexOf("@D @C []");
        int receiverStart = srcCode.indexOf("@Z @A TypeUseAnnotationOrdering this");

        // When
        SpoonAstModel spoonAstModel = parseAstModelFromJavaFixtureResource(TYPE_USE_ANNOTATIONS);

        // Then
        assertThat(spoonAstModel.getAnnotationSrcGroups())
                .filteredOn(group -> group.getStart() == firstDimensionStart
                        || group.getStart() == secondDimensionStart
                        || group.getStart() == receiverStart)
                .extracting(
                        AnnotationSrcGroup::getStart,
                        AnnotationSrcGroup::getEndExclusive,
                        AnnotationSrcGroup::getReplacementCode)
                .containsExactly(
                        tuple(firstDimensionStart, srcCode.indexOf("[]", firstDimensionStart), "@Z @A "),
                        tuple(secondDimensionStart, srcCode.indexOf("[]", secondDimensionStart), "@D @C "),
                        tuple(receiverStart, srcCode.indexOf("TypeUseAnnotationOrdering this", receiverStart), "@Z @A "));
    }

    @NonNull
    private static SpoonAstModel invokeBuildSpoonAstModel(SrcFile srcFile, Launcher launcher) throws Exception {
        Method buildSpoonAstModel = SpoonParser.class.getDeclaredMethod(
                "buildSpoonAstModel", SrcFile.class, Launcher.class, PrinterConfig.class);
        buildSpoonAstModel.setAccessible(true);
        try {
            return (SpoonAstModel)
                    buildSpoonAstModel.invoke(null, srcFile, launcher, new PrinterConfig(true, true, false));
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw exception;
        }
    }
}

// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils.parseAstModelFromJavaFixtureResource;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.RelocationDetector.hasReorderedAnnotations;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.JHarmonizerConfigurationManager;
import io.github.lemon_ant.jharmonizer.core.sorter.Sorter;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSourceScanner.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSourceScanner.AnnotationSrcGroup;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtType;

class SpoonAnnotationSorterTest {
    private static final URL FIXTURE = requireClasspathResourceUrl(
            "/test-cases/core/e2e/printer/scenarios/17-annotation-fragments/input/AnnotationFragmentPreservation.java");

    private static CompiledConfig compiledConfig;

    @BeforeAll
    static void setUp() {
        compiledConfig =
                Unified2CompiledModelCompiler.compile(JHarmonizerConfigurationManager.parseUnifiedDefaultConfig());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void sort_annotationSrcGroups_preservesAstAnnotationsAndReturnsImmutableOrder(boolean sourcePositionAvailable) {
        // Given
        SpoonAstModel model = parseAstModelFromJavaFixtureResource(FIXTURE);
        CtType<?> type = model.getMainType().orElseThrow();
        if (!sourcePositionAvailable) {
            type.getAnnotations().get(0).setPosition(SourcePosition.NOPOSITION);
        }
        List<CtAnnotation<?>> originalAnnotations = List.copyOf(type.getAnnotations());
        List<SourcePosition> originalPositions =
                originalAnnotations.stream().map(CtAnnotation::getPosition).toList();
        List<AnnotationSrcGroup> originalGroups = model.getAnnotationSrcGroups();
        List<AnnotationSrcFragment> originalTypeAnnotations =
                originalGroups.get(0).getFragmentsInPrintOrder();
        Sorter sorter = new Sorter(compiledConfig);

        // When
        SpoonAstModel sortedModel = sorter.sort(model).getSortedSpoonAstModel();

        // Then
        assertThat(hasReorderedAnnotations(sortedModel)).isTrue();
        assertThat(hasReorderedAnnotations(model)).isFalse();
        assertThat(sortedModel.getCompilationUnit()).isSameAs(model.getCompilationUnit());
        assertThat(type.getAnnotations()).containsExactlyElementsOf(originalAnnotations);
        assertThat(type.getAnnotations())
                .allSatisfy(annotation -> assertThat(annotation.getParent()).isSameAs(type));
        assertThat(originalAnnotations.stream().map(CtAnnotation::getPosition).toList())
                .containsExactlyElementsOf(originalPositions);
        assertThat(sortedModel.getAnnotationSrcGroups()).hasSameSizeAs(originalGroups);
        assertThat(sortedModel.getAnnotationSrcGroups().get(0).getFragmentsInPrintOrder())
                .containsExactly(originalTypeAnnotations.get(1), originalTypeAnnotations.get(0));
        assertThat(sortedModel.getAnnotationSrcGroups().get(0).getStart())
                .isEqualTo(originalGroups.get(0).getStart());
        assertThat(sortedModel.getAnnotationSrcGroups().get(0).getEndExclusive())
                .isEqualTo(originalGroups.get(0).getEndExclusive());
        assertThat(sortedModel.getAnnotationSrcGroups().get(0).getGapsInSrcOrder())
                .isSameAs(originalGroups.get(0).getGapsInSrcOrder());
        assertThat(originalTypeAnnotations)
                .extracting(fragment -> fragment.getDescriptor().getName())
                .containsExactly("Zed", "Able");
        assertThat(sortedModel.getAnnotationSrcGroups().stream()
                        .flatMap(group -> group.getFragmentsInPrintOrder().stream())
                        .toList())
                .containsExactlyInAnyOrderElementsOf(originalGroups.stream()
                        .flatMap(group -> group.getFragmentsInPrintOrder().stream())
                        .toList());
        assertThatThrownBy(() -> sortedModel.getAnnotationSrcGroups().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> sortedModel
                        .getAnnotationSrcGroups()
                        .get(0)
                        .getFragmentsInPrintOrder()
                        .clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

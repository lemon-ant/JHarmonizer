// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedAnnotationOrderingRule.*;
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.requireClasspathResourceUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter.JHarmonizer2UnifiedConverter;
import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfigMerger;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import lombok.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnnotationOrderingConfigurationTest {
    private static final String FIXTURES = "/test-cases/core/config/input/jharmonizer/annotations/";

    private UnifiedConfig defaultConfig;

    @BeforeAll
    void setUp() {
        defaultConfig = JHarmonizer2UnifiedConverter.convert2Unified(JHarmonizerConfigLoader.loadDefault());
    }

    @Test
    void compile_yamlOverlay_appliesConfiguredCriteria() {
        // Given
        FlexibleUnifiedConfig baseline = FlexibleUnifiedConfig.builder()
                .annotationOrderingRules(defaultConfig.getAnnotationOrderingRules())
                .build();
        FlexibleUnifiedConfig overlay = JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromClasspathResource(
                requireClasspathResourceUrl(FIXTURES + "criteria.yml"));
        FlexibleUnifiedConfig flexible = UnifiedConfigMerger.merge(baseline, overlay);
        UnifiedConfig merged = UnifiedConfigMerger.merge(defaultConfig, flexible);

        // When
        CompiledConfig compiled = Unified2CompiledModelCompiler.compile(merged);

        // Then
        assertThat(flexible.getAnnotationOrderingRules())
                .contains(List.of(NAME_LENGTH_ASC, DECLARATION_LENGTH_ASC, ALPHA, ARGUMENTS_ALPHA));
        assertThat(merged.getAnnotationOrderingRules())
                .containsExactly(NAME_LENGTH_ASC, DECLARATION_LENGTH_ASC, ALPHA, ARGUMENTS_ALPHA);
        assertThat(compiled.getAnnotationComparator()
                        .compare(new AnnotationDescriptor("z", 30, "A"), new AnnotationDescriptor("a", 20, "LongName")))
                .isNegative();
    }

    @Test
    void loadDefault_annotationCriteria_usesArgumentAlphabetAsFinalCriterion() {
        assertThat(defaultConfig.getAnnotationOrderingRules())
                .containsExactly(DECLARATION_LENGTH_ASC, NAME_LENGTH_ASC, ALPHA, ARGUMENTS_ALPHA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown.yml", "null-entry.yml", "object.yml"})
    void loadFlexible_invalidAnnotationCriterion_rejectsConfiguration(@NonNull String fixture) {
        URL resource = requireClasspathResourceUrl(FIXTURES + fixture);
        assertThatThrownBy(
                        () -> JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromClasspathResource(resource))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseInstanceOf(JsonMappingException.class);
    }

    @Test
    void merge_emptyAnnotationOverlay_disablesSortingAcrossBothMergeStages() {
        // Given
        FlexibleUnifiedConfig annotations = FlexibleUnifiedConfig.builder()
                .annotationOrderingRules(List.of(ALPHA))
                .build();
        FlexibleUnifiedConfig disabled =
                JHarmonizerConfigurationManager.parseFlexibleUnifiedConfigFromClasspathResource(
                        requireClasspathResourceUrl(FIXTURES + "disabled.yml"));

        // When
        FlexibleUnifiedConfig flexible = UnifiedConfigMerger.merge(annotations, disabled);
        UnifiedConfig merged = UnifiedConfigMerger.merge(defaultConfig, flexible);

        // Then
        assertThat(flexible.getAnnotationOrderingRules()).contains(List.of());
        assertThat(merged.getAnnotationOrderingRules()).isEmpty();
    }

    @Test
    void merge_unrelatedOverlay_preservesAnnotationCriteriaAcrossBothMergeStages() {
        // Given
        FlexibleUnifiedConfig annotations = FlexibleUnifiedConfig.builder()
                .annotationOrderingRules(List.of(NAME_LENGTH_ASC))
                .build();
        FlexibleUnifiedConfig unrelated =
                FlexibleUnifiedConfig.builder().backupsEnabled(false).build();

        // When
        FlexibleUnifiedConfig flexible = UnifiedConfigMerger.merge(annotations, unrelated);
        UnifiedConfig merged = UnifiedConfigMerger.merge(defaultConfig, flexible);

        // Then
        assertThat(merged.getAnnotationOrderingRules()).containsExactly(NAME_LENGTH_ASC);
        assertThat(UnifiedConfigMerger.merge(defaultConfig, unrelated).getAnnotationOrderingRules())
                .containsExactlyElementsOf(defaultConfig.getAnnotationOrderingRules());
    }
}

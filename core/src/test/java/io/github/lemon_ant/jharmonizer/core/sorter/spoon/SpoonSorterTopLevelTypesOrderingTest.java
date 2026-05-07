// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

// @jharmonizer:fully-off
// jharmonizer v1.0.1 incorrectly reorders dependent static fields in test classes;
// remove this directive once jharmonizer is upgraded to a version that respects field initialization order.
import static io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils.TEST_CASES_DIR;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
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
import io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsMode;
import io.github.lemon_ant.jharmonizer.core.testutils.SpoonTestCaseUtils;
import io.github.lemon_ant.jharmonizer.core.testutils.TestCaseResourceUtils;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonAstModel;
import java.net.URL;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class SpoonSorterTopLevelTypesOrderingTest {
    private static final String FIXTURES_RESOURCE_ROOT =
            "/" + TEST_CASES_DIR + "/core/sorter/spoon/top-level-types-ordering/valid/";
    private static final URL FIXTURES_RESOURCE_ROOT_URL =
            TestCaseResourceUtils.requireClasspathDirectoryUrl(FIXTURES_RESOURCE_ROOT);

    @Test
    void sortCompilationUnitRecursively_mainTypeFirstAndGroupedAlpha_reorderTopLevelTypes() {
        // Given
        SpoonAstModel spoonAstModel = parseFixture("MainTypeFirstFixture.java");
        SpoonSorter spoonSorter = new SpoonSorter(createCompiledConfig(UnifiedTopLevelTypesOrdering.builder()
                .mainTypeFirst(true)
                .topLevelTypeSelectors(List.of(
                        new UnifiedTopLevelTypeSelector(Set.of(UnifiedTypeKind.CLASS, UnifiedTypeKind.RECORD)),
                        new UnifiedTopLevelTypeSelector(Set.of(UnifiedTypeKind.INTERFACE)),
                        new UnifiedTopLevelTypeSelector(Set.of(UnifiedTypeKind.ENUM)),
                        new UnifiedTopLevelTypeSelector(Set.of(UnifiedTypeKind.ANNOTATION))))
                .orderingRules(List.of(UnifiedOrderingRule.VISIBILITY_DESC, UnifiedOrderingRule.ALPHA))
                .build()));

        // When
        spoonSorter.sortCompilationUnitRecursively(spoonAstModel.getCompilationUnit(), Set.of());

        // Then
        assertThat(spoonAstModel.getCompilationUnit().getDeclaredTypes().stream()
                        .map(type -> type.getSimpleName())
                        .toList())
                .containsExactly(
                        "MainTypeFirstFixture", "AlphaHelper", "BravoRecord", "AlphaContract", "ZebraKind", "Marker");
    }

    @Test
    void sortCompilationUnitRecursively_groupedPreserveWithoutMainTypeFirst_keepOriginalOrderInsideGroups() {
        // Given
        SpoonAstModel spoonAstModel = parseFixture("PreserveGroupedFixture.java");
        SpoonSorter spoonSorter = new SpoonSorter(createCompiledConfig(UnifiedTopLevelTypesOrdering.builder()
                .mainTypeFirst(false)
                .topLevelTypeSelectors(List.of(
                        new UnifiedTopLevelTypeSelector(Set.of(UnifiedTypeKind.INTERFACE, UnifiedTypeKind.ANNOTATION)),
                        new UnifiedTopLevelTypeSelector(
                                Set.of(UnifiedTypeKind.CLASS, UnifiedTypeKind.RECORD, UnifiedTypeKind.ENUM))))
                .orderingRules(List.of(UnifiedOrderingRule.PRESERVE))
                .build()));

        // When
        spoonSorter.sortCompilationUnitRecursively(spoonAstModel.getCompilationUnit(), Set.of());

        // Then
        assertThat(spoonAstModel.getCompilationUnit().getDeclaredTypes().stream()
                        .map(type -> type.getSimpleName())
                        .toList())
                .containsExactly(
                        "FirstInterface",
                        "FirstAnnotation",
                        "SecondInterface",
                        "SecondAnnotation",
                        "DeltaClass",
                        "BetaEnum",
                        "GammaRecord",
                        "AlphaClass");
    }

    @Test
    void sortCompilationUnitRecursively_singleUnifiedGroupWithVisibilityAndAlpha_sortAllTopLevelTypesTogether() {
        // Given
        SpoonAstModel spoonAstModel = parseFixture("SingleUnifiedGroupFixture.java");
        SpoonSorter spoonSorter = new SpoonSorter(createCompiledConfig(UnifiedTopLevelTypesOrdering.builder()
                .mainTypeFirst(false)
                .topLevelTypeSelectors(List.of(new UnifiedTopLevelTypeSelector(Set.of(
                        UnifiedTypeKind.CLASS,
                        UnifiedTypeKind.RECORD,
                        UnifiedTypeKind.INTERFACE,
                        UnifiedTypeKind.ENUM,
                        UnifiedTypeKind.ANNOTATION))))
                .orderingRules(List.of(UnifiedOrderingRule.VISIBILITY_DESC, UnifiedOrderingRule.ALPHA))
                .build()));

        // When
        spoonSorter.sortCompilationUnitRecursively(spoonAstModel.getCompilationUnit(), Set.of());

        // Then
        assertThat(spoonAstModel.getCompilationUnit().getDeclaredTypes().stream()
                        .map(type -> type.getSimpleName())
                        .toList())
                .containsExactly(
                        "SingleUnifiedGroupFixture",
                        "AlphaAnnotation",
                        "AlphaUtility",
                        "BetaRecord",
                        "GammaContract",
                        "ZetaKind");
    }

    @NonNull
    private static SpoonAstModel parseFixture(String fixtureFileName) {
        URL fixtureResourceUrl = TestCaseResourceUtils.resolveRelativeUrl(FIXTURES_RESOURCE_ROOT_URL, fixtureFileName);
        return SpoonTestCaseUtils.parseAstModelFromJavaFixtureResource(fixtureResourceUrl);
    }

    @NonNull
    private static CompiledConfig createCompiledConfig(UnifiedTopLevelTypesOrdering topLevelTypesOrdering) {
        UnifiedMemberGroup rootMemberGroup = UnifiedMemberGroup.builder()
                .groupName("Root")
                .selectorBlock(UnifiedMemberGroupSelectorBlock.builder().build())
                .separator(UnifiedSeparator.NONE)
                .orderingRules(List.of(UnifiedOrderingRule.PRESERVE))
                .build();
        UnifiedConfig unifiedConfig = UnifiedConfig.builder()
                .topLevelTypesOrdering(topLevelTypesOrdering)
                .formatting(new UnifiedFormatting(true, UnifiedFormatterStyle.PALANTIR, true, true, false))
                .backupsEnabled(false)
                .processingStatisticsMode(ProcessingStatisticsMode.FULL)
                .headerLine(new UnifiedHeaderLine('-', 0))
                .rootMemberGroup(rootMemberGroup)
                .build();
        return Unified2CompiledModelCompiler.compile(unifiedConfig);
    }
}

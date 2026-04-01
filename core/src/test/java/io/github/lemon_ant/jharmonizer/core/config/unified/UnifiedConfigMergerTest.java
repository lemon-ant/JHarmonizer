package io.github.lemon_ant.jharmonizer.core.config.unified;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class UnifiedConfigMergerTest {

    private static final UnifiedFormatting FORMATTING = new UnifiedFormatting(true, UnifiedFormatterStyle.PALANTIR);
    private static final UnifiedHeaderLine HEADER_LINE = new UnifiedHeaderLine('-', 2);
    private static final UnifiedMemberGroupSelectorBlock SELECTOR_BLOCK =
            UnifiedMemberGroupSelectorBlock.builder().build();
    private static final UnifiedTopLevelTypesOrdering TOP_LEVEL_TYPES_ORDERING = UnifiedTopLevelTypesOrdering.builder()
            .mainTypeFirst(true)
            .topLevelTypeSelectors(List.of(new UnifiedTopLevelTypeSelector(Set.of(UnifiedTypeKind.CLASS))))
            .orderingRules(List.of(UnifiedOrderingRule.ALPHA))
            .build();

    @Test
    void merge_rootMemberGroupsNotProvided_preservesBaselineGroups() {
        // Given
        UnifiedMemberGroup baselineFirstGroup = createGroup("Default Rule");
        UnifiedMemberGroup baselineSecondGroup = createGroup("Units");
        UnifiedConfig baselineConfig = createConfig(List.of(baselineFirstGroup, baselineSecondGroup));
        FlexibleUnifiedConfig overlayConfig = FlexibleUnifiedConfig.builder().build();

        // When
        UnifiedConfig mergedConfig = UnifiedConfigMerger.merge(baselineConfig, overlayConfig);

        // Then
        assertThat(mergedConfig.getRootMemberGroups()).containsExactly(baselineFirstGroup, baselineSecondGroup);
    }

    @Test
    void merge_rootMemberGroupsMatchedAndNewNamesProvided_insertsNewGroupsFirstAndKeepsReplacementPositions() {
        // Given
        UnifiedMemberGroup baselineDefaultGroup = createGroup("Default Rule");
        UnifiedMemberGroup baselineUnitsGroup = createGroup("Units");
        UnifiedMemberGroup baselineFallbackGroup = createGroup("Fallback");
        UnifiedConfig baselineConfig =
                createConfig(List.of(baselineDefaultGroup, baselineUnitsGroup, baselineFallbackGroup));
        UnifiedMemberGroup replacementFallbackGroup = createGroup("Fallback");
        UnifiedMemberGroup newUtilityGroup = createGroup("Utility");
        UnifiedMemberGroup replacementDefaultGroup = createGroup("Default Rule");
        UnifiedMemberGroup newAuditGroup = createGroup("Audit");
        FlexibleUnifiedConfig overlayConfig = FlexibleUnifiedConfig.builder()
                .rootMemberGroups(
                        List.of(replacementFallbackGroup, newUtilityGroup, replacementDefaultGroup, newAuditGroup))
                .build();

        // When
        UnifiedConfig mergedConfig = UnifiedConfigMerger.merge(baselineConfig, overlayConfig);

        // Then
        assertThat(mergedConfig.getRootMemberGroups())
                .containsExactly(
                        newUtilityGroup,
                        newAuditGroup,
                        replacementDefaultGroup,
                        baselineUnitsGroup,
                        replacementFallbackGroup);
    }

    @Test
    void merge_rootMemberGroupsMatchedName_replacesWholeRootGroupWithoutDeepMerge() {
        // Given
        UnifiedMemberGroup baselineNestedGroup =
                createGroup("Default Rule", List.of(createGroup("Baseline Fields"), createGroup("Baseline Methods")));
        UnifiedMemberGroup baselineFallbackGroup = createGroup("Fallback");
        UnifiedConfig baselineConfig = createConfig(List.of(baselineNestedGroup, baselineFallbackGroup));
        UnifiedMemberGroup overlayNestedGroup = createGroup("Default Rule", List.of(createGroup("Overlay Methods")));
        FlexibleUnifiedConfig overlayConfig = FlexibleUnifiedConfig.builder()
                .rootMemberGroups(List.of(overlayNestedGroup))
                .build();

        // When
        UnifiedConfig mergedConfig = UnifiedConfigMerger.merge(baselineConfig, overlayConfig);

        // Then
        assertThat(mergedConfig.getRootMemberGroups()).containsExactly(overlayNestedGroup, baselineFallbackGroup);
        assertThat(mergedConfig.getRootMemberGroups().getFirst().getMemberSubGroups())
                .extracting(UnifiedMemberGroup::getGroupName)
                .containsExactly("Overlay Methods");
    }

    @Test
    void merge_overlayRootGroupNameMissing_prependsUnnamedGroup() {
        // Given
        UnifiedMemberGroup baselineDefaultGroup = createGroup("Default Rule");
        UnifiedMemberGroup unnamedOverlayGroup = createGroup(null);
        FlexibleUnifiedConfig overlayConfig = FlexibleUnifiedConfig.builder()
                .rootMemberGroups(List.of(unnamedOverlayGroup))
                .build();

        // When
        UnifiedConfig mergedConfig =
                UnifiedConfigMerger.merge(createConfig(List.of(baselineDefaultGroup)), overlayConfig);

        // Then
        assertThat(mergedConfig.getRootMemberGroups()).containsExactly(unnamedOverlayGroup, baselineDefaultGroup);
    }

    @Test
    void merge_baselineRootGroupNameMissing_preservesUnnamedGroupInOriginalPosition() {
        // Given
        UnifiedMemberGroup baselineNamedGroup = createGroup("Default Rule");
        UnifiedMemberGroup unnamedBaselineGroup = createGroup(null);
        UnifiedMemberGroup baselineTrailingNamedGroup = createGroup("Trailing");
        UnifiedMemberGroup overlayReplacementNamedGroup = createGroup("Default Rule");
        UnifiedMemberGroup overlayNewUnnamedGroup = createGroup(null);
        FlexibleUnifiedConfig overlayConfig = FlexibleUnifiedConfig.builder()
                .rootMemberGroups(List.of(overlayReplacementNamedGroup, overlayNewUnnamedGroup))
                .build();

        // When
        UnifiedConfig mergedConfig = UnifiedConfigMerger.merge(
                createConfig(List.of(baselineNamedGroup, unnamedBaselineGroup, baselineTrailingNamedGroup)),
                overlayConfig);

        // Then
        assertThat(mergedConfig.getRootMemberGroups())
                .containsExactly(
                        overlayNewUnnamedGroup,
                        overlayReplacementNamedGroup,
                        unnamedBaselineGroup,
                        baselineTrailingNamedGroup);
    }

    @Test
    void merge_duplicateBaselineNames_throwsException() {
        // Given
        UnifiedMemberGroup firstBaselineGroup = createGroup("Default Rule");
        UnifiedMemberGroup duplicateBaselineGroup = createGroup("Default Rule", List.of(createGroup("Duplicate")));
        FlexibleUnifiedConfig overlayConfig =
                FlexibleUnifiedConfig.builder().rootMemberGroups(List.of()).build();

        // When / Then
        assertThatThrownBy(() -> UnifiedConfigMerger.merge(
                        createConfig(List.of(firstBaselineGroup, duplicateBaselineGroup)), overlayConfig))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Baseline root member group names must be unique");
    }

    @Test
    void merge_flexibleOverlayProvided_overridesBackupsAndKeepsBaselineFields() {
        // Given
        FlexibleUnifiedConfig baselineConfig = FlexibleUnifiedConfig.builder()
                .topLevelTypesOrdering(TOP_LEVEL_TYPES_ORDERING)
                .formatting(FORMATTING)
                .backupsEnabled(true)
                .headerLine(HEADER_LINE)
                .rootMemberGroups(List.of(createGroup("Default Rule")))
                .build();
        FlexibleUnifiedConfig overlayConfig =
                FlexibleUnifiedConfig.builder().backupsEnabled(false).build();

        // When
        FlexibleUnifiedConfig mergedConfig = UnifiedConfigMerger.merge(baselineConfig, overlayConfig);

        // Then
        assertThat(mergedConfig.getBackupsEnabled()).contains(false);
        assertThat(mergedConfig.getFormatting()).contains(FORMATTING);
        assertThat(mergedConfig.getTopLevelTypesOrdering()).contains(TOP_LEVEL_TYPES_ORDERING);
        assertThat(mergedConfig.getHeaderLine()).contains(HEADER_LINE);
        assertThat(mergedConfig.getRootMemberGroups()).contains(List.of(createGroup("Default Rule")));
    }

    @Test
    void merge_flexibleConfigsWithoutRootGroupsProvided_keepsRootGroupsAbsent() {
        // Given
        FlexibleUnifiedConfig baselineConfig =
                FlexibleUnifiedConfig.builder().backupsEnabled(true).build();
        FlexibleUnifiedConfig overlayConfig =
                FlexibleUnifiedConfig.builder().backupsEnabled(false).build();

        // When
        FlexibleUnifiedConfig mergedConfig = UnifiedConfigMerger.merge(baselineConfig, overlayConfig);

        // Then
        assertThat(mergedConfig.getBackupsEnabled()).contains(false);
        assertThat(mergedConfig.getRootMemberGroups()).isEmpty();
    }

    @Test
    void merge_flexibleRootGroupsProvided_mergesRootGroupsLikeStrictMerge() {
        // Given
        UnifiedMemberGroup baselineDefaultGroup = createGroup("Default Rule");
        UnifiedMemberGroup baselineUnitsGroup = createGroup("Units");
        FlexibleUnifiedConfig baselineConfig = FlexibleUnifiedConfig.builder()
                .rootMemberGroups(List.of(baselineDefaultGroup, baselineUnitsGroup))
                .build();
        UnifiedMemberGroup replacementDefaultGroup = createGroup("Default Rule");
        UnifiedMemberGroup newAuditGroup = createGroup("Audit");
        FlexibleUnifiedConfig overlayConfig = FlexibleUnifiedConfig.builder()
                .rootMemberGroups(List.of(replacementDefaultGroup, newAuditGroup))
                .build();

        // When
        FlexibleUnifiedConfig mergedConfig = UnifiedConfigMerger.merge(baselineConfig, overlayConfig);

        // Then
        assertThat(mergedConfig.getRootMemberGroups())
                .contains(List.of(newAuditGroup, replacementDefaultGroup, baselineUnitsGroup));
    }

    @Test
    void merge_flexibleBaselineUnnamedGroupsProvided_preservesUnnamedPositions() {
        // Given
        UnifiedMemberGroup baselineNamedGroup = createGroup("Default Rule");
        UnifiedMemberGroup baselineUnnamedGroup = createGroup(null);
        UnifiedMemberGroup baselineTrailingNamedGroup = createGroup("Trailing");
        FlexibleUnifiedConfig baselineConfig = FlexibleUnifiedConfig.builder()
                .rootMemberGroups(List.of(baselineNamedGroup, baselineUnnamedGroup, baselineTrailingNamedGroup))
                .build();
        UnifiedMemberGroup overlayReplacementNamedGroup = createGroup("Default Rule");
        UnifiedMemberGroup overlayNewNamedGroup = createGroup("Audit");
        FlexibleUnifiedConfig overlayConfig = FlexibleUnifiedConfig.builder()
                .rootMemberGroups(List.of(overlayReplacementNamedGroup, overlayNewNamedGroup))
                .build();

        // When
        FlexibleUnifiedConfig mergedConfig = UnifiedConfigMerger.merge(baselineConfig, overlayConfig);

        // Then
        assertThat(mergedConfig.getRootMemberGroups())
                .contains(List.of(
                        overlayNewNamedGroup,
                        overlayReplacementNamedGroup,
                        baselineUnnamedGroup,
                        baselineTrailingNamedGroup));
    }

    @Test
    void merge_flexibleRootGroupsMissingOnBothSides_keepsRootMemberGroupsEmpty() {
        // Given
        FlexibleUnifiedConfig baselineConfig = FlexibleUnifiedConfig.builder().build();
        FlexibleUnifiedConfig overlayConfig = FlexibleUnifiedConfig.builder().build();

        // When
        FlexibleUnifiedConfig mergedConfig = UnifiedConfigMerger.merge(baselineConfig, overlayConfig);

        // Then
        assertThat(mergedConfig.getRootMemberGroups()).contains(List.of());
    }

    @NonNull
    private static UnifiedConfig createConfig(List<UnifiedMemberGroup> rootMemberGroups) {
        return UnifiedConfig.builder()
                .topLevelTypesOrdering(TOP_LEVEL_TYPES_ORDERING)
                .formatting(FORMATTING)
                .backupsEnabled(true)
                .printProcessingStatistics(true)
                .headerLine(HEADER_LINE)
                .rootMemberGroups(rootMemberGroups)
                .build();
    }

    @NonNull
    private static UnifiedMemberGroup createGroup(String groupName) {
        return createGroup(groupName, List.of());
    }

    @NonNull
    private static UnifiedMemberGroup createGroup(String groupName, List<UnifiedMemberGroup> memberSubGroups) {
        return UnifiedMemberGroup.builder()
                .groupName(groupName)
                .memberSubGroups(memberSubGroups)
                .selectorBlock(SELECTOR_BLOCK)
                .separator(UnifiedSeparator.NONE)
                .orderingRule(UnifiedOrderingRule.ALPHA)
                .build();
    }
}

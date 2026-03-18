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
        FlexibleUnifiedConfig overlayConfig = new FlexibleUnifiedConfig(null, null, null, null, null);

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
        FlexibleUnifiedConfig overlayConfig = new FlexibleUnifiedConfig(
                null,
                null,
                null,
                null,
                List.of(replacementFallbackGroup, newUtilityGroup, replacementDefaultGroup, newAuditGroup));

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
        FlexibleUnifiedConfig overlayConfig =
                new FlexibleUnifiedConfig(null, null, null, null, List.of(overlayNestedGroup));

        // When
        UnifiedConfig mergedConfig = UnifiedConfigMerger.merge(baselineConfig, overlayConfig);

        // Then
        assertThat(mergedConfig.getRootMemberGroups()).containsExactly(overlayNestedGroup, baselineFallbackGroup);
        assertThat(mergedConfig.getRootMemberGroups().getFirst().getMemberSubGroups())
                .extracting(UnifiedMemberGroup::getGroupName)
                .containsExactly("Overlay Methods");
    }

    @Test
    void merge_duplicateOverlayNames_usesLastReplacement() {
        // Given
        UnifiedMemberGroup baselineDefaultGroup = createGroup("Default Rule");
        UnifiedMemberGroup baselineFallbackGroup = createGroup("Fallback");
        UnifiedConfig baselineConfig = createConfig(List.of(baselineDefaultGroup, baselineFallbackGroup));
        UnifiedMemberGroup firstReplacementGroup = createGroup("Default Rule", List.of(createGroup("First Overlay")));
        UnifiedMemberGroup lastReplacementGroup = createGroup("Default Rule", List.of(createGroup("Last Overlay")));
        FlexibleUnifiedConfig overlayConfig =
                new FlexibleUnifiedConfig(null, null, null, null, List.of(firstReplacementGroup, lastReplacementGroup));

        // When
        UnifiedConfig mergedConfig = UnifiedConfigMerger.merge(baselineConfig, overlayConfig);

        // Then
        assertThat(mergedConfig.getRootMemberGroups()).containsExactly(lastReplacementGroup, baselineFallbackGroup);
        assertThat(mergedConfig.getRootMemberGroups().getFirst().getMemberSubGroups())
                .extracting(UnifiedMemberGroup::getGroupName)
                .containsExactly("Last Overlay");
    }

    @Test
    void merge_overlayRootGroupNameMissing_prependsUnnamedGroup() {
        // Given
        UnifiedMemberGroup baselineDefaultGroup = createGroup("Default Rule");
        UnifiedMemberGroup unnamedOverlayGroup = createGroup(null);
        FlexibleUnifiedConfig overlayConfig =
                new FlexibleUnifiedConfig(null, null, null, null, List.of(unnamedOverlayGroup));

        // When
        UnifiedConfig mergedConfig =
                UnifiedConfigMerger.merge(createConfig(List.of(baselineDefaultGroup)), overlayConfig);

        // Then
        assertThat(mergedConfig.getRootMemberGroups()).containsExactly(unnamedOverlayGroup, baselineDefaultGroup);
    }

    @Test
    void merge_baselineRootGroupNameMissing_throwsException() {
        // Given
        UnifiedMemberGroup unnamedBaselineGroup = createGroup(null);
        FlexibleUnifiedConfig overlayConfig = new FlexibleUnifiedConfig(null, null, null, null, List.of());

        // When / Then
        assertThatThrownBy(() -> UnifiedConfigMerger.merge(createConfig(List.of(unnamedBaselineGroup)), overlayConfig))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Baseline root member groups must have non-null names");
    }

    @NonNull
    private static UnifiedConfig createConfig(List<UnifiedMemberGroup> rootMemberGroups) {
        return UnifiedConfig.builder()
                .topLevelTypesOrdering(TOP_LEVEL_TYPES_ORDERING)
                .formatting(FORMATTING)
                .backupsEnabled(true)
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

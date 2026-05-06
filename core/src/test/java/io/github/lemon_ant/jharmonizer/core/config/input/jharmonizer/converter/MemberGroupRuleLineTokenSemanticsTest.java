// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledConfig;
import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.Unified2CompiledModelCompiler;
import io.github.lemon_ant.jharmonizer.core.config.unified.DeclarationModifier;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberKind;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedHeaderLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupRuleLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedOrderingRule;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypeSelector;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTypeKind;
import io.github.lemon_ant.jharmonizer.core.processing_stat.ProcessingStatisticsMode;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import org.junit.jupiter.api.Test;

class MemberGroupRuleLineTokenSemanticsTest {

    @Test
    void compileSelectorBlock_staticInitializerTokens_requireInitializerKindAndStaticModifier() {
        // Given
        UnifiedMemberGroupRuleLine staticInitializerRuleLine =
                MemberGroupRuleLineParser.parse(Set.of("STATIC", "Initializer"));
        UnifiedMemberGroupSelectorBlock selectorBlock =
                new UnifiedMemberGroupSelectorBlock(Set.of(), Set.of(staticInitializerRuleLine));
        UnifiedMemberGroup rootGroup = UnifiedMemberGroup.builder()
                .groupName("StaticInitializers")
                .selectorBlock(selectorBlock)
                .separator(UnifiedSeparator.NONE)
                .orderingRules(List.of(UnifiedOrderingRule.PRESERVE))
                .build();
        CompiledMemberGroup compiledRootGroup = compileSingleRootGroup(rootGroup);
        MemberDescriptor staticInitializerDescriptor = MemberDescriptor.builder()
                .memberKind(MemberKind.INIT_BLOCK)
                .declarationModifier(DeclarationModifier.STATIC)
                .build();
        MemberDescriptor instanceInitializerDescriptor =
                MemberDescriptor.builder().memberKind(MemberKind.INIT_BLOCK).build();

        // When
        boolean matchesStaticInitializer = compiledRootGroup.getSelectorBlock().match(staticInitializerDescriptor);
        boolean matchesInstanceInitializer =
                compiledRootGroup.getSelectorBlock().match(instanceInitializerDescriptor);

        // Then
        assertThat(matchesStaticInitializer).isTrue();
        assertThat(matchesInstanceInitializer).isFalse();
    }

    @Test
    void compileSelectorBlock_instanceInitializerTokens_includeInitializerButExcludeStatic() {
        // Given
        UnifiedMemberGroupRuleLine initializerRuleLine = MemberGroupRuleLineParser.parse(Set.of("initializer"));
        UnifiedMemberGroupRuleLine staticModifierRuleLine = MemberGroupRuleLineParser.parse(Set.of("static"));
        UnifiedMemberGroupSelectorBlock selectorBlock =
                new UnifiedMemberGroupSelectorBlock(Set.of(staticModifierRuleLine), Set.of(initializerRuleLine));
        UnifiedMemberGroup rootGroup = UnifiedMemberGroup.builder()
                .groupName("InstanceInitializers")
                .selectorBlock(selectorBlock)
                .separator(UnifiedSeparator.NONE)
                .orderingRules(List.of(UnifiedOrderingRule.PRESERVE))
                .build();
        CompiledMemberGroup compiledRootGroup = compileSingleRootGroup(rootGroup);
        MemberDescriptor staticInitializerDescriptor = MemberDescriptor.builder()
                .memberKind(MemberKind.INIT_BLOCK)
                .declarationModifier(DeclarationModifier.STATIC)
                .build();
        MemberDescriptor instanceInitializerDescriptor =
                MemberDescriptor.builder().memberKind(MemberKind.INIT_BLOCK).build();

        // When
        boolean matchesStaticInitializer = compiledRootGroup.getSelectorBlock().match(staticInitializerDescriptor);
        boolean matchesInstanceInitializer =
                compiledRootGroup.getSelectorBlock().match(instanceInitializerDescriptor);

        // Then
        assertThat(matchesStaticInitializer).isFalse();
        assertThat(matchesInstanceInitializer).isTrue();
    }

    @NonNull
    private static CompiledMemberGroup compileSingleRootGroup(UnifiedMemberGroup rootGroup) {
        UnifiedConfig unifiedConfig = UnifiedConfig.builder()
                .formatting(new UnifiedFormatting(true, UnifiedFormatterStyle.PALANTIR, true, true, false))
                .backupsEnabled(false)
                .processingStatisticsMode(ProcessingStatisticsMode.FULL)
                .headerLine(new UnifiedHeaderLine('-', 0))
                .topLevelTypesOrdering(createMinimalTopLevelTypesOrdering())
                .rootMemberGroup(rootGroup)
                .build();

        CompiledConfig compiledConfig = Unified2CompiledModelCompiler.compile(unifiedConfig);
        return compiledConfig.getRootMemberGroups().getFirst();
    }

    @NonNull
    private static UnifiedTopLevelTypesOrdering createMinimalTopLevelTypesOrdering() {
        return UnifiedTopLevelTypesOrdering.builder()
                .mainTypeFirst(false)
                .topLevelTypeSelectors(List.of(new UnifiedTopLevelTypeSelector(Set.of(UnifiedTypeKind.CLASS))))
                .orderingRules(List.of(UnifiedOrderingRule.ALPHA))
                .build();
    }
}

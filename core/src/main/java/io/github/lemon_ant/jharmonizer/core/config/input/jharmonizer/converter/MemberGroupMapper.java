package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerOrderingRule;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup.UnifiedMemberGroupBuilder;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedOrderingRule;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Maps vendor JHarmonizerMemberGroup to unified counterpart.
 */
@UtilityClass
final class MemberGroupMapper {

    /**
     * Performs the map.
     * @param srcMemberGroup the source member group
     * @return the result
     */
    @NonNull
    static UnifiedMemberGroup map(@NonNull JHarmonizerMemberGroup srcMemberGroup) {
        UnifiedMemberGroupSelectorBlock.UnifiedMemberGroupSelectorBlockBuilder selectorBlockBuilder =
                UnifiedMemberGroupSelectorBlock.builder();
        srcMemberGroup.getIncludes().stream()
                .map(MemberGroupRuleLineParser::parse)
                .forEach(selectorBlockBuilder::include);
        srcMemberGroup.getExcludes().stream()
                .map(MemberGroupRuleLineParser::parse)
                .forEach(selectorBlockBuilder::exclude);
        UnifiedMemberGroupSelectorBlock selectorBlock = selectorBlockBuilder.build();

        List<UnifiedOrderingRule> orderingRules = Optional.ofNullable(srcMemberGroup.getOrderingRules())
                .map(rawRules -> rawRules.stream()
                        .map(JHarmonizerOrderingRule::getUnifiedOrderingRule)
                        .toList())
                .orElse(null);

        UnifiedMemberGroupBuilder unifiedMemberGroupBuilder = UnifiedMemberGroup.builder()
                .groupName(srcMemberGroup.getName())
                .selectorBlock(selectorBlock)
                .orderingRules(orderingRules)
                .keepAccessorsTogether(srcMemberGroup.getKeepAccessorsTogether())
                .relaxedForwardReferences(srcMemberGroup.getRelaxedForwardReferences())
                .separator(Optional.ofNullable(srcMemberGroup.getSeparator())
                        .map(separator -> separator.getUnifiedSeparator())
                        .orElse(null));

        srcMemberGroup.getMemberSubGroups().stream()
                .map(MemberGroupMapper::map)
                .forEach(unifiedMemberGroupBuilder::memberSubGroup);
        return unifiedMemberGroupBuilder.build();
    }
}

package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerOrderingRule;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup.UnifiedMemberGroupBuilder;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedOrderingRule;
import java.util.List;
import lombok.experimental.UtilityClass;

/**
 * Maps vendor JHarmonizerMemberGroup to unified counterpart.
 */
@UtilityClass
final class MemberGroupMapper {

    static UnifiedMemberGroup map(JHarmonizerMemberGroup srcMemberGroup) {
        UnifiedMemberGroupSelectorBlock.UnifiedMemberGroupSelectorBlockBuilder selectorBlockBuilder =
                UnifiedMemberGroupSelectorBlock.builder();
        srcMemberGroup.getIncludes().stream()
                .map(MemberGroupRuleLineParser::parse)
                .forEach(selectorBlockBuilder::include);
        srcMemberGroup.getExcludes().stream()
                .map(MemberGroupRuleLineParser::parse)
                .forEach(selectorBlockBuilder::exclude);
        UnifiedMemberGroupSelectorBlock selectorBlock = selectorBlockBuilder.build();

        List<UnifiedOrderingRule> orderingRules = srcMemberGroup.getOrderingRules().stream()
                .map(JHarmonizerOrderingRule::getUnifiedOrderingRule)
                .toList();

        UnifiedMemberGroupBuilder unifiedMemberGroupBuilder = UnifiedMemberGroup.builder()
                .groupName(srcMemberGroup.getName())
                .selectorBlock(selectorBlock)
                .orderingRules(orderingRules)
                .keepAccessorsTogether(srcMemberGroup.getKeepAccessorsTogether())
                .separator(srcMemberGroup.getSeparator().getUnifiedSeparator());

        srcMemberGroup.getMemberSubGroups().stream()
                .map(MemberGroupMapper::map)
                .forEach(unifiedMemberGroupBuilder::memberSubGroup);
        return unifiedMemberGroupBuilder.build();
    }
}

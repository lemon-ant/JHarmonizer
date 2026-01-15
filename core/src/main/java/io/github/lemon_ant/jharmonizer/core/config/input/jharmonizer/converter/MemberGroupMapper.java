package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerSortKey;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup.UnifiedMemberGroupBuilder;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroupSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortKey;
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

        List<UnifiedSortKey> sortKeys = srcMemberGroup.getSortKeys().stream()
                .map(JHarmonizerSortKey::getUnifiedSortKey)
                .toList();

        UnifiedMemberGroupBuilder unifiedMemberGroupBuilder = UnifiedMemberGroup.builder()
                .groupName(srcMemberGroup.getName())
                .selectorBlock(selectorBlock)
                .sortKeys(sortKeys)
                .keepAccessorsTogether(srcMemberGroup.isKeepAccessorsTogether())
                .separator(srcMemberGroup.getSeparator().getUnifiedSeparator());

        srcMemberGroup.getMemberSubGroups().stream()
                .map(MemberGroupMapper::map)
                .forEach(unifiedMemberGroupBuilder::memberSubGroup);
        return unifiedMemberGroupBuilder.build();
    }
}

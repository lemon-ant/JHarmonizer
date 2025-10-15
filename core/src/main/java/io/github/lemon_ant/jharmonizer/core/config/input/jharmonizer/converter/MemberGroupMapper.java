package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.MemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.SortKey;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup.UnifiedMemberGroupBuilder;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSelectorBlock;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortKey;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSortingBehavior;
import java.util.List;
import lombok.experimental.UtilityClass;

/** Maps vendor MemberGroup to unified counterpart. */
@UtilityClass
final class MemberGroupMapper {

    static UnifiedMemberGroup map(MemberGroup srcMemberGroup) {
        UnifiedSelectorBlock.UnifiedSelectorBlockBuilder selectorBlockBuilder = UnifiedSelectorBlock.builder();
        srcMemberGroup.getIncludes().stream().map(RuleLineParser::parse).forEach(selectorBlockBuilder::include);
        srcMemberGroup.getExcludes().stream().map(RuleLineParser::parse).forEach(selectorBlockBuilder::exclude);
        UnifiedSelectorBlock selectorBlock = selectorBlockBuilder.build();

        List<UnifiedSortKey> sortKeys = srcMemberGroup.getSortKeys().stream()
                .map(SortKey::getUnifiedSortKey)
                .toList();

        UnifiedSortingBehavior sortingBehavior = UnifiedSortingBehavior.builder()
                .unifiedSortKeys(sortKeys)
                .keepAccessorsTogether(srcMemberGroup.isKeepAccessorsTogether())
                .build();

        UnifiedMemberGroupBuilder unifiedMemberGroupBuilder = UnifiedMemberGroup.builder()
                .groupName(srcMemberGroup.getName())
                .selectorBlock(selectorBlock)
                .sortingBehavior(sortingBehavior);

        srcMemberGroup.getMemberSubGroups().stream()
                .map(MemberGroupMapper::map)
                .forEach(unifiedMemberGroupBuilder::memberSubGroup);
        return unifiedMemberGroupBuilder.build();
    }
}

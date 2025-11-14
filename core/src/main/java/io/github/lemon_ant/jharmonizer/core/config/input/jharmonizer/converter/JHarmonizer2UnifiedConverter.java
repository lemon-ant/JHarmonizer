package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatterStyle;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedHeaderLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Thin facade: routes conversion to focused mappers.
 * This dramatically reduces PMD metrics (TooManyMethods, WMC, Coupling).
 */
@Slf4j
@UtilityClass
public final class JHarmonizer2UnifiedConverter {

    public static UnifiedConfig convert2Unified(@NonNull JHarmonizerConfig vendor) {
        // 1) Top-level types ordering
        UnifiedTopLevelTypesOrdering top = TopLevelTypesOrderingMapper.map(vendor.getTopLevelTypesOrdering());

        // 2) fixImports
        boolean fixImports = vendor.getFormatting().isFixImports();

        // 3) formatterStyle
        UnifiedFormatterStyle style = vendor.getFormatting().getFormatterStyle().getUnifiedFormatterStyle();

        boolean backupsEnabled = vendor.isBackupsEnabled();

        // 4) headerLine
        UnifiedHeaderLine header = new UnifiedHeaderLine(
                vendor.getHeaderLine().getCharacter(), vendor.getHeaderLine().getLeftPadding());

        // 5) root member groups
        List<UnifiedMemberGroup> root =
                vendor.getMemberGroups().stream().map(MemberGroupMapper::map).toList();

        return UnifiedConfig.builder()
                .topLevelTypesOrdering(top)
                .formatting(new UnifiedFormatting(fixImports, style))
            .backupsEnabled(backupsEnabled)
                .headerLine(header)
                .rootMemberGroups(root)
                .build();
    }
}

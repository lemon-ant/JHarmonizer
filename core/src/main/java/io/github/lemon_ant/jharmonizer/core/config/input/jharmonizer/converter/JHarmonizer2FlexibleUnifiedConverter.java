package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerFlexibleConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerFormatting;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerHeaderLine;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerTopLevelTypesOrdering;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedHeaderLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JHarmonizer2FlexibleUnifiedConverter {

    @NonNull
    public static FlexibleUnifiedConfig convert2Flexible(@NonNull JHarmonizerFlexibleConfig vendorConfig) {
        UnifiedTopLevelTypesOrdering topLevelTypesOrdering = readTopLevelTypesOrdering(vendorConfig);
        UnifiedFormatting formatting = readFormatting(vendorConfig);
        Boolean backupsEnabled = vendorConfig.getBackupsEnabled().orElse(null);
        UnifiedHeaderLine headerLine = readHeaderLine(vendorConfig);
        List<UnifiedMemberGroup> rootMemberGroups = readRootMemberGroups(vendorConfig);
        return new FlexibleUnifiedConfig(
                topLevelTypesOrdering, formatting, backupsEnabled, headerLine, rootMemberGroups);
    }

    private static UnifiedTopLevelTypesOrdering readTopLevelTypesOrdering(JHarmonizerFlexibleConfig vendorConfig) {
        JHarmonizerTopLevelTypesOrdering vendorTopLevelTypesOrdering =
                vendorConfig.getTopLevelTypesOrdering().orElse(null);
        return vendorTopLevelTypesOrdering != null
                ? TopLevelTypesOrderingMapper.map(vendorTopLevelTypesOrdering)
                : null;
    }

    private static UnifiedFormatting readFormatting(JHarmonizerFlexibleConfig vendorConfig) {
        JHarmonizerFormatting vendorFormatting = vendorConfig.getFormatting().orElse(null);
        if (vendorFormatting == null) {
            return null;
        }
        return new UnifiedFormatting(
                vendorFormatting.isFixImports(),
                vendorFormatting.getFormatterStyle().getUnifiedFormatterStyle());
    }

    private static UnifiedHeaderLine readHeaderLine(JHarmonizerFlexibleConfig vendorConfig) {
        JHarmonizerHeaderLine vendorHeaderLine = vendorConfig.getHeaderLine().orElse(null);
        if (vendorHeaderLine == null) {
            return null;
        }
        return new UnifiedHeaderLine(vendorHeaderLine.getCharacter(), vendorHeaderLine.getLeftPadding());
    }

    private static List<UnifiedMemberGroup> readRootMemberGroups(JHarmonizerFlexibleConfig vendorConfig) {
        List<JHarmonizerMemberGroup> vendorMemberGroups =
                vendorConfig.getMemberGroups().orElse(null);
        return vendorMemberGroups != null
                ? vendorMemberGroups.stream().map(MemberGroupMapper::map).toList()
                : null;
    }
}

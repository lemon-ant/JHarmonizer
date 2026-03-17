package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerFlexibleConfig;
import io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model.JHarmonizerFormatting;
import io.github.lemon_ant.jharmonizer.core.config.unified.FlexibleUnifiedConfig;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedHeaderLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JHarmonizerFlexible2FlexibleUnifiedConverter {

    @NonNull
    public static FlexibleUnifiedConfig convert2FlexibleUnified(@NonNull JHarmonizerFlexibleConfig vendorConfig) {
        UnifiedTopLevelTypesOrdering topLevelTypesOrdering = readTopLevelTypesOrdering(vendorConfig);
        UnifiedFormatting formatting = readFormatting(vendorConfig);
        Boolean backupsEnabled = vendorConfig.getOptionalBackupsEnabled().orElse(null);
        UnifiedHeaderLine headerLine = readHeaderLine(vendorConfig);
        List<UnifiedMemberGroup> rootMemberGroups = readRootMemberGroups(vendorConfig);
        return new FlexibleUnifiedConfig(
                topLevelTypesOrdering, formatting, backupsEnabled, headerLine, rootMemberGroups);
    }

    private static UnifiedTopLevelTypesOrdering readTopLevelTypesOrdering(
            @NonNull JHarmonizerFlexibleConfig vendorConfig) {
        return vendorConfig
                .getOptionalTopLevelTypesOrdering()
                .map(TopLevelTypesOrderingMapper::map)
                .orElse(null);
    }

    private static UnifiedFormatting readFormatting(@NonNull JHarmonizerFlexibleConfig vendorConfig) {
        return vendorConfig
                .getOptionalFormatting()
                .map(JHarmonizerFlexible2FlexibleUnifiedConverter::mapFormatting)
                .orElse(null);
    }

    private static UnifiedHeaderLine readHeaderLine(@NonNull JHarmonizerFlexibleConfig vendorConfig) {
        return vendorConfig
                .getOptionalHeaderLine()
                .map(headerLine -> new UnifiedHeaderLine(headerLine.getCharacter(), headerLine.getLeftPadding()))
                .orElse(null);
    }

    private static List<UnifiedMemberGroup> readRootMemberGroups(@NonNull JHarmonizerFlexibleConfig vendorConfig) {
        return vendorConfig
                .getOptionalMemberGroups()
                .map(memberGroups ->
                        memberGroups.stream().map(MemberGroupMapper::map).toList())
                .orElse(null);
    }

    @NonNull
    private static UnifiedFormatting mapFormatting(@NonNull JHarmonizerFormatting vendorFormatting) {
        return new UnifiedFormatting(
                vendorFormatting.isFixImports(),
                vendorFormatting.getFormatterStyle().getUnifiedFormatterStyle());
    }
}

package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.converter;

import edu.umd.cs.findbugs.annotations.Nullable;
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

    /**
     * Converts a flexible JHarmonizer config into the flexible unified config model.
     *
     * @param vendorConfig the flexible vendor config to convert
     * @return the converted flexible unified config
     */
    @NonNull
    public static FlexibleUnifiedConfig convert2FlexibleUnified(@NonNull JHarmonizerFlexibleConfig vendorConfig) {
        UnifiedTopLevelTypesOrdering topLevelTypesOrdering = readTopLevelTypesOrdering(vendorConfig);
        UnifiedFormatting formatting = readFormatting(vendorConfig);
        Boolean backupsEnabled = vendorConfig.getBackupsEnabled().orElse(null);
        Boolean printProcessingStatistics =
                vendorConfig.getPrintProcessingStatistics().orElse(null);
        UnifiedHeaderLine headerLine = readHeaderLine(vendorConfig);
        List<UnifiedMemberGroup> rootMemberGroups = readRootMemberGroups(vendorConfig);
        FlexibleUnifiedConfig.FlexibleUnifiedConfigBuilder builder = FlexibleUnifiedConfig.builder()
                .topLevelTypesOrdering(topLevelTypesOrdering)
                .formatting(formatting)
                .backupsEnabled(backupsEnabled)
                .printProcessingStatistics(printProcessingStatistics)
                .headerLine(headerLine);
        if (rootMemberGroups != null) {
            builder.rootMemberGroups(rootMemberGroups);
        }
        return builder.build();
    }

    @Nullable
    private static UnifiedTopLevelTypesOrdering readTopLevelTypesOrdering(JHarmonizerFlexibleConfig vendorConfig) {
        return vendorConfig
                .getTopLevelTypesOrdering()
                .map(TopLevelTypesOrderingMapper::map)
                .orElse(null);
    }

    @Nullable
    private static UnifiedFormatting readFormatting(JHarmonizerFlexibleConfig vendorConfig) {
        return vendorConfig
                .getFormatting()
                .map(JHarmonizerFlexible2FlexibleUnifiedConverter::mapFormatting)
                .orElse(null);
    }

    @Nullable
    private static UnifiedHeaderLine readHeaderLine(JHarmonizerFlexibleConfig vendorConfig) {
        return vendorConfig
                .getHeaderLine()
                .map(headerLine -> new UnifiedHeaderLine(headerLine.getCharacter(), headerLine.getLeftPadding()))
                .orElse(null);
    }

    @Nullable
    private static List<UnifiedMemberGroup> readRootMemberGroups(JHarmonizerFlexibleConfig vendorConfig) {
        return vendorConfig
                .getMemberGroups()
                .map(memberGroups ->
                        memberGroups.stream().map(MemberGroupMapper::map).toList())
                .orElse(null);
    }

    @NonNull
    private static UnifiedFormatting mapFormatting(JHarmonizerFormatting vendorFormatting) {
        return new UnifiedFormatting(
                vendorFormatting.isFixImports(),
                vendorFormatting.getFormatterStyle().getUnifiedFormatterStyle(),
                vendorFormatting.isBlankLineAfterTypeHeader(),
                vendorFormatting.isBlankLineBeforeAnnotation(),
                vendorFormatting.isBlankLineBeforeComment());
    }
}

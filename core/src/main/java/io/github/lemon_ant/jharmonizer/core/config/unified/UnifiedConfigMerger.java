package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.List;
import java.util.Objects;
import lombok.experimental.UtilityClass;

/**
 * Performs a field-wise overlay of FlexibleUnifiedConfig onto a STRICT baseline UnifiedConfig.
 * No defaults are provided here; caller must supply the baseline with all fields fully set.
 */
@UtilityClass
public class UnifiedConfigMerger {

    public static UnifiedConfig merge(UnifiedConfig baseline, FlexibleUnifiedConfig overlay) {
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(overlay, "overlay");

        UnifiedTopLevelTypesOrdering top =
                overlay.getTopLevelTypesOrdering().orElse(baseline.getTopLevelTypesOrdering());

        UnifiedFormatting formatting = overlay.getFormatting().orElse(baseline.getFormatting());

        UnifiedHeaderLine header = overlay.getHeaderLine().orElse(baseline.getHeaderLine());

        List<UnifiedMemberGroup> root = overlay.getRootMemberGroups().orElse(baseline.getRootMemberGroups());

        return UnifiedConfig.builder()
                .topLevelTypesOrdering(top)
                .formatting(formatting)
                .headerLine(header)
                .rootMemberGroups(root)
                .build();
    }
}

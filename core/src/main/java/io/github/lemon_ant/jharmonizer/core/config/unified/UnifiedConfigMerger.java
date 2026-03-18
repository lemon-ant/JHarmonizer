package io.github.lemon_ant.jharmonizer.core.config.unified;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Performs a field-wise overlay of FlexibleUnifiedConfig onto a STRICT baseline UnifiedConfig.
 * No defaults are provided here; caller must supply the baseline with all fields fully set.
 * Root member groups are merged only at the top level: new overlay groups are prepended,
 * and groups with matching names replace baseline groups in their original positions.
 */
@UtilityClass
public class UnifiedConfigMerger {

    /**
     * Performs the merge.
     * @param baseline the baseline
     * @param overlay the overlay
     * @return the result
     */
    @NonNull
    public static UnifiedConfig merge(@NonNull UnifiedConfig baseline, @NonNull FlexibleUnifiedConfig overlay) {
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(overlay, "overlay");

        UnifiedTopLevelTypesOrdering top =
                overlay.getTopLevelTypesOrdering().orElse(baseline.getTopLevelTypesOrdering());

        UnifiedFormatting formatting = overlay.getFormatting().orElse(baseline.getFormatting());

        UnifiedHeaderLine header = overlay.getHeaderLine().orElse(baseline.getHeaderLine());

        Boolean backupsEnabled = overlay.getBackupsEnabled().orElse(baseline.isBackupsEnabled());

        List<UnifiedMemberGroup> root = overlay.getRootMemberGroups()
                .map(overlayRootGroups -> mergeRootMemberGroups(baseline.getRootMemberGroups(), overlayRootGroups))
                .orElse(baseline.getRootMemberGroups());

        return UnifiedConfig.builder()
                .topLevelTypesOrdering(top)
                .formatting(formatting)
                .headerLine(header)
                .backupsEnabled(backupsEnabled)
                .rootMemberGroups(root)
                .build();
    }

    @NonNull
    private static List<UnifiedMemberGroup> mergeRootMemberGroups(
            List<UnifiedMemberGroup> baselineRootGroups, List<UnifiedMemberGroup> overlayRootGroups) {
        Set<String> baselineGroupNames = collectNamedGroupNames(baselineRootGroups);
        List<UnifiedMemberGroup> mergedRootGroups =
                new ArrayList<>(baselineRootGroups.size() + overlayRootGroups.size());

        for (UnifiedMemberGroup overlayRootGroup : overlayRootGroups) {
            String overlayGroupName = overlayRootGroup.getGroupName();
            if (overlayGroupName == null || !baselineGroupNames.contains(overlayGroupName)) {
                mergedRootGroups.add(overlayRootGroup);
            }
        }

        for (UnifiedMemberGroup baselineRootGroup : baselineRootGroups) {
            String baselineGroupName = baselineRootGroup.getGroupName();
            UnifiedMemberGroup replacementGroup = findReplacementGroup(overlayRootGroups, baselineGroupName);
            mergedRootGroups.add(replacementGroup == null ? baselineRootGroup : replacementGroup);
        }

        return List.copyOf(mergedRootGroups);
    }

    @NonNull
    private static Set<String> collectNamedGroupNames(List<UnifiedMemberGroup> memberGroups) {
        Set<String> groupNames = new LinkedHashSet<>();
        for (UnifiedMemberGroup memberGroup : memberGroups) {
            String groupName = memberGroup.getGroupName();
            if (groupName != null) {
                groupNames.add(groupName);
            }
        }
        return groupNames;
    }

    @Nullable
    private static UnifiedMemberGroup findReplacementGroup(
            List<UnifiedMemberGroup> overlayRootGroups, @Nullable String baselineGroupName) {
        if (baselineGroupName == null) {
            return null;
        }
        for (int overlayGroupIndex = overlayRootGroups.size() - 1; overlayGroupIndex >= 0; overlayGroupIndex--) {
            UnifiedMemberGroup overlayRootGroup = overlayRootGroups.get(overlayGroupIndex);
            if (baselineGroupName.equals(overlayRootGroup.getGroupName())) {
                return overlayRootGroup;
            }
        }
        return null;
    }
}

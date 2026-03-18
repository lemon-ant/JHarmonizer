package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
        Map<String, UnifiedMemberGroup> baselineRootGroupsByName = collectGroupsByName(baselineRootGroups);
        List<UnifiedMemberGroup> prependedNewRootGroups = new ArrayList<>();
        for (UnifiedMemberGroup overlayRootGroup : overlayRootGroups) {
            String overlayGroupName = overlayRootGroup.getGroupName();
            if (overlayGroupName == null
                    || baselineRootGroupsByName.replace(overlayGroupName, overlayRootGroup) == null) {
                prependedNewRootGroups.add(overlayRootGroup);
            }
        }
        return Stream.concat(
                        prependedNewRootGroups.stream(),
                        baselineRootGroups.stream()
                                .map(baselineRootGroup ->
                                        resolveMergedBaselineRootGroup(baselineRootGroupsByName, baselineRootGroup)))
                .toList();
    }

    @NonNull
    private static Map<String, UnifiedMemberGroup> collectGroupsByName(List<UnifiedMemberGroup> memberGroups) {
        return memberGroups.stream()
                .collect(Collectors.toMap(
                        UnifiedConfigMerger::requireGroupName,
                        memberGroup -> memberGroup,
                        (ignoredExistingGroup, duplicateGroup) -> duplicateGroup,
                        LinkedHashMap::new));
    }

    @NonNull
    private static UnifiedMemberGroup resolveMergedBaselineRootGroup(
            Map<String, UnifiedMemberGroup> baselineRootGroupsByName, UnifiedMemberGroup baselineRootGroup) {
        String baselineRootGroupName = requireGroupName(baselineRootGroup);
        return baselineRootGroupsByName.getOrDefault(baselineRootGroupName, baselineRootGroup);
    }

    @NonNull
    private static String requireGroupName(UnifiedMemberGroup memberGroup) {
        String groupName = memberGroup.getGroupName();
        if (groupName == null) {
            throw new IllegalArgumentException("Baseline root member groups must have names to support overlay merge");
        }
        return groupName;
    }
}

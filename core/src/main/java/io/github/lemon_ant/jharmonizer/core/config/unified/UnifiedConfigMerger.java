package io.github.lemon_ant.jharmonizer.core.config.unified;

import edu.umd.cs.findbugs.annotations.Nullable;
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
            UnifiedMemberGroup newGroupOrNull =
                    getNewGroupOrUpdateBaselineMap(baselineRootGroupsByName, overlayRootGroup);
            if (newGroupOrNull != null) {
                prependedNewRootGroups.add(newGroupOrNull);
            }
        }
        Stream<UnifiedMemberGroup> mergedBaselineRootGroups = baselineRootGroups.stream()
                .map(baselineRootGroup -> getMergedBaselineRootGroup(baselineRootGroupsByName, baselineRootGroup));
        return Stream.concat(prependedNewRootGroups.stream(), mergedBaselineRootGroups)
                .toList();
    }

    @NonNull
    private static Map<String, UnifiedMemberGroup> collectGroupsByName(List<UnifiedMemberGroup> memberGroups) {
        return memberGroups.stream()
                .filter(memberGroup -> memberGroup.getGroupName() != null)
                .collect(Collectors.toMap(
                        UnifiedMemberGroup::getGroupName,
                        memberGroup -> memberGroup,
                        (ignoredExistingGroup, duplicateGroup) -> duplicateGroup,
                        LinkedHashMap::new));
    }

    @NonNull
    private static UnifiedMemberGroup getMergedBaselineRootGroup(
            Map<String, UnifiedMemberGroup> baselineRootGroupsByName, UnifiedMemberGroup baselineRootGroup) {
        String baselineGroupName = baselineRootGroup.getGroupName();
        if (baselineGroupName == null) {
            return baselineRootGroup;
        }
        return baselineRootGroupsByName.getOrDefault(baselineGroupName, baselineRootGroup);
    }

    @Nullable
    private static UnifiedMemberGroup getNewGroupOrUpdateBaselineMap(
            Map<String, UnifiedMemberGroup> baselineRootGroupsByName, UnifiedMemberGroup overlayRootGroup) {
        String overlayGroupName = overlayRootGroup.getGroupName();
        if (overlayGroupName == null || !baselineRootGroupsByName.containsKey(overlayGroupName)) {
            return overlayRootGroup;
        }
        baselineRootGroupsByName.put(overlayGroupName, overlayRootGroup);
        return null;
    }
}

package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Performs a field-wise overlay of FlexibleUnifiedConfig onto a STRICT baseline UnifiedConfig.
 * No defaults are provided here; caller must supply the baseline with all fields fully set.
 * Root member groups are merged only at the top level: new overlay groups are prepended,
 * and groups with matching names replace baseline groups in their original positions.
 * Unnamed baseline groups are kept in their original relative positions.
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
        UnifiedTopLevelTypesOrdering top =
                overlay.getTopLevelTypesOrdering().orElse(baseline.getTopLevelTypesOrdering());
        UnifiedFormatting formatting = overlay.getFormatting().orElse(baseline.getFormatting());
        UnifiedHeaderLine header = overlay.getHeaderLine().orElse(baseline.getHeaderLine());
        Boolean backupsEnabled = overlay.getBackupsEnabled().orElse(baseline.isBackupsEnabled());
        Boolean printProcessingStatistics =
                overlay.getPrintProcessingStatistics().orElse(baseline.isPrintProcessingStatistics());
        List<UnifiedMemberGroup> root = overlay.getRootMemberGroups()
                .map(overlayRootGroups -> mergeRootMemberGroups(baseline.getRootMemberGroups(), overlayRootGroups))
                .orElse(baseline.getRootMemberGroups());

        return UnifiedConfig.builder()
                .topLevelTypesOrdering(top)
                .formatting(formatting)
                .headerLine(header)
                .backupsEnabled(backupsEnabled)
                .printProcessingStatistics(printProcessingStatistics)
                .rootMemberGroups(root)
                .build();
    }

    /**
     * Performs a field-wise overlay of one flexible config onto another flexible config.
     *
     * @param baseline the baseline flexible config
     * @param overlay the overlay flexible config
     * @return merged flexible config
     */
    @NonNull
    public static FlexibleUnifiedConfig merge(
            @NonNull FlexibleUnifiedConfig baseline, @NonNull FlexibleUnifiedConfig overlay) {
        UnifiedTopLevelTypesOrdering top = overlay.getTopLevelTypesOrdering()
                .orElse(baseline.getTopLevelTypesOrdering().orElse(null));
        UnifiedFormatting formatting =
                overlay.getFormatting().orElse(baseline.getFormatting().orElse(null));
        UnifiedHeaderLine header =
                overlay.getHeaderLine().orElse(baseline.getHeaderLine().orElse(null));
        Boolean backupsEnabled =
                overlay.getBackupsEnabled().orElse(baseline.getBackupsEnabled().orElse(null));
        Boolean printProcessingStatistics = overlay.getPrintProcessingStatistics()
                .orElse(baseline.getPrintProcessingStatistics().orElse(null));
        List<UnifiedMemberGroup> root = overlay.getRootMemberGroups()
                .map(overlayRootGroups -> baseline.getRootMemberGroups()
                        .map(baselineRootGroups -> mergeRootMemberGroups(baselineRootGroups, overlayRootGroups))
                        .orElse(overlayRootGroups))
                .orElse(baseline.getRootMemberGroups().orElse(null));

        FlexibleUnifiedConfig.FlexibleUnifiedConfigBuilder configBuilder = FlexibleUnifiedConfig.builder()
                .topLevelTypesOrdering(top)
                .formatting(formatting)
                .backupsEnabled(backupsEnabled)
                .printProcessingStatistics(printProcessingStatistics)
                .headerLine(header);
        if (root != null) {
            configBuilder.rootMemberGroups(root);
        }
        return configBuilder.build();
    }

    @NonNull
    private static List<UnifiedMemberGroup> mergeRootMemberGroups(
            List<UnifiedMemberGroup> baselineRootGroups, List<UnifiedMemberGroup> overlayRootGroups) {
        Map<String, Integer> baselineRootGroupIndicesByName = collectNamedGroupIndicesInOrder(baselineRootGroups);
        List<UnifiedMemberGroup> mergedBaselineRootGroups = new ArrayList<>(baselineRootGroups);
        List<UnifiedMemberGroup> prependedNewRootGroups = collectPrependedNewRootGroups(
                overlayRootGroups, mergedBaselineRootGroups, baselineRootGroupIndicesByName);
        return Stream.concat(prependedNewRootGroups.stream(), mergedBaselineRootGroups.stream())
                .toList();
    }

    @NonNull
    private static Map<String, Integer> collectNamedGroupIndicesInOrder(List<UnifiedMemberGroup> memberGroups) {
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, Integer> groupIndicesByName = new HashMap<>();
        for (int groupIndex = 0; groupIndex < memberGroups.size(); groupIndex++) {
            UnifiedMemberGroup memberGroup = memberGroups.get(groupIndex);
            String groupName = memberGroup.getGroupName();
            if (groupName == null) {
                continue;
            }
            if (groupIndicesByName.putIfAbsent(groupName, groupIndex) != null) {
                throw new IllegalStateException("Baseline root member group names must be unique");
            }
        }
        return groupIndicesByName;
    }

    @NonNull
    private static List<UnifiedMemberGroup> collectPrependedNewRootGroups(
            List<UnifiedMemberGroup> overlayRootGroups,
            List<UnifiedMemberGroup> mergedBaselineRootGroups,
            Map<String, Integer> baselineRootGroupIndicesByName) {
        List<UnifiedMemberGroup> prependedNewRootGroups = new ArrayList<>();
        for (UnifiedMemberGroup overlayRootGroup : overlayRootGroups) {
            String overlayGroupName = overlayRootGroup.getGroupName();
            Integer baselineGroupIndex =
                    overlayGroupName == null ? null : baselineRootGroupIndicesByName.get(overlayGroupName);
            if (baselineGroupIndex == null) {
                prependedNewRootGroups.add(overlayRootGroup);
                continue;
            }
            mergedBaselineRootGroups.set(baselineGroupIndex, overlayRootGroup);
        }
        return prependedNewRootGroups;
    }
}

package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
        Set<String> baselineGroupNames = collectNamedGroupNames(baselineRootGroups);
        Stream<UnifiedMemberGroup> prependedNewRootGroups = overlayRootGroups.stream()
                .filter(overlayRootGroup -> isNewRootGroup(overlayRootGroup, baselineGroupNames));
        Stream<UnifiedMemberGroup> mergedBaselineRootGroups = baselineRootGroups.stream()
                .map(baselineRootGroup -> findReplacementGroup(overlayRootGroups, baselineRootGroup)
                        .orElse(baselineRootGroup));
        return Stream.concat(prependedNewRootGroups, mergedBaselineRootGroups).toList();
    }

    @NonNull
    private static Set<String> collectNamedGroupNames(List<UnifiedMemberGroup> memberGroups) {
        return memberGroups.stream()
                .map(UnifiedMemberGroup::getGroupName)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @NonNull
    private static Optional<UnifiedMemberGroup> findReplacementGroup(
            List<UnifiedMemberGroup> overlayRootGroups, UnifiedMemberGroup baselineRootGroup) {
        String baselineGroupName = baselineRootGroup.getGroupName();
        if (baselineGroupName == null) {
            return Optional.empty();
        }
        return overlayRootGroups.stream()
                .filter(overlayRootGroup -> baselineGroupName.equals(overlayRootGroup.getGroupName()))
                .reduce((ignoredPreviousGroup, currentGroup) -> currentGroup);
    }

    private static boolean isNewRootGroup(UnifiedMemberGroup overlayRootGroup, Set<String> baselineGroupNames) {
        String overlayGroupName = overlayRootGroup.getGroupName();
        return overlayGroupName == null || !baselineGroupNames.contains(overlayGroupName);
    }
}

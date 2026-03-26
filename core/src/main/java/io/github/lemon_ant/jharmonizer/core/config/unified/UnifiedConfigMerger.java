package io.github.lemon_ant.jharmonizer.core.config.unified;

import edu.umd.cs.findbugs.annotations.Nullable;
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
        List<UnifiedMemberGroup> root = overlay.getRootMemberGroups()
                .map(overlayRootGroups -> baseline.getRootMemberGroups()
                        .map(baselineRootGroups -> mergeRootMemberGroups(baselineRootGroups, overlayRootGroups))
                        .orElse(overlayRootGroups))
                .orElse(baseline.getRootMemberGroups().orElse(null));

        return new FlexibleUnifiedConfig(top, formatting, backupsEnabled, header, root);
    }

    @NonNull
    private static List<UnifiedMemberGroup> mergeRootMemberGroups(
            List<UnifiedMemberGroup> baselineRootGroups, List<UnifiedMemberGroup> overlayRootGroups) {
        Map<String, UnifiedMemberGroup> baselineRootGroupsByName = collectGroupsByName(baselineRootGroups);
        Stream<UnifiedMemberGroup> prependedNewRootGroups = overlayRootGroups.stream()
                .map(overlayRootGroup -> findPrependedNewRootGroup(baselineRootGroupsByName, overlayRootGroup))
                .filter(Objects::nonNull);
        return Stream.concat(prependedNewRootGroups, baselineRootGroupsByName.values().stream())
                .toList();
    }

    @NonNull
    private static Map<String, UnifiedMemberGroup> collectGroupsByName(List<UnifiedMemberGroup> memberGroups) {
        return memberGroups.stream()
                .collect(Collectors.toMap(
                        memberGroup -> {
                            String groupName = memberGroup.getGroupName();
                            if (groupName == null) {
                                throw new IllegalStateException("Baseline root member groups must have non-null names");
                            }
                            return groupName;
                        },
                        memberGroup -> memberGroup,
                        (ignoredExistingGroup, duplicateGroup) -> {
                            throw new IllegalStateException("Baseline root member group names must be unique");
                        },
                        LinkedHashMap::new));
    }

    @Nullable
    private static UnifiedMemberGroup findPrependedNewRootGroup(
            Map<String, UnifiedMemberGroup> baselineRootGroupsByName, UnifiedMemberGroup overlayRootGroup) {
        String overlayGroupName = overlayRootGroup.getGroupName();
        return overlayGroupName == null || baselineRootGroupsByName.replace(overlayGroupName, overlayRootGroup) == null
                ? overlayRootGroup
                : null;
    }
}

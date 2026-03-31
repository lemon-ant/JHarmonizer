package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedConfig;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Translates strict Unified configuration into the Compiled runtime model.
 * The Compiled model is branch-free at runtime: each rule-line is compiled into a single predicate.
 * <p>
 * This compiler performs the following steps:
 * - Compiles selector blocks (includes/excludes) into {@link CompiledMemberGroupSelectorBlock}
 * - Maps sorting behavior knobs to {@link CompiledMemberGroupSortingBehavior}
 * - Builds a DFS-ordered tree of {@link CompiledMemberGroup} nodes and assigns post-order indexes
 */
@UtilityClass
public class Unified2CompiledModelCompiler {

    /**
     * Compiles a full Unified configuration into an Compiled configuration.
     */
    @NonNull
    public static CompiledConfig compile(@NonNull UnifiedConfig unifiedConfig) {
        // TODO Today we only compile the member groups; top-level type ordering may feed into future phases.
        List<CompiledMemberGroup> memberGroups =
                MemberGroupCompiler.compileTopLevelGroups(unifiedConfig.getRootMemberGroups());
        CompiledTopLevelTypesOrdering topLevelTypesOrdering =
                TopLevelTypesOrderingCompiler.compileTopLevelTypesOrdering(unifiedConfig.getTopLevelTypesOrdering());

        return CompiledConfig.builder()
                .rootMemberGroups(memberGroups)
                .topLevelTypesOrdering(topLevelTypesOrdering)
                .formatting(unifiedConfig.getFormatting())
                .backupsEnabled(unifiedConfig.isBackupsEnabled())
                .printProcessingStatistics(unifiedConfig.isPrintProcessingStatistics())
                .headerLine(unifiedConfig.getHeaderLine())
                .build();
    }
}

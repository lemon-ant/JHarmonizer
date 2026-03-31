package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static java.util.Collections.unmodifiableList;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedHeaderLine;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Full compiled config with top-level roots and post-order numbering.
 * Top-level roots are checked top-to-bottom; the first matching root captures the member and DFS continues.
 */
@Value
public class CompiledConfig {
    /**
     * Cohesive formatting definition (preferred API).
     */
    @NonNull
    UnifiedFormatting formatting;

    boolean backupsEnabled;

    boolean printProcessingStatistics;

    /**
     * Header line descriptor (character + leftPadding).
     */
    @NonNull
    UnifiedHeaderLine headerLine;

    @NonNull
    List<CompiledMemberGroup> rootMemberGroups;
    /**
     * Compiled top-level order as a sequence of predicates.
     * Compiler is responsible for populating it (including optional head predicates).
     */
    @NonNull
    CompiledTopLevelTypesOrdering topLevelTypesOrdering;

    /**
     * Creates a new CompiledConfig.
     * @param rootMemberGroups the root member groups
     * @param topLevelTypesOrdering the top level types ordering
     * @param formatting the formatting
     * @param backupsEnabled the backups enabled
     * @param printProcessingStatistics the print processing statistics flag
     * @param headerLine the header line
     */
    @Builder(access = AccessLevel.PACKAGE)
    private CompiledConfig(
            @NonNull List<CompiledMemberGroup> rootMemberGroups,
            @NonNull CompiledTopLevelTypesOrdering topLevelTypesOrdering,
            @NonNull UnifiedFormatting formatting,
            boolean backupsEnabled,
            boolean printProcessingStatistics,
            @NonNull UnifiedHeaderLine headerLine) {
        this.rootMemberGroups = unmodifiableList(rootMemberGroups);
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.formatting = formatting;
        this.backupsEnabled = backupsEnabled;
        this.printProcessingStatistics = printProcessingStatistics;
        this.headerLine = headerLine;
    }

    /**
     * Matches the root group.
     * @param descriptor the member descriptor to inspect
     * @return the optional result
     */
    @NonNull
    public Optional<CompiledMemberGroup> matchRootGroup(@NonNull MemberDescriptor descriptor) {
        return rootMemberGroups.stream()
                .filter(typeRoot -> typeRoot.getSelectorBlock().match(descriptor))
                .findFirst();
    }
}

package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static java.util.Collections.unmodifiableList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedHeaderLine;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.Value;

/**
 * Full compiled config with top-level roots and post-order numbering.
 * Top-level roots are checked top-to-bottom; the first matching root captures the member and DFS continues.
 */
@Value
@SuppressFBWarnings
public class CompiledConfig {
    @NonNull
    List<CompiledMemberGroup> rootMemberGroups;

    /**
     * Compiled top-level order as a sequence of predicates.
     * Compiler is responsible for populating it (including optional head predicates).
     */
    @NonNull
    CompiledTopLevelTypesOrdering topLevelTypesOrdering;

    /**
     * Cohesive formatting definition (preferred API).
     */
    @NonNull
    UnifiedFormatting formatting;

    /**
     * Header line descriptor (character + leftPadding).
     */
    @NonNull
    UnifiedHeaderLine headerLine;

    CompiledConfig(
            @NonNull List<CompiledMemberGroup> rootMemberGroups,
            @NonNull CompiledTopLevelTypesOrdering topLevelTypesOrdering,
            @NonNull UnifiedFormatting formatting,
            @NonNull UnifiedHeaderLine headerLine) {
        this.rootMemberGroups = unmodifiableList(rootMemberGroups);
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.formatting = formatting;
        this.headerLine = headerLine;
    }

    @NonNull
    public Optional<CompiledMemberGroup> matchGroup(@NonNull MemberDescriptor descriptor) {
        for (CompiledMemberGroup typeRoot : rootMemberGroups) {
            Optional<CompiledMemberGroup> hit = typeRoot.classify(descriptor);
            if (hit.isPresent()) return hit;
        }
        return Optional.empty();
    }
}

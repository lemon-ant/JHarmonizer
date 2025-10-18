package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static java.util.Collections.unmodifiableList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedFormatting;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedHeaderLine;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedTopLevelTypesOrdering;
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
    List<CompiledGroup> rootMemberGroups;

    /**
     * Top-level types ordering (mainTypeFirst, typeGroups, sortKeys).
     */
    @NonNull
    UnifiedTopLevelTypesOrdering topLevelTypesOrdering;

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

    public CompiledConfig(
            @NonNull List<CompiledGroup> rootMemberGroups,
            @NonNull UnifiedTopLevelTypesOrdering topLevelTypesOrdering,
            @NonNull UnifiedFormatting formatting,
            @NonNull UnifiedHeaderLine headerLine) {
        this.rootMemberGroups = unmodifiableList(rootMemberGroups);
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.formatting = formatting;
        this.headerLine = headerLine;
    }

    @NonNull
    public Optional<CompiledGroup> matchGroup(@NonNull MemberDescriptor descriptor) {
        for (CompiledGroup typeRoot : rootMemberGroups) {
            Optional<CompiledGroup> hit = typeRoot.classify(descriptor);
            if (hit.isPresent()) return hit;
        }
        return Optional.empty();
    }
}

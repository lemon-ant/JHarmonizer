package io.github.lemon_ant.jharmonizer.core.config.unified;

import static java.util.Optional.ofNullable;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/**
 * Flexible overlay for UnifiedConfig. All fields are optional.
 * Use UnifiedConfigMerger.merge(baseline, overlay) to produce a strict UnifiedConfig.
 * This class does NOT define defaults and does NOT invent values.
 */
@Value
@Builder
@SuppressWarnings("PMD.DataClass")
public class FlexibleUnifiedConfig {

    /** Optional override for top-level types ordering. */
    @Nullable
    UnifiedTopLevelTypesOrdering topLevelTypesOrdering;

    /** Optional override for fixImports flag. */
    @Nullable
    Boolean fixImports;

    /** Optional override for formatter style. */
    @Nullable
    UnifiedFormatterStyle formatterStyle;

    /** Optional override for header line. */
    @Nullable
    UnifiedHeaderLine headerLine;

    /** Optional override for root member groups (full replacement). */
    @Nullable
    List<UnifiedMemberGroup> rootMemberGroups;

    public FlexibleUnifiedConfig(
            @Nullable UnifiedTopLevelTypesOrdering topLevelTypesOrdering,
            @Nullable Boolean fixImports,
            @Nullable UnifiedFormatterStyle formatterStyle,
            @Nullable UnifiedHeaderLine headerLine,
            @Nullable @Singular List<UnifiedMemberGroup> rootMemberGroups) {
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.fixImports = fixImports;
        this.formatterStyle = formatterStyle;
        this.headerLine = headerLine;
        this.rootMemberGroups =
                ofNullable(rootMemberGroups).map(Collections::unmodifiableList).orElse(List.of());
    }

    @NonNull
    public Optional<UnifiedTopLevelTypesOrdering> getTopLevelTypesOrdering() {
        return ofNullable(topLevelTypesOrdering);
    }

    @NonNull
    public Optional<Boolean> getFixImports() {
        return ofNullable(fixImports);
    }

    @NonNull
    public Optional<UnifiedFormatterStyle> getFormatterStyle() {
        return ofNullable(formatterStyle);
    }

    @NonNull
    public Optional<UnifiedHeaderLine> getHeaderLine() {
        return ofNullable(headerLine);
    }

    @NonNull
    public Optional<List<UnifiedMemberGroup>> getRootMemberGroups() {
        return ofNullable(rootMemberGroups);
    }
}

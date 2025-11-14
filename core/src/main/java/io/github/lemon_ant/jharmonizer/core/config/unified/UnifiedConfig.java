package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.lang3.Validate;

/**
 * Unified, strongly-typed configuration model prepared for compilation into the Compiled model.
 * <p>
 * This model is a normalized, vendor-independent representation assembled from any vendor-specific
 * inputs (IDE exports, YAML/XML, proprietary configs). It mirrors the MemberDescriptor capabilities
 * on the configuration side (kinds, access, modifiers, names, annotations, behaviors) and is
 * deliberately strict and explicit.
 */
@Value
public class UnifiedConfig {

    /**
     * Cohesive formatting definition (preferred API).
     */
    @NonNull
    UnifiedFormatting formatting;

    boolean backupsEnabled;

    /**
     * Header line descriptor (character + leftPadding).
     */
    @NonNull
    UnifiedHeaderLine headerLine;

    /**
     * Root member groups.
     */
    @NonNull
    List<UnifiedMemberGroup> rootMemberGroups;

    /**
     * Top-level types ordering (mainTypeFirst, typeGroups, sortKeys).
     */
    @NonNull
    UnifiedTopLevelTypesOrdering topLevelTypesOrdering;

    // TODO Remove builder
    @Builder
    public UnifiedConfig(
            @NonNull UnifiedTopLevelTypesOrdering topLevelTypesOrdering,
            @NonNull UnifiedFormatting formatting,
            @NonNull Boolean backupsEnabled,
            @NonNull UnifiedHeaderLine headerLine,
            @NonNull @Singular List<UnifiedMemberGroup> rootMemberGroups) {
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.formatting = formatting;
        this.backupsEnabled = backupsEnabled;
        this.headerLine = headerLine;
        Validate.notEmpty(rootMemberGroups, "Root member groups cannot be empty");
        this.rootMemberGroups = Collections.unmodifiableList(rootMemberGroups);
    }
}

package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.lang3.Validate;

/**
 * Unified, strongly-typed configuration model prepared for compilation into the effective model.
 * <p>
 * This model is a normalized, vendor-independent representation assembled from any vendor-specific
 * inputs (IDE exports, YAML/XML, proprietary configs). It mirrors the MemberDescriptor capabilities
 * on the configuration side (kinds, access, modifiers, names, annotations, behaviors) and is
 * deliberately strict and explicit.
 */
@Value
public class UnifiedConfig {

    /**
     * Top-level types ordering (mainTypeFirst, typeGroups, sortKeys).
     */
    @NonNull
    UnifiedTopLevelTypesOrdering topLevelTypesOrdering;

    /**
     * Whether to fix/reorder imports.
     */
    boolean fixImports;

    /**
     * Formatter style (AOP, GOOGLE, NONE, PALANTIR).
     */
    @NonNull
    UnifiedFormatterStyle formatterStyle;

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

    @Builder
    public UnifiedConfig(
            @NonNull UnifiedTopLevelTypesOrdering topLevelTypesOrdering,
            boolean fixImports,
            @NonNull UnifiedFormatterStyle formatterStyle,
            @NonNull UnifiedHeaderLine headerLine,
            @NonNull @Singular List<UnifiedMemberGroup> rootMemberGroups) {
        this.topLevelTypesOrdering = topLevelTypesOrdering;
        this.fixImports = fixImports;
        this.formatterStyle = formatterStyle;
        this.headerLine = headerLine;
        Validate.notEmpty(rootMemberGroups, "Root member groups cannot be empty");
        this.rootMemberGroups = Collections.unmodifiableList(rootMemberGroups);
    }
}

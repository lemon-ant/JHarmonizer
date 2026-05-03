// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

/**
 * Top-level type ordering section of a JHarmonizer YAML config.
 * Specifies whether the main type appears first, the ordered list of type-kind selectors,
 * and the ordering rules applied across top-level types.
 */
@Value
public class JHarmonizerTopLevelTypesOrdering {

    boolean mainTypeFirst;

    @NonNull
    List<JHarmonizerOrderingRule> orderingRules;

    @NonNull
    List<@NonNull JHarmonizerTopLevelTypeSelector> topLevelTypeSelectors;

    /**
     * Creates a new JHarmonizerTopLevelTypesOrdering.
     * @param mainTypeFirst the main type first
     * @param topLevelTypeSelectors the top level type selectors
     * @param orderingRules the ordering rules
     */
    JHarmonizerTopLevelTypesOrdering(
            @JsonProperty(value = "main-type-first", required = true) boolean mainTypeFirst,
            @NonNull @JsonProperty(value = "type-groups", required = true)
                    List<@NonNull JHarmonizerTopLevelTypeSelector> topLevelTypeSelectors,
            @NonNull
                    @JsonDeserialize(using = OrderingRulesDeserializer.class)
                    @JsonProperty(value = "ordering-rules", required = true)
                    List<JHarmonizerOrderingRule> orderingRules) {
        this.mainTypeFirst = mainTypeFirst;

        Validate.notEmpty(topLevelTypeSelectors, "type-groups cannot be empty");
        validateUniqueTypeKinds(topLevelTypeSelectors);
        this.topLevelTypeSelectors = Collections.unmodifiableList(topLevelTypeSelectors);

        Validate.notEmpty(orderingRules, "ordering-rules cannot be empty");
        this.orderingRules = Collections.unmodifiableList(orderingRules);
    }

    private static void validateUniqueTypeKinds(List<JHarmonizerTopLevelTypeSelector> typeGroups) {
        Set<JHarmonizerTypeKind> encounteredTypeKinds = EnumSet.noneOf(JHarmonizerTypeKind.class);
        typeGroups.stream()
                .flatMap(typeGroup -> typeGroup.getTypeKinds().stream())
                .filter(typeKind -> !encounteredTypeKinds.add(typeKind))
                .findFirst()
                .ifPresent(duplicateKind -> {
                    throw new IllegalArgumentException("Duplicate JHarmonizerTypeKind found: " + duplicateKind);
                });
    }
}

package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collections;
import java.util.HashSet;
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
        Set<JHarmonizerTypeKind> encounteredTypeKinds = new HashSet<>();
        typeGroups.stream()
                .flatMap(typeGroup -> typeGroup.getTypeKinds().stream())
                .filter(typeKind -> !encounteredTypeKinds.add(typeKind))
                .findFirst()
                .ifPresent(duplicateKind -> {
                    throw new IllegalArgumentException("Duplicate JHarmonizerTypeKind found: " + duplicateKind);
                });
    }

    /**
     * Checks whether this jharmonizer top level types ordering matches another object.
     * @param o the object to compare with
     * @return {@code true} if the check succeeds; otherwise {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JHarmonizerTopLevelTypesOrdering that)) {
            return false;
        }

        return mainTypeFirst == that.mainTypeFirst
                && topLevelTypeSelectors.equals(that.topLevelTypeSelectors)
                && orderingRules.equals(that.orderingRules);
    }

    /**
     * Returns the hash code of this jharmonizer top level types ordering.
     * @return the hash code value
     */
    @Override
    public int hashCode() {
        int result = Boolean.hashCode(mainTypeFirst);
        result = 31 * result + topLevelTypeSelectors.hashCode();
        result = 31 * result + orderingRules.hashCode();
        return result;
    }
}

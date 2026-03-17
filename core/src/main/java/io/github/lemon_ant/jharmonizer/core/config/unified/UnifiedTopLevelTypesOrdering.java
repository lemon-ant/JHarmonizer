package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

/**
 * Exact mirror of vendor JHarmonizerTopLevelTypesOrdering.
 */
@Value
public class UnifiedTopLevelTypesOrdering {

    boolean mainTypeFirst;

    @NonNull
    List<UnifiedOrderingRule> orderingRules;

    @NonNull
    List<UnifiedTopLevelTypeSelector> topLevelTypeSelectors;

    // TODO Remove builder
    /**
     * Creates a new UnifiedTopLevelTypesOrdering.
     * @param mainTypeFirst the main type first
     * @param topLevelTypeSelectors the top level type selectors
     * @param orderingRules the ordering rules
     */
    @Builder
    public UnifiedTopLevelTypesOrdering(
            boolean mainTypeFirst,
            @NonNull List<UnifiedTopLevelTypeSelector> topLevelTypeSelectors,
            @NonNull List<UnifiedOrderingRule> orderingRules) {
        this.mainTypeFirst = mainTypeFirst;

        Validate.notEmpty(topLevelTypeSelectors, "typeGroups cannot be empty");
        this.topLevelTypeSelectors = Collections.unmodifiableList(topLevelTypeSelectors);

        Validate.notEmpty(orderingRules, "orderingRules cannot be empty");
        this.orderingRules = Collections.unmodifiableList(orderingRules);
    }
}

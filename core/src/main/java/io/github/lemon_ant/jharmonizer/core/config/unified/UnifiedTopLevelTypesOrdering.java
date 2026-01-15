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
    List<UnifiedSortKey> sortKeys;

    @NonNull
    List<UnifiedTopLevelTypeSelector> topLevelTypeSelectors;

    // TODO Remove builder
    @Builder
    public UnifiedTopLevelTypesOrdering(
            boolean mainTypeFirst,
            @NonNull List<UnifiedTopLevelTypeSelector> topLevelTypeSelectors,
            @NonNull List<UnifiedSortKey> sortKeys) {
        this.mainTypeFirst = mainTypeFirst;

        Validate.notEmpty(topLevelTypeSelectors, "typeGroups cannot be empty");
        this.topLevelTypeSelectors = Collections.unmodifiableList(topLevelTypeSelectors);

        Validate.notEmpty(sortKeys, "sortKeys cannot be empty");
        this.sortKeys = Collections.unmodifiableList(sortKeys);
    }
}

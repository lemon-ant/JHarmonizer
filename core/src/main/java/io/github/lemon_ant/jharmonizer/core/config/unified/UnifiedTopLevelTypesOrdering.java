package io.github.lemon_ant.jharmonizer.core.config.unified;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.lang3.Validate;

/** Exact mirror of vendor JHarmonizerTopLevelTypesOrdering. */
@Value
public class UnifiedTopLevelTypesOrdering {

    boolean mainTypeFirst;

    @NonNull
    List<UnifiedTypeGroup> typeGroups;

    @NonNull
    List<UnifiedSortKey> sortKeys;

    @Builder
    public UnifiedTopLevelTypesOrdering(
            boolean mainTypeFirst,
            @NonNull @Singular List<UnifiedTypeGroup> typeGroups,
            @NonNull @Singular List<UnifiedSortKey> sortKeys) {
        this.mainTypeFirst = mainTypeFirst;

        Validate.notEmpty(typeGroups, "typeGroups cannot be empty");
        this.typeGroups = Collections.unmodifiableList(typeGroups);

        Validate.notEmpty(sortKeys, "sortKeys cannot be empty");
        this.sortKeys = Collections.unmodifiableList(sortKeys);
    }
}

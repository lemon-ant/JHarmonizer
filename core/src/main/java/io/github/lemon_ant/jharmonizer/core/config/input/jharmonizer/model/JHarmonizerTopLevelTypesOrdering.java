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

@Value
public class JHarmonizerTopLevelTypesOrdering {

    boolean mainTypeFirst;

    @NonNull
    List<JHarmonizerSortKey> sortKeys;

    @NonNull
    List<@NonNull JHarmonizerTopLevelTypeSelector> topLevelTypeSelectors;

    JHarmonizerTopLevelTypesOrdering(
            @JsonProperty(value = "main-type-first", required = true) boolean mainTypeFirst,
            @NonNull @JsonProperty(value = "type-groups", required = true)
                    List<@NonNull JHarmonizerTopLevelTypeSelector> topLevelTypeSelectors,
            @NonNull
                    @JsonDeserialize(using = SortKeysDeserializer.class)
                    @JsonProperty(value = "sort-keys", required = true)
                    List<JHarmonizerSortKey> sortKeys) {
        this.mainTypeFirst = mainTypeFirst;

        Validate.notEmpty(topLevelTypeSelectors, "type-groups cannot be empty");
        validateUniqueTypeKinds(topLevelTypeSelectors);
        this.topLevelTypeSelectors = Collections.unmodifiableList(topLevelTypeSelectors);

        Validate.notEmpty(sortKeys, "sort-keys cannot be empty");
        this.sortKeys = Collections.unmodifiableList(sortKeys);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JHarmonizerTopLevelTypesOrdering that)) {
            return false;
        }

        return mainTypeFirst == that.mainTypeFirst
                && topLevelTypeSelectors.equals(that.topLevelTypeSelectors)
                && sortKeys.equals(that.sortKeys);
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(mainTypeFirst);
        result = 31 * result + topLevelTypeSelectors.hashCode();
        result = 31 * result + sortKeys.hashCode();
        return result;
    }

    private static void validateUniqueTypeKinds(List<JHarmonizerTopLevelTypeSelector> typeGroups) {
        Set<JHarmonizerTypeKind> allTypes = new HashSet<>();
        for (JHarmonizerTopLevelTypeSelector group : typeGroups) {
            for (JHarmonizerTypeKind kind : group.getTypeKinds()) {
                if (!allTypes.add(kind)) {
                    throw new IllegalArgumentException("Duplicate JHarmonizerTypeKind found: " + kind);
                }
            }
        }
    }
}

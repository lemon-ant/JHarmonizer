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
    List<@NonNull JHarmonizerTypeGroup> typeGroups;

    JHarmonizerTopLevelTypesOrdering(
            @JsonProperty(value = "main-type-first", required = true) boolean mainTypeFirst,
            @NonNull @JsonProperty(value = "type-groups", required = true) List<@NonNull JHarmonizerTypeGroup> typeGroups,
            @NonNull
                    @JsonDeserialize(using = SortKeysDeserializer.class)
                    @JsonProperty(value = "sort-keys", required = true)
                    List<JHarmonizerSortKey> sortKeys) {
        this.mainTypeFirst = mainTypeFirst;

        Validate.notEmpty(typeGroups, "type-groups cannot be empty");
        validateUniqueTypeKinds(typeGroups);
        this.typeGroups = Collections.unmodifiableList(typeGroups);

        Validate.notEmpty(sortKeys, "sort-keys cannot be empty");
        this.sortKeys = Collections.unmodifiableList(sortKeys);
    }

    private static void validateUniqueTypeKinds(List<JHarmonizerTypeGroup> typeGroups) {
        Set<JHarmonizerTypeKind> allTypes = new HashSet<>();
        for (JHarmonizerTypeGroup group : typeGroups) {
            for (JHarmonizerTypeKind kind : group.getTypeKinds()) {
                if (!allTypes.add(kind)) {
                    throw new IllegalArgumentException("Duplicate JHarmonizerTypeKind found: " + kind);
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JHarmonizerTopLevelTypesOrdering that)) {
            return false;
        }

        return mainTypeFirst == that.mainTypeFirst
                && typeGroups.equals(that.typeGroups)
                && sortKeys.equals(that.sortKeys);
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(mainTypeFirst);
        result = 31 * result + typeGroups.hashCode();
        result = 31 * result + sortKeys.hashCode();
        return result;
    }
}

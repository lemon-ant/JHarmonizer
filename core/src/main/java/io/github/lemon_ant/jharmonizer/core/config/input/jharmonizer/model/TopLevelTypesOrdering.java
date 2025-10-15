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
public class TopLevelTypesOrdering {

    boolean mainTypeFirst;

    @NonNull
    List<SortKey> sortKeys;

    @NonNull
    List<@NonNull TypeGroup> typeGroups;

    TopLevelTypesOrdering(
            @JsonProperty(value = "main-type-first", required = true) boolean mainTypeFirst,
            @NonNull @JsonProperty(value = "type-groups", required = true) List<@NonNull TypeGroup> typeGroups,
            @NonNull
                    @JsonDeserialize(using = SortKeysDeserializer.class)
                    @JsonProperty(value = "sort-keys", required = true)
                    List<SortKey> sortKeys) {
        this.mainTypeFirst = mainTypeFirst;

        Validate.notEmpty(typeGroups, "type-groups cannot be empty");
        validateUniqueTypeKinds(typeGroups);
        this.typeGroups = Collections.unmodifiableList(typeGroups);

        Validate.notEmpty(sortKeys, "sort-keys cannot be empty");
        this.sortKeys = Collections.unmodifiableList(sortKeys);
    }

    private static void validateUniqueTypeKinds(List<TypeGroup> typeGroups) {
        Set<TypeKind> allTypes = new HashSet<>();
        for (TypeGroup group : typeGroups) {
            for (TypeKind kind : group.getTypeKinds()) {
                if (!allTypes.add(kind)) {
                    throw new IllegalArgumentException("Duplicate TypeKind found: " + kind);
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TopLevelTypesOrdering that)) {
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

package io.github.antonlem.jharmonizer.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.Value;

@Value
public class TopLevelTypesOrdering {

    boolean mainTypeFirst;

    @NonNull
    List<@NonNull TypeGroup> typeGroups;

    @NonNull
    List<SortKey> sortKeys;

    TopLevelTypesOrdering(
            @JsonProperty(value = "main-type-first", required = true) boolean mainTypeFirst,
            @NonNull @JsonProperty(value = "type-groups", required = true) List<@NonNull TypeGroup> typeGroups,
            @NonNull
                    @JsonDeserialize(using = SortKeysDeserializer.class)
                    @JsonProperty(value = "sort-keys", required = true)
                    List<SortKey> sortKeys) {
        this.mainTypeFirst = mainTypeFirst;
        validateUniqueTypeKinds(typeGroups);
        this.typeGroups = Collections.unmodifiableList(typeGroups);
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

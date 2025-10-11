package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.Value;

@Value
public class MemberGroup implements Serializable {

    @Serial
    // TODO We need it while we pass MemberGroup to the Comparator as a constructor parameter
    private static final long serialVersionUID = 3113522117531985752L;

    @NonNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    Set<Set<String>> excludes;

    @NonNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    List<@NonNull MemberGroup> memberSubGroups;

    @NonNull
    Set<Set<String>> includes;

    boolean keepAccessorsTogether;

    @NonNull
    String name;

    @NonNull
    Separator separator;

    @NonNull
    List<SortKey> sortKeys;

    MemberGroup(
            @NonNull @JsonProperty(value = "name", required = true) String name,
            @NonNull
                    @JsonDeserialize(using = SelectorsDeserializer.class)
                    @JsonProperty(value = "includes", required = true)
                    Set<Set<String>> includes,
            @Nullable @JsonDeserialize(using = SelectorsDeserializer.class) @JsonProperty(value = "excludes")
                    Set<Set<String>> excludes,
            @NonNull
                    @JsonDeserialize(using = SortKeysDeserializer.class)
                    @JsonProperty(value = "sort-keys", required = true)
                    List<SortKey> sortKeys,
            @Nullable @JsonProperty(value = "separator") Separator separator,
            @Nullable @JsonProperty(value = "keepAccessorsTogether") Boolean keepAccessorsTogether,
            // TODO Rename to subgroups
            @Nullable @JsonProperty(value = "groups") List<@NonNull MemberGroup> memberSubGroups) {
        this.name = name;
        this.includes = Collections.unmodifiableSet(includes);
        this.excludes = ofNullable(excludes).map(Collections::unmodifiableSet).orElse(Set.of());
        this.sortKeys = Collections.unmodifiableList(sortKeys);
        this.separator = ofNullable(separator).orElse(Separator.NONE);
        this.keepAccessorsTogether = ofNullable(keepAccessorsTogether).orElse(false);
        this.memberSubGroups =
                ofNullable(memberSubGroups).map(Collections::unmodifiableList).orElse(List.of());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MemberGroup that)) {
            return false;
        }

        return keepAccessorsTogether == that.keepAccessorsTogether
                && name.equals(that.name)
                && includes.equals(that.includes)
                && excludes.equals(that.excludes)
                && sortKeys.equals(that.sortKeys)
                && separator == that.separator
                && memberSubGroups.equals(that.memberSubGroups);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + includes.hashCode();
        result = 31 * result + excludes.hashCode();
        result = 31 * result + sortKeys.hashCode();
        result = 31 * result + separator.hashCode();
        result = 31 * result + Boolean.hashCode(keepAccessorsTogether);
        result = 31 * result + memberSubGroups.hashCode();
        return result;
    }
}

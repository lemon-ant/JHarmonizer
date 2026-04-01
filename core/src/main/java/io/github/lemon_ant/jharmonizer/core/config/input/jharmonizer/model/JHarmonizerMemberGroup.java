package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer.model;

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
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.Validate;

/**
 * Deserialization model for a single member-group entry in a JHarmonizer YAML config.
 * Captures the group name, include/exclude selector sets, ordering rules, separator style,
 * accessor-pair bundling flag, and optional sub-groups.
 */
@Value
public class JHarmonizerMemberGroup implements Serializable {

    @Serial
    // TODO We need it while we pass JHarmonizerMemberGroup to the Comparator as a constructor parameter
    private static final long serialVersionUID = 3113522117531985752L;

    @NonNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    Set<Set<String>> excludes;

    @NonNull
    Set<Set<String>> includes;

    @Nullable
    Boolean keepAccessorsTogether;

    @NonNull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    List<@NonNull JHarmonizerMemberGroup> memberSubGroups;

    @NonNull
    String name;

    @Nullable
    JHarmonizerSeparator separator;

    @Nullable
    List<JHarmonizerOrderingRule> orderingRules;

    @Builder
    private JHarmonizerMemberGroup(
            @NonNull @JsonProperty(value = "name", required = true) String name,
            @NonNull
                    @JsonDeserialize(using = SelectorsDeserializer.class)
                    @JsonProperty(value = "includes", required = true)
                    Set<Set<String>> includes,
            @Nullable @JsonDeserialize(using = SelectorsDeserializer.class) @JsonProperty(value = "excludes")
                    Set<Set<String>> excludes,
            @Nullable @JsonDeserialize(using = OrderingRulesDeserializer.class) @JsonProperty(value = "ordering-rules")
                    List<JHarmonizerOrderingRule> orderingRules,
            @Nullable @JsonProperty(value = "separator") JHarmonizerSeparator separator,
            @Nullable @JsonProperty(value = "keepAccessorsTogether") Boolean keepAccessorsTogether,
            @Nullable @JsonProperty(value = "groups") List<@NonNull JHarmonizerMemberGroup> memberSubGroups) {
        this.name = name;

        Validate.notEmpty(includes, "includes cannot be empty");
        this.includes = Collections.unmodifiableSet(includes);

        this.excludes = ofNullable(excludes).map(Collections::unmodifiableSet).orElse(Set.of());

        this.orderingRules =
                ofNullable(orderingRules).map(Collections::unmodifiableList).orElse(null);

        this.separator = separator;

        this.keepAccessorsTogether = keepAccessorsTogether;

        this.memberSubGroups =
                ofNullable(memberSubGroups).map(Collections::unmodifiableList).orElse(List.of());
    }
}

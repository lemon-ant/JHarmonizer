package io.github.lemon_ant.jharmonizer.core.config.input.jharmonizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import java.util.List;
import java.util.Set;

@UtilityClass
public class CreationHelper {
    public static HeaderLine createHeaderLine(char character,
                                             int leftPadding){return new HeaderLine(character, leftPadding);}

    public static TypeGroup createTypeGroup(@NonNull Set<@NonNull TypeKind> typeKinds){
        return new TypeGroup(typeKinds);
    }

    public static TopLevelTypesOrdering createTopLevelTypesOrdering(
        boolean mainTypeFirst,
        @NonNull  List<@NonNull TypeGroup> typeGroups,
        @NonNull
        List<SortKey> sortKeys) {
        return new TopLevelTypesOrdering(mainTypeFirst, typeGroups, sortKeys);

    }
}

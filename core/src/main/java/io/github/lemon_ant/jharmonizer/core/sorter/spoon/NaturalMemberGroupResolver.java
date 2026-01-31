package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static java.util.stream.Collectors.toUnmodifiableMap;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Default resolver: finds the first matching root group, then the deepest matching subgroup (via CompiledMemberGroup#classify).
 * <p>
 * Important: this assumes the config contains a "catch-all" group (e.g., empty includes) so every member is classified.
 */
@UtilityClass
class NaturalMemberGroupResolver {

    @NonNull
    Map<@NonNull CtTypeMember, @NonNull CompiledMemberGroup> resolveNaturalGroups(
            @NonNull CompiledMemberGroup rootMemberGroup,
            @NonNull Map<@NonNull CtTypeMember, @NonNull MemberDescriptor> typeMember2Descriptor) {

        return typeMember2Descriptor.entrySet().stream()
                .collect(toUnmodifiableMap(
                        Map.Entry::getKey, entry -> resolveSingleMemberGroup(rootMemberGroup, entry.getValue())));
    }

    private static CompiledMemberGroup resolveSingleMemberGroup(
            CompiledMemberGroup rootMemberGroup, MemberDescriptor memberDescriptor) {
        return rootMemberGroup.getCompiledSubGroups().stream()
                .map(compiledSubGroup -> compiledSubGroup.classifyRecursively(memberDescriptor))
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(rootMemberGroup);
    }
}

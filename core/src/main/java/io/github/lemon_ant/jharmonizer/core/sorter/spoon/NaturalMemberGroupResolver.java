package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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

    /**
     * Resolves the natural groups.
     * @param rootMemberGroup the root member group
     * @param typeMember2Descriptor the type member2 descriptor
     * @return the natural groups
     */
    @NonNull
    Map<@NonNull CtTypeMember, @NonNull CompiledMemberGroup> resolveNaturalGroups(
            @NonNull CompiledMemberGroup rootMemberGroup,
            @NonNull Map<@NonNull CtTypeMember, @NonNull MemberDescriptor> typeMember2Descriptor) {

        return typeMember2Descriptor.entrySet().stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> resolveSingleMemberGroup(rootMemberGroup, entry.getValue()),
                                (existingGroup, newGroup) -> {
                                    throw new IllegalStateException(
                                            "Unexpected duplicate CtTypeMember while resolving natural groups.");
                                },
                                LinkedHashMap::new),
                        Collections::unmodifiableMap));
    }

    @NonNull
    private static CompiledMemberGroup resolveSingleMemberGroup(
            CompiledMemberGroup rootMemberGroup, MemberDescriptor memberDescriptor) {
        return rootMemberGroup.getCompiledSubGroups().stream()
                .map(compiledSubGroup -> compiledSubGroup.classifyRecursively(memberDescriptor))
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(rootMemberGroup);
    }
}

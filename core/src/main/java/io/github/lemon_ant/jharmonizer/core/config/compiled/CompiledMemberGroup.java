package io.github.lemon_ant.jharmonizer.core.config.compiled;

import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * Tree node with precompiled include/exclude blocks and children.
 * Fallback to the parent when none of the children matched.
 */
@Value
@Builder
public class CompiledMemberGroup {
    @NonNull
    @Singular
    List<@NonNull CompiledMemberGroup> compiledSubGroups; // unmodifiable after build, ordered

    // TODO How to compile it???
    boolean keepAccessorsTogether;

    @Nullable
    String name;

    int orderIndex;

    @Builder.Default
    boolean relaxedForwardReferences = true;

    @NonNull
    CompiledMemberGroupSelectorBlock selectorBlock;

    @NonNull
    UnifiedSeparator separator;

    @NonNull
    @Singular
    // TODO How to compile it???
    List<@NonNull OrderingRule> orderingRules;

    /**
     * Classifies the recursively.
     * @param descriptor the member descriptor to inspect
     * @return the recursively
     */
    @NonNull
    public Optional<CompiledMemberGroup> classifyRecursively(@NonNull MemberDescriptor descriptor) {
        if (!selectorBlock.match(descriptor)) {
            return Optional.empty();
        }

        return compiledSubGroups.stream()
                .map(child -> child.classifyRecursively(descriptor))
                .flatMap(Optional::stream)
                .findFirst()
                .or(() -> Optional.of(this)); // fallback to parent bucket
    }
}

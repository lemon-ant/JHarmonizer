package io.github.lemon_ant.jharmonizer.core.config.compiled;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/**
 * Tree node with precompiled include/exclude blocks and children.
 * Fallback to the parent when none of the children matched.
 */
@Value
public class CompiledMemberGroup {
    @NonNull
    List<CompiledMemberGroup> compiledSubGroups; // immutable, ordered

    // TODO How to compile it???
    boolean keepAccessorsTogether;

    @Nullable
    String name;

    int orderIndex;

    @NonNull
    CompiledMemberGroupSelectorBlock selectorBlock;

    @NonNull
    UnifiedSeparator separator;

    @NonNull
    @Singular
    // TODO How to compile it???
    List<@NonNull OrderingRule> orderingRules;

    @Builder
    private CompiledMemberGroup(
            @NonNull List<CompiledMemberGroup> compiledSubGroups,
            boolean keepAccessorsTogether,
            int orderIndex,
            @Nullable String name,
            @NonNull CompiledMemberGroupSelectorBlock selectorBlock,
            @NonNull UnifiedSeparator separator,
            @NonNull @Singular List<@NonNull OrderingRule> orderingRules) {
        this.compiledSubGroups = Collections.unmodifiableList(compiledSubGroups);
        this.keepAccessorsTogether = keepAccessorsTogether;

        this.orderIndex = orderIndex;
        this.name = name;
        this.selectorBlock = selectorBlock;

        this.separator = separator;
        this.orderingRules = Collections.unmodifiableList(orderingRules);
    }

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

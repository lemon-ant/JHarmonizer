package io.github.lemon_ant.jharmonizer.core.config.compiled;

import static java.util.Collections.unmodifiableList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.Value;

/**
 * Full compiled config with top-level roots and post-order numbering.
 * Top-level roots are checked top-to-bottom; the first matching root captures the member and DFS continues.
 */
@Value
@SuppressWarnings("PMD")
@SuppressFBWarnings
public class CompiledConfig {
    @NonNull
    List<CompiledGroup> typeRoots;

    public CompiledConfig(@NonNull List<CompiledGroup> typeRoots) {
        this.typeRoots = unmodifiableList(typeRoots);
    }

    // TODO Delete it
    public static CompiledConfig assignPostOrderIndexes(@NonNull List<CompiledGroup> roots) {
        List<CompiledGroup> reindexed = new ArrayList<>(roots.size());
        int[] counter = new int[] {0};
        for (CompiledGroup root : roots) reindexed.add(assignRec(root, counter));
        return new CompiledConfig(unmodifiableList(reindexed));
    }

    // TODO Delete it
    private static CompiledGroup assignRec(CompiledGroup node, int[] counter) {
        List<CompiledGroup> newChildren =
                new ArrayList<>(node.getCompiledSubGroups().size());
        for (CompiledGroup c : node.getCompiledSubGroups()) newChildren.add(assignRec(c, counter));
        int idx = counter[0]++;
        return new CompiledGroup(
                node.getName(),
                idx,
                node.getSelectorBlock(),
                node.getGroupSortingBehavior(),
                unmodifiableList(newChildren),
                node.getSeparator());
    }

    @NonNull
    public Optional<CompiledGroup> matchGroup(@NonNull MemberDescriptor descriptor) {
        for (CompiledGroup typeRoot : typeRoots) {
            Optional<CompiledGroup> hit = typeRoot.classify(descriptor);
            if (hit.isPresent()) return hit;
        }
        return Optional.empty();
    }
}

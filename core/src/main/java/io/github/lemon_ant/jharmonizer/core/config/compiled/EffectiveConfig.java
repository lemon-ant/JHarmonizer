package io.github.lemon_ant.jharmonizer.core.config.compiled;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.lemon_ant.jharmonizer.core.config.unified.MemberDescriptor;
import java.util.ArrayList;
import java.util.Collections;
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
public class EffectiveConfig {
    @NonNull
    List<EffectiveGroup> typeRoots;

    public EffectiveConfig(@NonNull List<EffectiveGroup> typeRoots) {
        this.typeRoots = Collections.unmodifiableList(typeRoots);
    }

    public static EffectiveConfig assignPostOrderIndexes(@NonNull List<EffectiveGroup> roots) {
        List<EffectiveGroup> reindexed = new ArrayList<>(roots.size());
        int[] counter = new int[] {0};
        for (EffectiveGroup root : roots) reindexed.add(assignRec(root, counter));
        return new EffectiveConfig(Collections.unmodifiableList(reindexed));
    }

    private static EffectiveGroup assignRec(EffectiveGroup node, int[] counter) {
        List<EffectiveGroup> newChildren =
                new ArrayList<>(node.getNestedEffectiveGroups().size());
        for (EffectiveGroup c : node.getNestedEffectiveGroups()) newChildren.add(assignRec(c, counter));
        int idx = counter[0]++;
        return new EffectiveGroup(
                node.getName(),
                idx,
                node.getSelectorBlock(),
                node.getSortingBehavior(),
                java.util.Collections.unmodifiableList(newChildren));
    }

    @NonNull
    public Optional<EffectiveGroup> matchGroup(@NonNull MemberDescriptor descriptor) {
        for (EffectiveGroup typeRoot : typeRoots) {
            Optional<EffectiveGroup> hit = typeRoot.classify(descriptor);
            if (hit.isPresent()) return hit;
        }
        return Optional.empty();
    }
}

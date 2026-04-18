package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import lombok.Value;

/**
 * Holds the configuration settings used by all {@link MemberDependencyProvider} implementations
 * when detecting dependency edges between type members.
 *
 * <p>This object is created once per dependent member in {@link MemberDependencyGraphBuilder} and
 * passed to every provider, so all settings are grouped here for easy future extension.
 */
@Value
class DependencyDetectorConfig {

    /**
     * When {@code true}, getter/setter pairs for the same property are kept adjacent by adding
     * {@link MemberDependencyEdgeKind#ACCESSOR_BUNDLE} edges between them.
     */
    boolean keepAccessorsTogether;

    /**
     * When {@code true} (default, relaxed mode), only backward field references — where the provider
     * field is declared before the dependent member in source order — contribute dependency edges.
     * Forward references (to fields declared later) are ignored, allowing more freedom to reorder.
     *
     * <p>When {@code false} (strict mode), forward references also contribute dependency edges,
     * enforcing stricter declaration ordering constraints.
     */
    boolean relaxedForwardReferences;
}

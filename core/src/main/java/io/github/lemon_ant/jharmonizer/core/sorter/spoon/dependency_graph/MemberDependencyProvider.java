/*
 * SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.Set;
import lombok.NonNull;
import lombok.Value;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Contributes dependency edges to the graph.
 *
 * <p>Contract: for the given dependent member, returns direct provider edges.
 * The graph builder will add them as {@code provider -> dependent}.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
interface MemberDependencyProvider {

    /**
     * Finds direct provider edges for the dependent member.
     *
     * @param dependentMember the dependent member to inspect
     * @param providerConfig the provider configuration controlling accessor bundling and forward-reference strictness
     * @return the direct provider edges for the member
     */
    @NonNull
    Set<@NonNull MemberDependencyArc> findDirectProviderEdges(
            @NonNull CtTypeMember dependentMember, @NonNull ProviderConfig providerConfig);

    /**
     * Holds the configuration settings used by all {@link MemberDependencyProvider} implementations
     * when detecting dependency edges between type members.
     *
     * <p>This object is created once per dependent member in {@link MemberDependencyGraphBuilder}
     * and passed to every provider, so all settings are grouped here for easy future extension.
     */
    @Value
    class ProviderConfig {

        /**
         * When {@code true}, getter/setter pairs for the same property are kept adjacent by adding
         * {@link MemberDependencyEdgeKind#ACCESSOR_BUNDLE} edges between them.
         */
        boolean keepAccessorsTogether;

        /**
         * When {@code true} (default, relaxed mode), only backward field references — where the
         * provider field is declared before the dependent member in source order — contribute
         * dependency edges. Forward references (to fields declared later) are ignored, allowing
         * more freedom to reorder.
         *
         * <p>When {@code false} (strict mode), forward references also contribute dependency edges,
         * enforcing stricter declaration ordering constraints.
         */
        boolean relaxedForwardReferences;
    }
}

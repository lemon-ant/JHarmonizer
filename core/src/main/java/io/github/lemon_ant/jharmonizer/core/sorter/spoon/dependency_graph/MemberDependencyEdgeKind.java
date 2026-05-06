// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

/**
 * Describes how a dependency edge should be interpreted.
 *
 * <p>Only {@link #DECLARATION_DEPENDENCY} edges are allowed to participate in topo-ordering.
 * {@link #ACCESSOR_BUNDLE} edges exist only to keep accessors in the same effective group
 * and must be ignored by ordering algorithms.
 */
public enum MemberDependencyEdgeKind {
    /**
     * A real declaration-order constraint: provider must not appear later than dependent.
     * Used for topo-ordering.
     */
    DECLARATION_DEPENDENCY,

    /**
     * A non-ordering edge used to keep JavaBeans accessors (get/is/has/set) for the same property together.
     * Must be ignored by topo-ordering.
     */
    ACCESSOR_BUNDLE
}

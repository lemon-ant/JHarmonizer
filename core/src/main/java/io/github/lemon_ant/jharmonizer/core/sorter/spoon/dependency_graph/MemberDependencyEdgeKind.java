package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

/**
 * Describes how a dependency edge should be interpreted.
 *
 * <p>{@link #DECLARATION_DEPENDENCY} and {@link #SOURCE_ORDER_CONSTRAINT} edges participate in ordering.
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
     * A source-order-only constraint for cases that are not semantic dependencies but still become illegal when a
     * same-type implicit field access is moved above an earlier compile-time constant provider.
     */
    SOURCE_ORDER_CONSTRAINT,

    /**
     * A non-ordering edge used to keep JavaBeans accessors (get/is/has/set) for the same property together.
     * Must be ignored by topo-ordering.
     */
    ACCESSOR_BUNDLE
}

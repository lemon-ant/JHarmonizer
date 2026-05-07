// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import spoon.reflect.declaration.CtTypeMember;

/*
TODO(performance): Reduce allocations in transitive reachability computation (BFS).

Current situation:
- computeTransitiveNeighbors(...) performs BFS and repeatedly calls findDirectNeighbors(...).
- findDirectNeighbors(...) builds a new Set via stream().filter().map().collect(...) on every BFS step.
- On large types this causes a high volume of short-lived Set allocations and extra stream overhead.

Why "pure stream recursion" is risky:
- A recursive Stream.concat/flatMap graph traversal still needs a visited-set to avoid cycles.
- Without an explicit visited-set it can loop indefinitely on cycles and/or explode in work.
- With visited-set it becomes stateful anyway, so the "pure" stream approach is mostly cosmetic and can be slower.

Proposed improvements (keep semantics, minimal invasiveness):
1) Remove per-step Set allocation:
   - Replace findDirectNeighbors(...) with an iterator/stream over existing adjacency arcs.
   - Filter by allowed edge kinds using an int bitmask (already computed for cache key).
   - BFS iterates outgoing arcs directly and enqueues neighbors without creating intermediate collections.

2) Provide a stream façade without changing core logic:
   - Expose streamDirectNeighbors(...) returning outgoingArcs.stream().filter(...).map(...).
   - Optionally expose streamTransitiveNeighbors(...) backed by a lazy BFS Iterator
     (still uses a single visited-set for correctness, but avoids per-step collections).

Optional (more invasive) improvement:
3) Store adjacency indexed by edge kind to avoid per-arc filtering:
   - Maintain EnumMap<MemberDependencyEdgeKind, Set<CtTypeMember>> per member (outgoing + incoming).
   - This allows fetching neighbors for a single kind in O(1) without scanning all arcs.
   - Trade-off: more objects / memory; must be evaluated with benchmarks.

Correctness constraints:
- Transitive computation must remain cycle-safe (visited-set is required).
- Semantics of edge kind filtering must match current implementation.
- Start member should not be emitted as its own neighbor (current behavior).

Suggested micro-benchmark:
- Add JMH benchmark or at least a stress test over a large synthetic CtType
  to compare allocations/time before/after (focus on transitiveOutgoing/incoming queries).
*/
/**
 * Directed graph between members of a single type.
 *
 * <p>Edges are always directed {@code provider -> dependent}, but their meaning depends on
 * {@link MemberDependencyEdgeKind}:
 * <ul>
 *   <li>{@link MemberDependencyEdgeKind#DECLARATION_DEPENDENCY} encodes a real declaration-order constraint.</li>
 *   <li>{@link MemberDependencyEdgeKind#ACCESSOR_BUNDLE} encodes a keep-together constraint for accessors.</li>
 * </ul>
 *
 * <p>Implementation note: edges are stored as "flat" neighbor+kind values.
 * Filtering by edge kind is applied on query rather than being encoded into the storage structure.
 *
 * <p>Performance note: transitive queries are cached per (start member, edge-kind mask).
 * The graph is expected to be populated first and queried afterwards; any edge insertion invalidates caches.
 */
@SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.TooManyMethods"})
public final class MemberDependencyGraph {
    private static final Set<MemberDependencyEdgeKind> ALL_EDGE_KINDS = EnumSet.allOf(MemberDependencyEdgeKind.class);
    private static final int ALL_EDGE_KIND_MASK = (1 << MemberDependencyEdgeKind.values().length) - 1;
    private static final Set<MemberDependencyEdgeKind> DECLARATION_DEPENDENCY_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);
    private static final int ONE = 1;

    private final Map<CtTypeMember, Set<MemberDependencyArc>> incomingEdgesByDependent = new HashMap<>();
    private final Map<CtTypeMember, Set<MemberDependencyArc>> outgoingEdgesByProvider = new HashMap<>();
    private final Map<CtTypeMember, Map<Integer, Set<CtTypeMember>>> transitiveDependentsCacheByProvider =
            new HashMap<>();
    private final Map<CtTypeMember, Map<Integer, Set<CtTypeMember>>> transitiveProvidersCacheByDependent =
            new HashMap<>();

    /**
     * Finds the direct dependents.
     * @param providerMember the provider member
     * @param allowedEdgeKinds the allowed edge kinds
     * @return the matching direct dependents
     */
    @NonNull
    public Set<@NonNull CtTypeMember> findDirectDependents(
            @NonNull CtTypeMember providerMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        return findDirectNeighbors(outgoingEdgesByProvider, providerMember, allowedEdgeKinds);
    }

    /**
     * Finds the direct providers.
     * @param dependentMember the dependent member
     * @param allowedEdgeKinds the allowed edge kinds
     * @return the matching direct providers
     */
    @NonNull
    public Set<@NonNull CtTypeMember> findDirectProviders(
            @NonNull CtTypeMember dependentMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        return findDirectNeighbors(incomingEdgesByDependent, dependentMember, allowedEdgeKinds);
    }

    /**
     * Finds the transitive dependents.
     * @param providerMember the provider member
     * @return the matching transitive dependents
     */
    @NonNull
    public Set<@NonNull CtTypeMember> findTransitiveDependents(@NonNull CtTypeMember providerMember) {
        return findTransitiveDependents(providerMember, ALL_EDGE_KINDS);
    }

    /**
     * Finds the transitive dependents.
     * @param providerMember the provider member
     * @param allowedEdgeKinds the allowed edge kinds
     * @return the matching transitive dependents
     */
    @NonNull
    public Set<@NonNull CtTypeMember> findTransitiveDependents(
            @NonNull CtTypeMember providerMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        return findTransitiveNeighborsWithCaching(
                providerMember, allowedEdgeKinds, transitiveDependentsCacheByProvider, outgoingEdgesByProvider);
    }

    /**
     * Finds the transitive providers.
     * @param dependentMember the dependent member
     * @return the matching transitive providers
     */
    @NonNull
    public Set<@NonNull CtTypeMember> findTransitiveProviders(@NonNull CtTypeMember dependentMember) {
        return findTransitiveProviders(dependentMember, ALL_EDGE_KINDS);
    }

    /**
     * Finds the transitive providers.
     * @param dependentMember the dependent member
     * @param allowedEdgeKinds the allowed edge kinds
     * @return the matching transitive providers
     */
    @NonNull
    public Set<@NonNull CtTypeMember> findTransitiveProviders(
            @NonNull CtTypeMember dependentMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        return findTransitiveNeighborsWithCaching(
                dependentMember, allowedEdgeKinds, transitiveProvidersCacheByDependent, incomingEdgesByDependent);
    }

    /**
     * Performs the add edge.
     * @param providerMember the provider member
     * @param dependentMember the dependent member
     * @param edgeKind the edge kind
     */
    void addEdge(
            @NonNull CtTypeMember providerMember,
            @NonNull CtTypeMember dependentMember,
            @NonNull MemberDependencyEdgeKind edgeKind) {
        outgoingEdgesByProvider
                .computeIfAbsent(providerMember, ignored -> new HashSet<>())
                .add(new MemberDependencyArc(dependentMember, edgeKind));
        incomingEdgesByDependent
                .computeIfAbsent(dependentMember, ignored -> new HashSet<>())
                .add(new MemberDependencyArc(providerMember, edgeKind));

        invalidateTransitiveCaches();
    }

    /**
     * Finds the cycle path among {@link MemberDependencyEdgeKind#DECLARATION_DEPENDENCY} edges.
     *
     * <p>Only declaration-dependency edges are checked because they are the edges affected by
     * forward-reference strictness and the only edges that can produce ordering cycles.
     * Accessor-bundle edges do not participate in topological ordering.
     *
     * <p>The returned list represents the full cycle with the origin member repeated at both ends,
     * for example {@code [A, B, C, A]}, so the caller can render it as {@code "A -> B -> C -> A"}.
     *
     * @return the cycle path with the first member repeated at the end, or an empty list if no cycle exists
     */
    @NonNull
    List<CtTypeMember> findDeclarationDependencyCyclePath() {
        // Only members that have at least one outgoing edge can be part of a cycle.
        // Members appearing only in incomingEdgesByDependent are leaf nodes with no outgoing
        // edges and therefore cannot form or participate in a cycle.
        Set<CtTypeMember> membersWithOutgoingEdges = outgoingEdgesByProvider.keySet();

        Set<CtTypeMember> fullyVisited = new HashSet<>();
        Set<CtTypeMember> currentPath = new LinkedHashSet<>();

        for (CtTypeMember member : membersWithOutgoingEdges) {
            if (!fullyVisited.contains(member)) {
                List<CtTypeMember> cyclePath = detectCyclePathDfs(member, fullyVisited, currentPath);
                if (!cyclePath.isEmpty()) {
                    return cyclePath;
                }
            }
        }
        return List.of();
    }

    private static int calculateAllowedEdgeKindsMask(Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        if (allowedEdgeKinds == null
                || allowedEdgeKinds.isEmpty()
                || allowedEdgeKinds.size() == MemberDependencyEdgeKind.values().length) {
            return ALL_EDGE_KIND_MASK;
        }

        return allowedEdgeKinds.stream()
                .mapToInt(allowedEdgeKind -> 1 << allowedEdgeKind.ordinal())
                .reduce(0, (leftMask, edgeKindBit) -> leftMask | edgeKindBit);
    }

    @NonNull
    private static Set<CtTypeMember> computeTransitiveNeighbors(
            CtTypeMember startMember,
            Set<MemberDependencyEdgeKind> allowedEdgeKinds,
            Map<CtTypeMember, Set<MemberDependencyArc>> adjacency) {
        Set<CtTypeMember> visitedMembers = new HashSet<>();
        Deque<CtTypeMember> processingQueue = new ArrayDeque<>();

        processingQueue.add(startMember);

        while (!processingQueue.isEmpty()) {
            CtTypeMember currentMember = processingQueue.removeFirst();
            findDirectNeighbors(adjacency, currentMember, allowedEdgeKinds).stream()
                    .filter(visitedMembers::add)
                    .forEach(processingQueue::addLast);
        }

        return Collections.unmodifiableSet(visitedMembers);
    }

    @NonNull
    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    private static List<CtTypeMember> extractCyclePath(CtTypeMember cycleStart, Set<CtTypeMember> currentPath) {
        List<CtTypeMember> cycle = new ArrayList<>();
        boolean collecting = false;
        for (CtTypeMember pathMember : currentPath) {
            if (pathMember == cycleStart) {
                collecting = true;
            }
            if (collecting) {
                cycle.add(pathMember);
            }
        }
        cycle.add(cycleStart);
        return Collections.unmodifiableList(cycle);
    }

    @NonNull
    private static Set<@NonNull CtTypeMember> findDirectNeighbors(
            Map<CtTypeMember, Set<MemberDependencyArc>> adjacency,
            CtTypeMember vertex,
            Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        Set<MemberDependencyArc> memberDependencyArcs = adjacency.get(vertex);
        if (memberDependencyArcs == null || memberDependencyArcs.isEmpty()) {
            return Set.of();
        }

        boolean noFilteringRequested = allowedEdgeKinds == null
                || allowedEdgeKinds.isEmpty()
                || allowedEdgeKinds.size() == MemberDependencyEdgeKind.values().length;

        Stream<MemberDependencyArc> dependencyEdgeStream = memberDependencyArcs.stream();

        if (!noFilteringRequested) {
            if (allowedEdgeKinds.size() == ONE) {
                MemberDependencyEdgeKind singleEdgeKind =
                        allowedEdgeKinds.iterator().next();
                dependencyEdgeStream =
                        dependencyEdgeStream.filter(memberEdge -> memberEdge.getEdgeKind() == singleEdgeKind);
            } else {
                dependencyEdgeStream =
                        dependencyEdgeStream.filter(memberEdge -> allowedEdgeKinds.contains(memberEdge.getEdgeKind()));
            }
        }

        return dependencyEdgeStream.map(MemberDependencyArc::getAdjacentMember).collect(Collectors.toUnmodifiableSet());
    }

    @NonNull
    private List<CtTypeMember> detectCyclePathDfs(
            CtTypeMember current, Set<CtTypeMember> fullyVisited, Set<CtTypeMember> currentPath) {
        currentPath.add(current);

        for (CtTypeMember neighbor :
                findDirectNeighbors(outgoingEdgesByProvider, current, DECLARATION_DEPENDENCY_ONLY)) {
            if (currentPath.contains(neighbor)) {
                return extractCyclePath(neighbor, currentPath);
            }
            if (!fullyVisited.contains(neighbor)) {
                List<CtTypeMember> cyclePath = detectCyclePathDfs(neighbor, fullyVisited, currentPath);
                if (!cyclePath.isEmpty()) {
                    return cyclePath;
                }
            }
        }

        currentPath.remove(current);
        fullyVisited.add(current);
        return List.of();
    }

    @NonNull
    private Set<CtTypeMember> findTransitiveNeighborsWithCaching(
            CtTypeMember startMember,
            Set<MemberDependencyEdgeKind> allowedEdgeKinds,
            Map<CtTypeMember, Map<Integer, Set<CtTypeMember>>> transitiveCacheByStartMember,
            Map<CtTypeMember, Set<MemberDependencyArc>> adjacency) {
        int allowedEdgeKindsMask = calculateAllowedEdgeKindsMask(allowedEdgeKinds);

        Map<Integer, Set<CtTypeMember>> cachedNeighborsByEdgeKindMask = transitiveCacheByStartMember.get(startMember);

        if (cachedNeighborsByEdgeKindMask != null) {
            Set<CtTypeMember> cachedNeighbors = cachedNeighborsByEdgeKindMask.get(allowedEdgeKindsMask);
            if (cachedNeighbors != null) {
                return cachedNeighbors;
            }
        }

        Set<CtTypeMember> computedNeighbors = computeTransitiveNeighbors(startMember, allowedEdgeKinds, adjacency);

        transitiveCacheByStartMember
                .computeIfAbsent(startMember, ignored -> new HashMap<>())
                .put(allowedEdgeKindsMask, computedNeighbors);

        return computedNeighbors;
    }

    private void invalidateTransitiveCaches() {
        transitiveDependentsCacheByProvider.clear();
        transitiveProvidersCacheByDependent.clear();
    }
}

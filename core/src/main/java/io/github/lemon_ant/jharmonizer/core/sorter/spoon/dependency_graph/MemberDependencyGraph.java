package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import spoon.reflect.declaration.CtTypeMember;

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

    private static final int ALL_EDGE_KIND_MASK = (1 << MemberDependencyEdgeKind.values().length) - 1;
    private static final Set<MemberDependencyEdgeKind> ALL_EDGE_KINDS = EnumSet.allOf(MemberDependencyEdgeKind.class);
    private static final int ONE = 1;

    private final Map<CtTypeMember, Set<DependencyEdge>> outgoingEdgesByProvider = new HashMap<>();
    private final Map<CtTypeMember, Set<DependencyEdge>> incomingEdgesByDependent = new HashMap<>();

    private final Map<CtTypeMember, Map<Integer, Set<CtTypeMember>>> transitiveDependentsCacheByProvider =
            new HashMap<>();
    private final Map<CtTypeMember, Map<Integer, Set<CtTypeMember>>> transitiveProvidersCacheByDependent =
            new HashMap<>();

    @NonNull
    private static Set<CtTypeMember> computeTransitiveNeighbors(
            CtTypeMember startMember,
            Set<MemberDependencyEdgeKind> allowedEdgeKinds,
            Map<CtTypeMember, Set<DependencyEdge>> adjacency) {
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

    private static int toEdgeKindMask(Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        if (allowedEdgeKinds == null || allowedEdgeKinds.isEmpty()) {
            return ALL_EDGE_KIND_MASK;
        }

        if (allowedEdgeKinds.size() == MemberDependencyEdgeKind.values().length) {
            return ALL_EDGE_KIND_MASK;
        }

        int mask = 0;
        for (MemberDependencyEdgeKind allowedEdgeKind : allowedEdgeKinds) {
            mask |= (1 << allowedEdgeKind.ordinal());
        }
        return mask;
    }

    @NonNull
    private static Set<@NonNull CtTypeMember> findDirectNeighbors(
            Map<CtTypeMember, Set<DependencyEdge>> adjacency,
            CtTypeMember vertex,
            Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        Set<DependencyEdge> dependencyEdges = adjacency.get(vertex);
        if (dependencyEdges == null || dependencyEdges.isEmpty()) {
            return Set.of();
        }

        boolean noFilteringRequested = allowedEdgeKinds == null
                || allowedEdgeKinds.isEmpty()
                || allowedEdgeKinds.size() == MemberDependencyEdgeKind.values().length;

        Stream<DependencyEdge> dependencyEdgeStream = dependencyEdges.stream();

        if (!noFilteringRequested) {
            if (allowedEdgeKinds.size() == ONE) {
                MemberDependencyEdgeKind singleEdgeKind =
                        allowedEdgeKinds.iterator().next();
                dependencyEdgeStream =
                        dependencyEdgeStream.filter(dependencyEdge -> dependencyEdge.getEdgeKind() == singleEdgeKind);
            } else {
                dependencyEdgeStream = dependencyEdgeStream.filter(
                        dependencyEdge -> allowedEdgeKinds.contains(dependencyEdge.getEdgeKind()));
            }
        }

        return dependencyEdgeStream.map(DependencyEdge::getDependentMember).collect(Collectors.toUnmodifiableSet());
    }

    void addEdge(
            @NonNull CtTypeMember providerMember,
            @NonNull CtTypeMember dependentMember,
            @NonNull MemberDependencyEdgeKind edgeKind) {
        outgoingEdgesByProvider
                .computeIfAbsent(providerMember, ignored -> new HashSet<>())
                .add(new DependencyEdge(dependentMember, edgeKind));
        incomingEdgesByDependent
                .computeIfAbsent(dependentMember, ignored -> new HashSet<>())
                .add(new DependencyEdge(providerMember, edgeKind));

        invalidateTransitiveCaches();
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findTransitiveDependents(@NonNull CtTypeMember providerMember) {
        return findTransitiveDependents(providerMember, ALL_EDGE_KINDS);
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findTransitiveDependents(
            @NonNull CtTypeMember providerMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        return findTransitiveNeighborsWithCaching(
                providerMember, allowedEdgeKinds, transitiveDependentsCacheByProvider, outgoingEdgesByProvider);
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findTransitiveProviders(@NonNull CtTypeMember dependentMember) {
        return findTransitiveProviders(dependentMember, ALL_EDGE_KINDS);
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findTransitiveProviders(
            @NonNull CtTypeMember dependentMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        return findTransitiveNeighborsWithCaching(
                dependentMember, allowedEdgeKinds, transitiveProvidersCacheByDependent, incomingEdgesByDependent);
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findDirectDependents(
            @NonNull CtTypeMember providerMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        return findDirectNeighbors(outgoingEdgesByProvider, providerMember, allowedEdgeKinds);
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findDirectProviders(
            @NonNull CtTypeMember dependentMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        return findDirectNeighbors(incomingEdgesByDependent, dependentMember, allowedEdgeKinds);
    }

    @NonNull
    private Set<CtTypeMember> findTransitiveNeighborsWithCaching(
            CtTypeMember startMember,
            Set<MemberDependencyEdgeKind> allowedEdgeKinds,
            Map<CtTypeMember, Map<Integer, Set<CtTypeMember>>> transitiveCacheByStartMember,
            Map<CtTypeMember, Set<DependencyEdge>> adjacency) {
        int allowedEdgeKindsMask = toEdgeKindMask(allowedEdgeKinds);

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

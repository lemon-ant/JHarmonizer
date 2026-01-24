package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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
@SuppressWarnings("PMD.UseConcurrentHashMap")
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

        int allowedEdgeKindsMask = toEdgeKindMask(allowedEdgeKinds);

        Map<Integer, Set<CtTypeMember>> cachedDependentsByEdgeKindMask =
                transitiveDependentsCacheByProvider.get(providerMember);

        if (cachedDependentsByEdgeKindMask != null) {
            Set<CtTypeMember> cachedDependents = cachedDependentsByEdgeKindMask.get(allowedEdgeKindsMask);
            if (cachedDependents != null) {
                return cachedDependents;
            }
        }

        Set<CtTypeMember> computedDependents = computeTransitiveDependents(providerMember, allowedEdgeKinds);
        transitiveDependentsCacheByProvider
                .computeIfAbsent(providerMember, ignored -> new HashMap<>())
                .put(allowedEdgeKindsMask, computedDependents);

        return computedDependents;
    }

    @NonNull
    private Set<CtTypeMember> computeTransitiveDependents(
            @NonNull CtTypeMember providerMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {

        Set<CtTypeMember> foundDependents = new HashSet<>();
        Deque<CtTypeMember> processingProviders = new ArrayDeque<>();

        processingProviders.add(providerMember);

        while (!processingProviders.isEmpty()) {
            CtTypeMember currentProviderMember = processingProviders.removeFirst();
            findDirectDependents(currentProviderMember, allowedEdgeKinds).stream()
                    .filter(foundDependents::add)
                    .forEach(processingProviders::addLast);
        }

        return Collections.unmodifiableSet(foundDependents);
    }

    @NonNull
    // TODO Do we need it???
    Set<@NonNull CtTypeMember> findTransitiveProviders(@NonNull CtTypeMember dependentMember) {
        return findTransitiveProviders(dependentMember, ALL_EDGE_KINDS);
    }

    @NonNull
    // TODO Do we need it???
    Set<@NonNull CtTypeMember> findTransitiveProviders(
            @NonNull CtTypeMember dependentMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {

        int allowedEdgeKindsMask = toEdgeKindMask(allowedEdgeKinds);

        Map<Integer, Set<CtTypeMember>> cachedProvidersByEdgeKindMask =
                transitiveProvidersCacheByDependent.get(dependentMember);

        if (cachedProvidersByEdgeKindMask != null) {
            Set<CtTypeMember> cachedProviders = cachedProvidersByEdgeKindMask.get(allowedEdgeKindsMask);
            if (cachedProviders != null) {
                return cachedProviders;
            }
        }

        Set<CtTypeMember> computedProviders = computeTransitiveProviders(dependentMember, allowedEdgeKinds);
        transitiveProvidersCacheByDependent
                .computeIfAbsent(dependentMember, ignored -> new HashMap<>())
                .put(allowedEdgeKindsMask, computedProviders);

        return computedProviders;
    }

    @NonNull
    private Set<CtTypeMember> computeTransitiveProviders(
            @NonNull CtTypeMember dependentMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {

        Set<CtTypeMember> visitedMembers = new LinkedHashSet<>();
        Deque<CtTypeMember> queue = new ArrayDeque<>();

        queue.add(dependentMember);

        while (!queue.isEmpty()) {
            CtTypeMember currentDependentMember = queue.removeFirst();
            findDirectProviders(currentDependentMember, allowedEdgeKinds).stream()
                    .filter(visitedMembers::add)
                    .forEach(queue::addLast);
        }

        return Collections.unmodifiableSet(visitedMembers);
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findDirectDependents(
            @NonNull CtTypeMember providerMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        return findDirectNeighbors(outgoingEdgesByProvider, providerMember, allowedEdgeKinds);
    }

    @NonNull
    Set<@NonNull CtTypeMember> findDirectProviders(
            @NonNull CtTypeMember dependentMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        return findDirectNeighbors(incomingEdgesByDependent, dependentMember, allowedEdgeKinds);
    }

    private void invalidateTransitiveCaches() {
        transitiveDependentsCacheByProvider.clear();
        transitiveProvidersCacheByDependent.clear();
    }

    private static int toEdgeKindMask(@Nullable Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
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
            @Nullable Set<MemberDependencyEdgeKind> allowedEdgeKinds) {

        Set<DependencyEdge> dependencyEdges = adjacency.get(vertex);
        if (dependencyEdges == null || dependencyEdges.isEmpty()) {
            return Set.of();
        }

        // If no filter is needed, keep the predicate null and avoid extra checks in the stream.
        Predicate<DependencyEdge> edgeFilterPredicate = null;

        boolean noFilteringRequested = allowedEdgeKinds == null
                || allowedEdgeKinds.isEmpty()
                || allowedEdgeKinds.size() == MemberDependencyEdgeKind.values().length;

        if (!noFilteringRequested) {
            if (allowedEdgeKinds.size() == ONE) {
                MemberDependencyEdgeKind singleEdgeKind =
                        allowedEdgeKinds.iterator().next();
                edgeFilterPredicate = dependencyEdge -> dependencyEdge.getEdgeKind() == singleEdgeKind;
            } else {
                edgeFilterPredicate = dependencyEdge -> allowedEdgeKinds.contains(dependencyEdge.getEdgeKind());
            }
        }

        return (edgeFilterPredicate == null
                        ? dependencyEdges.stream()
                        : dependencyEdges.stream().filter(edgeFilterPredicate))
                .map(DependencyEdge::getDependentMember)
                .collect(Collectors.toUnmodifiableSet());
    }
}

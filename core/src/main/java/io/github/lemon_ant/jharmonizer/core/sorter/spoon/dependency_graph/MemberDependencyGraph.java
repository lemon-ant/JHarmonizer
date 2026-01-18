package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
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
 */
public final class MemberDependencyGraph {

    private final Map<CtTypeMember, Set<DependencyEdge>> outgoingEdgesByProvider = new HashMap<>();
    private final Map<CtTypeMember, Set<DependencyEdge>> incomingEdgesByDependent = new HashMap<>();

    void addEdge(
            @NonNull CtTypeMember providerMember,
            @NonNull CtTypeMember dependentMember,
            @NonNull MemberDependencyEdgeKind edgeKind) {

        registerVertex(providerMember);
        registerVertex(dependentMember);

        outgoingEdgesByProvider.get(providerMember).add(new DependencyEdge(dependentMember, edgeKind));
        incomingEdgesByDependent.get(dependentMember).add(new DependencyEdge(providerMember, edgeKind));
    }

    @NonNull
    Set<@NonNull CtTypeMember> getAllVertices() {
        // Both maps are always kept in sync via registerVertex().
        return Collections.unmodifiableSet(outgoingEdgesByProvider.keySet());
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findTransitiveDependents(@NonNull CtTypeMember providerMember) {
        return findTransitiveDependents(providerMember, Set.of(MemberDependencyEdgeKind.values()));
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findTransitiveDependents(
            @NonNull CtTypeMember providerMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {

        Set<CtTypeMember> visitedMembers = new HashSet<>();
        Deque<CtTypeMember> queue = new ArrayDeque<>();

        queue.add(providerMember);

        while (!queue.isEmpty()) {
            CtTypeMember currentProviderMember = queue.removeFirst();
            findDirectDependents(currentProviderMember, allowedEdgeKinds).stream()
                    .filter(visitedMembers::add)
                    .forEach(queue::addLast);
        }

        return Collections.unmodifiableSet(visitedMembers);
    }

    @NonNull
    // TODO Do we need it???
    Set<@NonNull CtTypeMember> findTransitiveProviders(@NonNull CtTypeMember dependentMember) {
        return findTransitiveProviders(dependentMember, Set.of(MemberDependencyEdgeKind.values()));
    }

    @NonNull
    // TODO Do we need it???
    Set<@NonNull CtTypeMember> findTransitiveProviders(
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
    Set<@NonNull CtTypeMember> findDirectDependents(
            @NonNull CtTypeMember providerMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        return findDirectNeighbors(outgoingEdgesByProvider, providerMember, allowedEdgeKinds);
    }

    @NonNull
    Set<@NonNull CtTypeMember> findDirectProviders(
            @NonNull CtTypeMember dependentMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        return findDirectNeighbors(incomingEdgesByDependent, dependentMember, allowedEdgeKinds);
    }

    private void registerVertex(CtTypeMember typeMember) {
        outgoingEdgesByProvider.computeIfAbsent(typeMember, ignored -> new HashSet<>());
        incomingEdgesByDependent.computeIfAbsent(typeMember, ignored -> new HashSet<>());
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
            if (allowedEdgeKinds.size() == 1) {
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

package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
 */
public final class MemberDependencyGraph {

    private final Map<CtTypeMember, EnumMap<MemberDependencyEdgeKind, Set<CtTypeMember>>> directDependentsByProvider =
            new HashMap<>();

    private final Map<CtTypeMember, EnumMap<MemberDependencyEdgeKind, Set<CtTypeMember>>> directProvidersByDependent =
            new HashMap<>();

    void addEdge(@NonNull MemberDependencyEdge dependencyEdge) {
        CtTypeMember providerMember = dependencyEdge.getProviderMember();
        CtTypeMember dependentMember = dependencyEdge.getDependentMember();
        MemberDependencyEdgeKind edgeKind = dependencyEdge.getEdgeKind();

        registerVertex(providerMember);
        registerVertex(dependentMember);

        directDependentsByProvider.get(providerMember).get(edgeKind).add(dependentMember);
        directProvidersByDependent.get(dependentMember).get(edgeKind).add(providerMember);
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findTransitiveDependents(@NonNull CtTypeMember providerMember) {
        return findTransitiveDependents(providerMember, EnumSet.allOf(MemberDependencyEdgeKind.class));
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findTransitiveDependents(
            @NonNull CtTypeMember providerMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {

        Set<CtTypeMember> visitedMembers = new HashSet<>();
        Deque<CtTypeMember> queue = new ArrayDeque<>();

        queue.add(providerMember);

        while (!queue.isEmpty()) {
            CtTypeMember currentProviderMember = queue.removeFirst();
            for (CtTypeMember directDependentMember : findDirectDependents(currentProviderMember, allowedEdgeKinds)) {
                if (visitedMembers.add(directDependentMember)) {
                    queue.addLast(directDependentMember);
                }
            }
        }

        return Collections.unmodifiableSet(visitedMembers);
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findTransitiveProviders(@NonNull CtTypeMember dependentMember) {
        return findTransitiveProviders(dependentMember, EnumSet.allOf(MemberDependencyEdgeKind.class));
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findTransitiveProviders(
            @NonNull CtTypeMember dependentMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {

        Set<CtTypeMember> visitedMembers = new LinkedHashSet<>();
        Deque<CtTypeMember> queue = new ArrayDeque<>();

        queue.add(dependentMember);

        while (!queue.isEmpty()) {
            CtTypeMember currentDependentMember = queue.removeFirst();
            for (CtTypeMember directProviderMember : findDirectProviders(currentDependentMember, allowedEdgeKinds)) {
                if (visitedMembers.add(directProviderMember)) {
                    queue.addLast(directProviderMember);
                }
            }
        }

        return Collections.unmodifiableSet(visitedMembers);
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findDirectDependents(
            @NonNull CtTypeMember providerMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        return findDirectNeighbors(directDependentsByProvider, providerMember, allowedEdgeKinds);
    }

    @NonNull
    public Set<@NonNull CtTypeMember> findDirectProviders(
            @NonNull CtTypeMember dependentMember, @NonNull Set<MemberDependencyEdgeKind> allowedEdgeKinds) {
        return findDirectNeighbors(directProvidersByDependent, dependentMember, allowedEdgeKinds);
    }

    private void registerVertex(@NonNull CtTypeMember typeMember) {
        directDependentsByProvider.computeIfAbsent(typeMember, ignored -> createEmptyAdjacencyByKind());
        directProvidersByDependent.computeIfAbsent(typeMember, ignored -> createEmptyAdjacencyByKind());
    }

    private static EnumMap<MemberDependencyEdgeKind, Set<CtTypeMember>> createEmptyAdjacencyByKind() {
        EnumMap<MemberDependencyEdgeKind, Set<CtTypeMember>> adjacencyByKind =
                new EnumMap<>(MemberDependencyEdgeKind.class);
        for (MemberDependencyEdgeKind edgeKind : MemberDependencyEdgeKind.values()) {
            adjacencyByKind.put(edgeKind, new HashSet<>());
        }
        return adjacencyByKind;
    }

    private static Set<CtTypeMember> findDirectNeighbors(
            Map<CtTypeMember, EnumMap<MemberDependencyEdgeKind, Set<CtTypeMember>>> adjacency,
            CtTypeMember vertex,
            Set<MemberDependencyEdgeKind> allowedEdgeKinds) {

        EnumMap<MemberDependencyEdgeKind, Set<CtTypeMember>> neighborsByKind = adjacency.get(vertex);
        if (neighborsByKind == null) {
            return Set.of();
        }

        if (allowedEdgeKinds.size() == 1) {
            MemberDependencyEdgeKind singleEdgeKind =
                    allowedEdgeKinds.iterator().next();
            return Collections.unmodifiableSet(neighborsByKind.getOrDefault(singleEdgeKind, Set.of()));
        }

        Set<CtTypeMember> mergedNeighbors = new LinkedHashSet<>();
        for (MemberDependencyEdgeKind edgeKind : allowedEdgeKinds) {
            mergedNeighbors.addAll(neighborsByKind.getOrDefault(edgeKind, Set.of()));
        }
        return Collections.unmodifiableSet(mergedNeighbors);
    }
}

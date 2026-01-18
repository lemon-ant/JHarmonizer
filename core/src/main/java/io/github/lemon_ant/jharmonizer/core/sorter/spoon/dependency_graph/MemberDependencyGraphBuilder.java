package io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Builds a directed dependency graph between members of a single {@link CtType}.
 */
@UtilityClass
public class MemberDependencyGraphBuilder {

    private static final EnumSet<MemberDependencyEdgeKind> ACCESSOR_BUNDLE_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE);
    private static final EnumSet<MemberDependencyEdgeKind> DECLARATION_DEPENDENCY_ONLY =
            EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

    private static final Collection<@NonNull MemberDependencyProvider> memberDependencyProviders = List.of(
            new AccessorPairDependencyProvider(),
            new FieldInitializerDependencyProvider(),
            new InitializerBlockDependencyProvider());

    @NonNull
    public static MemberDependencyGraph buildDependencyGraph(
            @NonNull CtType<?> type, @NonNull Map<CtTypeMember, CompiledMemberGroup> typeMember2NaturalMemberGroup) {

        MemberDependencyGraph memberDependencyGraph = new MemberDependencyGraph();

        type.getTypeMembers().forEach(providerMember -> {
            CompiledMemberGroup providerNaturalGroup =
                    resolveNaturalGroupOrThrow(providerMember, typeMember2NaturalMemberGroup);

            boolean keepAccessorsTogether = providerNaturalGroup.isKeepAccessorsTogether();

            memberDependencyProviders.stream()
                    .flatMap(memberDependencyProvider ->
                            memberDependencyProvider.findDirectEdges(providerMember, keepAccessorsTogether).stream())
                    .forEach(directEdge -> memberDependencyGraph.addEdge(
                            providerMember, directEdge.getDependentMember(), directEdge.getEdgeKind()));
        });

        propagateDeclarationDependenciesAcrossAccessorBundles(memberDependencyGraph);

        return memberDependencyGraph;
    }

    private static void propagateDeclarationDependenciesAcrossAccessorBundles(
            @NonNull MemberDependencyGraph memberDependencyGraph) {

        List<Set<CtTypeMember>> accessorBundleComponents = resolveAccessorBundleComponents(memberDependencyGraph);
        if (accessorBundleComponents.isEmpty()) {
            return;
        }

        for (Set<CtTypeMember> accessorBundleMembers : accessorBundleComponents) {
            Set<CtTypeMember> bundleDeclarationProviders = accessorBundleMembers.stream()
                    .flatMap(bundleMember ->
                            memberDependencyGraph
                                    .findDirectProviders(bundleMember, DECLARATION_DEPENDENCY_ONLY)
                                    .stream())
                    .collect(Collectors.toUnmodifiableSet());

            // If any accessor depends on an external provider, all accessors in the bundle must depend on it.
            bundleDeclarationProviders.forEach(providerMember -> accessorBundleMembers.stream()
                    .filter(accessorBundleMember -> providerMember != accessorBundleMember)
                    .forEach(accessorBundleMember -> memberDependencyGraph.addEdge(
                            providerMember, accessorBundleMember, MemberDependencyEdgeKind.DECLARATION_DEPENDENCY)));
        }
    }

    private static List<Set<CtTypeMember>> resolveAccessorBundleComponents(
            @NonNull MemberDependencyGraph memberDependencyGraph) {

        Set<CtTypeMember> visitedMembers = new HashSet<>();
        List<Set<CtTypeMember>> accessorBundleComponents = new ArrayList<>();

        for (CtTypeMember vertex : memberDependencyGraph.getAllVertices()) {
            if (visitedMembers.contains(vertex)) {
                continue;
            }

            Set<CtTypeMember> bundledNeighbors =
                    memberDependencyGraph.findDirectDependents(vertex, ACCESSOR_BUNDLE_ONLY);

            if (bundledNeighbors.isEmpty()) {
                continue;
            }

            // ACCESSOR_BUNDLE is guaranteed to be bidirectional (and effectively complete) within the bundle,
            // so direct neighbors from any member represent the whole accessor bundle.
            visitedMembers.addAll(bundledNeighbors);
            Set<CtTypeMember> componentMembers = new HashSet<>(bundledNeighbors);
            componentMembers.add(vertex);
            accessorBundleComponents.add(Collections.unmodifiableSet(componentMembers));
        }

        return Collections.unmodifiableList(accessorBundleComponents);
    }

    private static CompiledMemberGroup resolveNaturalGroupOrThrow(
            CtTypeMember dependentMember, Map<CtTypeMember, CompiledMemberGroup> typeMember2CompiledMemberGroup) {

        return Optional.ofNullable(typeMember2CompiledMemberGroup.get(dependentMember))
                .orElseThrow(() -> new IllegalStateException("Natural group was not resolved for type member. "
                        + "Expected typeMember2CompiledMemberGroup to contain all CtType.getTypeMembers(). "
                        + "Missing member: "
                        + dependentMember));
    }
}

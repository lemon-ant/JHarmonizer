package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.SortKey;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

@UtilityClass
class GroupMembersOrderer {

    @NonNull
    static List<@NonNull MemberGroupBlock> orderMembersInsideGroups(
            @NonNull List<@NonNull MemberGroupBlock> unorderedMemberGroupBlocks,
            @NonNull MemberDependencyGraph memberDependencyGraph) {

        List<MemberGroupBlock> orderedBlocks = new ArrayList<>(unorderedMemberGroupBlocks.size());

        for (MemberGroupBlock block : unorderedMemberGroupBlocks) {
            CompiledMemberGroup compiledMemberGroup = block.getCompiledMemberGroup();
            List<CtTypeMember> groupMembers = block.getTypeMembers();

            List<CtTypeMember> orderedMembers =
                    orderMembersInsideSingleGroup(compiledMemberGroup, groupMembers, memberDependencyGraph);

            orderedBlocks.add(new MemberGroupBlock(compiledMemberGroup, orderedMembers));
        }

        return List.copyOf(orderedBlocks);
    }

    @NonNull
    private static List<@NonNull CtTypeMember> orderMembersInsideSingleGroup(
            @NonNull CompiledMemberGroup compiledMemberGroup,
            @NonNull List<@NonNull CtTypeMember> groupMembers,
            @NonNull MemberDependencyGraph memberDependencyGraph) {

        if (groupMembers.size() <= 1) {
            return List.copyOf(groupMembers);
        }

        Comparator<CtTypeMember> baseComparator = buildComparatorForGroup(compiledMemberGroup);

        Set<CtTypeMember> groupMemberSet = Set.copyOf(groupMembers);

        // Reachability cache (provider -> all transitive dependents) restricted to the current group.
        Map<CtTypeMember, Set<CtTypeMember>> transitiveDependentsByProvider = groupMembers.stream()
                .collect(Collectors.toMap(providerMember -> providerMember, providerMember -> {
                    Set<CtTypeMember> reachableMembers = new LinkedHashSet<>();
                    reachableMembers.add(providerMember);
                    reachableMembers.addAll(memberDependencyGraph.findTransitiveDependents(providerMember));
                    reachableMembers.retainAll(groupMemberSet);
                    return Set.copyOf(reachableMembers);
                }));

        // 1) Build SCC-like units using mutual reachability. Accessor bundling typically creates such cycles.
        List<MemberUnit> memberUnits = buildMemberUnits(groupMembers, transitiveDependentsByProvider, baseComparator);

        Map<CtTypeMember, MemberUnit> memberUnitByMember = memberUnits.stream()
                .flatMap(memberUnit -> memberUnit.getMembers().stream().map(member -> Map.entry(member, memberUnit)))
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, Map.Entry::getValue, (existingValue, newValue) -> {
                            throw new IllegalStateException(
                                    "Unexpected duplicate member while building member->unit map." + " member="
                                            + describeTypeMember(existingValue.getRepresentativeMember()));
                        }));

        // 2) Build condensed dependency graph between units using reachability.
        Map<MemberUnit, Set<MemberUnit>> dependentUnitsByProviderUnit =
                buildDependentUnitsByProviderUnit(memberUnits, transitiveDependentsByProvider, memberUnitByMember);

        // 3) Stable topo-sort of units using sort keys as tie-breakers.
        List<MemberUnit> orderedUnits =
                topologicallyOrderUnits(memberUnits, dependentUnitsByProviderUnit, baseComparator);

        // 4) Expand units back to a flat list.
        List<CtTypeMember> orderedMembers = new ArrayList<>(groupMembers.size());
        for (MemberUnit memberUnit : orderedUnits) {
            List<CtTypeMember> orderedMembersInsideUnit = new ArrayList<>(memberUnit.getMembers());
            orderedMembersInsideUnit.sort(buildIntraUnitComparator(baseComparator));
            orderedMembers.addAll(orderedMembersInsideUnit);
        }

        return List.copyOf(orderedMembers);
    }

    @NonNull
    private static List<MemberUnit> buildMemberUnits(
            @NonNull List<CtTypeMember> groupMembers,
            @NonNull Map<CtTypeMember, Set<CtTypeMember>> transitiveDependentsByProvider,
            @NonNull Comparator<CtTypeMember> baseComparator) {

        UnionFind<CtTypeMember> unionFind = new UnionFind<>(groupMembers);

        for (CtTypeMember leftMember : groupMembers) {
            Set<CtTypeMember> leftReachableMembers = transitiveDependentsByProvider.getOrDefault(leftMember, Set.of());
            for (CtTypeMember rightMember : leftReachableMembers) {
                Set<CtTypeMember> rightReachableMembers =
                        transitiveDependentsByProvider.getOrDefault(rightMember, Set.of());
                if (rightReachableMembers.contains(leftMember)) {
                    unionFind.union(leftMember, rightMember);
                }
            }
        }

        Map<CtTypeMember, List<CtTypeMember>> membersByRoot = new LinkedHashMap<>();
        for (CtTypeMember typeMember : groupMembers) {
            CtTypeMember rootMember = unionFind.find(typeMember);
            membersByRoot
                    .computeIfAbsent(rootMember, ignored -> new ArrayList<>())
                    .add(typeMember);
        }

        Comparator<CtTypeMember> intraUnitComparator = buildIntraUnitComparator(baseComparator);

        List<MemberUnit> memberUnits = new ArrayList<>();
        int assignedUnitId = 0;
        for (List<CtTypeMember> unitMembers : membersByRoot.values()) {
            List<CtTypeMember> sortedUnitMembers = new ArrayList<>(unitMembers);
            sortedUnitMembers.sort(intraUnitComparator);

            CtTypeMember representativeMember = sortedUnitMembers.getFirst();
            int representativeSourceStart = extractSourceStart(representativeMember);

            memberUnits.add(new MemberUnit(
                    assignedUnitId++, List.copyOf(sortedUnitMembers), representativeMember, representativeSourceStart));
        }

        return List.copyOf(memberUnits);
    }

    @NonNull
    private static Map<MemberUnit, Set<MemberUnit>> buildDependentUnitsByProviderUnit(
            @NonNull List<MemberUnit> memberUnits,
            @NonNull Map<CtTypeMember, Set<CtTypeMember>> transitiveDependentsByProvider,
            @NonNull Map<CtTypeMember, MemberUnit> memberUnitByMember) {

        Map<MemberUnit, Set<MemberUnit>> dependentUnitsByProviderUnit = new LinkedHashMap<>();
        for (MemberUnit memberUnit : memberUnits) {
            dependentUnitsByProviderUnit.put(memberUnit, new LinkedHashSet<>());
        }

        for (Map.Entry<CtTypeMember, Set<CtTypeMember>> entry : transitiveDependentsByProvider.entrySet()) {
            CtTypeMember providerMember = entry.getKey();
            MemberUnit providerUnit = memberUnitByMember.get(providerMember);
            if (providerUnit == null) {
                continue;
            }

            for (CtTypeMember dependentMember : entry.getValue()) {
                if (providerMember == dependentMember) {
                    continue;
                }

                MemberUnit dependentUnit = memberUnitByMember.get(dependentMember);
                if (dependentUnit == null || dependentUnit == providerUnit) {
                    continue;
                }

                dependentUnitsByProviderUnit.get(providerUnit).add(dependentUnit);
            }
        }

        return dependentUnitsByProviderUnit.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }

    @NonNull
    private static List<MemberUnit> topologicallyOrderUnits(
            @NonNull List<MemberUnit> memberUnits,
            @NonNull Map<MemberUnit, Set<MemberUnit>> dependentUnitsByProviderUnit,
            @NonNull Comparator<CtTypeMember> baseComparator) {

        Map<MemberUnit, Integer> incomingEdgesByUnit = new LinkedHashMap<>();
        for (MemberUnit memberUnit : memberUnits) {
            incomingEdgesByUnit.put(memberUnit, 0);
        }

        for (Map.Entry<MemberUnit, Set<MemberUnit>> entry : dependentUnitsByProviderUnit.entrySet()) {
            for (MemberUnit dependentUnit : entry.getValue()) {
                incomingEdgesByUnit.computeIfPresent(dependentUnit, (unit, previousValue) -> previousValue + 1);
            }
        }

        Comparator<MemberUnit> unitTieBreakerComparator = Comparator.comparing(
                        MemberUnit::getRepresentativeMember, baseComparator)
                .thenComparingInt(MemberUnit::getRepresentativeSourceStart)
                .thenComparingInt(MemberUnit::getUnitId);

        PriorityQueue<MemberUnit> readyQueue = new PriorityQueue<>(unitTieBreakerComparator);
        for (Map.Entry<MemberUnit, Integer> entry : incomingEdgesByUnit.entrySet()) {
            if (entry.getValue() == 0) {
                readyQueue.add(entry.getKey());
            }
        }

        List<MemberUnit> orderedUnits = new ArrayList<>(memberUnits.size());
        Map<MemberUnit, Integer> remainingIncomingEdgesByUnit = new LinkedHashMap<>(incomingEdgesByUnit);

        while (!readyQueue.isEmpty()) {
            MemberUnit currentUnit = readyQueue.poll();
            orderedUnits.add(currentUnit);

            for (MemberUnit dependentUnit : dependentUnitsByProviderUnit.getOrDefault(currentUnit, Set.of())) {
                int updatedIncomingEdges = remainingIncomingEdgesByUnit.computeIfPresent(
                        dependentUnit, (ignored, previousValue) -> previousValue - 1);

                if (updatedIncomingEdges == 0) {
                    readyQueue.add(dependentUnit);
                }
            }
        }

        // This should not normally happen because SCC units should eliminate cycles.
        if (orderedUnits.size() != memberUnits.size()) {
            List<MemberUnit> fallback = new ArrayList<>(memberUnits);
            fallback.sort(unitTieBreakerComparator);
            return List.copyOf(fallback);
        }

        return List.copyOf(orderedUnits);
    }

    @NonNull
    private static Comparator<CtTypeMember> buildComparatorForGroup(@NonNull CompiledMemberGroup compiledMemberGroup) {
        List<SortKey> sortKeys = compiledMemberGroup.getSortKeys();

        // If no keys are configured (should not happen), preserve source order as a safe default.
        if (sortKeys.isEmpty()) {
            return Comparator.comparingInt(GroupMembersOrderer::extractSourceStart)
                    .thenComparing(GroupMembersOrderer::deriveAlphaKey);
        }

        Comparator<CtTypeMember> chainedComparator = null;

        for (SortKey sortKey : sortKeys) {
            Comparator<CtTypeMember> keyComparator =
                    switch (sortKey) {
                        case PRESERVE -> Comparator.comparingInt(GroupMembersOrderer::extractSourceStart);
                        case ALPHA -> Comparator.comparing(GroupMembersOrderer::deriveAlphaKey);
                        case SIGNATURE -> Comparator.comparing(GroupMembersOrderer::deriveSignatureKey);
                        case VISIBILITY_ASC ->
                            Comparator.comparingInt(GroupMembersOrderer::deriveVisibilityRankAscending);
                        case VISIBILITY_DESC ->
                            Comparator.comparingInt(GroupMembersOrderer::deriveVisibilityRankDescending);
                        default -> throw new IllegalStateException("TODO Normal message");
                    };

            chainedComparator =
                    (chainedComparator == null) ? keyComparator : chainedComparator.thenComparing(keyComparator);
        }

        // Deterministic tie-breakers regardless of configured keys.
        return chainedComparator
                .thenComparingInt(GroupMembersOrderer::extractSourceStart)
                .thenComparing(GroupMembersOrderer::deriveSignatureKey);
    }

    @NonNull
    private static Comparator<CtTypeMember> buildIntraUnitComparator(@NonNull Comparator<CtTypeMember> baseComparator) {
        return (leftMember, rightMember) -> {
            if (leftMember == rightMember) {
                return 0;
            }

            Optional<AccessorDescriptor> leftAccessorDescriptor = tryParseAccessorDescriptor(leftMember);
            Optional<AccessorDescriptor> rightAccessorDescriptor = tryParseAccessorDescriptor(rightMember);

            if (leftAccessorDescriptor.isPresent() && rightAccessorDescriptor.isPresent()) {
                AccessorDescriptor leftDescriptor = leftAccessorDescriptor.get();
                AccessorDescriptor rightDescriptor = rightAccessorDescriptor.get();

                boolean sameProperty = leftDescriptor.getPropertyName().equals(rightDescriptor.getPropertyName())
                        && leftDescriptor
                                .getPropertyTypeQualifiedName()
                                .equals(rightDescriptor.getPropertyTypeQualifiedName());

                if (sameProperty) {
                    int accessorKindComparison = Integer.compare(
                            leftDescriptor.getAccessorKind().getSortOrder(),
                            rightDescriptor.getAccessorKind().getSortOrder());
                    if (accessorKindComparison != 0) {
                        return accessorKindComparison;
                    }
                }
            }

            return baseComparator.compare(leftMember, rightMember);
        };
    }

    @NonNull
    private static Optional<AccessorDescriptor> tryParseAccessorDescriptor(@NonNull CtTypeMember typeMember) {
        if (!(typeMember instanceof CtMethod<?> method)) {
            return Optional.empty();
        }

        String methodName = method.getSimpleName();
        if (methodName == null || methodName.isBlank()) {
            return Optional.empty();
        }

        // Getter: getX(): T
        if (methodName.startsWith("get")
                && methodName.length() > 3
                && method.getParameters().isEmpty()
                && method.getType() != null
                && !"void".equalsIgnoreCase(method.getType().getSimpleName())) {

            String propertyName = decapitalize(methodName.substring(3));
            String propertyTypeQualifiedName = normalizeTypeQualifiedName(method.getType());

            return Optional.of(new AccessorDescriptor(AccessorKind.GET, propertyName, propertyTypeQualifiedName));
        }

        // Boolean getter: isX(): boolean/Boolean
        if (methodName.startsWith("is")
                && methodName.length() > 2
                && method.getParameters().isEmpty()
                && method.getType() != null
                && isBooleanLikeType(method.getType())) {

            String propertyName = decapitalize(methodName.substring(2));
            String propertyTypeQualifiedName = normalizeTypeQualifiedName(method.getType());

            return Optional.of(new AccessorDescriptor(AccessorKind.IS, propertyName, propertyTypeQualifiedName));
        }

        // Boolean getter: hasX(): boolean/Boolean
        if (methodName.startsWith("has")
                && methodName.length() > 3
                && method.getParameters().isEmpty()
                && method.getType() != null
                && isBooleanLikeType(method.getType())) {

            String propertyName = decapitalize(methodName.substring(3));
            String propertyTypeQualifiedName = normalizeTypeQualifiedName(method.getType());

            return Optional.of(new AccessorDescriptor(AccessorKind.HAS, propertyName, propertyTypeQualifiedName));
        }

        // Setter: setX(T): void
        if (methodName.startsWith("set")
                && methodName.length() > 3
                && method.getParameters().size() == 1) {

            CtTypeReference<?> parameterType = method.getParameters().getFirst().getType();
            if (parameterType == null) {
                return Optional.empty();
            }

            String propertyName = decapitalize(methodName.substring(3));
            String propertyTypeQualifiedName = normalizeTypeQualifiedName(parameterType);

            return Optional.of(new AccessorDescriptor(AccessorKind.SET, propertyName, propertyTypeQualifiedName));
        }

        return Optional.empty();
    }

    private static boolean isBooleanLikeType(@NonNull CtTypeReference<?> typeReference) {
        String qualifiedName = normalizeTypeQualifiedName(typeReference);
        return "boolean".equals(qualifiedName) || "java.lang.Boolean".equals(qualifiedName);
    }

    @NonNull
    private static String normalizeTypeQualifiedName(@NonNull CtTypeReference<?> typeReference) {
        CtTypeReference<?> erasure = typeReference.getTypeErasure();
        CtTypeReference<?> boxed = erasure == null ? typeReference : erasure.box();
        CtTypeReference<?> normalized = boxed == null ? typeReference : boxed;
        return Objects.requireNonNullElse(normalized.getQualifiedName(), "");
    }

    @NonNull
    private static String decapitalize(@NonNull String input) {
        if (input.isEmpty()) {
            return input;
        }

        // Follow JavaBeans Introspector.decapitalize semantics in a simplified form:
        // if first two chars are uppercase, keep as-is; otherwise lower the first char.
        if (input.length() >= 2 && Character.isUpperCase(input.charAt(0)) && Character.isUpperCase(input.charAt(1))) {
            return input;
        }

        return Character.toLowerCase(input.charAt(0)) + input.substring(1);
    }

    private static int deriveVisibilityRankAscending(@NonNull CtTypeMember typeMember) {
        // Ascending: public (0) -> protected (1) -> package-private (2) -> private (3)
        return deriveVisibilityRank(typeMember);
    }

    private static int deriveVisibilityRankDescending(@NonNull CtTypeMember typeMember) {
        // Descending: private (0) -> package-private (1) -> protected (2) -> public (3)
        int ascendingRank = deriveVisibilityRank(typeMember);
        return switch (ascendingRank) {
            case 0 -> 3;
            case 1 -> 2;
            case 2 -> 1;
            case 3 -> 0;
            default -> 1;
        };
    }

    private static int deriveVisibilityRank(@NonNull CtTypeMember typeMember) {
        Set<ModifierKind> modifiers = typeMember.getModifiers();
        if (modifiers.contains(ModifierKind.PUBLIC)) {
            return 0;
        }
        if (modifiers.contains(ModifierKind.PROTECTED)) {
            return 1;
        }
        if (modifiers.contains(ModifierKind.PRIVATE)) {
            return 3;
        }
        return 2; // package-private
    }

    private static int extractSourceStart(@NonNull CtTypeMember typeMember) {
        if (typeMember.getPosition() == null || !typeMember.getPosition().isValidPosition()) {
            return Integer.MAX_VALUE;
        }
        return typeMember.getPosition().getSourceStart();
    }

    @NonNull
    private static String deriveAlphaKey(@NonNull CtTypeMember typeMember) {
        return switch (typeMember) {
            case CtMethod<?> method ->
                method.getSimpleName() + "(" + deriveParameterTypeList(method.getParameters()) + ")";
            case CtConstructor<?> constructor -> "<init>(" + deriveParameterTypeList(constructor.getParameters()) + ")";
            case CtField<?> field -> field.getSimpleName();
            case CtType<?> nestedType -> nestedType.getSimpleName();
            default -> typeMember.getSimpleName();
        };
    }

    @NonNull
    private static String deriveSignatureKey(@NonNull CtTypeMember typeMember) {
        switch (typeMember) {
            case CtMethod<?> method -> {
                String methodName = method.getSimpleName();
                String parameters = deriveParameterTypeList(method.getParameters());
                String returnType = method.getType() == null ? "" : normalizeTypeQualifiedName(method.getType());
                return methodName + "(" + parameters + "):" + returnType;
            }
            case CtConstructor<?> constructor -> {
                String parameters = deriveParameterTypeList(constructor.getParameters());
                return "<init>(" + parameters + ")";
            }
            case CtField<?> field -> {
                String fieldName = field.getSimpleName();
                String fieldType = field.getType() == null ? "" : normalizeTypeQualifiedName(field.getType());
                return fieldName + ":" + fieldType;
            }
            case CtType<?> nestedType -> {
                return nestedType.getQualifiedName();
            }
            default -> {}
        }
        return typeMember.getSimpleName();
    }

    @NonNull
    private static String deriveParameterTypeList(@NonNull List<?> parameters) {
        // Spoon's parameter list is typed, but we keep this helper generic to avoid a large type signature here.
        return parameters.stream()
                .map(parameter -> {
                    if (parameter instanceof spoon.reflect.declaration.CtParameter<?> ctParameter) {
                        CtTypeReference<?> parameterType = ctParameter.getType();
                        return parameterType == null ? "" : normalizeTypeQualifiedName(parameterType);
                    }
                    return "";
                })
                .collect(Collectors.joining(","));
    }

    @NonNull
    private static String describeTypeMember(@NonNull CtTypeMember typeMember) {
        return typeMember.getClass().getSimpleName()
                + "{signature=" + deriveSignatureKey(typeMember)
                + ",sourceStart=" + extractSourceStart(typeMember)
                + "}";
    }

    @Value
    private static class MemberUnit {
        int unitId;

        @NonNull
        List<@NonNull CtTypeMember> members;

        @NonNull
        CtTypeMember representativeMember;

        int representativeSourceStart;
    }

    @Value
    private static class AccessorDescriptor {
        @NonNull
        AccessorKind accessorKind;

        @NonNull
        String propertyName;

        @NonNull
        String propertyTypeQualifiedName;
    }

    private enum AccessorKind {
        GET(0),
        IS(0),
        HAS(0),
        SET(1);

        private final int sortOrder;

        AccessorKind(int sortOrder) {
            this.sortOrder = sortOrder;
        }

        int getSortOrder() {
            return sortOrder;
        }
    }

    private static final class UnionFind<T> {

        private final Map<T, T> parentByItem = new LinkedHashMap<>();
        private final Map<T, Integer> rankByItem = new LinkedHashMap<>();

        UnionFind(@NonNull Collection<T> items) {
            for (T item : items) {
                parentByItem.put(item, item);
                rankByItem.put(item, 0);
            }
        }

        @NonNull
        T find(@NonNull T item) {
            T parent = parentByItem.get(item);
            if (parent == null) {
                parentByItem.put(item, item);
                rankByItem.put(item, 0);
                return item;
            }

            if (Objects.equals(parent, item)) {
                return item;
            }

            T root = find(parent);
            parentByItem.put(item, root);
            return root;
        }

        void union(@NonNull T leftItem, @NonNull T rightItem) {
            T leftRoot = find(leftItem);
            T rightRoot = find(rightItem);

            if (Objects.equals(leftRoot, rightRoot)) {
                return;
            }

            int leftRank = rankByItem.getOrDefault(leftRoot, 0);
            int rightRank = rankByItem.getOrDefault(rightRoot, 0);

            if (leftRank < rightRank) {
                parentByItem.put(leftRoot, rightRoot);
                return;
            }

            if (leftRank > rightRank) {
                parentByItem.put(rightRoot, leftRoot);
                return;
            }

            parentByItem.put(rightRoot, leftRoot);
            rankByItem.put(leftRoot, leftRank + 1);
        }
    }
}

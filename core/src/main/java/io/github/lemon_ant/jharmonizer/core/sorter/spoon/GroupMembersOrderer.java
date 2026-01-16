package io.github.lemon_ant.jharmonizer.core.sorter.spoon;

import static java.util.Objects.requireNonNull;

import io.github.lemon_ant.jharmonizer.core.config.compiled.CompiledMemberGroup;
import io.github.lemon_ant.jharmonizer.core.config.compiled.SortKey;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyEdgeKind;
import io.github.lemon_ant.jharmonizer.core.sorter.spoon.dependency_graph.MemberDependencyGraph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;

/**
 * Orders members inside already resolved effective groups.
 *
 * <p>Rules:
 * <ul>
 *   <li>Declaration dependencies ({@link MemberDependencyEdgeKind#DECLARATION_DEPENDENCY}) are respected via
 *       a stable topo-ordering.</li>
 *   <li>Accessor bundle edges ({@link MemberDependencyEdgeKind#ACCESSOR_BUNDLE}) do not impose ordering, but
 *       force accessors of the same property to stay contiguous. Bundles are topo-ordered as atomic blocks.</li>
 *   <li>When multiple valid orders exist, group sort keys ({@link SortKey}) define a deterministic tie-break.</li>
 * </ul>
 */
@UtilityClass
class GroupMembersOrderer {

    @NonNull
    List<@NonNull MemberGroupBlock> orderMembersInsideGroups(
            @NonNull List<@NonNull MemberGroupBlock> unorderedBlocks,
            @NonNull MemberDependencyGraph memberDependencyGraph) {

        List<MemberGroupBlock> orderedBlocks = new ArrayList<>(unorderedBlocks.size());

        for (MemberGroupBlock memberGroupBlock : unorderedBlocks) {
            CompiledMemberGroup compiledMemberGroup = memberGroupBlock.getCompiledMemberGroup();
            List<CtTypeMember> unorderedGroupMembers = memberGroupBlock.getTypeMembers();

            List<CtTypeMember> orderedGroupMembers =
                    orderMembersInsideSingleGroup(compiledMemberGroup, unorderedGroupMembers, memberDependencyGraph);

            orderedBlocks.add(new MemberGroupBlock(compiledMemberGroup, orderedGroupMembers));
        }

        return List.copyOf(orderedBlocks);
    }

    @NonNull
    private static List<CtTypeMember> orderMembersInsideSingleGroup(
            @NonNull CompiledMemberGroup compiledMemberGroup,
            @NonNull List<CtTypeMember> unorderedGroupMembers,
            @NonNull MemberDependencyGraph memberDependencyGraph) {

        if (unorderedGroupMembers.size() <= 1) {
            return List.copyOf(unorderedGroupMembers);
        }

        Comparator<CtTypeMember> preferredMemberComparator = buildPreferredMemberComparator(compiledMemberGroup);

        List<CtTypeMember> preferredMemberOrder =
                unorderedGroupMembers.stream().sorted(preferredMemberComparator).toList();

        Map<CtTypeMember, Integer> preferredIndexByMember = buildPreferredIndexByMember(preferredMemberOrder);
        Set<CtTypeMember> groupMemberSet = Set.copyOf(unorderedGroupMembers);

        List<MemberBundle> accessorBundles = splitIntoAccessorBundles(
                preferredMemberOrder, groupMemberSet, preferredIndexByMember, memberDependencyGraph);

        Map<CtTypeMember, Integer> bundleIdByMember = buildBundleIdByMember(accessorBundles);

        List<Integer> orderedBundleIds =
                topoOrderBundles(accessorBundles, groupMemberSet, bundleIdByMember, memberDependencyGraph);

        List<CtTypeMember> orderedMembers = new ArrayList<>(unorderedGroupMembers.size());
        for (Integer bundleId : orderedBundleIds) {
            MemberBundle memberBundle = accessorBundles.get(bundleId);
            orderedMembers.addAll(orderMembersInsideBundle(memberBundle.getMembers(), preferredMemberComparator));
        }

        return List.copyOf(orderedMembers);
    }

    @Value
    private static class MemberBundle {
        @NonNull
        Set<@NonNull CtTypeMember> members;

        int preferredRank;
    }

    @NonNull
    private static Map<CtTypeMember, Integer> buildPreferredIndexByMember(@NonNull List<CtTypeMember> preferredOrder) {
        Map<CtTypeMember, Integer> preferredIndexByMember = new HashMap<>(preferredOrder.size());
        for (int index = 0; index < preferredOrder.size(); index++) {
            preferredIndexByMember.put(preferredOrder.get(index), index);
        }
        return Map.copyOf(preferredIndexByMember);
    }

    @NonNull
    private static List<MemberBundle> splitIntoAccessorBundles(
            @NonNull List<CtTypeMember> preferredMemberOrder,
            @NonNull Set<CtTypeMember> groupMemberSet,
            @NonNull Map<CtTypeMember, Integer> preferredIndexByMember,
            @NonNull MemberDependencyGraph memberDependencyGraph) {

        Set<CtTypeMember> visitedMembers = new HashSet<>();
        List<MemberBundle> bundlesInPreferredOrder = new ArrayList<>();

        EnumSet<MemberDependencyEdgeKind> accessorBundleEdges = EnumSet.of(MemberDependencyEdgeKind.ACCESSOR_BUNDLE);

        for (CtTypeMember startMember : preferredMemberOrder) {
            if (!visitedMembers.add(startMember)) {
                continue;
            }

            Set<CtTypeMember> bundleMembers = new HashSet<>();
            Deque<CtTypeMember> queue = new ArrayDeque<>();

            bundleMembers.add(startMember);
            queue.addLast(startMember);

            while (!queue.isEmpty()) {
                CtTypeMember currentMember = queue.removeFirst();

                Set<CtTypeMember> directBundledDependents =
                        memberDependencyGraph.findDirectDependents(currentMember, accessorBundleEdges).stream()
                                .filter(groupMemberSet::contains)
                                .collect(java.util.stream.Collectors.toUnmodifiableSet());

                Set<CtTypeMember> directBundledProviders =
                        memberDependencyGraph.findDirectProviders(currentMember, accessorBundleEdges).stream()
                                .filter(groupMemberSet::contains)
                                .collect(java.util.stream.Collectors.toUnmodifiableSet());

                for (CtTypeMember neighbor : directBundledDependents) {
                    if (visitedMembers.add(neighbor)) {
                        bundleMembers.add(neighbor);
                        queue.addLast(neighbor);
                    }
                }

                for (CtTypeMember neighbor : directBundledProviders) {
                    if (visitedMembers.add(neighbor)) {
                        bundleMembers.add(neighbor);
                        queue.addLast(neighbor);
                    }
                }
            }

            int preferredRank = bundleMembers.stream()
                    .map(preferredIndexByMember::get)
                    .filter(index -> index != null)
                    .min(Integer::compareTo)
                    .orElse(Integer.MAX_VALUE);

            bundlesInPreferredOrder.add(new MemberBundle(Set.copyOf(bundleMembers), preferredRank));
        }

        return List.copyOf(bundlesInPreferredOrder);
    }

    @NonNull
    private static Map<CtTypeMember, Integer> buildBundleIdByMember(@NonNull List<MemberBundle> bundles) {
        Map<CtTypeMember, Integer> bundleIdByMember = new HashMap<>();

        for (int bundleId = 0; bundleId < bundles.size(); bundleId++) {
            for (CtTypeMember member : bundles.get(bundleId).getMembers()) {
                Integer previousBundleId = bundleIdByMember.put(member, bundleId);
                if (previousBundleId != null) {
                    throw new IllegalStateException("Type member was assigned to multiple accessor bundles. member="
                            + describeTypeMember(member)
                            + ", previousBundleId=" + previousBundleId
                            + ", newBundleId=" + bundleId);
                }
            }
        }

        return Map.copyOf(bundleIdByMember);
    }

    @NonNull
    private static List<Integer> topoOrderBundles(
            @NonNull List<MemberBundle> bundles,
            @NonNull Set<CtTypeMember> groupMemberSet,
            @NonNull Map<CtTypeMember, Integer> bundleIdByMember,
            @NonNull MemberDependencyGraph memberDependencyGraph) {

        int bundleCount = bundles.size();

        List<Set<Integer>> dependentsByProviderBundle = new ArrayList<>(bundleCount);
        for (int index = 0; index < bundleCount; index++) {
            dependentsByProviderBundle.add(new HashSet<>());
        }

        int[] inDegreeByBundleId = new int[bundleCount];

        EnumSet<MemberDependencyEdgeKind> declarationDependencyEdges =
                EnumSet.of(MemberDependencyEdgeKind.DECLARATION_DEPENDENCY);

        for (CtTypeMember providerMember : groupMemberSet) {
            int providerBundleId = requireBundleId(bundleIdByMember, providerMember);

            for (CtTypeMember dependentMember :
                    memberDependencyGraph.findDirectDependents(providerMember, declarationDependencyEdges)) {

                if (!groupMemberSet.contains(dependentMember)) {
                    continue;
                }

                int dependentBundleId = requireBundleId(bundleIdByMember, dependentMember);
                if (providerBundleId == dependentBundleId) {
                    continue;
                }

                boolean added = dependentsByProviderBundle.get(providerBundleId).add(dependentBundleId);
                if (added) {
                    inDegreeByBundleId[dependentBundleId]++;
                }
            }
        }

        PriorityQueue<Integer> readyBundleIds = new PriorityQueue<>(Comparator.comparingInt(
                        (Integer bundleId) -> bundles.get(bundleId).getPreferredRank())
                .thenComparingInt(Integer::intValue));

        for (int bundleId = 0; bundleId < bundleCount; bundleId++) {
            if (inDegreeByBundleId[bundleId] == 0) {
                readyBundleIds.add(bundleId);
            }
        }

        List<Integer> orderedBundleIds = new ArrayList<>(bundleCount);

        while (!readyBundleIds.isEmpty()) {
            Integer providerBundleId = readyBundleIds.remove();
            orderedBundleIds.add(providerBundleId);

            for (Integer dependentBundleId : dependentsByProviderBundle.get(providerBundleId)) {
                inDegreeByBundleId[dependentBundleId]--;
                if (inDegreeByBundleId[dependentBundleId] == 0) {
                    readyBundleIds.add(dependentBundleId);
                }
            }
        }

        if (orderedBundleIds.size() == bundleCount) {
            return List.copyOf(orderedBundleIds);
        }

        // Cycle (or self-referential constraints) detected. Fall back to deterministic preferred order.
        return java.util.stream.IntStream.range(0, bundleCount)
                .boxed()
                .sorted(Comparator.comparingInt(
                                (Integer bundleId) -> bundles.get(bundleId).getPreferredRank())
                        .thenComparingInt(Integer::intValue))
                .toList();
    }

    private static int requireBundleId(@NonNull Map<CtTypeMember, Integer> bundleIdByMember, CtTypeMember member) {
        Integer bundleId = bundleIdByMember.get(member);
        if (bundleId == null) {
            throw new IllegalStateException(
                    "Bundle id was not resolved for type member. member=" + describeTypeMember(member));
        }
        return bundleId;
    }

    @NonNull
    private static List<CtTypeMember> orderMembersInsideBundle(
            @NonNull Set<@NonNull CtTypeMember> bundleMembers,
            @NonNull Comparator<CtTypeMember> preferredMemberComparator) {

        if (bundleMembers.size() <= 1) {
            return List.copyOf(bundleMembers);
        }

        Comparator<CtTypeMember> accessorAwareComparator = Comparator.comparingInt(
                        GroupMembersOrderer::resolveAccessorRank)
                .thenComparing(preferredMemberComparator)
                .thenComparingInt(GroupMembersOrderer::safeSourceStart);

        return bundleMembers.stream().sorted(accessorAwareComparator).toList();
    }

    private enum AccessorKind {
        GET,
        IS,
        HAS,
        SET
    }

    private static int resolveAccessorRank(@NonNull CtTypeMember typeMember) {
        if (!(typeMember instanceof CtMethod<?> method)) {
            return Integer.MAX_VALUE;
        }

        return tryResolveAccessorKind(method)
                .map(accessorKind -> switch (accessorKind) {
                    case GET -> 0;
                    case IS -> 1;
                    case HAS -> 2;
                    case SET -> 3;
                })
                .orElse(Integer.MAX_VALUE);
    }

    @NonNull
    private static java.util.Optional<AccessorKind> tryResolveAccessorKind(@NonNull CtMethod<?> method) {
        String methodName = method.getSimpleName();

        java.util.Optional<AccessorKind> contractMatch =
                tryMatchAccessorContract(method, methodName, "get", 0).map(ignored -> AccessorKind.GET);
        if (contractMatch.isPresent()) {
            return contractMatch;
        }

        contractMatch = tryMatchAccessorContract(method, methodName, "is", 0)
                .filter(ignored -> returnsBoolean(method))
                .map(ignored -> AccessorKind.IS);
        if (contractMatch.isPresent()) {
            return contractMatch;
        }

        contractMatch = tryMatchAccessorContract(method, methodName, "has", 0)
                .filter(ignored -> returnsBoolean(method))
                .map(ignored -> AccessorKind.HAS);
        if (contractMatch.isPresent()) {
            return contractMatch;
        }

        return tryMatchAccessorContract(method, methodName, "set", 1)
                .filter(ignored -> returnsVoid(method))
                .map(ignored -> AccessorKind.SET);
    }

    @NonNull
    private static java.util.Optional<String> tryMatchAccessorContract(
            @NonNull CtMethod<?> method,
            @NonNull String methodName,
            @NonNull String prefix,
            int expectedParameterCount) {

        if (!methodName.startsWith(prefix)) {
            return java.util.Optional.empty();
        }

        if (method.getParameters().size() != expectedParameterCount) {
            return java.util.Optional.empty();
        }

        int propertyNameStartIndex = prefix.length();
        if (methodName.length() <= propertyNameStartIndex) {
            return java.util.Optional.empty();
        }

        if (!Character.isUpperCase(methodName.charAt(propertyNameStartIndex))) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(methodName.substring(propertyNameStartIndex));
    }

    private static boolean returnsVoid(@NonNull CtMethod<?> method) {
        CtTypeReference<?> returnTypeReference = method.getType();
        return returnTypeReference != null && "void".equals(returnTypeReference.getQualifiedName());
    }

    private static boolean returnsBoolean(@NonNull CtMethod<?> method) {
        CtTypeReference<?> returnTypeReference = method.getType();
        if (returnTypeReference == null) {
            return false;
        }

        CtTypeReference<?> erasedTypeReference = returnTypeReference.getTypeErasure();
        CtTypeReference<?> boxedTypeReference =
                erasedTypeReference.isPrimitive() ? erasedTypeReference.box() : erasedTypeReference;

        return "java.lang.Boolean".equals(boxedTypeReference.getQualifiedName());
    }

    @NonNull
    private static Comparator<CtTypeMember> buildPreferredMemberComparator(@NonNull CompiledMemberGroup group) {
        List<SortKey> sortKeys = group.getSortKeys();
        if (sortKeys.isEmpty()) {
            throw new IllegalStateException("Sort keys list must not be empty. groupName=" + group.getName());
        }

        Comparator<CtTypeMember> combinedComparator = null;

        for (SortKey sortKey : sortKeys) {
            Comparator<CtTypeMember> comparatorForKey =
                    switch (sortKey) {
                        case PRESERVE, SOURCE_ORDER -> Comparator.comparingInt(GroupMembersOrderer::safeSourceStart);
                        case ALPHA ->
                            Comparator.comparing(GroupMembersOrderer::safeNameForAlpha)
                                    .thenComparingInt(GroupMembersOrderer::safeSourceStart);
                        case VISIBILITY_ASC ->
                            Comparator.comparingInt(GroupMembersOrderer::resolveVisibilityRank)
                                    .thenComparingInt(GroupMembersOrderer::safeSourceStart);
                        case VISIBILITY_DESC ->
                            Comparator.comparingInt(GroupMembersOrderer::resolveVisibilityRank)
                                    .reversed()
                                    .thenComparingInt(GroupMembersOrderer::safeSourceStart);
                        case SIGNATURE ->
                            Comparator.comparing(GroupMembersOrderer::safeSignatureForOrdering)
                                    .thenComparingInt(GroupMembersOrderer::safeSourceStart);
                    };

            combinedComparator = (combinedComparator == null)
                    ? comparatorForKey
                    : combinedComparator.thenComparing(comparatorForKey);
        }

        requireNonNull(combinedComparator, "Expected non-null combined comparator");
        return combinedComparator.thenComparingInt(GroupMembersOrderer::safeSourceStart);
    }

    private static int resolveVisibilityRank(@NonNull CtTypeMember typeMember) {
        Set<ModifierKind> modifierKinds = typeMember.getModifiers();
        if (modifierKinds.contains(ModifierKind.PUBLIC)) {
            return 0;
        }
        if (modifierKinds.contains(ModifierKind.PROTECTED)) {
            return 1;
        }
        if (modifierKinds.contains(ModifierKind.PRIVATE)) {
            return 3;
        }
        return 2; // package-private
    }

    @NonNull
    private static String safeSignatureForOrdering(@NonNull CtTypeMember typeMember) {
        return switch (typeMember) {
            case CtMethod<?> method -> method.getSignature();
            case CtConstructor<?> constructor -> constructor.getSignature();
            case CtField<?> field -> safeTypeName(field.getType()) + " " + safeNameForAlpha(field);
            case CtType<?> nestedType -> nestedType.getQualifiedName();
            default -> typeMember.getClass().getSimpleName() + "(" + typeMember.getShortRepresentation() + ")";
        };
    }

    @NonNull
    private static String safeTypeName(CtTypeReference<?> typeReference) {
        if (typeReference == null) {
            return "<null>";
        }
        String qualifiedName = typeReference.getQualifiedName();
        return StringUtils.isNotBlank(qualifiedName) ? qualifiedName : "<unknown>";
    }

    private static int safeSourceStart(@NonNull CtTypeMember typeMember) {
        return typeMember.getPosition().isValidPosition()
                ? typeMember.getPosition().getSourceStart()
                : Integer.MAX_VALUE;
    }

    @NonNull
    private static String safeNameForAlpha(@NonNull CtTypeMember typeMember) {
        String simpleName = typeMember.getSimpleName();
        if (StringUtils.isNotBlank(simpleName)) {
            return simpleName;
        }

        String shortRepresentation = typeMember.getShortRepresentation();
        if (StringUtils.isNotBlank(shortRepresentation)) {
            return shortRepresentation;
        }

        return typeMember.getClass().getSimpleName();
    }

    @NonNull
    private static String describeTypeMember(@NonNull CtTypeMember typeMember) {
        return typeMember.getClass().getSimpleName() + "(" + typeMember.getShortRepresentation() + ")";
    }
}

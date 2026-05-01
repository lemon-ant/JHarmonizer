package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSrcTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.LongestIncreasingSubsequenceUtils.UNTRACKED;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.LongestIncreasingSubsequenceUtils.computeLisMask;

import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Utility class to detect relocations of type members in a reordered Spoon compilation unit.
 * This class identifies declared elements whose encounter order changed relative to the
 * original source snapshot captured before sorting.
 *
 * @deprecated This is a primitive utility to satisfy the basic needs of the Fail Fast processing. More verbose detector needed.
 */
@UtilityClass
public class RelocationDetector {

    /**
     * Computes the minimal set of member relocations needed to transform the original source
     * order into the sorted order of {@code reorderedCompilationUnit}.
     *
     * <p>Semantics:
     * <ul>
     *   <li>For each scope (file root, type body), the algorithm computes a Longest Increasing
     *       Subsequence (LIS) over the original-source indices of the sorted scope members.
     *       Members that participate in the LIS are considered <em>stable</em>; the remaining
     *       members are the minimal set that must be relocated to recover the sorted order from
     *       the original source.</li>
     *   <li>Consecutive moved members in the sorted order are glued into a single
     *       {@link MemberRelocation} chunk, so a developer reading the report sees exactly one
     *       insertion instruction per contiguous run.</li>
     *   <li>For each chunk, the predecessor and successor come from the sorted order at the
     *       chunk boundaries; either is {@code null} when the chunk sits at the start or end of
     *       its scope.</li>
     *   <li>Members with invalid source positions, or members not present in
     *       {@code originalMemberOrder}, are treated as stable and never reported as moved.</li>
     * </ul>
     *
     * @param originalMemberOrder flat list of all type members in their original source order,
     *                            as produced by {@link #snapshotOriginalMemberOrder}
     * @param reorderedCompilationUnit the reordered compilation unit to inspect
     * @return list of relocations for all detected chunks, in current encounter order
     */
    @NonNull
    public static List<MemberRelocation> findRelocations(
            @NonNull List<CtTypeMember> originalMemberOrder, @NonNull CtCompilationUnit reorderedCompilationUnit) {

        Map<CtTypeMember, Integer> originalIndex = buildOriginalIndexMap(originalMemberOrder);
        List<MemberRelocation> relocations = new ArrayList<>();
        List<CtType<?>> rootTypes = reorderedCompilationUnit.getDeclaredTypes();
        collectScopeRelocations(rootTypes, originalIndex, relocations);
        rootTypes.forEach(type -> collectTypeMemberRelocations(type, originalIndex, relocations));
        return List.copyOf(relocations);
    }

    /**
     * Returns whether any declared element has moved within its scope.
     *
     * <p>Compares the pre-sort {@code originalMemberOrder} snapshot with the flat member order
     * of {@code reorderedCompilationUnit} element by element. Any positional mismatch indicates
     * that at least one member was relocated.
     *
     * @param originalMemberOrder flat list of all type members in their original source order,
     *                            as produced by {@link #snapshotOriginalMemberOrder}
     * @param reorderedCompilationUnit the reordered compilation unit to inspect
     * @return {@code true} if any element is at a different position in the sorted order;
     *         otherwise {@code false}
     */
    // Identity comparison (!=) is intentional: after sorting the Spoon model the elements are
    // the same Java object references, just at different positions. We want reference equality
    // to detect positional changes, not structural equality.
    public static boolean isRelocated(
            @NonNull List<CtTypeMember> originalMemberOrder, @NonNull CtCompilationUnit reorderedCompilationUnit) {

        AtomicInteger index = new AtomicInteger(0);
        boolean mismatchFound = SpoonTypeUtils.streamDeclaredHierarchy(reorderedCompilationUnit)
                .anyMatch(member -> {
                    int currentIndex = index.getAndIncrement();
                    return currentIndex >= originalMemberOrder.size()
                            || originalMemberOrder.get(currentIndex) != member;
                });
        return mismatchFound || index.get() != originalMemberOrder.size();
    }

    /**
     * Computes the minimal moved set for {@code scopeMembers} via Longest Increasing Subsequence
     * over their original-source indices, then groups consecutive moved members in the sorted
     * order into a single {@link MemberRelocation} chunk.
     *
     * <p>This produces the smallest possible number of "insert this group between X and Y"
     * instructions a developer needs to apply to recover the sorted order from the original.
     *
     * @param scopeMembers   the ordered members of one scope (file root or a type body)
     * @param originalIndex  map from type member to its position in the flat original-order snapshot
     * @param relocations    accumulator for detected relocations
     */
    private static void collectScopeRelocations(
            List<? extends CtTypeMember> scopeMembers,
            Map<CtTypeMember, Integer> originalIndex,
            List<MemberRelocation> relocations) {
        int[] origIdx = computeOriginalIndices(scopeMembers, originalIndex);
        boolean[] inLis = computeLisMask(origIdx);
        emitMovedChunks(scopeMembers, origIdx, inLis, relocations);
    }

    /**
     * Builds an array of original-order indices for the given scope members.
     * Untracked positions (invalid source position or not present in the snapshot) are
     * represented by {@link #UNTRACKED}.
     *
     * @param scopeMembers   the ordered members of one scope
     * @param originalIndex  map from type member to its position in the flat original-order snapshot
     * @return original-order index per scope position, or {@link #UNTRACKED} when not mapped
     */
    @NonNull
    private static int[] computeOriginalIndices(
            List<? extends CtTypeMember> scopeMembers, Map<CtTypeMember, Integer> originalIndex) {
        int[] origIdx = new int[scopeMembers.size()];
        Arrays.fill(origIdx, UNTRACKED);
        for (int i = 0; i < scopeMembers.size(); i++) {
            Integer idx = originalIndex.get(scopeMembers.get(i));
            if (idx != null) {
                origIdx[i] = idx;
            }
        }
        return origIdx;
    }

    /**
     * Walks {@code origIdx} grouping consecutive tracked positions whose entry is not in the LIS
     * into one chunk per run, and emits a {@link MemberRelocation} for each chunk.
     *
     * @param scopeMembers  the ordered members of one scope
     * @param origIdx       original-order indices per scope position; {@link #UNTRACKED} for skip
     * @param inLis         mask marking scope positions that participate in the chosen LIS
     * @param relocations   accumulator for detected relocations
     */
    private static void emitMovedChunks(
            List<? extends CtTypeMember> scopeMembers,
            int[] origIdx,
            boolean[] inLis,
            List<MemberRelocation> relocations) {
        int n = origIdx.length;
        int i = 0;
        while (i < n) {
            if (!isMoved(origIdx, inLis, i)) {
                i++;
                continue;
            }
            int chunkStart = i;
            do {
                i++;
            } while (i < n && isMoved(origIdx, inLis, i));
            addMovedChunk(scopeMembers, chunkStart, i, relocations);
        }
    }

    private static boolean isMoved(int[] origIdx, boolean[] inLis, int position) {
        return origIdx[position] != UNTRACKED && !inLis[position];
    }

    private static void addMovedChunk(
            List<? extends CtTypeMember> scopeMembers,
            int chunkStart,
            int chunkEndExclusive,
            List<MemberRelocation> relocations) {
        CtTypeMember predecessor = chunkStart > 0 ? scopeMembers.get(chunkStart - 1) : null;
        CtTypeMember successor = chunkEndExclusive < scopeMembers.size() ? scopeMembers.get(chunkEndExclusive) : null;
        List<CtElement> chunk = scopeMembers.subList(chunkStart, chunkEndExclusive).stream()
                .map(CtElement.class::cast)
                .toList();
        relocations.add(new MemberRelocation(chunk, predecessor, successor));
    }

    /**
     * Checks the direct members of {@code type} for relocations, then recurses into nested types.
     *
     * @param type           the type whose member scope to check
     * @param originalIndex  map from type member to its position in the flat original-order snapshot
     * @param relocations    accumulator for detected relocations
     */
    private static void collectTypeMemberRelocations(
            CtType<?> type, Map<CtTypeMember, Integer> originalIndex, List<MemberRelocation> relocations) {
        List<CtTypeMember> members = streamExplicitSrcTypeMembers(type).toList();
        collectScopeRelocations(members, originalIndex, relocations);
        members.stream()
                .filter(typeMember -> typeMember instanceof CtType<?>)
                .map(typeMember -> (CtType<?>) typeMember)
                .forEach(nestedType -> collectTypeMemberRelocations(nestedType, originalIndex, relocations));
    }

    /**
     * Builds a flat snapshot of all type members declared in the compilation unit,
     * in source-code order (DFS: each type node followed by its own members recursively).
     *
     * <p>The returned list contains both the root type declarations and all their nested
     * members, interleaved in the order they appear in source code. The position of each
     * element in this list reflects its order in the original code. This snapshot can be
     * compared against the member order after sorting to detect relocations.
     *
     * @param compilationUnit the compilation unit to snapshot
     * @return immutable flat list of all type members in their original source order
     */
    @NonNull
    static List<CtTypeMember> snapshotOriginalMemberOrder(@NonNull CtCompilationUnit compilationUnit) {
        return SpoonTypeUtils.streamDeclaredHierarchy(compilationUnit).toList();
    }

    /**
     * Builds a map from each tracked member to its index in the flat original member-order snapshot.
     * Members with invalid positions are skipped.
     *
     * @param memberOrder the flat member order snapshot
     * @return map from type member to original-order index
     */
    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<CtTypeMember, Integer> buildOriginalIndexMap(List<CtTypeMember> memberOrder) {
        Map<CtTypeMember, Integer> result = new HashMap<>();
        for (int i = 0; i < memberOrder.size(); i++) {
            CtTypeMember member = memberOrder.get(i);
            if (member.getPosition().isValidPosition()) {
                result.put(member, i);
            }
        }
        return result;
    }
}

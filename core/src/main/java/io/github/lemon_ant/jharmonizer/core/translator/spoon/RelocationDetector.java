package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.processing_stat.PathDisplayFormatUtil.abbreviatePathForDisplay;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSrcTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.DeclarationHeaderRenderer.renderDeclarationHeader;
import static java.lang.System.lineSeparator;

import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.cu.SourcePosition;
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
@Deprecated
@SuppressWarnings("PMD.TooManyMethods")
// TODO Combine with ElementsFlatOrderIndexer
public class RelocationDetector {

    private static final int MAX_PATH_DISPLAY_LENGTH = 120;
    private static final int MAX_DISPLAYED_VIOLATIONS = 5;
    private static final int MAX_DISPLAYED_CHUNK_MEMBERS = 3;
    private static final String CHUNK_OMISSION_MARKER = "    ⋮";
    private static final int INITIAL_OUTPUT_BUFFER_CAPACITY = 256;

    /**
     * Computes relocations by comparing consecutive-pair relationships in the sorted order
     * against the original source order captured in {@code originalMemberOrder}.
     *
     * <p>Semantics:
     * <ul>
     *   <li>For each scope (file root, type body), the method walks consecutive pairs in the
     *       sorted member list and checks whether the original source had the same consecutive
     *       relationship.</li>
     *   <li>A break is reported when the element that follows a given member in the sorted order
     *       differs from the element that followed it in the original source.</li>
     *   <li>Consecutive elements in the sorted list that maintain their original relationships
     *       are grouped into a single {@link MemberRelocation} as a contiguous chunk.</li>
     *   <li>This means that moving one contiguous block generates a single relocation entry
     *       regardless of how many members are in the block.</li>
     *   <li>For each detected chunk, the predecessor and successor captured for diagnostic
     *       messages come from the sorted order at the chunk boundaries.</li>
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

        Map<SourcePosition, SourcePosition> originalSuccessors = buildScopeSuccessorMap(originalMemberOrder);
        List<MemberRelocation> relocations = new ArrayList<>();
        List<CtType<?>> rootTypes = reorderedCompilationUnit.getDeclaredTypes();
        collectScopeRelocations(rootTypes, originalSuccessors, relocations);
        rootTypes.forEach(type -> collectTypeMemberRelocations(type, originalSuccessors, relocations));
        return List.copyOf(relocations);
    }

    /**
     * Returns whether any declared element has moved within its scope.
     *
     * @param originalMemberOrder flat list of all type members in their original source order,
     *                            as produced by {@link #snapshotOriginalMemberOrder}
     * @param reorderedCompilationUnit the reordered compilation unit to inspect
     * @return {@code true} if any consecutive-pair relationship changed; otherwise {@code false}
     */
    public static boolean isRelocated(
            @NonNull List<CtTypeMember> originalMemberOrder, @NonNull CtCompilationUnit reorderedCompilationUnit) {

        Map<SourcePosition, SourcePosition> originalSuccessors = buildScopeSuccessorMap(originalMemberOrder);
        List<CtType<?>> rootTypes = reorderedCompilationUnit.getDeclaredTypes();
        return hasScopeRelocation(rootTypes, originalSuccessors)
                || rootTypes.stream().anyMatch(type -> hasTypeMemberRelocation(type, originalSuccessors));
    }

    /**
     * Formats the relocations into a human-readable string.
     *
     * <p>The path is placed on its own indented line after the header, abbreviated to
     * at most {@value MAX_PATH_DISPLAY_LENGTH} characters. Each numbered entry shows
     * only the declaring type name (no repeated "ordering violation" label), then the
     * predecessor, violating member (marked with {@code -->}), and successor snippets.
     * At most {@value MAX_DISPLAYED_VIOLATIONS} entries are printed; if there are more,
     * a footer line reports the total count.
     *
     * <p>Example output for a class where {@code void b()} should come after {@code void a()}:
     * <pre>
     * Detected member ordering violations in:
     *   Sample.java
     *   [1] com.example.Sample:
     *         public void a() { ... }
     *     --> public void b() { ... }
     * </pre>
     *
     * @param path        the path of the file where the relocations were detected
     * @param relocations the collection of relocations to format
     * @return a formatted string representing the relocations
     */
    @NonNull
    public static String printRelocations(@NonNull Path path, @NonNull Collection<MemberRelocation> relocations) {
        List<MemberRelocation> relocationList = List.copyOf(relocations);
        int totalCount = relocationList.size();
        int displayedCount = Math.min(totalCount, MAX_DISPLAYED_VIOLATIONS);
        StringBuilder sb = new StringBuilder(INITIAL_OUTPUT_BUFFER_CAPACITY);
        sb.append("Detected member ordering violations in:")
                .append(lineSeparator())
                .append("  ")
                .append(abbreviatePathForDisplay(path, MAX_PATH_DISPLAY_LENGTH));
        for (int i = 0; i < displayedCount; i++) {
            sb.append(lineSeparator());
            appendRelocationEntry(sb, relocationList.get(i), i + 1);
        }
        if (totalCount > MAX_DISPLAYED_VIOLATIONS) {
            sb.append(lineSeparator()).append(String.format("  ... %d violations total", totalCount));
        }
        return sb.toString();
    }

    private static void appendRelocationEntry(StringBuilder sb, MemberRelocation relocation, int index) {
        CtElement firstMember = relocation.getRelocatedMembers().get(0);
        String typeName = firstMember instanceof CtTypeMember member && member.getDeclaringType() != null
                ? member.getDeclaringType().getQualifiedName()
                : "<file root>";
        sb.append(String.format("  [%d] %s:", index, typeName));
        if (relocation.getSortedPredecessor() != null) {
            sb.append(lineSeparator())
                    .append(String.format("        %s", renderDeclarationHeader(relocation.getSortedPredecessor())));
        }
        appendChunkLines(sb, relocation.getRelocatedMembers());
        if (relocation.getSortedSuccessor() != null) {
            sb.append(lineSeparator())
                    .append(String.format("        %s", renderDeclarationHeader(relocation.getSortedSuccessor())));
        }
    }

    private static void appendChunkLines(StringBuilder sb, List<CtElement> members) {
        if (members.size() <= MAX_DISPLAYED_CHUNK_MEMBERS) {
            for (CtElement member : members) {
                sb.append(lineSeparator()).append(String.format("    --> %s", renderDeclarationHeader(member)));
            }
        } else {
            sb.append(lineSeparator())
                    .append(String.format("    --> %s", renderDeclarationHeader(members.get(0))))
                    .append(lineSeparator())
                    .append(CHUNK_OMISSION_MARKER)
                    .append(lineSeparator())
                    .append(String.format("    --> %s", renderDeclarationHeader(members.get(members.size() - 1))));
        }
    }

    /**
     * Scans {@code scopeMembers} for consecutive pairs whose relationship in the sorted order
     * differs from the original source order, and adds a {@link MemberRelocation} for each
     * detected contiguous chunk of displaced members.
     *
     * <p>When a break is detected at position {@code i} (i.e. the element at {@code i+1} is not
     * the expected original successor of the element at {@code i}), the algorithm extends the
     * chunk forward as long as consecutive sorted elements maintain their original consecutive
     * relationship. All such elements are grouped into a single {@link MemberRelocation}.
     *
     * @param scopeMembers         the ordered members of one scope (file root or a type body)
     * @param originalSuccessors   original within-scope successor position map keyed by source position
     * @param relocations          accumulator for detected relocations
     */
    private static void collectScopeRelocations(
            List<? extends CtTypeMember> scopeMembers,
            Map<SourcePosition, SourcePosition> originalSuccessors,
            List<MemberRelocation> relocations) {
        int i = 0;
        while (i < scopeMembers.size() - 1) {
            CtTypeMember current = scopeMembers.get(i);
            CtTypeMember actualNext = scopeMembers.get(i + 1);
            if (!current.getPosition().isValidPosition()) {
                i++;
                continue;
            }
            SourcePosition expectedNextPos = originalSuccessors.get(current.getPosition());
            if (!Objects.equals(expectedNextPos, actualNext.getPosition())) {
                int chunkEnd = findChunkEndInScope(scopeMembers, originalSuccessors, i + 1);
                List<CtElement> chunk = scopeMembers.subList(i + 1, chunkEnd + 1).stream()
                        .map(CtElement.class::cast)
                        .toList();
                CtTypeMember successor = chunkEnd + 1 < scopeMembers.size() ? scopeMembers.get(chunkEnd + 1) : null;
                relocations.add(new MemberRelocation(chunk, current, successor));
                i = chunkEnd + 1;
            } else {
                i++;
            }
        }
    }

    /**
     * Finds the last index of a contiguous chunk starting at {@code chunkStart} in
     * {@code scopeMembers}, where consecutive elements maintain their original consecutive
     * relationship recorded in {@code originalSuccessors}.
     *
     * @param scopeMembers       the ordered members of one scope
     * @param originalSuccessors original within-scope successor position map
     * @param chunkStart         starting index of the chunk (inclusive)
     * @return the last index of the chunk (inclusive)
     */
    private static int findChunkEndInScope(
            List<? extends CtTypeMember> scopeMembers,
            Map<SourcePosition, SourcePosition> originalSuccessors,
            int chunkStart) {
        int chunkEnd = chunkStart;
        while (chunkEnd + 1 < scopeMembers.size()) {
            CtTypeMember chunkTail = scopeMembers.get(chunkEnd);
            CtTypeMember chunkTailNext = scopeMembers.get(chunkEnd + 1);
            if (!chunkTail.getPosition().isValidPosition()) {
                break;
            }
            SourcePosition expectedAfterChunkTail = originalSuccessors.get(chunkTail.getPosition());
            if (!Objects.equals(expectedAfterChunkTail, chunkTailNext.getPosition())) {
                break;
            }
            chunkEnd++;
        }
        return chunkEnd;
    }

    /**
     * Checks the direct members of {@code type} for consecutive-pair relocation breaks, then
     * recurses into any nested types.
     *
     * @param type                 the type whose member scope to check
     * @param originalSuccessors   original within-scope successor position map keyed by source position
     * @param relocations          accumulator for detected relocations
     */
    private static void collectTypeMemberRelocations(
            CtType<?> type,
            Map<SourcePosition, SourcePosition> originalSuccessors,
            List<MemberRelocation> relocations) {
        List<CtTypeMember> members = streamExplicitSrcTypeMembers(type).toList();
        collectScopeRelocations(members, originalSuccessors, relocations);
        members.stream()
                .filter(m -> m instanceof CtType<?>)
                .map(m -> (CtType<?>) m)
                .forEach(nestedType -> collectTypeMemberRelocations(nestedType, originalSuccessors, relocations));
    }

    /**
     * Returns {@code true} if any consecutive-pair relationship in {@code scopeMembers} differs
     * from the original source order.
     *
     * @param scopeMembers         the ordered members of one scope
     * @param originalSuccessors   original within-scope successor position map keyed by source position
     * @return {@code true} if a break point was found; otherwise {@code false}
     */
    private static boolean hasScopeRelocation(
            List<? extends CtTypeMember> scopeMembers, Map<SourcePosition, SourcePosition> originalSuccessors) {
        for (int i = 0; i < scopeMembers.size() - 1; i++) {
            CtTypeMember current = scopeMembers.get(i);
            CtTypeMember actualNext = scopeMembers.get(i + 1);
            if (!current.getPosition().isValidPosition()) {
                continue;
            }
            SourcePosition expectedNextPos = originalSuccessors.get(current.getPosition());
            if (!Objects.equals(expectedNextPos, actualNext.getPosition())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if any direct member of {@code type} or any nested type member has a
     * break in the consecutive-pair order.
     *
     * @param type                 the type whose member scope to check recursively
     * @param originalSuccessors   original within-scope successor position map keyed by source position
     * @return {@code true} if a break point was found; otherwise {@code false}
     */
    private static boolean hasTypeMemberRelocation(
            CtType<?> type, Map<SourcePosition, SourcePosition> originalSuccessors) {
        List<CtTypeMember> members = streamExplicitSrcTypeMembers(type).toList();
        return hasScopeRelocation(members, originalSuccessors)
                || members.stream()
                        .filter(m -> m instanceof CtType<?>)
                        .map(m -> (CtType<?>) m)
                        .anyMatch(nestedType -> hasTypeMemberRelocation(nestedType, originalSuccessors));
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
        return SpoonTypeUtils.streamDeclaredHierarchy(compilationUnit)
                .map(element -> (CtTypeMember) element)
                .toList();
    }

    /**
     * Derives a within-scope successor map from the given flat member order.
     *
     * <p>For each element in {@code memberOrder}, the map records its source position mapped to
     * the source position of the next element that belongs to the same declaring scope (same
     * declaring type, or both root-level). Elements with no in-scope successor, or with invalid
     * positions, are absent from the map.
     *
     * @param memberOrder the flat member order snapshot
     * @return map from each element's source position to its original in-scope next sibling's
     *         source position
     */
    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<SourcePosition, SourcePosition> buildScopeSuccessorMap(List<CtTypeMember> memberOrder) {
        Map<SourcePosition, SourcePosition> result = new HashMap<>();
        for (int i = 0; i < memberOrder.size(); i++) {
            CtTypeMember current = memberOrder.get(i);
            if (!current.getPosition().isValidPosition()) {
                continue;
            }
            CtType<?> currentScope = current.getDeclaringType();
            for (int j = i + 1; j < memberOrder.size(); j++) {
                CtTypeMember candidate = memberOrder.get(j);
                if (Objects.equals(candidate.getDeclaringType(), currentScope)
                        && candidate.getPosition().isValidPosition()) {
                    result.put(current.getPosition(), candidate.getPosition());
                    break;
                }
            }
        }
        return result;
    }
}

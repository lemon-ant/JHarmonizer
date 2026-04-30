package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.processing_stat.PathDisplayFormatUtil.abbreviatePathForDisplay;
import static io.github.lemon_ant.jharmonizer.core.sorter.spoon.SpoonTypeMemberUtils.streamExplicitSrcTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.DeclarationHeaderRenderer.renderDeclarationHeader;
import static java.lang.System.lineSeparator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
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
// TODO Combine with ElementsFlatOrderIndexer
public class RelocationDetector {

    private static final int MAX_PATH_DISPLAY_LENGTH = 120;
    private static final int MAX_DISPLAYED_VIOLATIONS = 5;
    private static final int INITIAL_OUTPUT_BUFFER_CAPACITY = 256;

    /**
     * Computes relocations of declared elements by comparing their current within-scope position
     * against the previously captured original within-scope position.
     *
     * <p>Semantics:
     * <ul>
     *   <li>For each scope (file root, type body), the current position of each element within
     *       that scope is compared with its original within-scope position from
     *       {@code originalOrderIndices}.</li>
     *   <li>Elements not present in the original snapshot (newly added) are ignored.</li>
     *   <li>Only elements whose within-scope position changed are reported; elements that merely
     *       moved because their enclosing type changed position are not flagged.</li>
     *   <li>For each relocated element, the immediately preceding and following elements in the
     *       sorted order within the same scope are captured as {@link MemberRelocation#getSortedPredecessor()} and
     *       {@link MemberRelocation#getSortedSuccessor()}, so diagnostic messages can show the user
     *       exactly where the member should appear.</li>
     * </ul>
     *
     * <p>Notes:
     * <ul>
     *   <li>Positive offset means the element moved down (appears later) within its scope;
     *       negative offset means it moved up (appears earlier).</li>
     * </ul>
     *
     * @param originalOrderIndices original within-scope encounter indices keyed by source position
     * @param reorderedCompilationUnit the reordered compilation unit to inspect
     * @return list of relocations for all moved elements (within-scope offset ≠ 0), in current encounter order
     */
    @NonNull
    public static List<MemberRelocation> findRelocations(
            /*TODO Create a dedicated type*/ @NonNull Map<SourcePosition, Integer> originalOrderIndices,
            @NonNull CtCompilationUnit reorderedCompilationUnit) {

        List<MemberRelocation> relocations = new ArrayList<>();
        List<CtType<?>> rootTypes = reorderedCompilationUnit.getDeclaredTypes();
        collectScopeRelocations(rootTypes, originalOrderIndices, relocations);
        rootTypes.forEach(type -> collectTypeMemberRelocations(type, originalOrderIndices, relocations));
        return List.copyOf(relocations);
    }

    /**
     * Returns whether any declared element has moved within its scope.
     *
     * @param originalOrderIndices the original within-scope order indices by source position
     * @param reorderedCompilationUnit the reordered compilation unit to inspect
     * @return {@code true} if any element moved within its scope; otherwise {@code false}
     */
    public static boolean isRelocated(
            @NonNull Map<SourcePosition, Integer> originalOrderIndices,
            @NonNull CtCompilationUnit reorderedCompilationUnit) {

        List<CtType<?>> rootTypes = reorderedCompilationUnit.getDeclaredTypes();
        return hasScopeRelocation(rootTypes, originalOrderIndices)
                || rootTypes.stream().anyMatch(type -> hasTypeMemberRelocation(type, originalOrderIndices));
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
        String typeName =
                relocation.getViolatingElement() instanceof CtTypeMember member && member.getDeclaringType() != null
                        ? member.getDeclaringType().getQualifiedName()
                        : "<file root>";
        sb.append(String.format("  [%d] %s:", index, typeName));
        if (relocation.getSortedPredecessor() != null) {
            sb.append(lineSeparator())
                    .append(String.format("        %s", renderDeclarationHeader(relocation.getSortedPredecessor())));
        }
        sb.append(lineSeparator())
                .append(String.format("    --> %s", renderDeclarationHeader(relocation.getViolatingElement())));
        if (relocation.getSortedSuccessor() != null) {
            sb.append(lineSeparator())
                    .append(String.format("        %s", renderDeclarationHeader(relocation.getSortedSuccessor())));
        }
    }

    /**
     * Scans {@code scopeMembers} for elements whose current within-scope index differs from their
     * original within-scope index, and adds a {@link MemberRelocation} for each such element.
     *
     * @param scopeMembers         the ordered members of one scope (file root or a type body)
     * @param originalOrderIndices original within-scope index map keyed by source position
     * @param relocations          accumulator for detected relocations
     */
    private static void collectScopeRelocations(
            List<? extends CtTypeMember> scopeMembers,
            Map<SourcePosition, Integer> originalOrderIndices,
            List<MemberRelocation> relocations) {
        for (int i = 0; i < scopeMembers.size(); i++) {
            CtTypeMember member = scopeMembers.get(i);
            if (!member.getPosition().isValidPosition()) {
                continue;
            }
            Integer originalIdx = originalOrderIndices.get(member.getPosition());
            if (originalIdx == null || i == originalIdx) {
                continue;
            }
            CtTypeMember predecessor = i > 0 ? scopeMembers.get(i - 1) : null;
            CtTypeMember successor = i < scopeMembers.size() - 1 ? scopeMembers.get(i + 1) : null;
            relocations.add(new MemberRelocation(member, predecessor, successor, i - originalIdx));
        }
    }

    /**
     * Checks the direct members of {@code type} for within-scope relocations, then recurses
     * into any nested types.
     *
     * @param type                 the type whose member scope to check
     * @param originalOrderIndices original within-scope index map keyed by source position
     * @param relocations          accumulator for detected relocations
     */
    private static void collectTypeMemberRelocations(
            CtType<?> type, Map<SourcePosition, Integer> originalOrderIndices, List<MemberRelocation> relocations) {
        List<CtTypeMember> members = streamExplicitSrcTypeMembers(type).toList();
        collectScopeRelocations(members, originalOrderIndices, relocations);
        members.stream()
                .filter(m -> m instanceof CtType<?>)
                .map(m -> (CtType<?>) m)
                .forEach(nestedType -> collectTypeMemberRelocations(nestedType, originalOrderIndices, relocations));
    }

    /**
     * Returns {@code true} if any element in {@code scopeMembers} moved to a later within-scope
     * position than its original index.
     *
     * @param scopeMembers         the ordered members of one scope
     * @param originalOrderIndices original within-scope index map keyed by source position
     * @return {@code true} if a later-position relocation was found; otherwise {@code false}
     */
    private static boolean hasScopeRelocation(
            List<? extends CtTypeMember> scopeMembers, Map<SourcePosition, Integer> originalOrderIndices) {
        for (int i = 0; i < scopeMembers.size(); i++) {
            CtTypeMember member = scopeMembers.get(i);
            if (!member.getPosition().isValidPosition()) {
                continue;
            }
            Integer orig = originalOrderIndices.get(member.getPosition());
            if (orig != null && i > orig) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if any direct member of {@code type} or any nested type member moved
     * to a later within-scope position.
     *
     * @param type                 the type whose member scope to check recursively
     * @param originalOrderIndices original within-scope index map keyed by source position
     * @return {@code true} if a relocation was found; otherwise {@code false}
     */
    private static boolean hasTypeMemberRelocation(CtType<?> type, Map<SourcePosition, Integer> originalOrderIndices) {
        List<CtTypeMember> members = streamExplicitSrcTypeMembers(type).toList();
        return hasScopeRelocation(members, originalOrderIndices)
                || members.stream()
                        .filter(m -> m instanceof CtType<?>)
                        .map(m -> (CtType<?>) m)
                        .anyMatch(nestedType -> hasTypeMemberRelocation(nestedType, originalOrderIndices));
    }

    // TODO Create a dedicated type instead of Map
    /**
     * Indexes each element in the compilation unit by its <em>within-scope</em> position.
     *
     * <p>Root types are indexed by their position in the file's declared-type list.
     * Direct members of each type are indexed by their position within that type's member list.
     * Nested types are recursed into.
     *
     * @param compilationUnit the compilation unit to inspect
     * @return map from each element's source position to its within-scope sequential index
     */
    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    static Map<SourcePosition, Integer> indexElementsByOrder(@NonNull CtCompilationUnit compilationUnit) {
        Map<SourcePosition, Integer> result = new HashMap<>();
        List<CtType<?>> rootTypes = compilationUnit.getDeclaredTypes();
        for (int i = 0; i < rootTypes.size(); i++) {
            CtType<?> rootType = rootTypes.get(i);
            if (rootType.getPosition().isValidPosition()) {
                result.put(rootType.getPosition(), i);
            }
            indexTypeMembersInScope(rootType, result);
        }
        return Map.copyOf(result);
    }

    /**
     * Assigns within-scope indices to the direct members of {@code type}, then recurses into
     * any nested types.
     *
     * @param type   the type whose members to index
     * @param result accumulator map
     */
    private static void indexTypeMembersInScope(CtType<?> type, Map<SourcePosition, Integer> result) {
        List<CtTypeMember> members = streamExplicitSrcTypeMembers(type).toList();
        for (int i = 0; i < members.size(); i++) {
            CtTypeMember member = members.get(i);
            if (member.getPosition().isValidPosition()) {
                result.put(member.getPosition(), i);
            }
            if (member instanceof CtType<?> nestedType) {
                indexTypeMembersInScope(nestedType, result);
            }
        }
    }
}

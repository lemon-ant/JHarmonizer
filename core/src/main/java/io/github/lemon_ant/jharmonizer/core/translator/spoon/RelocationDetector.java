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
import java.util.Objects;
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
     * Computes relocations by comparing consecutive-pair relationships in the sorted order
     * against the original source order captured in {@code originalSuccessors}.
     *
     * <p>Semantics:
     * <ul>
     *   <li>For each scope (file root, type body), the method walks consecutive pairs in the
     *       sorted member list and checks whether the original source had the same consecutive
     *       relationship.</li>
     *   <li>A break is reported when the element that follows a given member in the sorted order
     *       differs from the element that followed it in the original source.</li>
     *   <li>This means that moving one contiguous block generates only the two seam breaks at
     *       the insertion and extraction points, regardless of how many members are in the block.</li>
     *   <li>For each break, the element appearing in the unexpected position is reported as
     *       {@link MemberRelocation#getViolatingElement()}, with its sorted predecessor and
     *       successor captured for diagnostic messages.</li>
     * </ul>
     *
     * @param originalSuccessors original within-scope successor positions keyed by source position
     * @param reorderedCompilationUnit the reordered compilation unit to inspect
     * @return list of relocations for all break points, in current encounter order
     */
    @NonNull
    public static List<MemberRelocation> findRelocations(
            @NonNull Map<SourcePosition, SourcePosition> originalSuccessors,
            @NonNull CtCompilationUnit reorderedCompilationUnit) {

        List<MemberRelocation> relocations = new ArrayList<>();
        List<CtType<?>> rootTypes = reorderedCompilationUnit.getDeclaredTypes();
        collectScopeRelocations(new ArrayList<>(rootTypes), originalSuccessors, relocations);
        rootTypes.forEach(type -> collectTypeMemberRelocations(type, originalSuccessors, relocations));
        return List.copyOf(relocations);
    }

    /**
     * Returns whether any declared element has moved within its scope.
     *
     * @param originalSuccessors the original within-scope successor positions by source position
     * @param reorderedCompilationUnit the reordered compilation unit to inspect
     * @return {@code true} if any consecutive-pair relationship changed; otherwise {@code false}
     */
    public static boolean isRelocated(
            @NonNull Map<SourcePosition, SourcePosition> originalSuccessors,
            @NonNull CtCompilationUnit reorderedCompilationUnit) {

        List<CtType<?>> rootTypes = reorderedCompilationUnit.getDeclaredTypes();
        return hasScopeRelocation(new ArrayList<>(rootTypes), originalSuccessors)
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
     * Scans {@code scopeMembers} for consecutive pairs whose relationship in the sorted order
     * differs from the original source order, and adds a {@link MemberRelocation} for each
     * such break point.
     *
     * <p>A break is reported when the element at position {@code i+1} in the sorted list is not
     * the element that originally followed the element at position {@code i}. The element at
     * {@code i+1} is reported as the violating element, with {@code i} as its sorted predecessor
     * and {@code i+2} (if present) as its sorted successor.
     *
     * @param scopeMembers         the ordered members of one scope (file root or a type body)
     * @param originalSuccessors   original within-scope successor position map keyed by source position
     * @param relocations          accumulator for detected relocations
     */
    private static void collectScopeRelocations(
            List<? extends CtTypeMember> scopeMembers,
            Map<SourcePosition, SourcePosition> originalSuccessors,
            List<MemberRelocation> relocations) {
        for (int i = 0; i < scopeMembers.size() - 1; i++) {
            CtTypeMember current = scopeMembers.get(i);
            CtTypeMember actualNext = scopeMembers.get(i + 1);
            if (!current.getPosition().isValidPosition()) {
                continue;
            }
            SourcePosition expectedNextPos = originalSuccessors.get(current.getPosition());
            if (!Objects.equals(expectedNextPos, actualNext.getPosition())) {
                CtTypeMember successor = i + 2 < scopeMembers.size() ? scopeMembers.get(i + 2) : null;
                relocations.add(new MemberRelocation(actualNext, current, successor));
            }
        }
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

    // TODO Create a dedicated type instead of Map
    /**
     * Builds a snapshot of the original consecutive-sibling relationships in the compilation unit.
     *
     * <p>For each scope (file root declared-type list, each type's member list), records the
     * source position of each element's immediate next sibling. The resulting map is used after
     * sorting to detect breaks where the consecutive-pair relationship changed, rather than
     * comparing absolute positions (which over-counts when a single block move shifts many indices).
     *
     * @param compilationUnit the compilation unit to snapshot
     * @return map from each element's source position to its original next sibling's source position;
     *         elements with no next sibling in their scope are absent from the map
     */
    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    static Map<SourcePosition, SourcePosition> snapshotOriginalSuccessors(@NonNull CtCompilationUnit compilationUnit) {
        Map<SourcePosition, SourcePosition> result = new HashMap<>();
        List<CtType<?>> rootTypes = compilationUnit.getDeclaredTypes();
        List<CtTypeMember> rootMembers = new ArrayList<>(rootTypes);
        for (int i = 0; i < rootMembers.size() - 1; i++) {
            CtTypeMember current = rootMembers.get(i);
            CtTypeMember next = rootMembers.get(i + 1);
            if (current.getPosition().isValidPosition() && next.getPosition().isValidPosition()) {
                result.put(current.getPosition(), next.getPosition());
            }
        }
        rootTypes.forEach(type -> recordTypeMemberSuccessors(type, result));
        return Map.copyOf(result);
    }

    /**
     * Records next-sibling relationships for the direct members of {@code type}, then recurses
     * into any nested types.
     *
     * @param type   the type whose members to record
     * @param result accumulator map
     */
    private static void recordTypeMemberSuccessors(CtType<?> type, Map<SourcePosition, SourcePosition> result) {
        List<CtTypeMember> members = streamExplicitSrcTypeMembers(type).toList();
        for (int i = 0; i < members.size() - 1; i++) {
            CtTypeMember current = members.get(i);
            CtTypeMember next = members.get(i + 1);
            if (current.getPosition().isValidPosition() && next.getPosition().isValidPosition()) {
                result.put(current.getPosition(), next.getPosition());
            }
        }
        members.stream()
                .filter(m -> m instanceof CtType<?>)
                .map(m -> (CtType<?>) m)
                .forEach(nestedType -> recordTypeMemberSuccessors(nestedType, result));
    }
}

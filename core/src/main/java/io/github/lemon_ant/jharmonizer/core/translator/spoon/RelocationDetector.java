package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.processing_stat.PathDisplayFormatUtil.abbreviatePathForDisplay;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.streamDeclaredHierarchy;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.DeclarationHeaderRenderer.renderDeclarationHeader;
import static java.lang.System.lineSeparator;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
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
// TODO Combine with ElementsFlatOrderIndexer
public class RelocationDetector {

    private static final int MAX_PATH_DISPLAY_LENGTH = 120;
    private static final int INITIAL_OUTPUT_BUFFER_CAPACITY = 256;

    /**
     * Computes relocations of declared elements by comparing their current encounter order
     * against the previously captured original order indices.
     *
     * <p>Semantics:
     * <ul>
     *   <li>Traverses the current Spoon model using {@code streamDeclaredHierarchy(...)} —
     *       i.e., top-level types, nested types, and all declarative members in encounter order.</li>
     *   <li>For each encountered element, looks up its original sequential index from
     *       {@code originalOrderIndices} and computes
     *       {@code offset = currentIndex - originalIndex}.</li>
     *   <li>Elements not present in the original snapshot (newly added) are ignored.</li>
     *   <li>Only non-zero offsets are reported (unchanged elements are skipped).</li>
     *   <li>For each relocated element, the immediately preceding and following elements in the
     *       sorted order are captured as {@link MemberRelocation#getSortedPredecessor()} and
     *       {@link MemberRelocation#getSortedSuccessor()}, so diagnostic messages can show the user
     *       exactly where the member should appear.</li>
     * </ul>
     *
     * <p>Notes:
     * <ul>
     *   <li>Positive offset means the element moved down (appears later) relative to the original order;
     *       negative offset means it moved up (appears earlier).</li>
     *   <li>If elements were replaced with new instances, ensure the original index snapshot
     *       used the same {@code CtElement} identities, or switch to a stable ID mapping.</li>
     * </ul>
     *
     * @param originalOrderIndices original encounter indices keyed by source position
     * @param reorderedCompilationUnit the reordered compilation unit to inspect
     * @return list of relocations for all moved elements (offset ≠ 0), in current encounter order
     */
    @NonNull
    public static List<MemberRelocation> findRelocations(
            /*TODO Create a dedicated type*/ @NonNull Map<SourcePosition, Integer> originalOrderIndices,
            @NonNull CtCompilationUnit reorderedCompilationUnit) {

        List<CtElement> sortedElements =
                streamDeclaredHierarchy(reorderedCompilationUnit).toList();
        List<MemberRelocation> relocations = new ArrayList<>();
        for (int currentIndex = 0; currentIndex < sortedElements.size(); currentIndex++) {
            CtElement element = sortedElements.get(currentIndex);
            Integer originalIndex = originalOrderIndices.get(element.getPosition());
            if (originalIndex == null) {
                continue;
            }
            int offset = currentIndex - originalIndex;
            if (offset == 0) {
                continue;
            }
            if (!(element instanceof CtTypeMember violatingMember)) {
                continue;
            }
            CtTypeMember predecessor = findScopePredecessor(sortedElements, currentIndex, violatingMember);
            CtTypeMember successor = findScopeSuccessor(sortedElements, currentIndex, violatingMember);
            relocations.add(new MemberRelocation(violatingMember, predecessor, successor, offset));
        }
        return List.copyOf(relocations);
    }

    /**
     * Returns whether is relocated.
     * @param originalOrderIndices the original order indices by source position
     * @param reorderedCompilationUnit the reordered compilation unit to inspect
     * @return {@code true} if is relocated; otherwise {@code false}
     */
    public static boolean isRelocated(
            @NonNull Map<SourcePosition, Integer> originalOrderIndices,
            @NonNull CtCompilationUnit reorderedCompilationUnit) {

        AtomicInteger runningIndex = new AtomicInteger(0);

        return streamDeclaredHierarchy(reorderedCompilationUnit)
                .filter(element -> element.getPosition().isValidPosition())
                // compute offset on the fly using the running encounter index
                .mapToInt(element -> {
                    int current = runningIndex.getAndIncrement();
                    Integer orig = originalOrderIndices.get(element.getPosition());
                    return orig != null ? current - orig : 0;
                })
                .anyMatch(offs -> offs > 0);
    }

    /**
     * Formats the relocations into a human-readable string.
     *
     * <p>The path is placed on its own indented line after the header, abbreviated to
     * at most {@value MAX_PATH_DISPLAY_LENGTH} characters. Each numbered entry shows
     * only the declaring type name (no repeated "ordering violation" label), then the
     * predecessor, violating member (marked with {@code -->}), and successor snippets.
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
        StringBuilder sb = new StringBuilder(INITIAL_OUTPUT_BUFFER_CAPACITY);
        sb.append("Detected member ordering violations in:")
                .append(lineSeparator())
                .append("  ")
                .append(abbreviatePathForDisplay(path, MAX_PATH_DISPLAY_LENGTH));
        for (int i = 0; i < relocationList.size(); i++) {
            sb.append(lineSeparator());
            appendRelocationEntry(sb, relocationList.get(i), i + 1);
        }
        return sb.toString();
    }

    private static void appendRelocationEntry(StringBuilder sb, MemberRelocation relocation, int index) {
        String typeName =
                relocation.getViolatingElement() instanceof CtTypeMember member && member.getDeclaringType() != null
                        ? member.getDeclaringType().getQualifiedName()
                        : "<unknown>";
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
     * Scans backward from {@code fromIndex} in {@code elements} and returns the nearest
     * {@link CtTypeMember} that belongs to the same declaring scope as {@code member}.
     *
     * @param elements  the flat sorted element list
     * @param fromIndex the index to start scanning from (exclusive)
     * @param member    the member whose declaring scope defines the target scope
     * @return the nearest same-scope predecessor, or {@code null} if none exists
     */
    @Nullable
    private static CtTypeMember findScopePredecessor(List<CtElement> elements, int fromIndex, CtTypeMember member) {
        CtType<?> scope = member.getDeclaringType();
        for (int i = fromIndex - 1; i >= 0; i--) {
            CtElement candidate = elements.get(i);
            if (candidate instanceof CtTypeMember m && Objects.equals(m.getDeclaringType(), scope)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Scans forward from {@code fromIndex} in {@code elements} and returns the nearest
     * {@link CtTypeMember} that belongs to the same declaring scope as {@code member}.
     *
     * @param elements  the flat sorted element list
     * @param fromIndex the index to start scanning from (exclusive)
     * @param member    the member whose declaring scope defines the target scope
     * @return the nearest same-scope successor, or {@code null} if none exists
     */
    @Nullable
    private static CtTypeMember findScopeSuccessor(List<CtElement> elements, int fromIndex, CtTypeMember member) {
        CtType<?> scope = member.getDeclaringType();
        for (int i = fromIndex + 1; i < elements.size(); i++) {
            CtElement candidate = elements.get(i);
            if (candidate instanceof CtTypeMember m && Objects.equals(m.getDeclaringType(), scope)) {
                return m;
            }
        }
        return null;
    }

    // TODO Create a dedicated type instead of Map
    /**
     * Indexes the elements by order.
     * @param compilationUnit the compilation unit to inspect
     * @return the index of elements by order
     */
    @NonNull
    static Map<SourcePosition, Integer> indexElementsByOrder(@NonNull CtCompilationUnit compilationUnit) {
        AtomicInteger runningIndex = new AtomicInteger(0);
        return streamDeclaredHierarchy(compilationUnit)
                .map(CtElement::getPosition)
                .filter(SourcePosition::isValidPosition)
                .collect(Collectors.toMap(
                        Function.identity(), // key: the source position
                        position -> runningIndex.getAndIncrement() // value: the element's sequential index
                        ));
    }
}

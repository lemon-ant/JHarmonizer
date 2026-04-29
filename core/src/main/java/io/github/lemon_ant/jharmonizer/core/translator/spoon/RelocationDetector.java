package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.streamDeclaredHierarchy;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
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
            CtElement predecessor = currentIndex > 0 ? sortedElements.get(currentIndex - 1) : null;
            CtElement successor =
                    currentIndex < sortedElements.size() - 1 ? sortedElements.get(currentIndex + 1) : null;
            relocations.add(new MemberRelocation(element, predecessor, successor, offset));
        }
        return Collections.unmodifiableList(relocations);
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
                    return Optional.ofNullable(originalOrderIndices.get(element.getPosition()))
                            .map(orig -> current - orig)
                            .orElse(0);
                })
                .anyMatch(offs -> offs > 0);
    }

    /**
     * Formats the relocations into a human-readable string.
     * For each violation the message shows where the member should be placed by naming
     * its immediate predecessor and successor in the correct sorted order.
     *
     * @param path        the path of the file where the relocations were detected
     * @param relocations the collection of relocations to format
     * @return a formatted string representing the relocations
     */
    @NonNull
    public static String printRelocations(@NonNull Path path, @NonNull Collection<MemberRelocation> relocations) {
        return String.format(
                "Detected member ordering violations in '%s':%n%s",
                path.getFileName(),
                relocations.stream()
                        .map(relocation -> {
                            String memberName = computeParentSimpleName(relocation.getViolatingElement());
                            String placement = computePlacementDescription(
                                    relocation.getSortedPredecessor(), relocation.getSortedSuccessor());
                            return String.format("  - %s: %s", memberName, placement);
                        })
                        .collect(joining(lineSeparator())));
    }

    @NonNull
    private static String computePlacementDescription(@Nullable CtElement predecessor, @Nullable CtElement successor) {
        if (predecessor != null && successor != null) {
            return String.format(
                    "should be between [%s] and [%s]",
                    computeParentSimpleName(predecessor), computeParentSimpleName(successor));
        }
        if (predecessor != null) {
            return String.format("should be after [%s] (as last member)", computeParentSimpleName(predecessor));
        }
        if (successor != null) {
            return String.format("should be before [%s] (as first member)", computeParentSimpleName(successor));
        }
        return "should be the only member";
    }

    /**
     * Recursively constructs the deep parent simple name of a type element.
     * This method traverses up the hierarchy of declaring types to build a fully qualified name.
     *
     * @param element the type element whose deep parent simple name is to be constructed
     * @return the deep parent simple name of the type element
     */
    @NonNull
    private static String computeParentSimpleName(CtElement element) {
        if (!(element instanceof CtTypeMember member)) {
            return "<nameless>";
        }
        String memberSignature;
        if (member instanceof CtConstructor<?> constructor) {
            memberSignature = constructor.getSignature();
        } else if (member instanceof CtMethod<?> method) {
            memberSignature = method.getSignature();
        } else {
            memberSignature = isBlank(member.getSimpleName()) ? "<initializer>" : member.getSimpleName();
        }
        if (member.getDeclaringType() == null) {
            return memberSignature;
        }
        return member.getDeclaringType().getQualifiedName() + "." + memberSignature;
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

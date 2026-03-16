package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.streamDeclaredHierarchy;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Utility class to detect relocations of type members in the sorted Spoon AST model.
 * This class is used to identify type members that have been moved from their original positions
 * during the sorting process.
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
     *       {@code spoonAstModel.getOriginalElements2OrderIndices()} and computes
     *       {@code offset = currentIndex - originalIndex}.</li>
     *   <li>Elements not present in the original snapshot (newly added) are ignored.</li>
     *   <li>Only non-zero offsets are reported (unchanged elements are skipped).</li>
     *   <li>Runs in a single pass; no intermediate "current index" map is created.</li>
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
     * @param spoonAstModel model holding the working {@code CtCompilationUnit} and the original indices snapshot
     * @return list of pairs {@code (element, offset)} for all moved elements (offset ≠ 0), in current encounter order
     */
    public static List<Pair<CtElement, Integer>> findRelocations(
            /*TODO Create a dedicated type*/ Map<SourcePosition, Integer> originalOrderIndices,
            CtCompilationUnit reorderedCompilationUnit) {

        AtomicInteger runningIndex = new AtomicInteger(0);

        return streamDeclaredHierarchy(reorderedCompilationUnit)
                // compute offset on the fly using the running encounter index
                .map(element -> {
                    int current = runningIndex.getAndIncrement();
                    return Optional.ofNullable(originalOrderIndices.get(element.getPosition()))
                            .map(orig -> current - orig)
                            .filter(offset -> offset != 0)
                            .map(offset -> Pair.of(element, offset));
                })
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Returns whether is relocated.
     * @param originalOrderIndices the original order indices by source position
     * @param reorderedCompilationUnit the reordered compilation unit to inspect
     * @return {@code true} if is relocated; otherwise {@code false}
     */
    public static boolean isRelocated(
            Map<SourcePosition, Integer> originalOrderIndices, CtCompilationUnit reorderedCompilationUnit) {

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
     *
     * @param path        the path of the file where the relocations were detected
     * @param relocations the collection of relocations to format
     * @return a formatted string representing the relocations
     */
    public static String printRelocations(Path path, Collection<Pair<CtElement, Integer>> relocations) {
        return String.format(
                "Scanning finished with a sorting failure on file: %s%n%s",
                path.getFileName(),
                relocations.stream()
                        .map(relocation -> {
                            // TODO: Implement more human friendly output for relocations printout
                            CtElement member = relocation.getLeft();
                            return String.format(
                                    "%s expected to relocate %s by %d",
                                    computeParentSimpleName(member),
                                    relocation.getRight() < 0 ? "UP" : "DOWN",
                                    relocation.getRight());
                        })
                        .collect(joining(lineSeparator())));
    }

    /**
     * Recursively constructs the deep parent simple name of a type element.
     * This method traverses up the hierarchy of declaring types to build a fully qualified name.
     *
     * @param element the type element whose deep parent simple name is to be constructed
     * @return the deep parent simple name of the type element
     */
    private static String computeParentSimpleName(CtElement element) {
        if (element instanceof CtTypeMember member) {
            var nonBlankName = isBlank(member.getSimpleName()) ? "<initializer>" : member.getSimpleName();
            if (member instanceof CtConstructor<?> constructor) {
                return constructor.getSignature();
            }
            if (member instanceof CtMethod<?> method) {
                nonBlankName = method.getSignature();
            }
            if (member.getDeclaringType() == null) {
                return nonBlankName;
            }
            return member.getDeclaringType().getQualifiedName() + "$" + nonBlankName;
        }
        return "<nameless>";
    }

    // TODO Create a dedicated type instead of Map
    /**
     * Indexes the elements by order.
     * @param compilationUnit the compilation unit to inspect
     * @return the index of elements by order
     */
    static Map<SourcePosition, Integer> indexElementsByOrder(CtCompilationUnit compilationUnit) {
        AtomicInteger runningIndex = new AtomicInteger(0);
        return streamDeclaredHierarchy(compilationUnit)
                .map(CtElement::getPosition)
                .filter(SourcePosition::isValidPosition)
                .collect(Collectors.toMap(
                        Function.identity(), // key: the element itself
                        e -> runningIndex.getAndIncrement() // value: the element's sequential index
                        ));
    }
}

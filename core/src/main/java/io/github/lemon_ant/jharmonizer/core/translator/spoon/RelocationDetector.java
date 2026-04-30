package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils.streamDeclaredHierarchy;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotationType;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtEnumValue;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.ModifierKind;

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
     *
     * <p>For each violation, a compact snippet is rendered showing the immediate predecessor
     * and successor in the correct sorted order together with the violating member itself,
     * so the developer can see at a glance how those declarations should appear one after another.
     * The violating member is highlighted with a {@code ►} marker.
     *
     * <p>Example output for a class where {@code void b()} should come after {@code void a()}:
     * <pre>
     * Detected member ordering violations in 'Sample.java':
     *   [1] Ordering violation in com.example.Sample:
     *         public void a() { ...
     *       ► public void b() { ...
     * </pre>
     *
     * @param path        the path of the file where the relocations were detected
     * @param relocations the collection of relocations to format
     * @return a formatted string representing the relocations
     */
    @NonNull
    public static String printRelocations(@NonNull Path path, @NonNull Collection<MemberRelocation> relocations) {
        List<MemberRelocation> relocationList = List.copyOf(relocations);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Detected member ordering violations in '%s':", path.getFileName()));
        for (int i = 0; i < relocationList.size(); i++) {
            sb.append(lineSeparator());
            appendRelocationEntry(sb, relocationList.get(i), i + 1);
        }
        return sb.toString();
    }

    private static void appendRelocationEntry(StringBuilder sb, MemberRelocation relocation, int index) {
        sb.append(String.format(
                "  [%d] Ordering violation in %s:", index, resolveDeclaringTypeName(relocation.getViolatingElement())));
        if (relocation.getSortedPredecessor() != null) {
            sb.append(lineSeparator());
            sb.append(String.format("        %s", renderDeclarationHeader(relocation.getSortedPredecessor())));
        }
        sb.append(lineSeparator());
        sb.append(String.format("      ► %s", renderDeclarationHeader(relocation.getViolatingElement())));
        if (relocation.getSortedSuccessor() != null) {
            sb.append(lineSeparator());
            sb.append(String.format("        %s", renderDeclarationHeader(relocation.getSortedSuccessor())));
        }
    }

    /**
     * Renders a compact declaration header for a type member, omitting bodies and initializers.
     *
     * <p>Format by member kind:
     * <ul>
     *   <li>Field: {@code [modifiers] [type] [name]}</li>
     *   <li>Method: {@code [modifiers] [returnType] [name]() { ...} or {@code [modifiers] [returnType] [name](...) { ...}</li>
     *   <li>Constructor: {@code [modifiers] [name]() { ...} or {@code [modifiers] [name](...) { ...}</li>
     *   <li>Nested type: {@code [modifiers] class|interface|enum|@interface [name] { ...}</li>
     *   <li>Enum value: {@code [name]}</li>
     *   <li>Initializer block: {@code { ...} or {@code static { ...}</li>
     * </ul>
     *
     * @param element the element to render
     * @return a compact single-line declaration header
     */
    @NonNull
    private static String renderDeclarationHeader(CtElement element) {
        if (!(element instanceof CtTypeMember member)) {
            return "<nameless>";
        }
        // Initializer blocks — getSimpleName() is blank and they are not a CtType
        if (isBlank(member.getSimpleName()) && !(member instanceof CtType<?>)) {
            return member.getModifiers().contains(ModifierKind.STATIC) ? "static { ..." : "{ ...";
        }
        // Enum constants — no modifiers, no type prefix
        if (member instanceof CtEnumValue<?> enumValue) {
            return enumValue.getSimpleName();
        }
        String modifiers = renderModifiers(member);
        if (member instanceof CtField<?> field) {
            return joinNonBlank(modifiers, field.getType().getSimpleName(), field.getSimpleName());
        }
        if (member instanceof CtMethod<?> method) {
            String params = method.getParameters().isEmpty() ? "()" : "(...)";
            return joinNonBlank(modifiers, method.getType().getSimpleName(), method.getSimpleName() + params)
                    + " { ...";
        }
        if (member instanceof CtConstructor<?> constructor) {
            String params = constructor.getParameters().isEmpty() ? "()" : "(...)";
            return joinNonBlank(modifiers, constructor.getSimpleName() + params) + " { ...";
        }
        if (member instanceof CtType<?> nestedType) {
            return joinNonBlank(modifiers, resolveTypeKeyword(nestedType), nestedType.getSimpleName()) + " { ...";
        }
        return isBlank(member.getSimpleName()) ? "<initializer>" : member.getSimpleName();
    }

    @NonNull
    private static String renderModifiers(CtTypeMember member) {
        return member.getModifiers().stream()
                .sorted(Comparator.comparingInt(ModifierKind::ordinal))
                .map(ModifierKind::toString)
                .collect(joining(" "));
    }

    @NonNull
    private static String joinNonBlank(String... parts) {
        return Stream.of(parts).filter(s -> !isBlank(s)).collect(joining(" "));
    }

    @NonNull
    private static String resolveDeclaringTypeName(CtElement element) {
        if (element instanceof CtTypeMember member && member.getDeclaringType() != null) {
            return member.getDeclaringType().getQualifiedName();
        }
        return "<unknown>";
    }

    @NonNull
    private static String resolveTypeKeyword(CtType<?> type) {
        if (type instanceof CtAnnotationType<?>) {
            return "@interface";
        }
        if (type instanceof CtEnum<?>) {
            return "enum";
        }
        if (type instanceof CtInterface<?>) {
            return "interface";
        }
        return "class";
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

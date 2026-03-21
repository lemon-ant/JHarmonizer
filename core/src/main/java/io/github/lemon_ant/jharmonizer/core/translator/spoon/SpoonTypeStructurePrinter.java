package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.GROUP_HEADER_METADATA;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.findIndentationStart;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.needsSeparatorAfter;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.needsSeparatorBefore;

import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import lombok.NonNull;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.TokenWriter;

/**
 * Prints structured type declarations while preserving original source fragments and skipped-type ranges.
 */
final class SpoonTypeStructurePrinter {
    @NonNull
    private final String originalSrcCode;

    @NonNull
    private final Set<CtType<?>> sortingSkippedTypes;

    @NonNull
    private final Map<CtType<?>, SrcCharacterRange> sortingSkippedTypeRanges;

    @NonNull
    private final Supplier<String> printedResultSupplier;

    @NonNull
    private final Supplier<TokenWriter> tokenWriterSupplier;

    /**
     * Creates a type-structure printer bound to the current source-printer state.
     *
     * @param srcCode the original source text
     * @param sortingSkippedTypes the types that must stay unsorted in the output
     * @param sortingSkippedTypeRanges the output ranges collected for skipped types
     * @param printedResultSupplier supplies the current rendered output
     * @param tokenWriterSupplier supplies the active Spoon token writer
     */
    SpoonTypeStructurePrinter(
            @NonNull String srcCode,
            @NonNull Set<CtType<?>> sortingSkippedTypes,
            @NonNull Map<CtType<?>, SrcCharacterRange> sortingSkippedTypeRanges,
            @NonNull Supplier<String> printedResultSupplier,
            @NonNull Supplier<TokenWriter> tokenWriterSupplier) {
        this.originalSrcCode = srcCode;
        this.sortingSkippedTypes = sortingSkippedTypes;
        this.sortingSkippedTypeRanges = sortingSkippedTypeRanges;
        this.printedResultSupplier = printedResultSupplier;
        this.tokenWriterSupplier = tokenWriterSupplier;
    }

    /**
     * Prints a type declaration using preserved source fragments and group-separator metadata.
     *
     * @param type the type declaration to print
     */
    void printTypeStructure(@NonNull CtType<?> type) {
        getTokenWriter().writeln();
        if (sortingSkippedTypes.contains(type)) {
            printPreservedSkippedType(type);
            return;
        }
        SourcePosition typePosition = type.getPosition();
        List<CtTypeMember> explicitTypeMembers = findExplicitTypeMembers(type);

        if (explicitTypeMembers.isEmpty()) {
            // If no nested elements, then print the original source fragment entirely
            // TODO Check if we have comments before and after
            printOriginalFragment(typePosition.getSourceStart(), typePosition.getSourceEnd())
                    .writeln();
            return;
        }
        printStructuredTypeMembers(typePosition, explicitTypeMembers);
    }

    /**
     * Prints an original source fragment while preserving indentation from the start of its line and collapsing
     * every trailing run of spaces, tabs, and line separators to a single line separator in the output.
     *
     * @param start the first significant source index of the fragment
     * @param end the inclusive last source index of the fragment
     * @return the active token writer after the fragment is written
     */
    @NonNull
    TokenWriter printOriginalFragment(int start, int end) {
        int startWithIndent = findIndentationStart(start, originalSrcCode);
        try {
            String originalCodeFragment =
                    originalSrcCode.substring(startWithIndent, end + 1).stripTrailing();
            return getTokenWriter().writeCodeSnippet(originalCodeFragment).writeln();
        } catch (IndexOutOfBoundsException exception) {
            throw new IllegalStateException(
                    "Invalid source fragment range: start=" + start
                            + ", end=" + end
                            + ", indentationStart=" + startWithIndent
                            + ", sourceLength=" + originalSrcCode.length(),
                    exception);
        }
    }

    private void printPreservedSkippedType(CtType<?> type) {
        int outputStart = printedResultSupplier.get().length();
        printOriginalFragment(
                type.getPosition().getSourceStart(), type.getPosition().getSourceEnd());
        int outputEndExclusive = printedResultSupplier.get().length();
        sortingSkippedTypeRanges.put(type, new SrcCharacterRange(outputStart, outputEndExclusive));
    }

    @NonNull
    private static List<CtTypeMember> findExplicitTypeMembers(CtType<?> type) {
        return type.getTypeMembers().stream()
                // Spoon creates implicit constructors which don't exist in the source code
                .filter(typeMember -> typeMember.getPosition().isValidPosition())
                /* TODO(RECORDS_DISABLED): Remove this guard when record headers/components are printed correctly.
                Today implicit record fields/components still produce wrong source-printer output. */
                .filter(typeMember -> !typeMember.isImplicit())
                .toList();
    }

    private void printStructuredTypeMembers(SourcePosition typePosition, List<CtTypeMember> explicitTypeMembers) {
        int minMemberStart = explicitTypeMembers.stream()
                .mapToInt(typeMember -> typeMember.getPosition().getSourceStart())
                .min()
                .orElseThrow(IllegalStateException::new);

        printOriginalFragment(typePosition.getSourceStart(), minMemberStart - 1);

        boolean first = true;
        boolean previousElementNeedSeparatorAfter = false;
        for (CtTypeMember member : explicitTypeMembers) {
            previousElementNeedSeparatorAfter =
                    printStructuredTypeMember(member, explicitTypeMembers, first, previousElementNeedSeparatorAfter);
            first = false;
        }

        int maxMemberEnd = explicitTypeMembers.stream()
                .mapToInt(typeMember -> typeMember.getPosition().getSourceEnd())
                .max()
                .orElseThrow(IllegalStateException::new);
        printOriginalFragment(maxMemberEnd + 1, typePosition.getSourceEnd());
    }

    private boolean printStructuredTypeMember(
            CtTypeMember member,
            List<CtTypeMember> explicitTypeMembers,
            boolean first,
            boolean previousElementNeedSeparatorAfter) {
        // TODO Orphaned comments
        boolean needsSeparatorBeforeCurrentMember = needsSeparatorBefore(member, first);
        if (needsSeparatorBeforeCurrentMember || previousElementNeedSeparatorAfter) {
            getTokenWriter().writeln();
        }
        boolean currentElementNeedsSeparatorAfter = needsSeparatorAfter(member);

        if (member instanceof CtType<?> typeMember) {
            printTypeStructure(typeMember);
            return currentElementNeedsSeparatorAfter;
        }

        Optional<String> groupHeaderMetadata =
                Optional.ofNullable(member.getMetadata(GROUP_HEADER_METADATA)).map(Object::toString);
        groupHeaderMetadata.ifPresent(groupHeader -> {
            if (!groupHeader.isEmpty()) {
                getTokenWriter().writeCodeSnippet("// " + groupHeader).writeln();
            } else {
                getTokenWriter().writeln();
            }
        });
        int nextElementStart = explicitTypeMembers.stream()
                .mapToInt(typeMember -> typeMember.getPosition().getSourceStart())
                .filter(start -> start > member.getPosition().getSourceEnd())
                .min()
                .orElse(member.getPosition().getSourceEnd() + 1);
        printOriginalFragment(member.getPosition().getSourceStart(), nextElementStart - 1);
        return currentElementNeedsSeparatorAfter;
    }

    @NonNull
    private TokenWriter getTokenWriter() {
        return tokenWriterSupplier.get();
    }
}

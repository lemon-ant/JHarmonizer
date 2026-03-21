package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.GROUP_HEADER_METADATA;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.needsSeparatorAfter;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSourcePrinterUtils.needsSeparatorBefore;

import io.github.lemon_ant.jharmonizer.core.source.SrcCodeUtils;
import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.TokenWriter;

/**
 * Prints structured type declarations while preserving original source fragments and skipped-type ranges.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class SpoonTypePrinter {
    @NonNull
    private final String originalSrcCode;

    @NonNull
    private final Set<CtType<?>> sortingSkippedTypes;

    @NonNull
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private final Map<CtType<?>, SrcCharacterRange> sortingSkippedTypeRanges = new HashMap<>();

    @NonNull
    private final TokenWriter tokenWriter;

    /**
     * Prints a type declaration using preserved source fragments and group-separator metadata.
     *
     * @param type the type declaration to print
     */
    void printType(@NonNull CtType<?> type) {
        tokenWriter.writeln();
        if (sortingSkippedTypes.contains(type)) {
            printSkippedType(type);
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
        printTypeMembers(typePosition, explicitTypeMembers);
    }

    /**
     * Prints an original source fragment while preserving indentation from the start of its line and collapsing
     * every trailing run of spaces, tabs, and line separators to a single line separator in the output.
     *
     * @param start the first significant source index of the fragment
     * @param end   the inclusive last source index of the fragment
     * @return the active token writer after the fragment is written
     */
    @NonNull
    TokenWriter printOriginalFragment(int start, int end) {
        int startWithIndent = SrcCodeUtils.findIndentationStart(start, originalSrcCode);
        try {
            String originalCodeFragment =
                    originalSrcCode.substring(startWithIndent, end + 1).stripTrailing();
            return tokenWriter.writeCodeSnippet(originalCodeFragment).writeln();
        } catch (IndexOutOfBoundsException exception) {
            throw new IllegalStateException(
                    "Invalid source fragment range: start=" + start
                            + ", end=" + end
                            + ", indentationStart=" + startWithIndent
                            + ", sourceLength=" + originalSrcCode.length(),
                    exception);
        }
    }

    private void printSkippedType(CtType<?> type) {
        int outputStart = tokenWriter.toString().length();
        printOriginalFragment(
                type.getPosition().getSourceStart(), type.getPosition().getSourceEnd());
        int outputEndExclusive = tokenWriter.toString().length();
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

    private void printTypeMembers(SourcePosition typePosition, List<CtTypeMember> explicitTypeMembers) {
        int minMemberStart = explicitTypeMembers.stream()
                .mapToInt(typeMember -> typeMember.getPosition().getSourceStart())
                .min()
                .orElseThrow(IllegalStateException::new /*TODO Message*/);

        printOriginalFragment(typePosition.getSourceStart(), minMemberStart - 1);

        boolean first = true;
        boolean previousElementNeedSeparatorAfter = false;
        for (CtTypeMember member : explicitTypeMembers) {
            previousElementNeedSeparatorAfter =
                    printTypeMember(member, explicitTypeMembers, first, previousElementNeedSeparatorAfter);
            first = false;
        }

        int maxMemberEnd = explicitTypeMembers.stream()
                .mapToInt(typeMember -> typeMember.getPosition().getSourceEnd())
                .max()
                .orElseThrow(IllegalStateException::new /*TODO Message*/);
        printOriginalFragment(maxMemberEnd + 1, typePosition.getSourceEnd());
    }

    private boolean printTypeMember(
            CtTypeMember member,
            List<CtTypeMember> explicitTypeMembers,
            boolean first,
            boolean previousElementNeedSeparatorAfter) {
        // TODO Check Orphaned comments
        boolean needsSeparatorBeforeCurrentMember = needsSeparatorBefore(member, first);
        if (needsSeparatorBeforeCurrentMember || previousElementNeedSeparatorAfter) {
            tokenWriter.writeln();
        }
        boolean currentElementNeedsSeparatorAfter = needsSeparatorAfter(member);

        Optional<String> groupHeaderMetadata =
                Optional.ofNullable(member.getMetadata(GROUP_HEADER_METADATA)).map(Object::toString);
        groupHeaderMetadata.ifPresent(groupHeader -> {
            if (!groupHeader.isEmpty()) {
                tokenWriter.writeCodeSnippet("// " + groupHeader).writeln();
            } else {
                tokenWriter.writeln();
            }
        });

        if (member instanceof CtType<?> typeMember) {
            printType(typeMember);
            return currentElementNeedsSeparatorAfter;
        }

        int nextElementStart = explicitTypeMembers.stream()
                .mapToInt(typeMember -> typeMember.getPosition().getSourceStart())
                .filter(start -> start > member.getPosition().getSourceEnd())
                .min()
                .orElse(member.getPosition().getSourceEnd() + 1);
        printOriginalFragment(member.getPosition().getSourceStart(), nextElementStart - 1);
        return currentElementNeedsSeparatorAfter;
    }

    @NonNull
    Map<CtType<?>, SrcCharacterRange> getSortingSkippedTypeRanges() {
        return Collections.unmodifiableMap(sortingSkippedTypeRanges);
    }
}

package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.GROUP_HEADER_METADATA;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.GROUP_SEPARATOR_NEW_LINE;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.needsSeparatorAfter;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.needsSeparatorBefore;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
import io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtEnum;
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

    @Nullable
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private Map<CtType<?>, SrcCharacterRange> sortingSkippedTypeRanges = new HashMap<>();

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
        int minMemberStart = explicitTypeMembers.stream()
                .mapToInt(typeMember -> typeMember.getPosition().getSourceStart())
                .min()
                .orElseThrow(() ->
                        new IllegalStateException("Failed to compute first member start from explicit type members"));
        printOriginalFragment(typePosition.getSourceStart(), minMemberStart - 1);
        if (type instanceof CtEnum<?>) {
            tokenWriter.writeln();
        }
        printTypeMembers(explicitTypeMembers);
        int maxMemberEnd = explicitTypeMembers.stream()
                .mapToInt(typeMember -> typeMember.getPosition().getSourceEnd())
                .max()
                .orElseThrow(() ->
                        new IllegalStateException("Failed to compute last member end from explicit type members"));
        printOriginalFragment(maxMemberEnd + 1, typePosition.getSourceEnd());
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
        requireSortingSkippedTypeRanges().put(type, new SrcCharacterRange(outputStart, outputEndExclusive));
    }

    @NonNull
    private static List<CtTypeMember> findExplicitTypeMembers(CtType<?> type) {
        return type.getTypeMembers().stream()
                // Spoon quirk: getTypeMembers() of a type may also include members whose declaring type
                // is a nested/anonymous class (e.g., the anonymous classes of enum constants). We only want
                // members that are declared directly in `type`, otherwise those nested members would be
                // printed as if they were top-level members of `type`.
                .filter(typeMember -> Objects.equals(typeMember.getDeclaringType(), type))
                .filter(typeMember -> Objects.equals(typeMember.getParent(), type))
                // Spoon creates implicit constructors which don't exist in the source code
                .filter(typeMember -> typeMember.getPosition().isValidPosition())
                /* TODO(RECORDS_DISABLED): Remove this guard when record headers/components are printed correctly.
                Today implicit record fields/components still produce wrong source-printer output. */
                .filter(typeMember -> !typeMember.isImplicit())
                .toList();
    }

    private void printTypeMembers(List<CtTypeMember> explicitTypeMembers) {
        boolean first = true;
        boolean previousElementNeedSeparatorAfter = false;
        for (CtTypeMember member : explicitTypeMembers) {
            previousElementNeedSeparatorAfter =
                    printTypeMember(member, explicitTypeMembers, first, previousElementNeedSeparatorAfter);
            first = false;
        }
    }

    private boolean printTypeMember(
            CtTypeMember member,
            List<CtTypeMember> explicitTypeMembers,
            boolean first,
            boolean previousElementNeedSeparatorAfter) {
        // TODO Check Orphaned comments

        boolean needsSeparatorBeforeCurrentMember = needsSeparatorBefore(member, first);
        boolean hasSeparatorAlreadyPrinted = needsSeparatorBeforeCurrentMember || previousElementNeedSeparatorAfter;
        if (hasSeparatorAlreadyPrinted) {
            tokenWriter.writeln();
        }
        boolean currentElementNeedsSeparatorAfter = needsSeparatorAfter(member);

        String groupHeader = findGroupHeader(member);
        if (GROUP_SEPARATOR_NEW_LINE.equals(groupHeader)) {
            if (!hasSeparatorAlreadyPrinted && !first) {
                tokenWriter.writeln();
            }
        } else if (groupHeader != null && !hasMatchingLeadingComment(member, groupHeader)) {
            tokenWriter.writeCodeSnippet("// " + groupHeader).writeln();
        }

        if (member instanceof CtType<?> typeMember) {
            printType(typeMember);
            return currentElementNeedsSeparatorAfter;
        }

        int nextElementStart = explicitTypeMembers.stream()
                .mapToInt(typeMember -> typeMember.getPosition().getSourceStart())
                .filter(start -> start > member.getPosition().getSourceStart())
                .min()
                .orElse(member.getPosition().getSourceEnd() + 1);
        printOriginalFragment(member.getPosition().getSourceStart(), nextElementStart - 1);
        return currentElementNeedsSeparatorAfter;
    }

    private static boolean hasMatchingLeadingComment(CtTypeMember member, String groupHeader) {
        return member.getComments().stream()
                .filter(comment -> comment.getPosition().getEndLine()
                        < member.getPosition().getLine())
                .map(comment -> comment.getContent().trim())
                .anyMatch(groupHeader::equals);
    }

    @Nullable
    private static String findGroupHeader(CtTypeMember member) {
        Object groupHeaderMetadata = member.getMetadata(GROUP_HEADER_METADATA);
        if (groupHeaderMetadata == null) {
            return null;
        }
        return groupHeaderMetadata.toString();
    }

    @SuppressWarnings("PMD.NullAssignment")
    @NonNull
    Map<CtType<?>, SrcCharacterRange> getSortingSkippedTypeRanges() {
        Map<CtType<?>, SrcCharacterRange> activeSortingSkippedTypeRanges = requireSortingSkippedTypeRanges();
        // After handing the ranges off, the printer must not be reused for further skipped-range collection.
        sortingSkippedTypeRanges = null;
        return Collections.unmodifiableMap(activeSortingSkippedTypeRanges);
    }

    @NonNull
    private Map<CtType<?>, SrcCharacterRange> requireSortingSkippedTypeRanges() {
        if (sortingSkippedTypeRanges == null) {
            throw new IllegalStateException("Sorting-skipped type ranges have already been finalized");
        }
        return sortingSkippedTypeRanges;
    }
}

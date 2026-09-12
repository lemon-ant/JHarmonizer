// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.GROUP_SEPARATOR_NEW_LINE;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.compileNeedsBlankLineAfterTypeHeader;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.compileNeedsSeparatorAfter;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.compileNeedsSeparatorBefore;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeMemberUtils.findEffectiveMemberEnd;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeMemberUtils.findExplicitTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeMemberUtils.findGroupHeader;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeMemberUtils.hasLeadingCommentOnSeparateLine;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeMemberUtils.hasMatchingLeadingComment;

import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
import io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Prints structured type declarations while preserving original source fragments and skipped-type ranges.
 * Blank-line insertion predicates are compiled once from the supplied {@link PrinterConfig}
 * so that no per-member flag checks are needed during printing.
 * Exception: the blank-line-before-comment feature stores {@link PrinterConfig#isBlankLineBeforeComment()}
 * separately and applies it per-member with a per-type set of member source lines, in order to
 * distinguish genuine leading comments from Spoon's misattributed trailing inline comments.
 */
final class SpoonTypeStructurePrinter {
    private final boolean blankLineBeforeComment;

    @NonNull
    private final Predicate<CtType<?>> needsBlankLineAfterTypeHeader;

    @NonNull
    private final Predicate<CtTypeMember> needsSeparatorAfter;

    @NonNull
    private final BiPredicate<CtTypeMember, Boolean> needsSeparatorBefore;

    @NonNull
    private final String originalSrcCode;

    @NonNull
    private final SpoonPrinterHelper printerHelper;

    @NonNull
    private final Set<CtType<?>> sortingSkippedTypes;

    @Nullable
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private Map<CtType<?>, SrcCharacterRange> sortingSkippedTypeRanges = new HashMap<>();

    /**
     * Creates a new SpoonTypeStructurePrinter with compiled printer predicates.
     *
     * @param originalSrcCode the original source text
     * @param sortingSkippedTypes the types that must be copied without sorting
     * @param printerHelper the shared output buffer
     * @param printerConfig the printer configuration used to compile blank-line predicates
     */
    SpoonTypeStructurePrinter(
            @NonNull String originalSrcCode,
            @NonNull Set<CtType<?>> sortingSkippedTypes,
            @NonNull SpoonPrinterHelper printerHelper,
            @NonNull PrinterConfig printerConfig) {
        this.originalSrcCode = originalSrcCode;
        this.sortingSkippedTypes = sortingSkippedTypes;
        this.printerHelper = printerHelper;
        this.needsSeparatorAfter = compileNeedsSeparatorAfter(printerConfig);
        this.needsSeparatorBefore = compileNeedsSeparatorBefore();
        this.needsBlankLineAfterTypeHeader = compileNeedsBlankLineAfterTypeHeader(printerConfig);
        this.blankLineBeforeComment = printerConfig.isBlankLineBeforeComment();
    }

    /**
     * Hands off the collected source ranges, preventing subsequent collection by this printer.
     *
     * @return the immutable ranges of types copied without sorting
     */
    @SuppressWarnings("PMD.NullAssignment")
    @NonNull
    Map<CtType<?>, SrcCharacterRange> getSortingSkippedTypeRanges() {
        Map<CtType<?>, SrcCharacterRange> activeSortingSkippedTypeRanges = requireSortingSkippedTypeRanges();
        // After handing the ranges off, the printer must not be reused for further skipped-range collection.
        sortingSkippedTypeRanges = null;
        return Collections.unmodifiableMap(activeSortingSkippedTypeRanges);
    }

    /**
     * Prints a source fragment's content and indentation, followed by one line terminator.
     * Whitespace between source fragments belongs to the enclosing declaration's layout, not to either fragment.
     *
     * @param start the first source index of the fragment
     * @param end   the inclusive last source index of the fragment
     * @return whether the range contained content to print
     */
    boolean printOriginalFragment(int start, int end) {
        try {
            // Skip the inter-fragment gap while preserving the first content line's indentation.
            int startWithIndent = SrcCodeUtils.findFragmentStartWithIndentation(start, end, originalSrcCode);
            // Empty gaps contribute neither text nor a line terminator.
            if (startWithIndent > end) {
                return false;
            }
            // Trailing spacing belongs to the enclosing declaration, not to this fragment.
            int contentEndExclusive = SrcCodeUtils.findFragmentEndExclusive(start, end, originalSrcCode);
            // Preserve the fragment's interior and terminate its last line exactly once.
            printerHelper
                    .write(originalSrcCode.substring(startWithIndent, contentEndExclusive))
                    .writeln();
            return true;
        } catch (IndexOutOfBoundsException exception) {
            throw new IllegalStateException(
                    "Invalid source fragment range: start=" + start
                            + ", end=" + end
                            + ", sourceLength=" + originalSrcCode.length(),
                    exception);
        }
    }

    /**
     * Prints a type declaration ending in one line terminator, without surrounding blank lines.
     *
     * @param type the type declaration to print
     */
    void printType(@NonNull CtType<?> type) {
        if (sortingSkippedTypes.contains(type)) {
            printSkippedType(type);
            return;
        }
        SourcePosition typePosition = type.getPosition();
        List<CtTypeMember> explicitTypeMembers = findExplicitTypeMembers(type);
        if (explicitTypeMembers.isEmpty()) {
            // If no nested elements, then print the original source fragment entirely
            // TODO Check if we have comments before and after
            printOriginalFragment(typePosition.getSourceStart(), typePosition.getSourceEnd());
            return;
        }
        Map<CtTypeMember, Integer> correctedEnumMemberStarts = type instanceof CtEnum<?>
                ? EnumMemberStartCorrectionResolver.resolveCorrectedStarts(originalSrcCode, explicitTypeMembers)
                : Collections.emptyMap();
        NavigableSet<Integer> memberStarts = Collections.unmodifiableNavigableSet(explicitTypeMembers.stream()
                .map(typeMember -> correctedEnumMemberStarts.getOrDefault(
                        typeMember, typeMember.getPosition().getSourceStart()))
                .collect(Collectors.toCollection(TreeSet::new)));
        printOriginalFragment(typePosition.getSourceStart(), memberStarts.first() - 1);
        printTypeMembers(
                explicitTypeMembers,
                correctedEnumMemberStarts,
                memberStarts,
                typePosition.getLine(),
                needsBlankLineAfterTypeHeader.test(type));
        int maxMemberEnd = explicitTypeMembers.stream()
                .mapToInt(SpoonTypeMemberUtils::findEffectiveMemberEnd)
                .max()
                .orElseThrow(() ->
                        new IllegalStateException("Failed to compute last member end from explicit type members"));
        printOriginalFragment(maxMemberEnd + 1, typePosition.getSourceEnd());
    }

    private void printMemberSeparator(
            CtTypeMember member,
            int memberStart,
            boolean first,
            boolean needsSeparatorAfterPrevious,
            Set<Integer> memberDeclarationEndLines,
            int typeDeclarationStartLine) {
        // TODO Check Orphaned comments

        boolean needsSeparatorBeforeCurrentMember = needsSeparatorBefore.test(member, first)
                || (blankLineBeforeComment
                        && hasLeadingCommentOnSeparateLine(
                                member, memberDeclarationEndLines, typeDeclarationStartLine));
        String groupHeader = findGroupHeader(member);
        boolean hasGroupSeparator = GROUP_SEPARATOR_NEW_LINE.equals(groupHeader);
        boolean hasGroupHeader = groupHeader != null && !hasGroupSeparator;
        if (needsSeparatorBeforeCurrentMember
                || needsSeparatorAfterPrevious
                || hasGroupHeader
                || (hasGroupSeparator && !first)) {
            printerHelper.writeln();
        }
        if (hasGroupHeader && !hasMatchingLeadingComment(member, groupHeader)) {
            // Raw fragments carry their own indentation, so Spoon's tab depth stays zero. Generated
            // group comments must use the following member's source indentation as well.
            int indentationStart = SrcCodeUtils.findIndentationStart(memberStart, originalSrcCode);
            printerHelper
                    .write(originalSrcCode.substring(indentationStart, memberStart))
                    .write("// ")
                    .write(groupHeader)
                    .writeln();
        }
    }

    private void printSkippedType(CtType<?> type) {
        int outputStart = printerHelper.getPrintedLength();
        printOriginalFragment(
                type.getPosition().getSourceStart(), type.getPosition().getSourceEnd());
        int outputEndExclusive = printerHelper.getPrintedLength();
        requireSortingSkippedTypeRanges().put(type, new SrcCharacterRange(outputStart, outputEndExclusive));
    }

    private void printTypeMember(CtTypeMember member, NavigableSet<Integer> memberStarts, int memberStart) {
        if (member instanceof CtType<?> typeMember) {
            printType(typeMember);
        } else {
            // Source order differs from print order. Index it once instead of rescanning all siblings per member.
            Integer nextElementStart = memberStarts.higher(member.getPosition().getSourceEnd());
            int fragmentEnd = nextElementStart == null ? findEffectiveMemberEnd(member) : nextElementStart - 1;
            printOriginalFragment(memberStart, fragmentEnd);
        }
    }

    private void printTypeMembers(
            List<CtTypeMember> explicitTypeMembers,
            Map<CtTypeMember, Integer> correctedEnumMemberStarts,
            NavigableSet<Integer> memberStarts,
            int typeDeclarationStartLine,
            boolean needsHeaderSeparator) {
        // Collect the last source line of each member declaration. Trailing inline comments (e.g. // comment)
        // are always on the last line of their member, so filtering by end line correctly identifies
        // misattributed trailing comments even when the declaration spans multiple lines.
        Set<Integer> memberDeclarationEndLines = blankLineBeforeComment
                ? explicitTypeMembers.stream()
                        .map(member -> member.getPosition().getEndLine())
                        .collect(Collectors.toUnmodifiableSet())
                : Collections.emptySet();
        boolean first = true;
        // The header and the first member request the same boundary, so combine their decisions.
        boolean needsSeparatorAfterPrevious = needsHeaderSeparator;
        for (CtTypeMember member : explicitTypeMembers) {
            int memberStart = correctedEnumMemberStarts.getOrDefault(
                    member, member.getPosition().getSourceStart());
            printMemberSeparator(
                    member,
                    memberStart,
                    first,
                    needsSeparatorAfterPrevious,
                    memberDeclarationEndLines,
                    typeDeclarationStartLine);
            printTypeMember(member, memberStarts, memberStart);
            needsSeparatorAfterPrevious = needsSeparatorAfter.test(member);
            first = false;
        }
    }

    @NonNull
    private Map<CtType<?>, SrcCharacterRange> requireSortingSkippedTypeRanges() {
        if (sortingSkippedTypeRanges == null) {
            throw new IllegalStateException("Sorting-skipped type ranges have already been finalized");
        }
        return sortingSkippedTypeRanges;
    }
}

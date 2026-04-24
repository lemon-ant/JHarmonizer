package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.GROUP_SEPARATOR_NEW_LINE;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.compileNeedsBlankLineAfterTypeHeader;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.compileNeedsSeparatorAfter;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.compileNeedsSeparatorBefore;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.findEffectiveMemberEnd;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.findExplicitTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.findGroupHeader;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.hasLeadingCommentOnSeparateLine;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.hasMatchingLeadingComment;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
import io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.NonNull;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.visitor.TokenWriter;

/**
 * Prints structured type declarations while preserving original source fragments and skipped-type ranges.
 * Blank-line insertion predicates are compiled once from the supplied {@link PrinterConfig}
 * so that no per-member flag checks are needed during printing.
 * Exception: the blank-line-before-comment feature stores {@link PrinterConfig#isBlankLineBeforeComment()}
 * separately and applies it per-member with a per-type set of member source lines, in order to
 * distinguish genuine leading comments from Spoon's misattributed trailing inline comments.
 */
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

    @NonNull
    private final Predicate<CtTypeMember> needsSeparatorAfter;

    @NonNull
    private final BiPredicate<CtTypeMember, Boolean> needsSeparatorBefore;

    @NonNull
    private final Predicate<CtType<?>> needsBlankLineAfterTypeHeader;

    private final boolean blankLineBeforeComment;

    /**
     * Creates a new SpoonTypePrinter with compiled printer predicates.
     *
     * @param originalSrcCode the original source text
     * @param sortingSkippedTypes the types that must be copied without sorting
     * @param tokenWriter the token writer for output
     * @param printerConfig the printer configuration used to compile blank-line predicates
     */
    SpoonTypePrinter(
            @NonNull String originalSrcCode,
            @NonNull Set<CtType<?>> sortingSkippedTypes,
            @NonNull TokenWriter tokenWriter,
            @NonNull PrinterConfig printerConfig) {
        this.originalSrcCode = originalSrcCode;
        this.sortingSkippedTypes = sortingSkippedTypes;
        this.tokenWriter = tokenWriter;
        this.needsSeparatorAfter = compileNeedsSeparatorAfter(printerConfig);
        this.needsSeparatorBefore = compileNeedsSeparatorBefore();
        this.needsBlankLineAfterTypeHeader = compileNeedsBlankLineAfterTypeHeader(printerConfig);
        this.blankLineBeforeComment = printerConfig.isBlankLineBeforeComment();
    }

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
        Map<CtTypeMember, Integer> correctedEnumMemberStarts = type instanceof CtEnum<?>
                ? EnumMemberStartCorrectionResolver.resolveCorrectedStarts(originalSrcCode, explicitTypeMembers)
                : Collections.emptyMap();
        int minMemberStart = explicitTypeMembers.stream()
                .mapToInt(typeMember -> correctedEnumMemberStarts.getOrDefault(
                        typeMember, typeMember.getPosition().getSourceStart()))
                .min()
                .orElseThrow(() ->
                        new IllegalStateException("Failed to compute first member start from explicit type members"));
        printOriginalFragment(typePosition.getSourceStart(), minMemberStart - 1);
        if (needsBlankLineAfterTypeHeader.test(type)) {
            tokenWriter.writeln();
        }
        printTypeMembers(explicitTypeMembers, correctedEnumMemberStarts);
        int maxMemberEnd = explicitTypeMembers.stream()
                .mapToInt(typeMember -> findEffectiveMemberEnd(typeMember))
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

    private void printTypeMembers(
            List<CtTypeMember> explicitTypeMembers, Map<CtTypeMember, Integer> correctedEnumMemberStarts) {
        // Collect the last source line of each member declaration. Trailing inline comments (e.g. // comment)
        // are always on the last line of their member, so filtering by end line correctly identifies
        // misattributed trailing comments even when the declaration spans multiple lines.
        Set<Integer> memberDeclarationEndLines = explicitTypeMembers.stream()
                .filter(member -> member.getPosition().isValidPosition())
                .map(member -> member.getPosition().getEndLine())
                .collect(Collectors.toUnmodifiableSet());
        boolean first = true;
        boolean previousElementNeedSeparatorAfter = false;
        for (CtTypeMember member : explicitTypeMembers) {
            previousElementNeedSeparatorAfter = printTypeMember(
                    member,
                    explicitTypeMembers,
                    correctedEnumMemberStarts,
                    first,
                    previousElementNeedSeparatorAfter,
                    memberDeclarationEndLines);
            first = false;
        }
    }

    private boolean printTypeMember(
            CtTypeMember member,
            List<CtTypeMember> explicitTypeMembers,
            Map<CtTypeMember, Integer> correctedEnumMemberStarts,
            boolean first,
            boolean previousElementNeedSeparatorAfter,
            Set<Integer> memberDeclarationEndLines) {
        // TODO Check Orphaned comments

        boolean needsSeparatorBeforeCurrentMember = needsSeparatorBefore.test(member, first)
                || (blankLineBeforeComment && hasLeadingCommentOnSeparateLine(member, memberDeclarationEndLines));
        boolean hasSeparatorAlreadyPrinted = needsSeparatorBeforeCurrentMember || previousElementNeedSeparatorAfter;
        if (hasSeparatorAlreadyPrinted) {
            tokenWriter.writeln();
        }
        boolean currentElementNeedsSeparatorAfter = needsSeparatorAfter.test(member);

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
                .mapToInt(typeMember -> correctedEnumMemberStarts.getOrDefault(
                        typeMember, typeMember.getPosition().getSourceStart()))
                .filter(start -> start > member.getPosition().getSourceEnd())
                .min()
                .orElse(findEffectiveMemberEnd(member) + 1);
        printOriginalFragment(
                correctedEnumMemberStarts.getOrDefault(
                        member, member.getPosition().getSourceStart()),
                nextElementStart - 1);
        return currentElementNeedsSeparatorAfter;
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

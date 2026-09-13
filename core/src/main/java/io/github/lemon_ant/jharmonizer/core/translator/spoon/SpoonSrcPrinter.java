// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator.HEADER;
import static io.github.lemon_ant.jharmonizer.core.config.unified.UnifiedSeparator.NEW_LINE;
import static io.github.lemon_ant.jharmonizer.core.spoon.SpoonGroupSeparatorUtils.resolveSeparator;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeMemberUtils.findEffectiveMemberEnd;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeMemberUtils.findExplicitTypeMembers;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeMemberUtils.hasLeadingCommentOnSeparateLine;
import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonTypeMemberUtils.hasMatchingLeadingComment;

import io.github.lemon_ant.jharmonizer.core.spoon.SpoonGroupSeparatorUtils.GroupSeparator;
import io.github.lemon_ant.jharmonizer.core.spoon.SpoonTypeUtils;
import io.github.lemon_ant.jharmonizer.core.translator.SerializedSrcWithSkippedTypeRanges;
import io.github.lemon_ant.jharmonizer.core.translator.SrcCharacterRange;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtEnum;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Stateless source-fragment printer with explicit configuration for each call.
 * Each invocation owns its output buffer and skipped-type ranges.
 */
@UtilityClass
final class SpoonSrcPrinter {

    /**
     * Prints the current declaration order with independent state for each invocation.
     * Calls for independent models may run concurrently with different configurations.
     * @param compilationUnit compilation unit with positions referring to {@code srcCode}
     * @param srcCode original source
     * @param sortingSkippedTypes types to copy without restructuring
     * @param printerConfig immutable spacing configuration
     * @return printed source and immutable skipped-type ranges, independent of subsequent calls
     */
    @NonNull
    static SerializedSrcWithSkippedTypeRanges serializeCompilationUnit(
            @NonNull CtCompilationUnit compilationUnit,
            @NonNull String srcCode,
            @NonNull Set<CtType<?>> sortingSkippedTypes,
            @NonNull PrinterConfig printerConfig) {
        return new Serialization(srcCode, sortingSkippedTypes, printerConfig).serializeCompilationUnit(compilationUnit);
    }

    /** Confines mutable printing state to one invocation. */
    private static final class Serialization {

        @NonNull
        private final PrinterConfig printerConfig;

        @NonNull
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        private final Map<CtType<?>, SrcCharacterRange> sortingSkippedTypeRanges = new HashMap<>();

        @NonNull
        private final Set<CtType<?>> sortingSkippedTypes;

        @NonNull
        private final SrcPrinterOutput srcPrinterOutput;

        private static boolean needsSeparatorBefore(CtTypeMember member, boolean first) {
            return member instanceof CtType<?>
                    || (!first && !(member instanceof CtField<?>))
                    || !member.getAnnotations().isEmpty();
        }

        private Serialization(String srcCode, Set<CtType<?>> sortingSkippedTypes, PrinterConfig printerConfig) {
            srcPrinterOutput = new SrcPrinterOutput(srcCode);
            this.sortingSkippedTypes = sortingSkippedTypes;
            this.printerConfig = printerConfig;
        }

        private boolean needsSeparatorAfter(CtTypeMember member) {
            return printerConfig.isBlankLineBetweenFields()
                    || !(member instanceof CtField<?>)
                    || !member.getAnnotations().isEmpty();
        }

        private void printMemberSeparator(
                CtTypeMember member,
                boolean first,
                boolean separatorAfterPrevious,
                Set<Integer> memberEndLines,
                int typeStartLine) {
            GroupSeparator groupSeparator = resolveSeparator(member);
            boolean leadingComment = printerConfig.isBlankLineBeforeComment()
                    && hasLeadingCommentOnSeparateLine(member, memberEndLines, typeStartLine);
            if (separatorAfterPrevious
                    || needsSeparatorBefore(member, first)
                    || leadingComment
                    || groupSeparator.getType() == HEADER
                    || (groupSeparator.getType() == NEW_LINE && !first)) {
                srcPrinterOutput.writeln();
            }
            String headerText = groupSeparator.getHeaderText();
            if (headerText != null && !hasMatchingLeadingComment(member, headerText)) {
                srcPrinterOutput.printGroupHeader(member.getPosition().getSourceStart(), headerText);
            }
        }

        private void printType(CtType<?> type) {
            SourcePosition position = type.getPosition();
            if (sortingSkippedTypes.contains(type)) {
                int start = srcPrinterOutput.getPrintedLength();
                srcPrinterOutput.printFragment(position.getSourceStart(), position.getSourceEnd());
                sortingSkippedTypeRanges.put(type, new SrcCharacterRange(start, srcPrinterOutput.getPrintedLength()));
                return;
            }
            List<CtTypeMember> members = findExplicitTypeMembers(type);
            if (members.isEmpty()) {
                srcPrinterOutput.printFragment(position.getSourceStart(), position.getSourceEnd());
                return;
            }
            int[] memberStarts = members.stream()
                    .mapToInt(member -> member.getPosition().getSourceStart())
                    .sorted()
                    .toArray();
            srcPrinterOutput.printFragment(position.getSourceStart(), memberStarts[0] - 1);
            printTypeMembers(
                    members,
                    memberStarts,
                    position.getLine(),
                    printerConfig.isBlankLineAfterTypeHeader() || type instanceof CtEnum<?>);
            int lastMemberEnd = members.stream()
                    .mapToInt(SpoonTypeMemberUtils::findEffectiveMemberEnd)
                    .max()
                    .orElseThrow(IllegalStateException::new);
            srcPrinterOutput.printFragment(lastMemberEnd + 1, position.getSourceEnd());
        }

        private void printTypeMember(CtTypeMember member, int... memberStarts) {
            if (member instanceof CtType<?> nestedType) {
                printType(nestedType);
                return;
            }
            // Locate the next original declaration even after sorting; equal starts can occur in multi-field
            // declarations.
            int boundary =
                    Arrays.binarySearch(memberStarts, member.getPosition().getSourceEnd() + 1);
            int nextIndex = boundary < 0 ? -boundary - 1 : boundary;
            int end = nextIndex < memberStarts.length ? memberStarts[nextIndex] - 1 : findEffectiveMemberEnd(member);
            srcPrinterOutput.printFragment(member.getPosition().getSourceStart(), end);
        }

        private void printTypeMembers(
                List<CtTypeMember> members, int[] memberStarts, int typeStartLine, boolean headerSeparator) {
            Set<Integer> memberEndLines = printerConfig.isBlankLineBeforeComment()
                    ? members.stream()
                            .map(member -> member.getPosition().getEndLine())
                            .collect(Collectors.toUnmodifiableSet())
                    : Set.of();
            boolean first = true;
            boolean separatorAfterPrevious = headerSeparator;
            for (CtTypeMember member : members) {
                printMemberSeparator(member, first, separatorAfterPrevious, memberEndLines, typeStartLine);
                printTypeMember(member, memberStarts);
                separatorAfterPrevious = needsSeparatorAfter(member);
                first = false;
            }
        }

        @NonNull
        private SerializedSrcWithSkippedTypeRanges serializeCompilationUnit(CtCompilationUnit compilationUnit) {
            List<CtType<?>> rootTypes = SpoonTypeUtils.getRootTypes(compilationUnit);
            int firstTypeStart = rootTypes.stream()
                    .mapToInt(type -> type.getPosition().getSourceStart())
                    .min()
                    .orElseThrow(IllegalStateException::new);
            if (srcPrinterOutput.printFragment(0, firstTypeStart - 1)) {
                srcPrinterOutput.writeln();
            }
            int lastTypeEnd = -1;
            boolean first = true;
            for (CtType<?> rootType : rootTypes) {
                if (!first) {
                    srcPrinterOutput.writeln();
                }
                printType(rootType);
                lastTypeEnd = Math.max(lastTypeEnd, rootType.getPosition().getSourceEnd());
                first = false;
            }
            // Spoon's type range can include trailing comments; copying after its end prevents duplicates.
            srcPrinterOutput.printTail(lastTypeEnd + 1);
            // This invocation owns the map exclusively; the result wraps it once.
            return new SerializedSrcWithSkippedTypeRanges(srcPrinterOutput.toString(), sortingSkippedTypeRanges);
        }
    }
}

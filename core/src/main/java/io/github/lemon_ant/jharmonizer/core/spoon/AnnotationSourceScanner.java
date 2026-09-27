// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.countLineSeparators;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.With;
import lombok.experimental.UtilityClass;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.core.util.PublicScanner;
import org.jspecify.annotations.Nullable;

/** Finds adjacent annotation fragments using the Java lexer already supplied by Spoon. */
@UtilityClass
// TODO Annotations: Can we reuse spoon model maximally and avoid using eclipse parser?
public class AnnotationSourceScanner {

    /** Two line separators leave the blank line that separates comment blocks. */
    private static final int BLANK_LINE_SEPARATOR_COUNT = 2;

    /**
     * Groups consecutive annotations, with one {@link AnnotationSrcFragment} per annotation, including singleton groups.
     * Whitespace and comments do not end a group; other Java tokens do. Groups follow source adjacency, not AST ownership.
     * Arguments are tokenized for boundaries and sorting keys without evaluating values. Annotation-like text in
     * comments or literals, and annotations nested inside arguments, do not create separate fragments.
     * @param srcCode original Java source
     * @return immutable groups in source order
     */
    @NonNull
    public static List<AnnotationSrcGroup> scan(@NonNull String srcCode) {
        if (srcCode.indexOf('@') < 0 && srcCode.indexOf('\\') < 0) {
            return List.of();
        }
        try {
            return new Parsing(srcCode).parseGroups();
        } catch (InvalidInputException exception) {
            throw new IllegalArgumentException("Cannot scan annotation source fragments", exception);
        }
    }

    private static int findIndentationEnd(String srcCode, int lineStart, int contentStart) {
        int indentationEnd = lineStart;
        while (indentationEnd < contentStart
                && (srcCode.charAt(indentationEnd) == ' ' || srcCode.charAt(indentationEnd) == '\t')) {
            indentationEnd++;
        }
        return indentationEnd;
    }

    private static int findLineSeparatorStart(String srcCode, int lineStart) {
        int separatorStart = lineStart - 1;
        if (srcCode.charAt(separatorStart) == '\n'
                && separatorStart > 0
                && srcCode.charAt(separatorStart - 1) == '\r') {
            separatorStart--;
        }
        return separatorStart;
    }

    @NonNull
    private static AnnotationSrcGap prepareGap(
            String srcCode, AnnotationSrcFragment annotation, int start, int endExclusive) {
        String gap = srcCode.substring(start, endExclusive);
        int followingContentStart = start;
        while (followingContentStart < endExclusive && Character.isWhitespace(srcCode.charAt(followingContentStart))) {
            followingContentStart++;
        }
        int lineStart = Math.max(
                        srcCode.lastIndexOf('\r', followingContentStart - 1),
                        srcCode.lastIndexOf('\n', followingContentStart - 1))
                + 1;
        String indentedRemainder =
                srcCode.substring(lineStart, findIndentationEnd(srcCode, lineStart, followingContentStart))
                        + srcCode.substring(followingContentStart, endExclusive);
        if (annotation.getTrailingLineSeparatorCount() == BLANK_LINE_SEPARATOR_COUNT && gap.isBlank()) {
            // These blank lines belong to the departing lower comment block. A plain annotation leaves one
            // line break; a commented annotation supplies its own separators before this slot's indentation.
            return new AnnotationSrcGap(
                    annotation.getStart(),
                    indentedRemainder,
                    gap,
                    0,
                    srcCode.substring(findLineSeparatorStart(srcCode, lineStart), lineStart) + indentedRemainder);
        }
        return new AnnotationSrcGap(
                annotation.getStart(),
                indentedRemainder,
                gap,
                countLineSeparators(start, followingContentStart, srcCode),
                gap);
    }

    /**
     * Prepared source text and sorting keys for one annotation, including its attached comments.
     * Separators required by trailing comments remain separate so existing destination line breaks can be reused.
     */
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class AnnotationSrcFragment {

        /** Sorting keys computed from the annotation's name, argument tokens, and internal comments. */
        @NonNull
        AnnotationDescriptor descriptor;

        /** Exact annotation text with attached leading and trailing comments, without final line separators. */
        @NonNull
        String srcCode;

        /** Offset of the opening {@code @}, used for original ordering, relocation detection, and opt-out ranges. */
        int start;

        /** Minimum trailing separators: zero normally, one after {@code //}, two after an attached lower block. */
        int trailingLineSeparatorCount;

        /** Original separators needed when a destination cannot preserve the trailing comment's attachment. */
        @NonNull
        String trailingLineSeparators;
    }

    /** Prepared gap text that stays in its source slot while adjacent annotations move. */
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class AnnotationSrcGap {

        /** Original preceding annotation's offset, used to preserve this gap verbatim when that annotation stays. */
        int annotationStart;

        /** Indentation and independent comments to append after separators supplied by a moved annotation. */
        @NonNull
        String indentedRemainder;

        /** Unmodified gap for an annotation that remains in its original slot. */
        @NonNull
        String originalSrcCode;

        /** Available leading separators; zero when the preceding attached comment block takes them with it. */
        int retainedLineSeparatorCount;

        /** Fixed gap text, with an attached lower comment block's blank lines reduced to one line break. */
        @NonNull
        String srcCode;
    }

    /** Original group boundaries and fixed gaps, with annotation fragments in their requested print order. */
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class AnnotationSrcGroup {

        /** First source offset after the final gap, before the following Java token. */
        int endExclusive;

        /** Sorting replaces this immutable list while preserving the original group boundaries and gaps. */
        @NonNull
        @With
        List<AnnotationSrcFragment> fragmentsInPrintOrder;

        /** One fixed gap after each original annotation slot, including the gap after the last annotation. */
        @NonNull
        List<AnnotationSrcGap> gapsInSrcOrder;

        /** First attached leading comment, or the first annotation's opening {@code @}. */
        int start;
    }

    private static final class Parsing {

        /** JDT lexer providing Java tokens and their original source offsets. */
        @NonNull
        private final IScanner scanner;

        /** Exact input text used to inspect whitespace gaps and preserve comment boundaries. */
        @NonNull
        private final String srcCode;

        /** Attached leading-comment candidate found while reading the current token. */
        private int leadingCommentStart;

        /** Cumulative comment length; differences count only comments inside an annotation declaration. */
        private int scannedCommentLength;

        /** Current non-comment token returned by lexer lookahead. */
        private int token;

        /** End of comments attached to the preceding token, copied into its annotation fragment. */
        private int trailingCommentEndExclusive;

        /** Separator requirement determined while collecting the preceding token's trailing comments. */
        private int trailingLineSeparatorCount;

        /** Input boundary of the separator that may need to move with those comments. */
        private int trailingLineSeparatorEndExclusive;

        private Parsing(String srcCode) throws InvalidInputException {
            this.srcCode = srcCode;
            // ToolFactory loads Eclipse platform classes that Spoon deliberately excludes. PublicScanner provides
            // the same IScanner contract without that platform; revisit this constructor when updating Spoon's JDT.
            scanner = new PublicScanner(
                    true,
                    false,
                    false,
                    // Source and compliance levels match SpoonParser; Java 21 also accepts older compatible syntax.
                    ClassFileConstants.JDK21,
                    ClassFileConstants.JDK21,
                    null,
                    null,
                    true,
                    false,
                    false);
            scanner.setSource(srcCode.toCharArray());
            token = readNextToken();
            // Comments before the first Java token belong to the file/declaration preamble.
            leadingCommentStart = scanner.getCurrentTokenStartPosition();
        }

        private void captureLeadingComment(int commentToken, int previousCommentEndExclusive) {
            if (commentToken == ITerminalSymbols.TokenNameCOMMENT_JAVADOC) {
                // Leading JavaDoc documents the declaration, rather than the first annotation in its list.
                leadingCommentStart = -1;
            } else if (scanner.getCurrentTokenStartPosition() >= trailingCommentEndExclusive
                    && (leadingCommentStart < 0
                            || countLineSeparators(
                                            previousCommentEndExclusive,
                                            scanner.getCurrentTokenStartPosition(),
                                            srcCode)
                                    >= BLANK_LINE_SEPARATOR_COUNT)) {
                // Spoon can attach these comments to the field; their source adjacency determines ownership.
                leadingCommentStart = scanner.getCurrentTokenStartPosition();
            }
        }

        private boolean captureTrailingBlock(boolean adjacent, int previousCommentEndExclusive) {
            if (adjacent
                    && countLineSeparators(previousCommentEndExclusive, scanner.getCurrentTokenStartPosition(), srcCode)
                            >= BLANK_LINE_SEPARATOR_COUNT) {
                if (previousCommentEndExclusive > trailingCommentEndExclusive) {
                    // A block adjacent above and separated below belongs to the preceding annotation.
                    trailingCommentEndExclusive = previousCommentEndExclusive;
                    trailingLineSeparatorCount = BLANK_LINE_SEPARATOR_COUNT;
                    trailingLineSeparatorEndExclusive = Math.max(
                                    srcCode.lastIndexOf('\r', scanner.getCurrentTokenStartPosition() - 1),
                                    srcCode.lastIndexOf('\n', scanner.getCurrentTokenStartPosition() - 1))
                            + 1;
                }
                return false;
            }
            return adjacent;
        }

        private boolean captureTrailingComment(int commentToken) {
            for (int offset = trailingCommentEndExclusive; offset < scanner.getCurrentTokenStartPosition(); offset++) {
                if (srcCode.charAt(offset) == '\r' || srcCode.charAt(offset) == '\n') {
                    return false;
                }
            }
            trailingCommentEndExclusive = scanner.getCurrentTokenEndPosition() + 1;
            trailingLineSeparatorEndExclusive = trailingCommentEndExclusive;
            if (commentToken != ITerminalSymbols.TokenNameCOMMENT_LINE) {
                return true;
            }
            // Keep the terminator separate: it is needed only when the destination gap has no line break.
            trailingCommentEndExclusive = findCommentEndExclusive();
            trailingLineSeparatorCount = 1;
            return false;
        }

        private int computeCommentLength(int commentToken) {
            char[] commentSource = scanner.getCurrentTokenSource();
            int commentLength = commentSource.length;
            // Line terminators separate Java tokens and must not change annotation ordering across line endings.
            if (commentToken == ITerminalSymbols.TokenNameCOMMENT_LINE) {
                while (commentLength > 0
                        && (commentSource[commentLength - 1] == '\r' || commentSource[commentLength - 1] == '\n')) {
                    commentLength--;
                }
            }
            return commentLength;
        }

        private int findCommentEndExclusive() {
            int endExclusive = scanner.getCurrentTokenEndPosition() + 1;
            while (endExclusive > scanner.getCurrentTokenStartPosition()
                    && (srcCode.charAt(endExclusive - 1) == '\r' || srcCode.charAt(endExclusive - 1) == '\n')) {
                endExclusive--;
            }
            return endExclusive;
        }

        @Nullable
        // The parsed source fixes the context: identifiers after @ and dots are annotation-name segments.
        @SuppressWarnings("deprecation")
        private AnnotationSrcFragment parseAnnotation() throws InvalidInputException {
            int start = scanner.getCurrentTokenStartPosition();
            int commentStart = leadingCommentStart;
            int declarationCommentStart = scannedCommentLength;
            token = readNextToken();
            if (token != ITerminalSymbols.TokenNameIdentifier) {
                return null; // @interface starts an annotation type declaration.
            }
            String name = new String(scanner.getCurrentTokenSource());
            int declarationLength = 1 + name.length();
            int declarationCommentEnd = scannedCommentLength;
            token = readNextToken();
            while (token == ITerminalSymbols.TokenNameDOT) {
                if (readNextToken() != ITerminalSymbols.TokenNameIdentifier) {
                    throw new IllegalArgumentException("Invalid annotation name at source offset " + start);
                }
                name = String.valueOf(scanner.getCurrentTokenSource());
                declarationLength += 1 + name.length();
                declarationCommentEnd = scannedCommentLength;
                token = readNextToken();
            }
            String arguments = null;
            if (token == ITerminalSymbols.TokenNameLPAREN) {
                String argumentList = parseArguments(start);
                declarationLength += argumentList.length();
                arguments = argumentList.substring(1, argumentList.length() - 1);
                declarationCommentEnd = scannedCommentLength;
                token = readNextToken();
            }
            // Lookahead can cross a comment outside the declaration; only count comments inside its source bounds.
            declarationLength += declarationCommentEnd - declarationCommentStart;
            return new AnnotationSrcFragment(
                    new AnnotationDescriptor(arguments, declarationLength, name),
                    srcCode.substring(commentStart, trailingCommentEndExclusive),
                    start,
                    trailingLineSeparatorCount,
                    srcCode.substring(trailingCommentEndExclusive, trailingLineSeparatorEndExclusive));
        }

        @NonNull
        private String parseArguments(int start) throws InvalidInputException {
            int depth = 0;
            StringBuilder arguments = new StringBuilder();
            while (true) {
                if (token == ITerminalSymbols.TokenNameEOF) {
                    throw new IllegalArgumentException("Unclosed annotation arguments at source offset " + start);
                }
                arguments.append(readTokenText());
                if (token == ITerminalSymbols.TokenNameLPAREN) {
                    depth++;
                } else if (token == ITerminalSymbols.TokenNameRPAREN) {
                    depth--;
                }
                if (depth == 0) {
                    return arguments.toString();
                }
                token = readNextToken();
            }
        }

        @NonNull
        private List<AnnotationSrcGroup> parseGroups() throws InvalidInputException {
            List<AnnotationSrcGroup> groups = new ArrayList<>();
            List<AnnotationSrcFragment> group = new ArrayList<>();
            List<AnnotationSrcGap> gaps = new ArrayList<>();
            int groupStart = 0;
            int groupEndExclusive = 0;
            int gapStart = 0;
            while (token != ITerminalSymbols.TokenNameEOF) {
                if (group.isEmpty()) {
                    groupStart = leadingCommentStart;
                }
                int fragmentStart = leadingCommentStart;
                AnnotationSrcFragment annotation = token == ITerminalSymbols.TokenNameAT ? parseAnnotation() : null;
                if (annotation != null) {
                    if (!group.isEmpty()) {
                        gaps.add(prepareGap(srcCode, group.get(group.size() - 1), gapStart, fragmentStart));
                    }
                    group.add(annotation);
                    gapStart = trailingCommentEndExclusive;
                    groupEndExclusive = scanner.getCurrentTokenStartPosition();
                    continue;
                }
                if (!group.isEmpty()) {
                    // @interface is a declaration, not another fragment. Its leading comments stay in the final gap.
                    gaps.add(prepareGap(srcCode, group.get(group.size() - 1), gapStart, groupEndExclusive));
                    // The lexer reuses this buffer; a read-only view would be emptied by clear().
                    groups.add(new AnnotationSrcGroup(
                            groupEndExclusive, List.copyOf(group), List.copyOf(gaps), groupStart));
                    group.clear();
                    gaps.clear();
                }
                token = readNextToken();
            }
            if (!group.isEmpty()) {
                gaps.add(prepareGap(srcCode, group.get(group.size() - 1), gapStart, groupEndExclusive));
                groups.add(
                        new AnnotationSrcGroup(groupEndExclusive, List.copyOf(group), List.copyOf(gaps), groupStart));
            }
            return Collections.unmodifiableList(groups);
        }

        private int readNextToken() throws InvalidInputException {
            trailingCommentEndExclusive = scanner.getCurrentTokenEndPosition() + 1;
            trailingLineSeparatorCount = 0;
            trailingLineSeparatorEndExclusive = trailingCommentEndExclusive;
            leadingCommentStart = -1;
            int previousCommentEndExclusive = trailingCommentEndExclusive;
            boolean adjacentCommentBlock = true;
            boolean captureComments = true;
            int nextToken = scanner.getNextToken();
            while (nextToken == ITerminalSymbols.TokenNameCOMMENT_BLOCK
                    || nextToken == ITerminalSymbols.TokenNameCOMMENT_JAVADOC
                    || nextToken == ITerminalSymbols.TokenNameCOMMENT_LINE) {
                if (captureComments) {
                    captureComments = captureTrailingComment(nextToken);
                }
                adjacentCommentBlock = captureTrailingBlock(adjacentCommentBlock, previousCommentEndExclusive);
                captureLeadingComment(nextToken, previousCommentEndExclusive);
                if (nextToken == ITerminalSymbols.TokenNameCOMMENT_JAVADOC
                        && scanner.getCurrentTokenStartPosition() >= trailingCommentEndExclusive) {
                    adjacentCommentBlock = false;
                }
                previousCommentEndExclusive = findCommentEndExclusive();
                scannedCommentLength += computeCommentLength(nextToken);
                nextToken = scanner.getNextToken();
            }
            captureTrailingBlock(adjacentCommentBlock, previousCommentEndExclusive);
            if (leadingCommentStart < 0
                    || countLineSeparators(previousCommentEndExclusive, scanner.getCurrentTokenStartPosition(), srcCode)
                            >= BLANK_LINE_SEPARATOR_COUNT) {
                leadingCommentStart = scanner.getCurrentTokenStartPosition();
            }
            return nextToken;
        }

        @NonNull
        private String readTokenText() {
            String text = new String(scanner.getCurrentTokenSource());
            // Text block indentation changes when its declaration moves or a formatter adjusts the margin.
            return token == ITerminalSymbols.TokenNameTextBlock
                    ? "\"\"\"" + text.substring(3, text.length() - 3).stripIndent() + "\"\"\""
                    : text;
        }
    }
}

// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.countLineSeparators;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findFragmentStart;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findIndentationEnd;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findLineContentEndExclusive;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findLineSeparatorStart;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findLineStart;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationGapLayout;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationSrcGap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.core.util.PublicScanner;
import org.jspecify.annotations.Nullable;

/**
 * Finds adjacent annotation fragments using the Java lexer already supplied by Spoon.
 *
 * <p>Spoon 11.5.0 annotation traversal misses array-dimension and receiver annotations in the language-constructs
 * fixture. Scan source tokens independently of AST annotation positions to retain those groups, exact argument text,
 * and comment ownership. Reconsider AST-based discovery only after the source-range and E2E contracts are preserved.
 */
@UtilityClass
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
            return new Parsing(srcCode).parseAnnotationSrcGroups();
        } catch (InvalidInputException exception) {
            throw new IllegalArgumentException("Cannot scan annotation source fragments", exception);
        }
    }

    @NonNull
    private static AnnotationSrcGap prepareAnnotationSrcGap(
            String srcCode, AnnotationSrcFragment annotation, int start, int endExclusive) {
        int contentStart = findFragmentStart(start, endExclusive - 1, srcCode);
        int lineStart = findLineStart(contentStart, srcCode);
        String indentation = srcCode.substring(lineStart, findIndentationEnd(lineStart, contentStart, srcCode));
        String leadingWhitespace = srcCode.substring(start, contentStart);
        boolean movesBlankLinesWithComment = annotation.requiresTrailingBlankLine() && contentStart == endExclusive;
        // A lower comment block takes its blank lines when moved. Prepare the remaining separator now so sorting
        // only selects whitespace prefixes; independent comments remain a shared component of every placement.
        AnnotationGapLayout layout = new AnnotationGapLayout(
                srcCode.substring(contentStart, endExclusive),
                indentation,
                leadingWhitespace,
                movesBlankLinesWithComment
                        ? srcCode.substring(findLineSeparatorStart(lineStart, srcCode), lineStart) + indentation
                        : leadingWhitespace,
                movesBlankLinesWithComment ? 0 : countLineSeparators(start, contentStart, srcCode));
        return new AnnotationSrcGap(endExclusive, layout, leadingWhitespace, start);
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
                    trailingLineSeparatorEndExclusive = findLineStart(scanner.getCurrentTokenStartPosition(), srcCode);
                }
                return false;
            }
            return adjacent;
        }

        private boolean captureTrailingComment(int commentToken) {
            if (countLineSeparators(trailingCommentEndExclusive, scanner.getCurrentTokenStartPosition(), srcCode) > 0) {
                return false;
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
            return findLineContentEndExclusive(
                    scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenEndPosition() + 1, srcCode);
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
                String argumentList = parseAnnotationArguments(start);
                declarationLength += argumentList.length();
                arguments = argumentList.substring(1, argumentList.length() - 1);
                declarationCommentEnd = scannedCommentLength;
                token = readNextToken();
            }
            // Lookahead can cross a comment outside the declaration; only count comments inside its source bounds.
            declarationLength += declarationCommentEnd - declarationCommentStart;
            return AnnotationSrcFragment.builder()
                    .annotationStart(start)
                    .descriptor(new AnnotationDescriptor(arguments, declarationLength, name))
                    .endExclusive(trailingCommentEndExclusive)
                    .srcCode(srcCode.substring(commentStart, trailingCommentEndExclusive))
                    .start(commentStart)
                    // A Unicode-escaped terminator is already part of the fragment; no physical separator is needed.
                    .trailingLineSeparatorCount(
                            trailingLineSeparatorEndExclusive == trailingCommentEndExclusive
                                    ? 0
                                    : trailingLineSeparatorCount)
                    .trailingLineSeparators(
                            srcCode.substring(trailingCommentEndExclusive, trailingLineSeparatorEndExclusive))
                    .build();
        }

        @NonNull
        private String parseAnnotationArguments(int start) throws InvalidInputException {
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
        // Each group retains its lists; reusing loop allocations would change previously parsed groups.
        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
        private List<AnnotationSrcGroup> parseAnnotationSrcGroups() throws InvalidInputException {
            List<AnnotationSrcGroup> groups = new ArrayList<>();
            List<AnnotationSrcFragment> annotationsInSrcOrder = new ArrayList<>();
            List<AnnotationSrcGap> gapsInSrcOrder = new ArrayList<>();
            int groupStart = 0;
            int groupEndExclusive = 0;
            int gapStart = 0;
            while (token != ITerminalSymbols.TokenNameEOF) {
                if (annotationsInSrcOrder.isEmpty()) {
                    groupStart = leadingCommentStart;
                }
                int fragmentStart = leadingCommentStart;
                AnnotationSrcFragment annotation = token == ITerminalSymbols.TokenNameAT ? parseAnnotation() : null;
                if (annotation != null) {
                    if (!annotationsInSrcOrder.isEmpty()) {
                        gapsInSrcOrder.add(prepareAnnotationSrcGap(
                                srcCode,
                                annotationsInSrcOrder.get(annotationsInSrcOrder.size() - 1),
                                gapStart,
                                fragmentStart));
                    }
                    annotationsInSrcOrder.add(annotation);
                    gapStart = trailingCommentEndExclusive;
                    groupEndExclusive = scanner.getCurrentTokenStartPosition();
                    continue;
                }
                if (!annotationsInSrcOrder.isEmpty()) {
                    // @interface is a declaration, not another fragment. Its leading comments stay in the final gap.
                    gapsInSrcOrder.add(prepareAnnotationSrcGap(
                            srcCode,
                            annotationsInSrcOrder.get(annotationsInSrcOrder.size() - 1),
                            gapStart,
                            groupEndExclusive));
                    // Groups retain views of these lists. Start fresh buffers instead of mutating handed-off state.
                    groups.add(new AnnotationSrcGroup(
                            annotationsInSrcOrder, gapsInSrcOrder, groupStart, groupEndExclusive));
                    annotationsInSrcOrder = new ArrayList<>();
                    gapsInSrcOrder = new ArrayList<>();
                }
                token = readNextToken();
            }
            if (!annotationsInSrcOrder.isEmpty()) {
                gapsInSrcOrder.add(prepareAnnotationSrcGap(
                        srcCode,
                        annotationsInSrcOrder.get(annotationsInSrcOrder.size() - 1),
                        gapStart,
                        groupEndExclusive));
                groups.add(
                        new AnnotationSrcGroup(annotationsInSrcOrder, gapsInSrcOrder, groupStart, groupEndExclusive));
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

// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation;

import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.countLineSeparators;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findFirstNonWhitespace;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findIndentationEnd;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findLineStart;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findPrecedingLineSeparatorStart;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findTrailingLineSeparatorsStart;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationGapLayout;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.translator.spoon.annotation.AnnotationSrcGroup.AnnotationSrcGap;
import io.github.lemon_ant.jharmonizer.core.utilities.JavaTokenSourceUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.core.util.PublicScanner;
import org.jspecify.annotations.Nullable;
import spoon.reflect.declaration.CtCompilationUnit;

/**
 * Uses the JDT lexer to capture annotation groups, exact source text, and comment ownership.
 * Spoon 11.5.0 can omit annotation uses on array dimensions and receiver parameters from AST traversal.
 * Lexical discovery includes these uses, while available Spoon bindings help resolve their type names.
 */
@UtilityClass
// Lexer APIs and prepared fragment types belong to this parser, which shares one token cursor.
// TODO Remove this suppression
@SuppressWarnings("PMD.ExcessiveImports")
public class AnnotationSourceScanner {

    /** Two line separators leave the blank line that separates comment blocks. */
    private static final int BLANK_LINE_SEPARATOR_COUNT = 2;

    // Matches candidates for an escaped '@'. JDT checks escape eligibility and lexical context.
    @NonNull
    private static final Pattern UNICODE_AT_PATTERN = Pattern.compile("\\\\u+0040");

    /**
     * Groups adjacent annotation uses, including singletons. Whitespace and comments can occur between uses in
     * the same group. Each outer use contributes a fragment, and nested uses contribute keys to that fragment.
     * @param srcCode original Java source
     * @param compilationUnit type-resolution context with any available original annotation positions
     * @return read-only groups in source order with their source text, gap layouts, and ordering keys
     */
    @NonNull
    public static List<AnnotationSrcGroup> scan(@NonNull String srcCode, @NonNull CtCompilationUnit compilationUnit) {
        if (srcCode.indexOf('@') < 0
                && (srcCode.indexOf('\\') < 0
                        || !UNICODE_AT_PATTERN.matcher(srcCode).find())) {
            return List.of();
        }
        try {
            return new Parsing(srcCode, compilationUnit).parseAnnotationSrcGroups();
        } catch (InvalidInputException exception) {
            throw new IllegalArgumentException("Cannot scan Java annotation source with the JDT lexer", exception);
        }
    }

    @NonNull
    private static AnnotationSrcGap prepareAnnotationSrcGap(
            String srcCode, AnnotationSrcFragment precedingAnnotationSrcFragment, int gapEndExclusive) {
        int start = precedingAnnotationSrcFragment.getEndExclusive();
        int contentStart = findFirstNonWhitespace(start, gapEndExclusive, srcCode);
        int lineStart = findLineStart(contentStart, srcCode);
        // On a line with several annotations, indentation ends before the first annotation.
        String indentation = srcCode.substring(lineStart, findIndentationEnd(lineStart, contentStart, srcCode));
        String leadingWhitespace = srcCode.substring(start, contentStart);
        boolean movesBlankLinesWithComment = precedingAnnotationSrcFragment.requiresBlankLineAfterTrailingComments()
                && contentStart == gapEndExclusive;
        // In @A\n// details\n\n@B, the blank line belongs to the comment moving with A.
        // The vacated gap keeps one line break unless it contains fixed comment text.
        String relocatedLeadingWhitespace = movesBlankLinesWithComment
                ? srcCode.substring(findPrecedingLineSeparatorStart(lineStart, srcCode), lineStart) + indentation
                : leadingWhitespace;
        AnnotationGapLayout requiredSeparatorsLayout = new AnnotationGapLayout(true, indentation);
        int retainedLineSeparatorCount =
                movesBlankLinesWithComment ? 0 : countLineSeparators(start, contentStart, srcCode);
        AnnotationGapLayout retainedLayout = new AnnotationGapLayout(false, relocatedLeadingWhitespace);
        // Share prefixes across the three separator requirements to keep preparation linear in the group size.
        List<AnnotationGapLayout> movedLayoutsByRequiredSeparatorCount = IntStream.rangeClosed(
                        0, BLANK_LINE_SEPARATOR_COUNT)
                .mapToObj(requiredSeparatorCount ->
                        requiredSeparatorCount > retainedLineSeparatorCount ? requiredSeparatorsLayout : retainedLayout)
                .toList();
        String content = srcCode.substring(contentStart, gapEndExclusive);
        return new AnnotationSrcGap(
                content, gapEndExclusive, movedLayoutsByRequiredSeparatorCount, leadingWhitespace + content, start);
    }

    /** Argument tokens and nested dependency keys for one annotation. */
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private static class ParsedAnnotationArguments {

        /** Tokens inside the outer parentheses, with comments and inter-token whitespace excluded. */
        @NonNull
        String arguments;

        /** Saved {@link Parsing#cumulativeTokenSrcLength} through the closing parenthesis. */
        int cumulativeTokenSrcLengthAtEnd;

        /** Immutable keys of visible nested annotation types, collected at every argument depth. */
        @NonNull
        Set<String> nestedOrderPreservationGroupKeys;
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private static class ParsedAnnotationTypeName {

        /** Saved {@link Parsing#cumulativeTokenSrcLength} through the name's final identifier. */
        int cumulativeTokenSrcLengthAtEnd;

        /**
         * Name segments after Unicode translation, retaining the written qualification and excluding comments.
         */
        @NonNull
        String writtenTypeName;
    }

    /**
     * Maintains one non-comment lookahead. {@link #readNextToken()} advances the lexer and assigns comments
     * between the preceding token and the new lookahead.
     */
    private static final class Parsing {

        @NonNull
        private final AnnotationDependencyKeyResolver annotationDependencyKeyResolver;

        @NonNull
        private final IScanner scanner;

        @NonNull
        private final String srcCode;

        /**
         * Measured UTF-16 length of tokens and comments through the current lookahead. Snapshot differences
         * give declaration lengths without rescanning and exclude inter-token whitespace and outside comments.
         * For example, {@code @Alpha ( 123 )} measures 11 from its five token spellings.
         */
        private int cumulativeTokenSrcLength;

        /** Current non-comment lookahead, updated only by {@link #readNextToken()}. */
        private int currentToken;

        /** Attached leading-comment candidate found while reading the current token. */
        private int leadingCommentStart;

        /** End of comments attached to the preceding token, copied into its annotation fragment. */
        private int trailingCommentEndExclusive;

        /** Separator requirement determined while collecting the preceding token's trailing comments. */
        private int trailingLineSeparatorCount;

        /** Input boundary of the separator that may need to move with those comments. */
        private int trailingLineSeparatorEndExclusive;

        private Parsing(String srcCode, CtCompilationUnit compilationUnit) throws InvalidInputException {
            this.srcCode = srcCode;
            annotationDependencyKeyResolver = new AnnotationDependencyKeyResolver(compilationUnit);
            // PublicScanner supplies JDT tokens without the Eclipse platform classes required by ToolFactory.
            scanner = new PublicScanner(
                    true,
                    false,
                    false,
                    // Source and compliance levels match SpoonParser.
                    ClassFileConstants.JDK21,
                    ClassFileConstants.JDK21,
                    null,
                    null,
                    true,
                    false,
                    false);
            scanner.setSource(srcCode.toCharArray());
            readNextToken();
            // Comments before the first Java token belong to the file/declaration preamble.
            leadingCommentStart = scanner.getCurrentTokenStartPosition();
        }

        /**
         * Tracks the start of a leading comment block for the next token. Earlier trailing comments and declaration
         * Javadoc stay outside this block. A blank line between comments starts a new block.
         */
        private void captureLeadingComment(int commentToken, int previousCommentEndExclusive) {
            if (commentToken == ITerminalSymbols.TokenNameCOMMENT_JAVADOC) {
                leadingCommentStart = -1;
            } else if (scanner.getCurrentTokenStartPosition() >= trailingCommentEndExclusive
                    && (leadingCommentStart < 0
                            || countLineSeparators(
                                            previousCommentEndExclusive,
                                            scanner.getCurrentTokenStartPosition(),
                                            srcCode)
                                    >= BLANK_LINE_SEPARATOR_COUNT)) {
                leadingCommentStart = scanner.getCurrentTokenStartPosition();
            }
        }

        /**
         * Attaches a comment that starts on the same physical line as the preceding token or attached comment.
         * @return whether another comment may continue the same-line chain
         */
        private boolean captureSameLineComment(int commentToken) {
            if (countLineSeparators(trailingCommentEndExclusive, scanner.getCurrentTokenStartPosition(), srcCode) > 0) {
                return false;
            }
            trailingCommentEndExclusive = scanner.getCurrentTokenEndPosition() + 1;
            trailingLineSeparatorEndExclusive = trailingCommentEndExclusive;
            if (commentToken != ITerminalSymbols.TokenNameCOMMENT_LINE) {
                return true;
            }
            // Store a physical line-comment terminator separately so the destination gap can supply it.
            trailingCommentEndExclusive = findTrailingLineSeparatorsStart(
                    scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenEndPosition() + 1, srcCode);
            trailingLineSeparatorCount = 1;
            return false;
        }

        /**
         * Attaches a comment block immediately below the preceding token when a blank line follows the block.
         * @return whether further comments can still belong to the block adjacent to the preceding token
         */
        private boolean captureTrailingBlock(
                boolean commentBlockAdjacentToPrecedingToken, int previousCommentEndExclusive) {
            if (commentBlockAdjacentToPrecedingToken
                    && countLineSeparators(previousCommentEndExclusive, scanner.getCurrentTokenStartPosition(), srcCode)
                            >= BLANK_LINE_SEPARATOR_COUNT) {
                if (previousCommentEndExclusive > trailingCommentEndExclusive) {
                    trailingCommentEndExclusive = previousCommentEndExclusive;
                    trailingLineSeparatorCount = BLANK_LINE_SEPARATOR_COUNT;
                    trailingLineSeparatorEndExclusive = findLineStart(scanner.getCurrentTokenStartPosition(), srcCode);
                }
                return false;
            }
            return commentBlockAdjacentToPrecedingToken;
        }

        /**
         * Reads arguments through the matching closing parenthesis, leaving the next lookahead current.
         * With no opening parenthesis, returns {@code null} without advancing.
         */
        @Nullable
        private ParsedAnnotationArguments parseAnnotationArguments() throws InvalidInputException {
            if (currentToken != ITerminalSymbols.TokenNameLPAREN) {
                return null;
            }
            int depth = 0;
            StringBuilder arguments = new StringBuilder();
            Set<String> nestedOrderPreservationGroupKeys = new HashSet<>();
            while (true) {
                if (currentToken == ITerminalSymbols.TokenNameEOF) {
                    throw new IllegalArgumentException("Unclosed annotation arguments at source offset "
                            + scanner.getCurrentTokenStartPosition()
                            + " during JDT annotation scanning");
                }
                if (currentToken == ITerminalSymbols.TokenNameAT) {
                    int nestedAnnotationStart = scanner.getCurrentTokenStartPosition();
                    arguments.append(readCurrentTokenText());
                    readNextToken();
                    ParsedAnnotationTypeName nestedTypeName = parseAnnotationTypeName();
                    arguments.append(nestedTypeName.getWrittenTypeName());
                    // Nested values can link an explicit container to direct uses even without library metadata.
                    nestedOrderPreservationGroupKeys.add(
                            annotationDependencyKeyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(
                                    nestedAnnotationStart, nestedTypeName.getWrittenTypeName()));
                    continue;
                }
                arguments.append(readCurrentTokenText());
                if (currentToken == ITerminalSymbols.TokenNameLPAREN) {
                    depth++;
                } else if (currentToken == ITerminalSymbols.TokenNameRPAREN) {
                    depth--;
                }
                if (depth == 0) {
                    int tokenSrcLengthThroughArguments = cumulativeTokenSrcLength;
                    String argumentTokens = arguments.substring(1, arguments.length() - 1);
                    readNextToken();
                    return new ParsedAnnotationArguments(
                            argumentTokens,
                            tokenSrcLengthThroughArguments,
                            Collections.unmodifiableSet(nestedOrderPreservationGroupKeys));
                }
                readNextToken();
            }
        }

        /**
         * Reads one annotation use and leaves the next lookahead current. An ordinary token gives {@code null}
         * without advancing. An {@code @} followed by a non-identifier, as in {@code @interface}, gives
         * {@code null} with the token after {@code @} current.
         */
        @Nullable
        @SuppressWarnings("deprecation")
        private AnnotationSrcFragment parseAnnotationSrcFragment() throws InvalidInputException {
            if (currentToken != ITerminalSymbols.TokenNameAT) {
                return null;
            }
            int start = scanner.getCurrentTokenStartPosition();
            int commentStart = leadingCommentStart;
            // Subtract the original spelling of '@', which may be a Unicode escape.
            int tokenSrcLengthBeforeAnnotation =
                    cumulativeTokenSrcLength - (scanner.getCurrentTokenEndPosition() + 1 - start);
            readNextToken();
            if (currentToken != ITerminalSymbols.TokenNameIdentifier) {
                return null;
            }
            ParsedAnnotationTypeName annotationTypeName = parseAnnotationTypeName();
            String writtenTypeName = annotationTypeName.getWrittenTypeName();
            String simpleName = writtenTypeName.substring(writtenTypeName.lastIndexOf('.') + 1);
            int tokenSrcLengthThroughAnnotation = annotationTypeName.getCumulativeTokenSrcLengthAtEnd();
            Set<String> orderPreservationGroupKeys = new HashSet<>();
            String orderPreservationGroupKey =
                    annotationDependencyKeyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(
                            start, writtenTypeName);
            orderPreservationGroupKeys.add(orderPreservationGroupKey);
            String annotationArguments = null;
            ParsedAnnotationArguments parsedAnnotationArguments = parseAnnotationArguments();
            if (parsedAnnotationArguments != null) {
                annotationArguments = parsedAnnotationArguments.getArguments();
                tokenSrcLengthThroughAnnotation = parsedAnnotationArguments.getCumulativeTokenSrcLengthAtEnd();
                orderPreservationGroupKeys.addAll(parsedAnnotationArguments.getNestedOrderPreservationGroupKeys());
            }
            int declarationLength = tokenSrcLengthThroughAnnotation - tokenSrcLengthBeforeAnnotation;
            return AnnotationSrcFragment.builder()
                    .annotationStart(start)
                    .descriptor(new AnnotationDescriptor(
                            annotationArguments, declarationLength, simpleName, orderPreservationGroupKeys))
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

        /**
         * Reads the rest of the group after its first fragment. The final gap ends at the token that
         * terminated the group, or at the opening {@code @} of a following annotation type declaration.
         */
        @NonNull
        private AnnotationSrcGroup parseAnnotationSrcGroup(AnnotationSrcFragment firstAnnotationSrcFragment)
                throws InvalidInputException {
            List<AnnotationSrcFragment> annotationSrcFragmentsInSrcOrder = new ArrayList<>();
            List<AnnotationSrcGap> annotationSrcGapsInSrcOrder = new ArrayList<>();
            AnnotationSrcFragment precedingAnnotationSrcFragment = firstAnnotationSrcFragment;
            int groupEndExclusive;
            do {
                annotationSrcFragmentsInSrcOrder.add(precedingAnnotationSrcFragment);
                groupEndExclusive = scanner.getCurrentTokenStartPosition();
                AnnotationSrcFragment followingAnnotationSrcFragment = parseAnnotationSrcFragment();
                // The next fragment starts at its attached leading comments, when present.
                int gapEndExclusive = followingAnnotationSrcFragment == null
                        ? groupEndExclusive
                        : followingAnnotationSrcFragment.getStart();
                AnnotationSrcGap annotationSrcGap =
                        prepareAnnotationSrcGap(srcCode, precedingAnnotationSrcFragment, gapEndExclusive);
                annotationSrcGapsInSrcOrder.add(annotationSrcGap);
                precedingAnnotationSrcFragment = followingAnnotationSrcFragment;
            } while (precedingAnnotationSrcFragment != null);
            List<Set<String>> groupKeysPerAnnotationInSrcOrder = annotationSrcFragmentsInSrcOrder.stream()
                    .map(annotationSrcFragment ->
                            annotationSrcFragment.getDescriptor().getOrderPreservationGroupKeys())
                    .toList();
            List<Set<String>> conservativeKeysPerAnnotationInSrcOrder =
                    annotationDependencyKeyResolver.collectConservativeOrderPreservationGroupKeys(
                            groupKeysPerAnnotationInSrcOrder);
            // Finalize alias keys while the parsing buffer is still mutable, before handing it to the group.
            for (int annotationIndex = 0;
                    annotationIndex < annotationSrcFragmentsInSrcOrder.size();
                    annotationIndex++) {
                AnnotationSrcFragment annotationSrcFragment = annotationSrcFragmentsInSrcOrder.get(annotationIndex);
                Set<String> conservativeGroupKeys = conservativeKeysPerAnnotationInSrcOrder.get(annotationIndex);
                AnnotationDescriptor annotationDescriptor = annotationSrcFragment.getDescriptor();
                if (conservativeGroupKeys.equals(annotationDescriptor.getOrderPreservationGroupKeys())) {
                    continue;
                }
                // A changed key set requires a new immutable fragment and descriptor.
                @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
                AnnotationSrcFragment annotationSrcFragmentWithConservativeGroupKeys = AnnotationSrcFragment.builder()
                        .annotationStart(annotationSrcFragment.getAnnotationStart())
                        .descriptor(new AnnotationDescriptor(
                                annotationDescriptor.getArguments(),
                                annotationDescriptor.getDeclarationLength(),
                                annotationDescriptor.getName(),
                                conservativeGroupKeys))
                        .endExclusive(annotationSrcFragment.getEndExclusive())
                        .srcCode(annotationSrcFragment.getSrcCode())
                        .start(annotationSrcFragment.getStart())
                        .trailingLineSeparatorCount(annotationSrcFragment.getTrailingLineSeparatorCount())
                        .trailingLineSeparators(annotationSrcFragment.getTrailingLineSeparators())
                        .build();
                annotationSrcFragmentsInSrcOrder.set(annotationIndex, annotationSrcFragmentWithConservativeGroupKeys);
            }
            return new AnnotationSrcGroup(
                    annotationSrcFragmentsInSrcOrder,
                    annotationSrcGapsInSrcOrder,
                    firstAnnotationSrcFragment.getStart(),
                    groupEndExclusive);
        }

        @NonNull
        private List<AnnotationSrcGroup> parseAnnotationSrcGroups() throws InvalidInputException {
            List<AnnotationSrcGroup> annotationSrcGroups = new ArrayList<>();
            while (currentToken != ITerminalSymbols.TokenNameEOF) {
                AnnotationSrcFragment annotationSrcFragment = parseAnnotationSrcFragment();
                if (annotationSrcFragment != null) {
                    AnnotationSrcGroup annotationSrcGroup = parseAnnotationSrcGroup(annotationSrcFragment);
                    annotationSrcGroups.add(annotationSrcGroup);
                    // The group parser has already advanced to its terminating token.
                    continue;
                }
                readNextToken();
            }
            return Collections.unmodifiableList(annotationSrcGroups);
        }

        /**
         * Reads the written name after an already consumed {@code @} and leaves the following lookahead current.
         * Reports a missing identifier at the unexpected token's source offset.
         */
        @NonNull
        @SuppressWarnings("deprecation")
        private ParsedAnnotationTypeName parseAnnotationTypeName() throws InvalidInputException {
            StringBuilder writtenTypeName = new StringBuilder();
            int tokenSrcLengthThroughName;
            while (true) {
                if (currentToken != ITerminalSymbols.TokenNameIdentifier) {
                    throw new IllegalArgumentException("Invalid annotation type name at source offset "
                            + scanner.getCurrentTokenStartPosition()
                            + " during JDT annotation scanning");
                }
                writtenTypeName.append(scanner.getCurrentTokenSource());
                // Snapshot before reading past the identifier to exclude comments after the final segment.
                tokenSrcLengthThroughName = cumulativeTokenSrcLength;
                readNextToken();
                if (currentToken != ITerminalSymbols.TokenNameDOT) {
                    break;
                }
                writtenTypeName.append('.');
                readNextToken();
            }
            return new ParsedAnnotationTypeName(tokenSrcLengthThroughName, writtenTypeName.toString());
        }

        @NonNull
        private String readCurrentTokenText() {
            return currentToken == ITerminalSymbols.TokenNameTextBlock
                    ? JavaTokenSourceUtils.readTextBlockToken(
                            srcCode, scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenEndPosition() + 1)
                    : new String(scanner.getCurrentTokenSource());
        }

        private void readNextToken() throws InvalidInputException {
            trailingCommentEndExclusive = scanner.getCurrentTokenEndPosition() + 1;
            trailingLineSeparatorCount = 0;
            trailingLineSeparatorEndExclusive = trailingCommentEndExclusive;
            leadingCommentStart = -1;
            int previousCommentEndExclusive = trailingCommentEndExclusive;
            boolean commentBlockAdjacentToPrecedingToken = true;
            boolean captureSameLineComments = true;
            int nextToken = scanner.getNextToken();
            while (nextToken == ITerminalSymbols.TokenNameCOMMENT_BLOCK
                    || nextToken == ITerminalSymbols.TokenNameCOMMENT_JAVADOC
                    || nextToken == ITerminalSymbols.TokenNameCOMMENT_LINE) {
                if (captureSameLineComments) {
                    captureSameLineComments = captureSameLineComment(nextToken);
                }
                commentBlockAdjacentToPrecedingToken =
                        captureTrailingBlock(commentBlockAdjacentToPrecedingToken, previousCommentEndExclusive);
                captureLeadingComment(nextToken, previousCommentEndExclusive);
                if (nextToken == ITerminalSymbols.TokenNameCOMMENT_JAVADOC
                        && scanner.getCurrentTokenStartPosition() >= trailingCommentEndExclusive) {
                    // Standalone Javadoc stays with the declaration. Already attached inline comments stay attached.
                    commentBlockAdjacentToPrecedingToken = false;
                }
                previousCommentEndExclusive = findTrailingLineSeparatorsStart(
                        scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenEndPosition() + 1, srcCode);
                cumulativeTokenSrcLength += JavaTokenSourceUtils.measureTokenSrcLength(
                        srcCode,
                        scanner.getCurrentTokenStartPosition(),
                        scanner.getCurrentTokenEndPosition() + 1,
                        nextToken);
                nextToken = scanner.getNextToken();
            }
            captureTrailingBlock(commentBlockAdjacentToPrecedingToken, previousCommentEndExclusive);
            if (leadingCommentStart < 0
                    || countLineSeparators(previousCommentEndExclusive, scanner.getCurrentTokenStartPosition(), srcCode)
                            >= BLANK_LINE_SEPARATOR_COUNT) {
                // A blank line between the candidate block and the next token prevents leading attachment.
                leadingCommentStart = scanner.getCurrentTokenStartPosition();
            }
            currentToken = nextToken;
            cumulativeTokenSrcLength += JavaTokenSourceUtils.measureTokenSrcLength(
                    srcCode,
                    scanner.getCurrentTokenStartPosition(),
                    scanner.getCurrentTokenEndPosition() + 1,
                    currentToken);
        }
    }
}

// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.spoon;

import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.countLineSeparators;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findFirstNonWhitespace;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findIndentationEnd;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findLineStart;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findPrecedingLineSeparatorStart;
import static io.github.lemon_ant.jharmonizer.core.utilities.SrcCodeUtils.findTrailingLineSeparatorsStart;

import io.github.lemon_ant.jharmonizer.core.config.unified.AnnotationDescriptor;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationSrcFragment;
import io.github.lemon_ant.jharmonizer.core.spoon.AnnotationSrcGroup.AnnotationSrcGap;
import io.github.lemon_ant.jharmonizer.core.utilities.JavaTokenSourceUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
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

    // Matches a backslash, one or more 'u' characters, then 0040: backslash-u0040 and backslash-uuu0040,
    // but not backslash-u0041. This only finds candidates; JDT checks escape eligibility and lexical context.
    @NonNull
    private static final Pattern UNICODE_AT_PATTERN = Pattern.compile("\\\\u+0040");

    /**
     * Groups consecutive annotations, with one {@link AnnotationSrcFragment} per annotation, including singleton groups.
     * Whitespace and comments do not end a group; other Java tokens do. Groups follow source adjacency, not AST ownership.
     * Arguments are tokenized for boundaries and sorting keys without evaluating values. Annotation-like text in
     * comments or literals, and annotations nested inside arguments, do not create separate fragments.
     * @param srcCode original Java source
     * @param compilationUnit type-resolution context with any available original annotation positions
     * @return immutable groups in source order with confirmed and conservative preservation keys
     */
    @NonNull
    public static List<AnnotationSrcGroup> scan(@NonNull String srcCode, @NonNull CtCompilationUnit compilationUnit) {
        // An annotation needs a raw '@' or a Unicode escape for it; unrelated escapes need no lexical scan.
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
        // In "    @Alpha @Beta", contentStart points to @Beta, but indentation ends at @Alpha after four spaces.
        // Taking everything from lineStart to contentStart would incorrectly include "@Alpha ".
        String indentation = srcCode.substring(lineStart, findIndentationEnd(lineStart, contentStart, srcCode));
        // For "    @Alpha @Beta", leadingWhitespace is " "; for "    @Alpha\n    @Beta", it is "\n    ".
        // Both have indentation "    ": the prefix retains separators, while indentation is only the line's
        // spaces/tabs.
        String leadingWhitespace = srcCode.substring(start, contentStart);
        boolean movesBlankLinesWithComment = precedingAnnotationSrcFragment.requiresBlankLineAfterTrailingComments()
                && contentStart == gapEndExclusive;
        // In @A\n// A details\n\n@B, the lower comment and blank line move with A; its old gap keeps one line break.
        // Independent comments retain the gap's whitespace; sorting only selects the prepared whitespace prefix.
        return AnnotationSrcGap.builder()
                .content(srcCode.substring(contentStart, gapEndExclusive))
                .indentation(indentation)
                .originalLeadingWhitespace(leadingWhitespace)
                .relocatedLeadingWhitespace(
                        movesBlankLinesWithComment
                                ? srcCode.substring(findPrecedingLineSeparatorStart(lineStart, srcCode), lineStart)
                                        + indentation
                                : leadingWhitespace)
                .retainedLineSeparatorCount(
                        movesBlankLinesWithComment ? 0 : countLineSeparators(start, contentStart, srcCode))
                .start(start)
                .endExclusive(gapEndExclusive)
                .build();
    }

    /** Argument tokens and dependencies captured before reading the token after the closing parenthesis. */
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private static class ParsedAnnotationArguments {

        /** Tokens inside the outer parentheses, with comments and inter-token whitespace excluded. */
        @NonNull
        String arguments;

        /**
         * Saved {@link Parsing#cumulativeTokenSrcLength} through ')', including the name and arguments.
         * For {@code int variable; @Alpha(123) @BehindAlpha}, the total before '@Alpha' is 12;
         * this value is 23, giving declaration length {@code 23 - 12 = 11}.
         * Reading the next '@' increases the running total to 24 without changing this saved value.
         */
        int cumulativeTokenSrcLengthAtEnd;

        /** Immutable keys of visible nested annotation types, collected at every argument depth. */
        @NonNull
        Set<String> nestedOrderPreservationGroupKeys;
    }

    /** Tokenized type-name state shared by outer and nested annotation parsing. */
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter(AccessLevel.PRIVATE)
    private static class ParsedAnnotationTypeName {

        /**
         * Saved {@link Parsing#cumulativeTokenSrcLength} through the name's final identifier, in UTF-16 code units.
         * For an annotation without arguments, subtracting the total before '@' gives its measured source length:
         * <pre>
         * Source: int variable; @Alpha @BehindAlpha
         * Before @Alpha: "int" (3) + "variable" (8) + ";" (1) = 12.
         * Through Alpha: 12 + "@" (1) + "Alpha" (5) = 18 (saved here).
         * Source length of @Alpha: 18 - 12 = 6; its token count is 2.
         * </pre>
         * Reading the next '@' increases the running total to 19; this value stays 18 to exclude the next token.
         * With {@code @Alpha // note\n@BehindAlpha}, this value is 6; reading the comment and next '@' raises
         * the running total to {@code 6 + 7 + 1 = 14}, while the annotation's saved boundary remains 6.
         */
        int cumulativeTokenSrcLengthAtEnd;

        /**
         * Type-name tokens after Unicode translation, e.g. {@code Tag} or {@code pkg.Outer.Tag}, without '@' or comments.
         * Used for type-identity resolution and reconstruction of nested argument tokens; raw length is tracked separately.
         * Outer fragments derive their simple sorting name from the suffix after the last dot.
         */
        @NonNull
        String writtenTypeName;
    }

    /**
     * Uses one non-comment lookahead token. After construction or {@link #readNextToken()}, {@code currentToken}
     * matches the lexer's current token; reading its text or source positions leaves both unchanged.
     * Only readNextToken reads new lexer tokens; its capture helpers inspect intermediate comment tokens before
     * currentToken is updated to the next non-comment token.
     * Parser methods below document the token they require on entry and leave on return.
     */
    private static final class Parsing {

        /** Resolves order-preservation group keys from annotation positions and written type names. */
        @NonNull
        private final AnnotationDependencyKeyResolver annotationDependencyKeyResolver;

        /** JDT lexer providing Java tokens and their original source offsets. */
        @NonNull
        private final IScanner scanner;

        /** Exact input text used to inspect whitespace gaps and preserve comment boundaries. */
        @NonNull
        private final String srcCode;

        /**
         * Sum of measured token/comment source lengths from source start through the current lookahead, in UTF-16
         * code units. For {@code int variable; @Annotation}, the total is 12 before '@', 13 after '@', and 23 after
         * {@code Annotation}; this identifier contributes 10 characters despite being one token.
         *
         * <p>Raw offsets include gaps that the declaration-length criterion excludes:
         * {@code @Alpha ( 123 )} spans 14 source units, but its five tokens measure {@code 1 + 5 + 1 + 3 + 1 = 11}.
         * Spaces inside literals still count; incidental text-block indentation is normalized separately.
         * Summing each lexed token once preserves these rules without rescanning annotation substrings.
         * Saved totals before '@' and at the declaration end exclude any already-read following comments/lookahead.
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
            readNextToken();
            // Comments before the first Java token belong to the file/declaration preamble.
            leadingCommentStart = scanner.getCurrentTokenStartPosition();
        }

        /**
         * Inspects the lexer's current comment without advancing it; currentToken still denotes the previous Java token.
         * Keeps a leading candidate only outside text already assigned to that previous token.
         * In {@code @Alpha\n// Beta details\n@Beta}, the comment starts after Alpha's retained end and can lead Beta;
         * an already captured inline comment starts before the retained end and cannot also lead Beta.
         */
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
                // Only comments outside the preceding token's retained range can lead the next annotation.
                // @Alpha\n// Beta details\n@Beta selects '// Beta details' when no candidate exists yet.
                // @Alpha\n// Alpha details\n\n// Beta details\n@Beta starts a new block after the blank line:
                // replace the candidate with '// Beta details'; '// Alpha details' stays with Alpha.
                leadingCommentStart = scanner.getCurrentTokenStartPosition();
            }
        }

        /**
         * Requires the lexer on a comment and leaves it there. Retains consecutive comments whose starts share
         * a physical line with the end of the preceding token/comment.
         * @return whether another comment may continue this chain; false after a line gap or a line comment
         */
        private boolean captureSameLineComment(int commentToken) {
            if (countLineSeparators(trailingCommentEndExclusive, scanner.getCurrentTokenStartPosition(), srcCode) > 0) {
                // In @Alpha\n// details\n@Beta, this comment needs the separate lower-block/leading ownership rules.
                return false;
            }
            trailingCommentEndExclusive = scanner.getCurrentTokenEndPosition() + 1;
            trailingLineSeparatorEndExclusive = trailingCommentEndExclusive;
            if (commentToken != ITerminalSymbols.TokenNameCOMMENT_LINE) {
                return true;
            }
            // @Long // note\n@Short stores "@Long // note" in the fragment and '\n' as a separate separator.
            // Moving Long between @Left @Right needs that separator before @Right. A destination gap already
            // containing a line break supplies it without emitting a second one.
            trailingCommentEndExclusive = findTrailingLineSeparatorsStart(
                    scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenEndPosition() + 1, srcCode);
            // This is the minimum needed to terminate // before another token: CRLF is one separator, too.
            // Further blank lines remain in the gap rather than increasing this comment's minimum requirement.
            trailingLineSeparatorCount = 1;
            // A line comment consumes its logical line; following comments use the lower-block/leading ownership rules.
            return false;
        }

        /**
         * Inspects the lexer's current comment or following Java token without advancing it.
         * A block directly below the preceding token belongs to it when a blank line follows:
         * {@code @Alpha\n// details\n\n@Beta} moves the details with Alpha;
         * {@code @Alpha\n\n// details\n@Beta} leaves them with Beta because the block starts after a blank line.
         * @return whether further comments can still belong to the block adjacent to the preceding token
         */
        private boolean captureTrailingBlock(
                boolean commentBlockAdjacentToPrecedingToken, int previousCommentEndExclusive) {
            if (commentBlockAdjacentToPrecedingToken
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
            return commentBlockAdjacentToPrecedingToken;
        }

        /**
         * Attempts arguments at the current lookahead. Without '(' returns null and leaves the token current.
         * At '(' reads through its matching ')' and leaves the following non-comment token current:
         * {@code @A(@B(1)) @C} leaves '@' before C; {@code @A() int field;} leaves 'int'.
         * Returns argument tokens without the outer parentheses, a counter snapshot through ')', and nested keys.
         * Capturing the counter before reading the following token excludes comments after the annotation.
         */
        @Nullable
        private ParsedAnnotationArguments parseAnnotationArguments(
                /*TODO Remove it*/ int annotationSrcFragmentStart) throws InvalidInputException {
            if (currentToken != ITerminalSymbols.TokenNameLPAREN) {
                return null;
            }
            int depth = 0;
            StringBuilder arguments = new StringBuilder();
            Set<String> nestedOrderPreservationGroupKeys = new HashSet<>();
            while (true) {
                if (currentToken == ITerminalSymbols.TokenNameEOF) {
                    throw new IllegalArgumentException("Unclosed annotation arguments at source offset "
                            + annotationSrcFragmentStart
                            + " during JDT annotation scanning");
                }
                if (currentToken == ITerminalSymbols.TokenNameAT) {
                    // Parenthesis depth covers nested annotation arguments; parse their names to collect type keys.
                    int nestedAnnotationStart = scanner.getCurrentTokenStartPosition();
                    arguments.append(readCurrentTokenText());
                    readNextToken();
                    ParsedAnnotationTypeName nestedTypeName = parseAnnotationTypeName();
                    arguments.append(nestedTypeName.getWrittenTypeName());
                    // A container and a direct occurrence share a dependency even without library metadata.
                    // A set keeps @Tags({@Tag("a"), @Tag("b")}) from creating a dependency on itself.
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
         * Attempts an annotation at the current lookahead. A token other than '@' returns null and stays current.
         * At '@', consumes the annotation and leaves its first following non-comment token, including EOF:
         * {@code @A @B} leaves '@' before B; {@code @A(1) int field;} leaves 'int'.
         * {@code @interface Local {}} returns null after consuming '@', leaving 'interface'; any other non-identifier
         * after '@' is likewise left as lookahead. A null result therefore does not imply an unchanged position.
         */
        @Nullable
        @SuppressWarnings("deprecation")
        private AnnotationSrcFragment parseAnnotationSrcFragment() throws InvalidInputException {
            if (currentToken != ITerminalSymbols.TokenNameAT) {
                return null;
            }
            int start = scanner.getCurrentTokenStartPosition();
            int commentStart = leadingCommentStart;
            // Remove the already counted '@' to snapshot the counter before this annotation. Its original spelling
            // can be longer than one character, for example backslash-u0040.
            int tokenSrcLengthBeforeAnnotation =
                    cumulativeTokenSrcLength - (scanner.getCurrentTokenEndPosition() + 1 - start);
            readNextToken();
            if (currentToken != ITerminalSymbols.TokenNameIdentifier) {
                return null; // @interface starts an annotation type declaration.
            }
            ParsedAnnotationTypeName annotationTypeName = parseAnnotationTypeName();
            String writtenTypeName = annotationTypeName.getWrittenTypeName();
            // @pkg.Outer.Tag sorts by Tag; translated token text determines identity, while raw ranges determine
            // length.
            String simpleName = writtenTypeName.substring(writtenTypeName.lastIndexOf('.') + 1);
            int tokenSrcLengthThroughAnnotation = annotationTypeName.getCumulativeTokenSrcLengthAtEnd();
            Set<String> orderPreservationGroupKeys = new HashSet<>();
            String orderPreservationGroupKey =
                    annotationDependencyKeyResolver.resolveAnnotationCanonicalTypeNameOrGroupKey(
                            start, writtenTypeName);
            orderPreservationGroupKeys.add(orderPreservationGroupKey);
            String annotationArguments = null;
            ParsedAnnotationArguments parsedAnnotationArguments = parseAnnotationArguments(start);
            if (parsedAnnotationArguments != null) {
                annotationArguments = parsedAnnotationArguments.getArguments();
                tokenSrcLengthThroughAnnotation = parsedAnnotationArguments.getCumulativeTokenSrcLengthAtEnd();
                orderPreservationGroupKeys.addAll(parsedAnnotationArguments.getNestedOrderPreservationGroupKeys());
            }
            // Snapshots exclude following comments/lookahead without losing original Unicode escape spellings.
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
         * Requires an already parsed first fragment and the lookahead it left after its declaration.
         * Parses the remaining adjacent annotations and leaves the terminating token current, including EOF:
         * {@code @A @B int field;} leaves 'int'; {@code @A @interface Local {}} leaves 'interface',
         * but the group's source range ends before that declaration's '@'.
         * Delays each following gap until the next annotation or declaration is identified.
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
                // Save '@' before a possible @interface attempt leaves 'interface' as lookahead.
                groupEndExclusive = scanner.getCurrentTokenStartPosition();
                // @B produces the next fragment; 'int', EOF or @interface produce null and finish the group.
                AnnotationSrcFragment followingAnnotationSrcFragment = parseAnnotationSrcFragment();
                // In @A\n// B details\n@B, A's gap ends before '// B details', which belongs to B's fragment.
                // In @A\n// type details\n@interface Marker {}, the comment stays in A's final gap, ending at '@'.
                int gapEndExclusive = followingAnnotationSrcFragment == null
                        ? groupEndExclusive
                        : followingAnnotationSrcFragment.getStart();
                // One gap per annotation, including the final, possibly empty gap at EOF after the snippet @A @B.
                // Its start follows the preceding fragment's attached comments, e.g. @A /*tail*/ @B.
                AnnotationSrcGap annotationSrcGap =
                        prepareAnnotationSrcGap(srcCode, precedingAnnotationSrcFragment, gapEndExclusive);
                annotationSrcGapsInSrcOrder.add(annotationSrcGap);
                // Repeat with the already parsed B in @A @B. A declaration token, @interface or EOF ends the group.
                precedingAnnotationSrcFragment = followingAnnotationSrcFragment;
            } while (precedingAnnotationSrcFragment != null);
            // One set per outer annotation; nested argument annotations contribute keys to that same set.
            // This list covers only the adjacent fragments collected by this invocation, not the complete file.
            List<Set<String>> groupKeysPerAnnotationInSrcOrder = annotationSrcFragmentsInSrcOrder.stream()
                    .map(annotationSrcFragment ->
                            annotationSrcFragment.getDescriptor().getOrderPreservationGroupKeys())
                    .toList();
            List<Set<String>> conservativeKeysPerAnnotationInSrcOrder =
                    annotationDependencyKeyResolver.collectConservativeOrderPreservationGroupKeys(
                            groupKeysPerAnnotationInSrcOrder);
            // Library Derived extends Base and inherits Base.Tag:
            // @library.Derived.Tag("z") @java.lang.Deprecated @library.Base.Tag("a") int value;
            // Only the complete group exposes both potential aliases; Deprecated remains independent.
            // This method owns the parsing buffer until group handoff; normalize its local entries before returning it.
            for (int annotationIndex = 0;
                    annotationIndex < annotationSrcFragmentsInSrcOrder.size();
                    annotationIndex++) {
                AnnotationSrcFragment annotationSrcFragment = annotationSrcFragmentsInSrcOrder.get(annotationIndex);
                Set<String> conservativeGroupKeys = conservativeKeysPerAnnotationInSrcOrder.get(annotationIndex);
                AnnotationDescriptor annotationDescriptor = annotationSrcFragment.getDescriptor();
                // Keep fragments whose keys did not change. A later @library.Base.Tag in a group containing
                // @library.Derived.Tag can add a shared alias key to both; replace only those affected fragments.
                // Mutating an existing descriptor or its key set would change an already constructed value object.
                if (conservativeGroupKeys.equals(annotationDescriptor.getOrderPreservationGroupKeys())) {
                    continue;
                }
                // Each changed key set needs its own immutable descriptor; unaffected fragments remain shared.
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

        /**
         * Starts at the first lookahead initialized by the constructor and consumes the source through EOF.
         * Collects adjacent annotation groups while skipping ordinary Java tokens and annotation type declarations.
         */
        @NonNull
        private List<AnnotationSrcGroup> parseAnnotationSrcGroups() throws InvalidInputException {
            List<AnnotationSrcGroup> annotationSrcGroups = new ArrayList<>();
            // A comment-only source /* @A */ starts at EOF and produces no group.
            while (currentToken != ITerminalSymbols.TokenNameEOF) {
                // class/int/';' return null and stay current; '@' starts annotation parsing.
                AnnotationSrcFragment annotationSrcFragment = parseAnnotationSrcFragment();
                // @A @B leaves '@' before B; @Tags({@Tag("x")}) int field; leaves 'int'.
                // @interface Local {} returns null with 'interface' current, after consuming its '@'.
                if (annotationSrcFragment != null) {
                    AnnotationSrcGroup annotationSrcGroup = parseAnnotationSrcGroup(annotationSrcFragment);
                    annotationSrcGroups.add(annotationSrcGroup);
                    // The group parser already read every annotation and left its terminating token current.
                    // Reuse 'int' in @A int field; or exit on EOF after an incomplete snippet @A @B.
                    continue;
                }
                // Read the token after the ordinary token or 'interface' left by @interface.
                readNextToken();
            }
            return Collections.unmodifiableList(annotationSrcGroups);
        }

        /**
         * Requires the first identifier after an already consumed '@', for outer and nested annotations.
         * Parses the qualified name and leaves its first following non-comment token:
         * {@code @pkg.Tag(1)} leaves '(', {@code @Tag @Other} leaves '@', and EOF after {@code @Tag} stays current.
         * A missing identifier at entry or after a dot is rejected at the unexpected token's source offset:
         * {@code @Tags({@()})} and {@code @pkg.()} both report '('.
         */
        @NonNull
        // The parsed source fixes the context: identifiers after @ and dots are annotation-name segments.
        @SuppressWarnings("deprecation")
        private ParsedAnnotationTypeName parseAnnotationTypeName() throws InvalidInputException {
            StringBuilder writtenTypeName = new StringBuilder();
            int tokenSrcLengthThroughName;
            while (true) {
                // Valid segments are Tag in @Tag, or pkg, Outer and Tag in @pkg.Outer.Tag.
                // Invalid @pkg.() has '(' after a dot; invalid @Tags({@()}) has '(' after the nested '@'.
                // Reject either missing segment. The outer caller handles @interface before entering this method.
                if (currentToken != ITerminalSymbols.TokenNameIdentifier) {
                    throw new IllegalArgumentException("Invalid annotation type name at source offset "
                            + scanner.getCurrentTokenStartPosition()
                            + " during JDT annotation scanning");
                }
                writtenTypeName.append(scanner.getCurrentTokenSource());
                // Snapshot the cumulative written token length before reading comments/lookahead outside this name.
                // @pkg/*inner*/.Tag /*after*/ int field; counts /*inner*/ inside the name, excluding /*after*/
                // scanned next. Updating this snapshot after each identifier includes comments around earlier dots.
                tokenSrcLengthThroughName = cumulativeTokenSrcLength;
                readNextToken();
                if (currentToken != ITerminalSymbols.TokenNameDOT) {
                    // A non-dot token ends the name: '(' in @Tag("x"), 'int' in @Tag int field;, '@' in @Tag @Other,
                    // or '}' in @Tags({@Marker}). EOF after @Tag ends the name too.
                    // Keep the current lookahead token; reading another one would skip input needed by the caller.
                    break;
                }
                // A dot continues qualification: @pkg.Outer.Tag parses Outer, then Tag, in the next iterations.
                // readNextToken skips comments/whitespace: @pkg /*left*/ . /*right*/ Tag still parses pkg.Tag.
                writtenTypeName.append('.');
                readNextToken();
            }
            // Keep pkg.Outer.Tag for @pkg.Outer.Tag; only outer fragments need its simple-name suffix.
            return new ParsedAnnotationTypeName(tokenSrcLengthThroughName, writtenTypeName.toString());
        }

        /** Reads the current token's normalized text without changing lookahead or comment ownership. */
        @NonNull
        private String readCurrentTokenText() {
            // JDT can return text-block values without delimiters and with interpreted Java escapes. Preserve
            // lexical triple quotes and spellings such as backslash-n, normalizing only incidental indentation.
            return currentToken == ITerminalSymbols.TokenNameTextBlock
                    ? JavaTokenSourceUtils.readTextBlockToken(
                            srcCode, scanner.getCurrentTokenStartPosition(), scanner.getCurrentTokenEndPosition() + 1)
                    : new String(scanner.getCurrentTokenSource());
        }

        /**
         * Reads the next non-comment token, or initializes the first token after setSource.
         * Updates currentToken together with the lexer's position, collecting crossed comments and their ownership.
         * The cumulative written length includes the new lookahead; declaration-end snapshots precede this read.
         * For {@code @A} followed by a same-line block comment and {@code @B}, reading after A leaves '@' before B
         * and records the comment on A.
         * A following call from '@' leaves B, so inspecting the current token must not read another one.
         */
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
                    // In @Alpha\n/** field docs */\n\nint field;, the JavaDoc documents the declaration, so it
                    // cannot become Alpha's lower comment block. An already captured inline JavaDoc stays with Alpha.
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
                // With no candidate, start at the Java token. In @Alpha\n// note\n\n@Beta, the blank line after
                // the comment also prevents it from leading Beta; the preceding-fragment/gap rules retain it instead.
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

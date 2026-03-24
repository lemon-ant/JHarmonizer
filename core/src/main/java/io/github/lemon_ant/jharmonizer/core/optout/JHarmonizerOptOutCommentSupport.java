package io.github.lemon_ant.jharmonizer.core.optout;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.github.lemon_ant.jharmonizer.core.files_handler.SrcFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtComment.CommentType;
import spoon.reflect.cu.SourcePosition;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class JHarmonizerOptOutCommentSupport {

    // Matches exactly two Java comment families in raw source:
    // 1) /\* ... *\/   (including multiline block comments)
    // 2) // ...        (single-line comments up to newline/end-of-file)
    //
    // Pattern breakdown:
    // - (?s) enables DOTALL so ".*?" can cross line breaks inside block comments.
    // - /\*.*?\*/ is a non-greedy block comment matcher (first closing */ wins).
    // - | alternates with single-line comments.
    // - //.*?(?:\R|$) captures line comments and stops at a line-break or EOF.
    //
    // We intentionally do not parse Java syntax here; this lexer-like pattern is only used for
    // file-scope opt-out probing in package-declaration and module-declaration units where Spoon
    // comment attachment is unreliable.
    private static final Pattern COMMENT_PATTERN = Pattern.compile("(?s)/\\*.*?\\*/|//.*?(?:\\R|$)");
    private static final char LINE_FEED = '\n';

    /**
     * Parses a type-level opt-out directive from a Spoon comment.
     *
     * @param comment candidate comment attached to a type
     * @return resolved mode, or {@code null} when the comment does not contain a valid opt-out directive
     */
    @Nullable
    static JHarmonizerOptOutMode parseTypeOptOutMode(@NonNull CtComment comment) {
        String normalizedContent = comment.getContent().trim().toLowerCase(Locale.ROOT);
        int tokenPrefixIndex = normalizedContent.indexOf(JHarmonizerOptOutMode.TOKEN_PREFIX);
        if (tokenPrefixIndex < 0) {
            return null;
        }
        if (comment.getCommentType() == CommentType.JAVADOC) {
            return null;
        }
        if (tokenPrefixIndex != 0) {
            return null;
        }

        try {
            return JHarmonizerOptOutMode.fromToken(normalizedContent);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * Parses a file-scope opt-out directive from a raw comment fragment.
     *
     * @param rawComment raw Java comment text
     * @param commentOffset absolute offset in the source file
     * @param srcFile source file used for warning location formatting
     * @return resolved mode, or {@code null} when the comment does not contain a valid opt-out directive
     */
    @Nullable
    static JHarmonizerOptOutMode parseFileScopeOptOutMode(
            @NonNull String rawComment, int commentOffset, @NonNull SrcFile srcFile) {
        // Normalize raw text by stripping Java comment delimiters, trimming incidental whitespace,
        // and switching to lowercase for case-insensitive token lookup.
        String normalizedContent = rawComment
                .replaceFirst("^//", "")
                .replaceFirst("^/\\*+", "")
                .replaceFirst("\\*+/$", "")
                .trim()
                .toLowerCase(Locale.ROOT);

        // Locate the directive token in normalized comment content.
        int tokenPrefixIndex = normalizedContent.indexOf(JHarmonizerOptOutMode.TOKEN_PREFIX);

        // Fast-exit for regular comments without any JHarmonizer directive token.
        if (tokenPrefixIndex < 0) {
            return null;
        }

        // Keep parity with AST parsing rules: Javadoc comments are never treated as opt-out directives.
        if (rawComment.startsWith("/**")) {
            logIgnoredFileOptOut(commentOffset, "Javadoc opt-out comments are ignored", srcFile);
            return null;
        }

        // Directive token must start at the beginning of comment payload after normalization.
        if (tokenPrefixIndex != 0) {
            logIgnoredFileOptOut(commentOffset, "Malformed opt-out comment is ignored", srcFile);
            return null;
        }

        // Delegate token-to-mode conversion to canonical enum parser and log unsupported tokens.
        try {
            return JHarmonizerOptOutMode.fromToken(normalizedContent);
        } catch (IllegalArgumentException exception) {
            logIgnoredFileOptOut(commentOffset, exception.getMessage(), srcFile);
            return null;
        }
    }

    /**
     * Collects raw Java comments from source code in encounter order.
     *
     * @param srcCode source text to scan
     * @return immutable list of raw matches with source offsets
     */
    @NonNull
    static List<RawCommentMatch> collectRawCommentsByRegex(@NonNull String srcCode) {
        Matcher commentMatcher = COMMENT_PATTERN.matcher(srcCode);
        List<RawCommentMatch> matches = new ArrayList<>();
        while (commentMatcher.find()) {
            matches.add(RawCommentMatch.of(commentMatcher.group(), commentMatcher.start()));
        }
        return Collections.unmodifiableList(matches);
    }

    /**
     * Formats a source position to path:line:column.
     *
     * @param srcFile source file that owns the position
     * @param sourcePosition Spoon source position
     * @return formatted location string
     */
    @NonNull
    static String formatLocation(@NonNull SrcFile srcFile, @NonNull SourcePosition sourcePosition) {
        return srcFile.getPath() + ":" + sourcePosition.getLine() + ":" + sourcePosition.getColumn();
    }

    /**
     * Formats an absolute character offset to path:line:column.
     *
     * @param srcFile source file that owns the offset
     * @param sourceOffset absolute source offset
     * @return formatted location string
     */
    @NonNull
    static String formatLocation(@NonNull SrcFile srcFile, int sourceOffset) {
        int line = 1;
        int column = 1;
        String srcCode = srcFile.getSrcCode();
        for (int index = 0; index < sourceOffset && index < srcCode.length(); index++) {
            if (srcCode.charAt(index) == LINE_FEED) {
                line++;
                column = 1;
                continue;
            }
            column++;
        }
        return srcFile.getPath() + ":" + line + ":" + column;
    }

    /**
     * Logs that a type-level directive was ignored.
     *
     * @param comment comment that contained the ignored directive
     * @param message ignore reason
     * @param srcFile source file used for location formatting
     */
    static void logIgnoredTypeOptOut(@NonNull CtComment comment, @NonNull String message, @NonNull SrcFile srcFile) {
        if (log.isWarnEnabled()) {
            log.warn("{} at {}", message, formatLocation(srcFile, comment.getPosition()));
        }
    }

    /**
     * Logs that a file-scope directive was ignored.
     *
     * @param commentOffset absolute offset of ignored directive
     * @param message ignore reason
     * @param srcFile source file used for location formatting
     */
    static void logIgnoredFileOptOut(int commentOffset, @NonNull String message, @NonNull SrcFile srcFile) {
        if (log.isWarnEnabled()) {
            log.warn("{} at {}", message, formatLocation(srcFile, commentOffset));
        }
    }

    /**
     * Logs that a resolved file-scope directive candidate was ignored.
     *
     * @param location formatted location
     * @param message ignore reason
     */
    static void logIgnoredFileOptOutAtLocation(@NonNull String location, @NonNull String message) {
        if (log.isWarnEnabled()) {
            log.warn("{} at {}", message, location);
        }
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE, staticName = "of")
    static class RawCommentMatch {
        @NonNull
        private final String rawComment;

        private final int commentOffset;
    }
}

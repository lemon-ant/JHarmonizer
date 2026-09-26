// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtComment.CommentType;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/** Inspects explicit declarations, source boundaries and attached comments. */
@UtilityClass
class SpoonTypeMemberUtils {

    /**
     * Returns the source end of the last trailing comment attached by Spoon to this member,
     * or the member's own source end when no such comment exists.
     * This prevents trailing comments from being cut off when there is no next member.
     *
     * @param member the type member to inspect
     * @return the inclusive source index of the effective end of this member
     */
    static int findEffectiveMemberEnd(@NonNull CtTypeMember member) {
        int memberEnd = member.getPosition().getSourceEnd();
        return member.getComments().stream()
                .filter(comment -> comment.getPosition().isValidPosition())
                .filter(comment -> comment.getPosition().getSourceStart() > memberEnd)
                .mapToInt(comment -> comment.getPosition().getSourceEnd())
                .max()
                .orElse(memberEnd);
    }

    /**
     * Finds the earliest source start of a declaration and its annotations.
     * @param member the declaration to inspect
     * @return first source offset including annotations outside Spoon's declaration range
     */
    static int findEffectiveMemberStart(@NonNull CtTypeMember member) {
        // Spoon can start a declaration at a comment between its annotations, excluding earlier annotations.
        return member.getAnnotations().stream()
                .map(CtAnnotation::getPosition)
                .filter(SourcePosition::isValidPosition)
                .mapToInt(SourcePosition::getSourceStart)
                .reduce(member.getPosition().getSourceStart(), Math::min);
    }

    /**
     * Returns the explicit (source-positioned, non-implicit) type members of the given type.
     *
     * @param type the type declaration to inspect
     * @return the list of explicit type members
     */
    @NonNull
    static List<CtTypeMember> findExplicitTypeMembers(@NonNull CtType<?> type) {
        return type.getTypeMembers().stream()
                // Spoon creates implicit constructors which don't exist in the source code
                .filter(typeMember -> typeMember.getPosition().isValidPosition())
                /* TODO(RECORDS_DISABLED): Remove this guard when record headers/components are printed correctly.
                Today implicit record fields/components still produce wrong source-printer output. */
                .filter(typeMember -> !typeMember.isImplicit())
                .toList();
    }

    /**
     * Finds comments adjacent to the first type that Spoon left outside its source range.
     * @param compilationUnit compilation unit containing the original comments
     * @param typeStart first type's original source start
     * @param srcCode original source text
     * @return start of the type's leading comments, excluding a separate file header
     */
    static int findLeadingTypeCommentStart(
            @NonNull CtCompilationUnit compilationUnit, int typeStart, @NonNull String srcCode) {
        List<CtComment> commentsBeforeTypeInReverseSrcOrder = compilationUnit.getComments().stream()
                .filter(comment -> comment.getPosition().isValidPosition())
                .filter(comment -> comment.getPosition().getSourceEnd() < typeStart)
                .sorted(Comparator.comparingInt(
                                (CtComment comment) -> comment.getPosition().getSourceStart())
                        .reversed())
                .toList();
        int firstCommentStart = typeStart;
        int firstJavaDocStart = typeStart;
        for (CtComment comment : commentsBeforeTypeInReverseSrcOrder) {
            SourcePosition commentPosition = comment.getPosition();
            if (!CommentGapPatterns.ADJACENT_COMMENT_GAP
                    .matcher(srcCode)
                    .region(commentPosition.getSourceEnd() + 1, firstCommentStart)
                    .matches()) {
                break;
            }
            firstCommentStart = commentPosition.getSourceStart();
            if (comment.getCommentType() == CommentType.JAVADOC) {
                firstJavaDocStart = firstCommentStart;
            }
        }
        // Without a package/import, Spoon can attach type comments to the compilation unit.
        // The loop collected one adjacent block of any length, with no blank line or source text between its
        // comments and the first type. If only whitespace precedes this block, treat it as reaching the file start.
        // Split such a block at the earliest JavaDoc in source order: earlier comments belong to the file;
        // the JavaDoc and all following comments in the block belong to the type.
        // Without JavaDoc, firstJavaDocStart remains typeStart, so the whole block belongs to the file.
        // If any non-whitespace content precedes the block (e.g. a separate license header), the entire adjacent
        // block belongs to the type, starting at firstCommentStart.
        // The caller keeps text before the returned offset at the file top; comments from that offset move with the
        // type.
        // Examples (\n denotes a line break):
        // "// license\nclass A {}" -> typeStart: the license stays at the top of the file.
        // "// license\n/** A docs */\n// details\nclass A {}" -> firstJavaDocStart: JavaDoc and details move with A.
        // "// license\n\n// A docs\nclass A {}" -> firstCommentStart: the adjacent comment moves with A.
        return srcCode.substring(0, firstCommentStart).isBlank() ? firstJavaDocStart : firstCommentStart;
    }

    /**
     * Detects leading comments, excluding misplaced trailing comments and enclosing-type JavaDoc.
     * @param member the member to inspect
     * @param memberDeclarationEndLines the set of last source lines of declarations in the same type
     * @param typeDeclarationStartLine the enclosing type's declaration line
     * @return {@code true} if the member has a genuine leading comment
     */
    static boolean hasLeadingCommentOnSeparateLine(
            @NonNull CtTypeMember member,
            @NonNull Set<Integer> memberDeclarationEndLines,
            int typeDeclarationStartLine) {
        return member.getComments().stream()
                .filter(comment -> comment.getPosition().isValidPosition())
                // Spoon can attach a sibling's trailing comment or the enclosing type's JavaDoc to this member.
                .filter(comment -> !memberDeclarationEndLines.contains(
                        comment.getPosition().getLine()))
                .filter(comment -> comment.getPosition().getLine() >= typeDeclarationStartLine)
                .anyMatch(comment -> comment.getPosition().getEndLine()
                        < member.getPosition().getLine());
    }

    /**
     * Returns whether the member has a leading comment whose content matches the given group header.
     *
     * @param member      the type member to inspect
     * @param groupHeader the expected group header text (trimmed, without comment delimiters)
     * @return {@code true} if a matching leading comment exists
     */
    static boolean hasMatchingLeadingComment(@NonNull CtTypeMember member, @NonNull String groupHeader) {
        return member.getComments().stream()
                .filter(comment -> comment.getPosition().getEndLine()
                        < member.getPosition().getLine())
                .map(comment -> comment.getContent().trim())
                .anyMatch(groupHeader::equals);
    }

    /** Whitespace rules for associating leading comments with a type. */
    @UtilityClass
    private static final class CommentGapPatterns {

        /**
         * Matches the entire gap between a comment and the following comment or type declaration.
         * Allows spaces, tabs ({@code \t}), and form feeds ({@code \f}) around zero or one line separator:
         * LF ({@code \n}), CR ({@code \r}), or CRLF ({@code \r\n}). Two separators form a blank line,
         * separating an earlier comment block from the type. Any other character also breaks adjacency.
         *
         * <p>Examples use Java string notation:
         * <pre>{@code
         * ""           -> matches: no gap
         * " \t\f"      -> matches: spaces, tabs, and form feeds
         * "\n    "     -> matches: LF followed by indentation
         * " \r\t"      -> matches: one CR with surrounding whitespace
         * " \r\n\t"    -> matches: CRLF counts as one separator
         * "\n\n"       -> does not match: a blank line
         * "\r\n \r\n"  -> does not match: a blank line containing a space
         * " class "    -> does not match: source text in the gap
         * }</pre>
         */
        @NonNull
        private static final Pattern ADJACENT_COMMENT_GAP = Pattern.compile("[ \\t\\f]*(?:\\r\\n|\\r|\\n)?[ \\t\\f]*");
    }
}

// SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
// SPDX-License-Identifier: Apache-2.0
package io.github.lemon_ant.jharmonizer.core.translator.spoon;

import static io.github.lemon_ant.jharmonizer.core.translator.spoon.SpoonSrcPrinterUtils.GROUP_HEADER_METADATA;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;

/**
 * Internal utilities for inspecting Spoon type members and their associated comments.
 * Separates member-inspection concerns from the separator-predicate compilation utilities
 * in {@link SpoonSrcPrinterUtils}.
 */
@UtilityClass
class SpoonTypeMemberUtils {

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

    /**
     * Returns the group-header metadata string attached to the member, or {@code null} if absent.
     *
     * @param member the type member to inspect
     * @return the group header, or {@code null}
     */
    @Nullable
    static String findGroupHeader(@NonNull CtTypeMember member) {
        Object groupHeaderMetadata = member.getMetadata(GROUP_HEADER_METADATA);
        if (groupHeaderMetadata == null) {
            return null;
        }
        return groupHeaderMetadata.toString();
    }

    /**
     * Returns {@code true} when the member has at least one genuine leading comment: a comment
     * whose end line is strictly before the member's own line, whose start line does not
     * coincide with the last source line of any other member declaration, and whose start line
     * is not before the enclosing type's own declaration line.
     *
     * <p>The second filter guards against Spoon's comment misattribution after member reordering.
     * When members are reordered, Spoon sometimes attributes a trailing {@code //} comment from
     * one member to the next element in the original source order. Such a comment always sits on
     * the last line of the member it was originally trailing (its {@code endLine}), so filtering
     * by {@code memberDeclarationEndLines} removes these spurious attributions while leaving
     * genuine leading comments (which occupy their own lines, not the end line of a declaration)
     * intact.
     *
     * <p>The third filter guards against a second Spoon misattribution pattern: when a nested type
     * has no blank line between its opening brace and its first member, Spoon can attribute the
     * enclosing type's own javadoc (which precedes the {@code interface}/{@code class} keyword) to
     * that first inner member instead. Such a comment is guaranteed to start on a line strictly
     * before the enclosing type's declaration start line, so filtering by
     * {@code typeDeclarationStartLine} removes these spurious attributions while leaving genuine
     * leading comments inside the type body (which start on or after the type's first declaration
     * line) intact.
     *
     * @param member the member to inspect
     * @param memberDeclarationEndLines the set of last source lines of declarations in the same type
     * @param typeDeclarationStartLine the first source line of the enclosing type's declaration
     *                                 (e.g. the line of the {@code interface} or {@code class}
     *                                 keyword); comments starting before this line are outside the
     *                                 type body and are filtered out
     * @return {@code true} if the member has a genuine leading comment
     */
    static boolean hasLeadingCommentOnSeparateLine(
            @NonNull CtTypeMember member,
            @NonNull Set<Integer> memberDeclarationEndLines,
            int typeDeclarationStartLine) {
        return member.getComments().stream()
                .filter(comment -> comment.getPosition().isValidPosition())
                .filter(comment -> !memberDeclarationEndLines.contains(
                        comment.getPosition().getLine()))
                .filter(comment -> comment.getPosition().getLine() >= typeDeclarationStartLine)
                .anyMatch(comment -> comment.getPosition().getEndLine()
                        < member.getPosition().getLine());
    }
}
